package org.processmining.onlineconformance.oracle.treebased;

import gnu.trove.map.TIntIntMap;

import org.processmining.onlineconformance.oracle.Oracle;
import org.processmining.plugins.etm.model.narytree.NAryTree;

public class NAryTreeOracle extends
		AbstractNAryTreeOracle<NAryTreeSimpleState, NAryTreeMovementSequence<NAryTreeSimpleState>> implements
		Oracle<NAryTreeSimpleState, NAryTreeLabel, NAryTreeNode> {

	public NAryTreeOracle(NAryTree tree, int configuration, TIntIntMap moveModelCost, int maxDepth) {
		super(tree, configuration, moveModelCost, maxDepth);
	}

	public NAryTreeOracle(NAryTree tree, int configuration, TIntIntMap moveModelCost, int maxDepth, boolean useCache,
			int stopAt) {
		super(tree, configuration, moveModelCost, maxDepth, useCache, stopAt);
	}

	protected NAryTreeSimpleState createState(NAryTreeSimpleState currentState, byte[] newState) {
		NAryTreeSimpleState toState = new NAryTreeSimpleState(newState);
		//				NAryTreeHistoryAwareState toState = new NAryTreeHistoryAwareState(newState);
		//				toState.setPredecessor(currentState);
		return toState;
	}

	public NAryTreeSimpleState getInitialState() {
		return new NAryTreeSimpleState(initialState);
		//				return new NAryTreeHistoryAwareState(initialState);
	}

	public NAryTreeSimpleState createCopy(NAryTreeSimpleState currentState) {
		//		return new NAryTreeHistoryAwareState(currentState.getState());
		return new NAryTreeSimpleState(currentState.getState());
	}

	protected NAryTreeMovementSequence<NAryTreeSimpleState> createMovementSequence(NAryTreeSimpleState startState,
			NAryTreeStateVisit<NAryTreeSimpleState> lastState, NAryTreeSimpleState toState, NAryTreeLabel label,
			int nodeEnabled, int cost) {
		return new NAryTreeMovementSequence<NAryTreeSimpleState>(toState, cost);
	}

	protected NAryTreeMovementSequence<NAryTreeSimpleState> appendOrToMovementSequence(
			NAryTreeMovementSequence<NAryTreeSimpleState> ms, NAryTreeSimpleState toState, int i, int costForNode) {
		return new NAryTreeMovementSequence<NAryTreeSimpleState>(toState, ms.getTotalCost() + costForNode);
	}

}
