package org.ggp.base.player.gamer.statemachine;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


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
public final class MonteCarloGamer extends SampleGamer {
	// TODO check to make sure we're not making immediately bad choices that lead us to lose the game
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine m = getStateMachine();
		long start_time = System.currentTimeMillis();
		long finishBy = timeout - 1000;

		List<Move> moves = m.getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(0);
		if (moves.size() > 1)	 selection = monte_carlo(moves, m, finishBy);	// ensures that there's actually a choice to make

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start_time));
		return selection;
	}

	private Move monte_carlo(List<Move> moves, StateMachine m, long finishBy) throws MoveDefinitionException, TransitionDefinitionException {
		int[] total_pts = new int[moves.size()];
		int[] num_visits = new int[moves.size()];

		// Depth charges for each candidate move, and keep track
		// of the total score and total attempts accumulated for each move.
		for (int i = 0; true; i = (i+1) % moves.size()) {
			if (System.currentTimeMillis() > finishBy)  	break;

			int score = depth_charge_score(getCurrentState(), moves.get(i));
			num_visits[i]++;
			total_pts[i] += score;
		}

		// Compute the expected score for each move.
		double[] moves_expected_pts = new double[moves.size()];
		for (int i = 0; i < moves.size(); i++)
			moves_expected_pts[i] = (double)total_pts[i] / num_visits[i];

		// Find move with the best expected score.
		int best_mv_index = 0;
		double best_mv_score = moves_expected_pts[0];
		for (int i = 1; i < moves.size(); i++) {
			if (moves_expected_pts[i] > best_mv_score) {
				best_mv_score = moves_expected_pts[i];
				best_mv_index = i;
			}
		}
		return moves.get(best_mv_index);

	}


	private int[] depth = new int[1];

	int depth_charge_score(MachineState state, Move myMove) {
	    StateMachine m = getStateMachine();
	    try {
            MachineState final_state = m.performDepthCharge(m.getRandomNextState(state, getRole(), myMove), depth);
            return m.getGoal(final_state, getRole());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
	}
}