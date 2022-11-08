package org.processmining.onlineconformance.oracle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.processmining.onlineconformance.models.impl.MoveImpl;

public abstract class AbstractHistoryAwareState<L, M> implements HistoryAwareState<L, M> {

	protected HistoryAwareState<L, M> predecessor;
	protected List<MoveImpl<L, M>> moves = Collections.emptyList();

	public HistoryAwareState<L, M> getPredecessor() {
		return predecessor;
	}

	public List<MoveImpl<L, M>> getMovementSequence() {
		return moves;
	}

	public void setMoves(List<MoveImpl<L, M>> moves) {
		this.moves = new ArrayList<>(moves);
	}

	public void setMove(MoveImpl<L, M> move) {
		this.moves = new ArrayList<>(1);
		this.moves.add(move);
	}

	public void setPredecessor(HistoryAwareState<L, M> predecessor) {
		this.predecessor = predecessor;
	}

}
