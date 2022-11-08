package org.processmining.onlineconformance.oracle;

import java.util.List;

import org.processmining.onlineconformance.models.impl.MoveImpl;

public interface HistoryAwareState<L, M> {

	public HistoryAwareState<L, M> getPredecessor();

	public List<MoveImpl<L, M>> getMovementSequence();

	public void setMoves(List<MoveImpl<L, M>> moves);

	public void setMove(MoveImpl<L, M> moves);

	public void setPredecessor(HistoryAwareState<L, M> predecessor);

}
