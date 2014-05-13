package org.ggp.base.player.gamer.statemachine;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

class MoveInfo {
	double avgScore = 0;
	int numVisits = 0;
}

class Node {
	int nvisits = 0;
	double utility = 0;
	MachineState state = null;
	public Map<Move, MoveInfo> myMovesMap;  // need to be able to find our move with highest avg at the end of the stateMachineSelectMove method
	Map<List<Move>, MoveInfo> oppMovesMap;  // allows us to choose best move for opponent (somewhat like minimax)
	Map<List<Move>, MoveInfo> jointMovesMap;
	Map<List<Move>, Node> children;  // gives us access to rest of tree
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
	Node parent = null;
	int numDepthChargesTotal = 0;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine m = getStateMachine();
		long start_time = System.currentTimeMillis();
		long finishBy = timeout - 1000;

		parent = new Node();
		parent.state = getCurrentState();

		List<Move> myMoves = m.getLegalMoves(getCurrentState(), getRole());
		for (int i = 0; i < myMoves.size(); i++) {
			parent.myMovesMap.put(myMoves.get(i), new MoveInfo());
		}

		List<List<Move>> opponentsMoves = m.getLegalOpponentJointMoves(getCurrentState(), getRole());
		for (int i = 0; i < opponentsMoves.size(); i++) {
			parent.oppMovesMap.put(opponentsMoves.get(i), new MoveInfo());
		}

		List<List<Move>> jointMoves = m.getLegalJointMoves(getCurrentState());
		for (int i = 0; i < jointMoves.size(); i++) {
			parent.jointMovesMap.put(jointMoves.get(i), new MoveInfo());
			Node child = new Node();
			child.state = m.getNextState(parent.state, jointMoves.get(i));
			parent.children.put(jointMoves.get(i), child);

		}

		// find a pair that hasn't been explored yet, or if none use selection equation
		Node selectedNode = selectNode(parent);
		expand(selectedNode);
		// do depth charge, back propagate



//		Move selection = moves.get(0);
//		if (moves.size() > 1)	selection = monte_carlo_tree_search(moves, getCurrentState(), machine, finishBy);		// checks that there's actually a choice to make

		long stop = System.currentTimeMillis();
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start_time));
		return selectedMove;
	}

	void expand(Node selectedNode) throws MoveDefinitionException, TransitionDefinitionException {
		StateMachine m = getStateMachine();
		List<Move> myMoves = m.getLegalMoves(selectedNode.state, getRole());
		for (int i = 0; i < myMoves.size(); i++) {
			selectedNode.myMovesMap.put(myMoves.get(i), new MoveInfo());
		}

		List<List<Move>> opponentsMoves = m.getLegalOpponentJointMoves(selectedNode.state, getRole());
		for (int i = 0; i < opponentsMoves.size(); i++) {
			selectedNode.oppMovesMap.put(opponentsMoves.get(i), new MoveInfo());
		}

		List<List<Move>> jointMoves = m.getLegalJointMoves(getCurrentState());
		for (int i = 0; i < jointMoves.size(); i++) {
			selectedNode.jointMovesMap.put(jointMoves.get(i), new MoveInfo());
			Node child = new Node();
			child.state = m.getNextState(selectedNode.state, jointMoves.get(i));
			selectedNode.children.put(jointMoves.get(i), child);
		}


	}

//	List<Move> selectJointMove(Node root) {
//		double bestAvg = 0;
//		List<Move> bestJointMove = null;
//
//		// go thru nVisitsToJointMove map to find one that has 0 visits
//		Iterator<Entry<List<Move>, MoveInfo>> it = root.jointMovesMap.entrySet().iterator();
//		while (it.hasNext()) {
//			Map.Entry<List<Move>, MoveInfo> pair = (Map.Entry<List<Move>, MoveInfo>)it.next();
//			if (pair.getValue().numVisits == 0)    return pair.getKey();
//			if (selectnFn(pair.getKey(), root) > bestAvg  ||  bestJointMove == null) {
//				bestJointMove = pair.getKey();
//				bestAvg = selectnFn(pair.getKey(), root);
//			}
//		}
//
//		// if we don't find one with 0 visits, use the selection equation to find the best one to visit
//		return selectJointMove(root.children.get(bestJointMove));
//	}

	Node selectNode(Node root) {
		if(root.children.size() == 0) return root;

		// go thru nVisitsToJointMove map to find one that has 0 visits
		//Map<List<Move>, Node> children
		Iterator<Entry<List<Move>, Node>> it = root.children.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<List<Move>, Node> pair = (Map.Entry<List<Move>, Node>)it.next();
			if (pair.getValue().nvisits == 0)    return pair.getValue();
		}

		Iterator<Entry<List<Move>, Node>> iter = root.children.entrySet().iterator();
		double bestAvg = 0;
		Node bestNode = null;
		while (iter.hasNext()) {
			Map.Entry<List<Move>, Node> pair = (Map.Entry<List<Move>, Node>)iter.next();
			if (selectnFn(pair.getValue(), root) > bestAvg) {
				bestNode = pair.getValue();
				bestAvg = selectnFn(pair.getValue(), root);
				}
		}

		return selectNode(bestNode);

		// if we don't find one with 0 visits, use the selection equation to find the best one to visit
	}

	double selectnFn(Node node, Node root) {
		return node.utility + Math.sqrt(2*Math.log(root.nvisits)/node.nvisits);
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