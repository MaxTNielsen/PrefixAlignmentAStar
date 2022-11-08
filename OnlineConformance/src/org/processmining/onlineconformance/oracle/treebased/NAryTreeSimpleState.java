package org.processmining.onlineconformance.oracle.treebased;

import java.util.Arrays;

public class NAryTreeSimpleState implements NAryTreeState {

	protected final byte[] state;
	protected int position;

	public NAryTreeSimpleState(byte[] state) {
		this.state = state;
	}

	public byte[] getState() {
		return state;
	}

	public int hashCode() {
		return Arrays.hashCode(state);
	}

	public boolean equals(Object o) {
		return o instanceof NAryTreeSimpleState ? Arrays.equals(((NAryTreeSimpleState) o).state, state) : false;
	}

	public String toString() {
		return Arrays.toString(state);
	}

	public void setPosition(int pos) {
		this.position = pos;
	}

	public int getPosition() {
		return position;
	}

}