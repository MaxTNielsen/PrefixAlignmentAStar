package org.processmining.onlineconformance.oracle;

public class SimpleMovementSequence<S, L, M> implements MovementSequence<S, L, M> {

	protected final S fromState;
	protected final S toState;
	protected final L lastLogMove;
	protected final M lastModelMove;
	protected final int cost;
	
	public SimpleMovementSequence(S fromState, S toState, L lastLogMove, M lastModelMove, int cost) {
		this.fromState = fromState;
		this.toState = toState;
		this.lastLogMove = lastLogMove;
		this.lastModelMove = lastModelMove;
		this.cost = cost;
	
	}

	public int getTotalCost() {
		return cost;
	}

	public L getLastLogMove() {
		return lastLogMove;
	}

	public M getLastModelMove() {
		return lastModelMove;
	}

	public S getFromState() {
		return fromState;
	}

	public S getToState() {
		return toState;
	}

}
