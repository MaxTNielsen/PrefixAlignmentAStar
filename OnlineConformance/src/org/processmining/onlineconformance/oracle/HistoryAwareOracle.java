package org.processmining.onlineconformance.oracle;

import java.util.Collection;

public interface HistoryAwareOracle<S extends HistoryAwareState<L, M>, L, M> extends Oracle<S, L, M> {

	/**
	 * Returns a list of moves needed to execute an activity labeled L. The list
	 * of moves is null if and only if there is no possibility to execute an
	 * activity L now or in the future according to the Oracle.
	 * 
	 * If the list is not empty, then the last element is a Move of which the
	 * type is sync and all other moves are moves of which the type is
	 * MOVEMODEL.
	 * 
	 * If label is null, then a list of modelmoves is given to reach the end
	 * state
	 * 
	 * @param currentState
	 * @param label
	 * @return
	 */
	@Override
	public Collection<? extends HistoryAwareMovementSequence<S, L, M>> getSyncronousMoveSequences(S currentState, L label);

}
