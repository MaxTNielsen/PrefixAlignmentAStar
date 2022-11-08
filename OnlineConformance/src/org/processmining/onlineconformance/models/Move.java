package org.processmining.onlineconformance.models;

import org.processmining.onlineconformance.models.impl.MoveImpl;

/**
 * a move in an alignment (theoretically (AxT)*)
 * 
 * @author svzelst
 *
 * @param <L>
 *            type of "event label"
 * @param <T>
 *            type of transition in model, i.e., model part of move
 */
public interface Move<L, T> {

	public enum Type {
		MOVE_SYNC, MOVE_MODEL, MOVE_LABEL
	};

	double getCost();

	L getEventLabel();

	T getTransition();

	Type getType();

	public class Factory {

		public static <L, T> Move<L, T> construct(final L logMove, final T modelMove, final double cost) {
			return new NaiveImpl<>(logMove, modelMove, cost);
		}

	}

	public class NaiveImpl<L, T> implements Move<L, T> {

		private final L logMove;
		private final T modelMove;
		private final double cost;
		private final Type type;

		/**
		 * constructs a move. The type is automatically assigned based on null
		 * values for the logMove or the modelMove
		 * 
		 * @param logMove
		 * @param modelMove
		 * @param cost
		 */
		public NaiveImpl(final L logMove, final T modelMove, final double cost) {
			this.logMove = logMove;
			this.modelMove = modelMove;
			this.cost = cost;
			if (logMove == null) {
				this.type = Type.MOVE_MODEL;
			} else if (modelMove == null) {
				this.type = Type.MOVE_LABEL;
			} else {
				this.type = Type.MOVE_SYNC;
			}
		}

		/**
		 * Returns the cost of this move
		 * 
		 * @return
		 */
		public double getCost() {
			return cost;
		}

		/**
		 * Returns the event label of this move or null if the type is
		 * MOVEMODEL;
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
		public T getTransition() {
			return modelMove;
		}

		/**
		 * returns the type of this move.
		 * 
		 * @return
		 */
		public Type getType() {
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
					MoveImpl<L, T> c = (MoveImpl<L, T>) o;
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

	public class Util {

		public static <L, T> Move.Type getType(final L label, final T transition) {
			if (label != null && transition != null) {
				return Type.MOVE_SYNC;
			} else if (label != null) {
				return Type.MOVE_LABEL;
			} else if (transition != null) {
				return Type.MOVE_MODEL;
			}
			return null;
		}

	}
}
