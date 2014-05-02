package org.ggp.base.player.gamer.statemachine;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * SampleLegalGamer is a minimal gamer which always plays the first
 * legal move it identifies, regardless of the state of the game.
 *
 * For your first players, you should extend the class SampleGamer
 * The only function that you are required to override is :
 * public Move stateMachineSelectMove(long timeout)
 *
 */
public final class OurMinimaxGamer extends SampleGamer {
	/**
	 * This function is called at the start of each round
	 * You are required to return the Move your player will play
	 * before the timeout.	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException	{
		// We get the current start time
		long start = System.currentTimeMillis();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move bestmove = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			Move move = moves.get(i);
			int result = minscore(move, getCurrentState(), timeout);
			if(result==-1){
				return bestmove;
			}
			if (result > score)		{
				score = result;
				bestmove = move;
			}
		}

		// We get the end time ... It is mandatory that stop<timeout
		long stop = System.currentTimeMillis();

		// leave this
		notifyObservers(new GamerSelectedMoveEvent(moves, bestmove, stop - start));
		return bestmove;
	}


	private int minscore(Move move, MachineState state, long timeout) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		// get list, loop thru all non-us roles and call min on those
		if(System.currentTimeMillis()>timeout) {
			return -1;
		}

		List<Move> temp = new ArrayList<Move>();
		temp.add(move);
		List<List<Move> > opps_moves = getStateMachine().getLegalJointMoves(state, getRole(), move);
		int score = 100;
		for (int i = 0; i < opps_moves.size(); i++) {
			MachineState newstate = getStateMachine().getNextState(state, opps_moves.get(i));

			int result = maxscore(newstate, timeout);
			if(result==-1) return -1;
			if (result < score)		score = result;
		}
		return score;
	}

	private int maxscore(MachineState state, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		if(System.currentTimeMillis()>timeout) {
			return -1;
		}

		if (getStateMachine().isTerminal(state))		return getStateMachine().getGoal(state, getRole());
		List<Move> my_moves = getStateMachine().getLegalMoves(state, getRole());
		int maxscore = 0;
		for (int i = 0; i < my_moves.size(); i++) {
			int result = minscore(my_moves.get(i), state, timeout);
			if(result==-1) return -1;
			if (result > maxscore) 	maxscore = result;
		}
		return maxscore;
	}
}

// WANT:
// - get_opponents_role()
// - get next state from current move
// - is a state my_move + opp_move  >>  do we have to make it move from state to state?
// - ask about A-B