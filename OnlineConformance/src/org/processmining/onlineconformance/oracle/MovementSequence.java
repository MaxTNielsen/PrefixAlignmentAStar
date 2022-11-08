package org.processmining.onlineconformance.oracle;

/**
 * Abstract representation of a sequence of moves where only the last move can
 * be identified. The total cost of the sequence is returned, but no information
 * has to be kept about the actual sequence of internal moves.
 * 
 * The movementsequence keeps track of the state in which it is enabled and the
 * state it reaches.
 * 
 * @author bfvdonge
 * 
 */
public interface MovementSequence<S, L, M> {

	/**
	 * return the total cost of this movement sequence, including the last move
	 * 
	 * @return
	 */
	public int getTotalCost();

	/**
	 * return the state that is the result of executing this movement sequence.
	 * 
	 * @return
	 */
	public S getToState();

}
