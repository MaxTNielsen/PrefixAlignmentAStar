package org.processmining.onlineconformance.oracle.examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.processmining.onlineconformance.TestExample;
import org.processmining.onlineconformance.TestExample.LM;
import org.processmining.onlineconformance.TestExample.MM;
import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.HistoryAwareOracle;
import org.processmining.onlineconformance.oracle.SimpleHistoryAwareMovementSequence;

public class ExampleOracleAEorBCD implements HistoryAwareOracle<ExampleState, TestExample.LM, TestExample.MM> {

	public ExampleOracleAEorBCD() {
		// implements a simple example for a sequential model A C B D
	};

	public List<SimpleHistoryAwareMovementSequence<ExampleState, TestExample.LM, TestExample.MM>> getSyncronousMoveSequences(
			ExampleState currentState, TestExample.LM label) {
		List<MoveImpl<TestExample.LM, TestExample.MM>> result = new ArrayList<>();
		if (currentState.equals(new ExampleState("P1"))) {
			if (label == LM.A) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.A, MM.A, 0));
			} else if (label == LM.B) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.B, MM.B, 0));
			} else if (label == LM.C) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.B, 1));
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.C, MM.C, 0));
			} else if (label == LM.D) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.B, 1));
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.C, 1));
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.D, MM.D, 0));
			} else if (label == LM.E) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.A, 1));
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.E, MM.E, 0));
			} else if (label == null) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.A, 1));
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.E, 1));
			}
		} else if (currentState.equals(new ExampleState("P2"))) {
			if (label == LM.A) {
				//not possible
			} else if (label == LM.B) {
				//not possible
			} else if (label == LM.C) {
				//not possible
			} else if (label == LM.D) {
				//not possible
			} else if (label == LM.E) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.E, MM.E, 0));
			} else if (label == null) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.E, 1));
			}
		} else if (currentState.equals(new ExampleState("P2P3"))) {
			if (label == LM.A) {
				//not possible
			} else if (label == LM.B) {
				//not possible
			} else if (label == LM.C) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.C, MM.C, 0));
			} else if (label == LM.D) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.C, 1));
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.D, MM.D, 0));
			} else if (label == LM.E) {
				//not possible
			} else if (label == null) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.C, 1));
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.D, 10));
			}
		} else if (currentState.equals(new ExampleState("P2P5"))) {
			if (label == LM.A) {
				//not possible
			} else if (label == LM.B) {
				//not possible
			} else if (label == LM.C) {
				//not possible
			} else if (label == LM.D) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(LM.D, MM.D, 0));
			} else if (label == LM.E) {
				//not possible
			} else if (label == null) {
				result.add(new MoveImpl<TestExample.LM, TestExample.MM>(null, MM.D, 10));
			}
		} else if (currentState.equals(new ExampleState("P4"))) {
			if (label == LM.A) {
				//not possible
			} else if (label == LM.B) {
				//not possible
			} else if (label == LM.C) {
				//not possible
			} else if (label == LM.D) {
				//not possible
			} else if (label == null) {
				//not possible
			}
		}
		if (result.isEmpty()) {
			return Collections.emptyList();
		}
		ExampleState newState = execute(currentState, result);

		List<SimpleHistoryAwareMovementSequence<ExampleState, TestExample.LM, TestExample.MM>> r = new ArrayList<>();

		r.add(new SimpleHistoryAwareMovementSequence<>(currentState, newState, result));

		return r;
	}

	private ExampleState execute(ExampleState currentState, List<MoveImpl<TestExample.LM, TestExample.MM>> moves) {
		if (moves.isEmpty()) {
			return currentState;
		} else {
			MoveImpl<LM, MM> m = moves.get(moves.size() - 1);
			if (m.getTransition() == MM.A) {
				return new ExampleState("P2");
			} else if (m.getTransition() == MM.B) {
				return new ExampleState("P2P3");
			} else if (m.getTransition() == MM.C) {
				return new ExampleState("P2P5");
			} else if (m.getTransition() == MM.D) {
				return new ExampleState("P4");
			} else if (m.getTransition() == MM.E) {
				return new ExampleState("P4");
			}
		}
		return null;
	}

	public boolean isFinal(ExampleState currentState) {
		return currentState.equals(new ExampleState("P4"));
	}

	public ExampleState getInitialState() {
		return new ExampleState("P1");
	}

	public ExampleState createCopy(ExampleState currentState) {
		return new ExampleState(currentState.name);
	}
	public int getScaling() {
		return 1;
	}

	public void setScaling(int scaling) {
		
	}


}