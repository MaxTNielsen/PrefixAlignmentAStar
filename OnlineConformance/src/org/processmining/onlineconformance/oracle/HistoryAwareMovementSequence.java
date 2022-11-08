package org.processmining.onlineconformance.oracle;

import java.util.List;

import org.processmining.onlineconformance.models.impl.MoveImpl;

/**
 * Interface to represent a movement sequence which gives information about the
 * internal list of movements.
 * 
 * As a general contract, the method <code>getTotalCost()</code> should return
 * the sum of costs of the movements in the list returned by
 * <code>getMovements()</code>
 * 
 * @author bfvdonge
 * 
 * @param <L>
 * @param <M>
 */
public interface HistoryAwareMovementSequence<S extends HistoryAwareState<L, M>, L, M> extends
		MovementSequence<S, L, M> {

	/**
	 * 
	 * A non-empty list of movements
	 * 
	 * @return
	 */
	public List<MoveImpl<L, M>> getMovementSequence();

	
}
