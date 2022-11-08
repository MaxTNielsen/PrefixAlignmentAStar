package org.processmining.onlineconformance.oracle.treebased;

import org.processmining.onlineconformance.oracle.MovementSequence;

public class NAryTreeMovementSequence<S extends NAryTreeState> implements
		MovementSequence<S, NAryTreeLabel, NAryTreeNode> {

	private final S toState;
	private final int cost;

	public NAryTreeMovementSequence(S toState, int cost) {
		this.toState = toState;
		this.cost = cost;
	}

	public int getTotalCost() {
		return cost;
	}

	public S getToState() {
		return toState;
	}

	public int hashCode() {
		return toState.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof NAryTreeMovementSequence)) {
			return false;
		}
		NAryTreeMovementSequence<?> ms = ((NAryTreeMovementSequence<?>) o);
		return toState.equals(ms.toState);
	}

	public String toString() {
		return toState.toString() + " c:" + cost;
	}
}
