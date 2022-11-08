package org.processmining.onlineconformance.oracle.examples;

import org.processmining.onlineconformance.TestExample.LM;
import org.processmining.onlineconformance.TestExample.MM;
import org.processmining.onlineconformance.oracle.AbstractHistoryAwareState;

public class ExampleState extends AbstractHistoryAwareState<LM, MM> {
	String name;

	public ExampleState(String name) {
		this.name = name;

	}

	public String toString() {
		return name;
	}

	public boolean equals(Object o) {
		return o instanceof ExampleState ? ((ExampleState) o).name.equals(name) : false;
	}

	public int hashCode() {
		return name.hashCode();
	}

}