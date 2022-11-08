package org.processmining.onlineconformance.oracle.treebased;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;

import java.util.Arrays;

import org.processmining.onlineconformance.oracle.HistoryAwareOracle;
import org.processmining.plugins.etm.model.narytree.NAryTree;

public class NAryTreeHistoryAwareOracle
		extends
		AbstractNAryTreeOracle<NAryTreeHistoryAwareState, NAryTreeHistoryAwareMovementSequence<NAryTreeHistoryAwareState>>
		implements HistoryAwareOracle<NAryTreeHistoryAwareState, NAryTreeLabel, NAryTreeNode> {

	public NAryTreeHistoryAwareOracle(NAryTree tree, int configuration, TIntIntMap moveModelCost, int maxDepth,
			boolean useCache, int stopAt) {
		super(tree, configuration, moveModelCost, maxDepth, useCache, stopAt);
	}

	public NAryTreeHistoryAwareOracle(NAryTree tree, int configuration, TIntIntMap moveModelCost, int maxDepth) {
		super(tree, configuration, moveModelCost, maxDepth);
	}

	protected NAryTreeHistoryAwareState createState(NAryTreeHistoryAwareState currentState, byte[] newState) {
		NAryTreeHistoryAwareState toState = new NAryTreeHistoryAwareState(newState);
		toState.setPredecessor(currentState);
		return toState;
	}

	public NAryTreeHistoryAwareState getInitialState() {
		return new NAryTreeHistoryAwareState(initialState);
	}

	public NAryTreeHistoryAwareState createCopy(NAryTreeHistoryAwareState currentState) {
		return new NAryTreeHistoryAwareState(currentState.getState());
	}

	protected NAryTreeHistoryAwareMovementSequence<NAryTreeHistoryAwareState> createMovementSequence(
			NAryTreeHistoryAwareState startState, NAryTreeStateVisit<NAryTreeHistoryAwareState> lastState,
			NAryTreeHistoryAwareState toState, NAryTreeLabel label, int nodeEnabled, int cost) {

		TIntList moves = new TIntArrayList();

		//		List<Move<NAryTreeLabel, NAryTreeNode>> moves = new LinkedList<>();
		int seqCost = 0;
		while (lastState.getModelMove() != null) {
			int modelMove = lastState.getModelMove().getNode();
			int c = getCostForNode(modelMove);
			seqCost += c;
			moves.add(c);
			moves.add(NAryTreeMoveList.makeMove(NAryTreeMoveList.NOMOVE, modelMove));
			//			moves.add(0, new Move<>((NAryTreeLabel) null, lastState.getModelMove(), getCostForNode(lastState
			//					.getModelMove().getNode())));
			lastState = lastState.getPredecessor();
		}
		moves.reverse();

		if (label != null) {
			//			moves.add(new Move<>(label, new NAryTreeNode(nodeEnabled), 0));
			moves.add(NAryTreeMoveList.makeMove(label.getLabel(), nodeEnabled));
			moves.add(0);
		} else if (nodeEnabled != NONODE) {
			int c = getCostForNode(nodeEnabled);
			seqCost += c;
			moves.add(NAryTreeMoveList.makeMove(NAryTreeMoveList.NOMOVE, c));
			moves.add(getCostForNode(nodeEnabled));
			//			moves.add(new Move<>(label, new NAryTreeNode(nodeEnabled), getCostForNode(nodeEnabled)));
		}
		return new NAryTreeHistoryAwareMovementSequence<NAryTreeHistoryAwareState>(toState, moves.toArray(), seqCost);
		//, label, new NAryTreeNode(nodeEnabled), cost + getCostFor(nodeEnabled));
	}

	protected NAryTreeHistoryAwareMovementSequence<NAryTreeHistoryAwareState> appendOrToMovementSequence(
			NAryTreeHistoryAwareMovementSequence<NAryTreeHistoryAwareState> ms, NAryTreeHistoryAwareState toState,
			int orNode, int costForNode) {
		int[] moves = Arrays.copyOf(ms.moves, ms.moves.length + 2);
		moves[moves.length - 2] = NAryTreeMoveList.makeMove(NAryTreeMoveList.NOMOVE, orNode);
		moves[moves.length - 1] = costForNode;
		return new NAryTreeHistoryAwareMovementSequence<NAryTreeHistoryAwareState>(toState, moves, ms.getTotalCost()
				+ costForNode);
	}

}
