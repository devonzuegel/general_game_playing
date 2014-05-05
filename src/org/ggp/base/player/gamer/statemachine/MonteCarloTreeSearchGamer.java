package org.ggp.base.player.gamer.statemachine;

import java.util.ArrayList;
import java.util.List;

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
	ArrayList<Node> children;
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
//	int node_i = 0;  // currently favoring one side (need to split up by time / monotonic heuristic later)
//	ArrayList<Node> curr_lvl;
//	Node<Node> curr_node;

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO check to make sure we're not making immediately bad choices that lead us to lose the game

		StateMachine machine = getStateMachine();
		long start_time = System.currentTimeMillis();
		long finishBy = timeout - 1000;

		List<Move> moves = machine.getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(0);
		// checks that there's actually a choice to make
		if (moves.size() > 1)	selection = monte_carlo(moves, machine, finishBy);

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start_time));
		return selection;
	}

	Move monte_carlo(List<Move> moves, StateMachine machine, long finishBy) throws MoveDefinitionException, TransitionDefinitionException {
//		double[] moves_avg_score = new double[moves.size()];
//		int[] n_depth_charge_attempts = new int[moves.size()];
//
//		Map<Move, List<MachineState>> moves_to_states = machine.getNextStates(getCurrentState(), getRole());



// -----------------------------------------------------------------------------------------------------------------------------
		int[] moveTotalPoints = new int[moves.size()];
		int[] moveTotalAttempts = new int[moves.size()];

		// Depth charges for each candidate move, and keep track
		// of the total score and total attempts accumulated for each move.
		for (int i = 0; true; i = (i+1) % moves.size()) {
			if (System.currentTimeMillis() > finishBy)  	break;

			int score = performDepthChargeFromMove(getCurrentState(), moves.get(i));
			moveTotalPoints[i] += score;
			moveTotalAttempts[i] += 1;
		}

		// Compute the expected score for each move.
		double[] moveExpectedPoints = new double[moves.size()];
		for (int i = 0; i < moves.size(); i++)
			moveExpectedPoints[i] = (double)moveTotalPoints[i] / moveTotalAttempts[i];

		// Find move with the best expected score.
		int bestMove = 0;
		double bestMoveScore = moveExpectedPoints[0];
		for (int i = 1; i < moves.size(); i++) {
			if (moveExpectedPoints[i] > bestMoveScore) {
				bestMoveScore = moveExpectedPoints[i];
				bestMove = i;
			}
		}
		return moves.get(bestMove);

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