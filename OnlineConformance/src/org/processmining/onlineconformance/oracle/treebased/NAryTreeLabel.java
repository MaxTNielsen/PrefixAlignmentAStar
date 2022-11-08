package org.processmining.onlineconformance.oracle.treebased;

public class NAryTreeLabel {

	protected final short label;

	public NAryTreeLabel(short label) {
		this.label = label;
	}

	public short getLabel() {
		return label;
	}

	public int hashCode() {
		return label;
	}

	public boolean equals(Object o) {
		return o instanceof NAryTreeLabel ? ((NAryTreeLabel) o).label == label : false;
	}

	public String toString() {
		return Short.toString(label);
	}

}
