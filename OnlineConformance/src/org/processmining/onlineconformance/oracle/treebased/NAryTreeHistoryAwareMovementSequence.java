package org.processmining.onlineconformance.oracle.treebased;

import java.util.Arrays;
import java.util.List;

import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.HistoryAwareMovementSequence;

public class NAryTreeHistoryAwareMovementSequence<S extends NAryTreeHistoryAwareState> extends NAryTreeMoveList
		implements HistoryAwareMovementSequence<S, NAryTreeLabel, NAryTreeNode> {

	private final S toState;

	public NAryTreeHistoryAwareMovementSequence(S toState, List<MoveImpl<NAryTreeLabel, NAryTreeNode>> moves) {
		super(moves);
		this.toState = toState;
	}

	NAryTreeHistoryAwareMovementSequence(S toState, int[] moves, int cost) {
		super(moves, cost);
		this.toState = toState;
	}

	public int getTotalCost() {
		return cost;
	}

	public NAryTreeLabel getLastLogMove() {
		if (moves.length == 0) {
			return null;
		}
		return get(size() - 1).getEventLabel();
	}

	public NAryTreeNode getLastModelMove() {
		if (moves.length == 0) {
			return null;
		}
		return get(size() - 1).getTransition();
	}

	public S getToState() {
		return toState;
	}

	public int hashCode() {
		return toState.hashCode() + 37 * Arrays.hashCode(moves);
	}

	public boolean equals(Object o) {
		if (!(o instanceof NAryTreeMovementSequence)) {
			return false;
		}
		NAryTreeHistoryAwareMovementSequence<?> ms = ((NAryTreeHistoryAwareMovementSequence<?>) o);
		return toState.equals(ms.toState) && Arrays.equals(moves, ms.moves);
	}

	public String toString() {
		return " -" + (moves.length / 2) + "-> " + toState.toString() + " c:" + cost;
	}

}
