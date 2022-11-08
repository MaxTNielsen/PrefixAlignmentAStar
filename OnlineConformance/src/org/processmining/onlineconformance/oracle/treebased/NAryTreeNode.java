package org.processmining.onlineconformance.oracle.treebased;

public class NAryTreeNode {

	protected final int node;

	public NAryTreeNode(int node) {
		this.node = node;
	}

	public int getNode() {
		return node;
	}

	public int hashCode() {
		return node;
	}

	public boolean equals(Object o) {
		return o instanceof NAryTreeNode ? ((NAryTreeNode) o).node == node : false;
	}

	public String toString() {
		return Integer.toString(node);
	}
	

}
