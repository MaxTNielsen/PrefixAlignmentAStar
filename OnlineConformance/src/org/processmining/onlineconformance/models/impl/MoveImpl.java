package org.processmining.onlineconformance.models.impl;

import org.processmining.onlineconformance.models.Move;

public class MoveImpl<L, M> {

	private final L logMove;
	private final M modelMove;
	private final int cost;
	private final Move.Type type;

	/**
	 * constructs a move. The type is automatically assigned based on null
	 * values for the logMove or the modelMove
	 * 
	 * @param logMove
	 * @param modelMove
	 * @param cost
	 */
	public MoveImpl(L logMove, M modelMove, int cost) {
		this.logMove = logMove;
		this.modelMove = modelMove;
		this.cost = cost;
		if (logMove == null) {
			this.type = Move.Type.MOVE_MODEL;
		} else if (modelMove == null) {
			this.type = Move.Type.MOVE_LABEL;
		} else {
			this.type = Move.Type.MOVE_SYNC;
		}
	}

	/**
	 * Returns the cost of this move
	 * 
	 * @return
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Returns the event label of this move or null if the type is MOVEMODEL;
	 * 
	 * @return
	 */
	public L getEventLabel() {
		return logMove;
	}

	/**
	 * Returns the model label of this move or null if the type is MOVELOG;
	 * 
	 * @return
	 */
	public M getTransition() {
		return modelMove;
	}

	/**
	 * returns the type of this move.
	 * 
	 * @return
	 */
	public Move.Type getType() {
		return type;
	}

	public String toString() {
		return "(" + (logMove == null ? ">>" : logMove.toString()) + ","
				+ (modelMove == null ? ">>" : modelMove.toString()) + "):" + cost;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		boolean res = o instanceof MoveImpl;
		if (res) {
			try {
				MoveImpl<L, M> c = (MoveImpl<L, M>) o;
				res &= getCost() == c.getCost();
				if (!(getEventLabel() == null && c.getEventLabel() == null)) {
					if ((getEventLabel() != null && c.getEventLabel() == null)
							|| (getEventLabel() == null && c.getEventLabel() != null)) {
						res = false;
					} else {
						res &= getEventLabel().equals(c.getEventLabel());
					}
				}
				if (!(getTransition() == null && c.getTransition() == null)) {
					if ((getTransition() != null && c.getTransition() == null)
							|| (getTransition() == null && c.getTransition() != null)) {
						res = false;
					} else {
						res &= getTransition().equals(c.getTransition());
					}
				}
				res &= getType().equals(c.getType());
			} catch (ClassCastException e) {
				res = false;
			}
		}
		return res;
	}

}
