package org.processmining.onlineconformance.oracle;

import java.util.Collections;
import java.util.List;

import org.processmining.onlineconformance.models.impl.MoveImpl;

public class SimpleHistoryAwareMovementSequence<S extends HistoryAwareState<L, M>, L, M> implements
		HistoryAwareMovementSequence<S, L, M> {

	public static <S extends HistoryAwareState<L, M>, L, M> SimpleHistoryAwareMovementSequence<S, L, M> emptySequence(
			S state) {
		return new SimpleHistoryAwareMovementSequence<S, L, M>(state, state, Collections.<MoveImpl<L, M>>emptyList());
	}

	protected final S fromState;
	protected final S toState;
	protected final List<MoveImpl<L, M>> moves;

	public SimpleHistoryAwareMovementSequence(S fromState, S toState, List<MoveImpl<L, M>> moves) {
		this.fromState = fromState;
		this.toState = toState;
		this.moves = moves;
	}

	public int getTotalCost() {
		int cost = 0;
		for (MoveImpl<L, M> move : moves) {
			cost += move.getCost();
		}
		return cost;
	}

	public L getLastLogMove() {
		if (moves.isEmpty()) {
			return null;
		}
		return moves.get(moves.size() - 1).getEventLabel();
	}

	public M getLastModelMove() {
		if (moves.isEmpty()) {
			return null;
		}
		return moves.get(moves.size() - 1).getTransition();
	}

	public S getFromState() {
		return fromState;
	}

	public S getToState() {
		return toState;
	}

	public List<MoveImpl<L, M>> getMovementSequence() {
		return moves;
	}
	
}
