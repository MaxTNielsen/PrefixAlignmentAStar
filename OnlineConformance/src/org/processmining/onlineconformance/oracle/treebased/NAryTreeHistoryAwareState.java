package org.processmining.onlineconformance.oracle.treebased;

import java.util.Arrays;

import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.HistoryAwareState;

public class NAryTreeHistoryAwareState extends NAryTreeMoveList implements NAryTreeState,
		HistoryAwareState<NAryTreeLabel, NAryTreeNode> {

	protected final byte[] state;
	protected NAryTreeHistoryAwareState predecessor;
	protected int position;

	public NAryTreeHistoryAwareState(byte[] state) {
		super();
		this.state = state;
	}

	public byte[] getState() {
		return state;
	}

	public int hashCode() {
		return Arrays.hashCode(state);
	}

	public boolean equals(Object o) {
		return o instanceof NAryTreeHistoryAwareState ? Arrays.equals(((NAryTreeHistoryAwareState) o).state, state)
				: false;
	}

	public String toString() {
		return Arrays.toString(state);
	}

	public NAryTreeHistoryAwareState getPredecessor() {
		return predecessor;
	}

	public void setMove(MoveImpl<NAryTreeLabel, NAryTreeNode> move) {
		moves = new int[2];
		moves[0] = 0xFFFFFFFF;
		if (move.getEventLabel() != null) {
			moves[0] &= 0x0000FFFF | move.getEventLabel().getLabel() << 16;
		}
		if (move.getTransition() != null) {
			moves[0] &= 0xFFFF0000 | move.getTransition().getNode();
		}
		moves[1] = move.getCost();
		cost = move.getCost();
	}

	public void setPredecessor(HistoryAwareState<NAryTreeLabel, NAryTreeNode> predecessor) {
		this.predecessor = (NAryTreeHistoryAwareState) predecessor;
	}

	public void setPosition(int pos) {
		this.position = pos;
	}

	public int getPosition() {
		return position;
	}
}
