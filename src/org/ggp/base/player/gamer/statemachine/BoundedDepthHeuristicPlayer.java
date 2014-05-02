package org.ggp.base.player.gamer.statemachine;

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
public final class BoundedDepthHeuristicPlayer extends SampleGamer {

	private int limit = 6; 		// make this dynamic
	private int maxMoves = 0;
//	private int bestHeuristic = 0;

	@Override
	public void stateMachineMetaGame(long startClock) {
		// Implement Meta Gaming, set bestHeuristic to 0 for goal proximity,
		// 1 for mobility, 2 for focus
	}

	/**
	 * This function is called at the start of each round
	 * You are required to return the Move your player will play
	 * before the timeout.	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException	{
		// We get the current start time
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		if(moves.size()>maxMoves)		maxMoves = moves.size();

		Move bestmove = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i++) {
			Move move = moves.get(i);
			int result = minscore(move, getCurrentState(), 0, 100, timeout, 0);
			if(result==-1) return bestmove;
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

	private int minscore(Move move, MachineState state, int alpha, int beta, long timeout, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if(System.currentTimeMillis()>(timeout-1000)) {
			return -1;
		}

		List<List<Move> > opps_moves = getStateMachine().getLegalJointMoves(state, getRole(), move);
		if(opps_moves.size()>maxMoves){
			maxMoves = opps_moves.size();
		}
		for (int i = 0; i < opps_moves.size(); i++) {
			MachineState newstate = getStateMachine().getNextState(state, opps_moves.get(i));

			int result = maxscore(newstate, alpha, beta, timeout, level+1);
			if(result==-1) return -1;
			beta = min(beta, result);
			if (beta <= alpha)	return alpha;
		}
		return beta;
	}

	int mobilityHeuristic(MachineState state) throws MoveDefinitionException{
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, getRole());
		return 80*(legalMoves.size()/maxMoves);
	}

	int focusHeuristic(MachineState state) throws MoveDefinitionException{
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, getRole());
		return 80*(1 - (legalMoves.size()/maxMoves));
	}

	int goalProximity(MachineState state) throws GoalDefinitionException {
		return getStateMachine().getGoal(state, getRole());
	}

	int evaluateState(MachineState state) throws GoalDefinitionException{
		return goalProximity(state);

//		switch(bestHeuristic){
//		case 0: return goalProximity(state);
//		case 1: return mobilityHeuristic(state);
//		default: return focusHeuristic(state);
//		}

	}

	private int maxscore(MachineState state, int alpha, int beta, long timeout, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if(System.currentTimeMillis()>(timeout-1000)) {
			return -1;
		}
		if (getStateMachine().isTerminal(state))		return getStateMachine().getGoal(state, getRole());
		if(level>=limit) return evaluateState(state);

		List<Move> my_moves = getStateMachine().getLegalMoves(state, getRole());
		if(my_moves.size()>maxMoves){
			maxMoves = my_moves.size();
		}
		for (int i = 0; i < my_moves.size(); i++) {
			int result = minscore(my_moves.get(i), state, alpha, beta, timeout, level+1);
			if(result==-1) return -1;
			alpha = max(alpha, result);
			if (alpha >= beta) 	return beta;
		}
		return alpha;
	}


	private int min(int x, int y) {
		if (x < y) return x;
		return y;
	}
	private int max(int x, int y) {
		if (x > y) return x;
		return y;
	}
}


// WANT:
// - get_opponents_role()
// - get next state from current move
// - is a state my_move + opp_move  >>  do we have to make it move from state to state?
// - ask about A-B