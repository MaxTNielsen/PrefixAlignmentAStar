package org.processmining.onlineconformance.oracle.treebased;

import org.processmining.onlineconformance.oracle.treebased.AbstractNAryTreeOracle.NodeSet;

public class NAryTreeStateVisit<S extends NAryTreeState> implements Comparable<NAryTreeStateVisit<S>> {

	protected final int costSoFar;
	protected final int depth;
	protected final S state;
	protected final NAryTreeStateVisit<S> predecessor;
	protected final int modelMove;
	protected final NodeSet allowedMoves;

	public NAryTreeStateVisit(S state, NAryTreeStateVisit<S> predecessor, NAryTreeNode modelMove,
			NodeSet allowedMoves, int costSoFar, int depth) {
		this.state = state;
		this.predecessor = predecessor;
		this.allowedMoves = allowedMoves;
		this.modelMove = modelMove == null ? -1 : modelMove.getNode();
		this.costSoFar = costSoFar;
		this.depth = depth;
	}

	public int getCostSoFar() {
		return costSoFar;
	}

	public int hashCode() {
		return state.hashCode();
	}

	public boolean equals(Object o) {
		return o instanceof NAryTreeStateVisit ? ((NAryTreeStateVisit) o).state.equals(state) : false;
	}

	public String toString() {
		return state.toString() + "c:" + costSoFar + " d:" + depth;
	}

	public int getDepth() {
		return depth;
	}

	public NAryTreeStateVisit<S> getPredecessor() {
		return predecessor;
	}

	public S getState() {
		return state;
	}

	public NAryTreeNode getModelMove() {
		if (modelMove < 0) {
			return null;
		}
		return new NAryTreeNode(modelMove);
	}

	public int compareTo(NAryTreeStateVisit<S> o) {
		if (costSoFar != o.costSoFar) {
			return costSoFar - o.costSoFar;
		}
		if (depth != o.depth) {
			return depth - o.depth;
		}
		byte[] s1 = state.getState();
		byte[] s2 = o.state.getState();
		for (int i = 0; i < s1.length; i++) {
			if (s1[i] != s2[i]) {
				return s2[i] - s1[i];
			}
		}
		return 0;
	}

	public NodeSet getAllowedMoves() {
		return allowedMoves;
	}
}
