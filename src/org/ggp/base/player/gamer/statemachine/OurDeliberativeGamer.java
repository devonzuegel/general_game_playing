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
public final class OurDeliberativeGamer extends SampleGamer {
	/**
	 * This function is called at the start of each round
	 * You are required to return the Move your player will play
	 * before the timeout.	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException	{
		// We get the current start time
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = bestMove(moves, getCurrentState());

		// We get the end time ... It is mandatory that stop<timeout
		long stop = System.currentTimeMillis();

		// leave this
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	// get the next possible states
	// return the move to the state that'll give the greatest possible points
	private Move bestMove(List<Move> moves, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		Move bestmove = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			Move move = moves.get(i);

			List<Move> mvs = new ArrayList<Move>();
			mvs.add(move);
			MachineState nextState = getStateMachine().getNextState(state, mvs);

			int result = maxScore(nextState);

			if (result == 100)	return move;
			if (result > score)	{
				score = result;
				bestmove = move;
			}
		}
		return bestmove;
	}

	private int maxScore(MachineState state/*, ArrayList<Move> movesToDo*/) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		int highestscore = 0;

		// BASE CASE
		if (getStateMachine().isTerminal(state))		return getStateMachine().getGoal(state, getRole());

		// RECURSIVE CALL
		List<MachineState> nextStates = getStateMachine().getNextStates(state);
		for (int i = 0; i < nextStates.size(); i++) {
			int result = maxScore(nextStates.get(i));
			if (result == 100)	return result;
			if (result > highestscore)	highestscore = result;
		}
		return highestscore;
	}
}