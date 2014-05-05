package org.ggp.base.player.gamer.statemachine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

class Node {
	boolean visited = false;
	double avg_score = 0;
	Map<Move, Node> possib_next_states;
	int n_children_visited = 0;
//	ArrayList<Node> children;
//	int i;
}

/**
 * MonteCarloGamer uses a pure Monte Carlo approach towards picking moves, doing
 * simulations, & then choosing the move that has the highest expected score.
 *
 * It is currently extremely mediocre... it doesn't even block one-move wins. This
 * is mostly due to the assumption that the opponent plays randomly.
 * @author1 Varun Datta
 * @author2 Leonard Bronner
 * @author3 Devon Zuegel
 */
public final class MonteCarloTreeSearchGamer extends SampleGamer {
	Node curr_state_node = null;

	private Node init_new_node(boolean visited, double avg_score, Map<Move, Node> possib_next_states, int n_children_visited) {
		Node n = new Node();
		n.visited = visited;
		n.avg_score = avg_score;
		n.possib_next_states = possib_next_states;
		n.n_children_visited = n_children_visited;
		return n;
	}

	private Node expand_visited_node(MachineState state, StateMachine machine) throws MoveDefinitionException {
		Node node = new Node();
		node.visited = true;
		node.n_children_visited = 0;
		node.avg_score = 0;

		// initialize possib_next_states to contain moves pointing to "blank" nodes
		List<Move> moves = machine.getLegalMoves(state, getRole());
		Map<Move, Node> possib_next_states = new HashMap<Move, Node>();
		for (int i = 0; i < moves.size(); i++) {
			Node n = init_new_node(false, 0, null, 0);
			possib_next_states.put(moves.get(i), n);
		}
		node.possib_next_states = possib_next_states;

		return node;
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		long start_time = System.currentTimeMillis();
		long finishBy = timeout - 1000;

		List<Move> moves = machine.getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(0);
		if (moves.size() > 1)	selection = monte_carlo_tree_search(moves, getCurrentState(), machine, finishBy);		// checks that there's actually a choice to make

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start_time));
		return selection;
	}

	Move monte_carlo_tree_search(List<Move> moves, MachineState state, StateMachine machine, long finishBy) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (curr_state_node == null)  curr_state_node = expand_visited_node(getCurrentState(), machine);

		double[] avg_scores = new double[moves.size()];

		while (System.currentTimeMillis() > finishBy) {
			for (int i = 0; i < moves.size(); i++) {
				Move mv = moves.get(i); // gets the move at i
				MachineState next_state = machine.getRandomNextState(state);
				Node next_state_node = curr_state_node.possib_next_states.get(mv); // finds the next node in the tree

				avg_scores[i] = update_move_i_avg_score(next_state_node,  next_state,  machine); // given that next node in the tree & the list of states
			}
		}

		// update our position in the tree to reflect that we've made a move choice
		int i = index_of_best_avg_score(avg_scores);
		curr_state_node = curr_state_node.possib_next_states.get(moves.get(i));
		return moves.get(i);
	}

	private int index_of_best_avg_score(double[] avg_scores) {
		int best = 0;
		for (int i = 0; i < avg_scores.length; i++) {
			if (avg_scores[i] > avg_scores[best])	best = i;
		}
		return best;
	}

	private double update_move_i_avg_score(Node node, MachineState state, StateMachine machine) throws GoalDefinitionException, MoveDefinitionException {
		node.visited = true;
		if (one_mv_oppnt_win(state, machine))	return 0.0;

		if (node.possib_next_states == null)  expand_visited_node(state, machine);

		// grab rand_mv from state, perform depth search on it
		int depth_surge_score = depth_charge_score(state, machine.getRandomMove(state, getRole()));

		// update node.avg_score to include the score calculated from the depth surge
		node.avg_score = (node.avg_score*node.n_children_visited + depth_surge_score) / (node.n_children_visited + 1);
		node.n_children_visited++;

		return node.avg_score;
	}

	int depth_charge_score(MachineState state, Move mv) {
		return performDepthChargeFromMove(state, mv);
	}

	// TODO  this function needs to be written to return true if the given move affords the opponent a one-move win
	private boolean one_mv_oppnt_win(MachineState state, StateMachine machine) throws GoalDefinitionException {
		return false;
	}


	private int[] depth = new int[1];

	int performDepthChargeFromMove(MachineState theState, Move myMove) {
	    StateMachine theMachine = getStateMachine();
	    try {
            MachineState finalState = theMachine.performDepthCharge(theMachine.getRandomNextState(theState, getRole(), myMove), depth);
            return theMachine.getGoal(finalState, getRole());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
	}
}