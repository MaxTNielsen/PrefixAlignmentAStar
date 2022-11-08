package org.processmining.onlineconformance.replayer;

import gnu.trove.map.TObjectIntMap;

import java.util.Map;

import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.HistoryAwareMovementSequence;
import org.processmining.onlineconformance.oracle.HistoryAwareOracle;
import org.processmining.onlineconformance.oracle.HistoryAwareState;
import org.processmining.onlineconformance.oracle.MovementSequence;

public class OracleBasedReplayerWithHistory<S extends HistoryAwareState<L, M>, L, M> extends
		OracleBasedReplayer<S, L, M> {

	public OracleBasedReplayerWithHistory(HistoryAwareOracle<S, L, M> oracle, int size,
			TObjectIntMap<? extends L> logMoveCost) {
		super(oracle, size, logMoveCost);
	}

	@Override
	protected void followEdge(Map<S, Integer> newStates, S fromState, S toState, MovementSequence<S, L, M> moves,
			int cost) {
		newStates.remove(toState);
		S copyOfNewState = oracle.createCopy(toState);
		newStates.put(copyOfNewState, cost);
		copyOfNewState.setPredecessor(fromState);
		if (moves != null) {
			copyOfNewState.setMoves(((HistoryAwareMovementSequence<S, L, M>) moves).getMovementSequence());
		}
	}

	@Override
	protected void followEdge(Map<S, Integer> newStates, S fromState, S toState, MoveImpl<L, M> move, int cost) {
		newStates.remove(toState);
		S copyOfNewState = oracle.createCopy(toState);
		newStates.put(copyOfNewState, cost);
		copyOfNewState.setPredecessor(fromState);
		copyOfNewState.setMove(move);
	}

}
