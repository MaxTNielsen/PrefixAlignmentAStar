package org.processmining.onlineconformance;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.HistoryAwareOracle;
import org.processmining.onlineconformance.oracle.HistoryAwareState;
import org.processmining.onlineconformance.oracle.examples.ExampleOracleAorBCDorE;
import org.processmining.onlineconformance.oracle.examples.ExampleState;
import org.processmining.onlineconformance.replayer.OracleBasedReplayerWithHistory;

public class TestExample {

	public enum LM {
		A, B, C, D, E
	};

	public enum MM {
		A, B, C, D, E
	};

	public static void main(String[] args) {
		// HistoryAwareOracle<ExampleState, TestExample.LM, TestExample.MM> oracle = new ExampleOracleACBD();
		//		HistoryAwareOracle<ExampleState, TestExample.LM, TestExample.MM> oracle = new ExampleOracleAEorBCD();
		HistoryAwareOracle<ExampleState, TestExample.LM, TestExample.MM> oracle = new ExampleOracleAorBCDorE();

		OracleBasedReplayerWithHistory<ExampleState, TestExample.LM, TestExample.MM> replayer;

		TObjectIntMap<LM> logMoveCost = new TObjectIntHashMap<>(10);
		for (LM lm : LM.values()) {
			logMoveCost.put(lm, 1);
		}

		LM[][] log = new LM[][] { { LM.C, LM.E }, //
		//				{ LM.A, LM.B, LM.C }, //
		//				{ LM.A, LM.B, LM.C, LM.D }, //
		//				{ LM.A, LM.C, LM.E, LM.D },//
		//				{ LM.A, LM.B, LM.B, LM.B, LM.B, LM.C, LM.E, LM.D } //
		};

		for (LM[] trace : log) {
			replayer = new OracleBasedReplayerWithHistory<>(oracle, 45000, logMoveCost);
			for (int i = 0; i < trace.length; i++) {
				replayer.update(trace[i]);
				System.out.println(replayer.getCurrentStates());

			}
			Entry<ExampleState, Integer> alignment = null;
			Iterator<Entry<ExampleState, Integer>> it = replayer.getCurrentStates().iterator();
			while (it.hasNext()) {
				Entry<ExampleState, Integer> partialAlignment = it.next();
				if (oracle.isFinal(partialAlignment.getKey())) {
					alignment = partialAlignment;
					break;
				}
			}

			if (alignment == null) {
				System.out.println("Trace: " + Arrays.toString(trace));
				Entry<ExampleState, Integer> partialAlignment = replayer.getCurrentStates().iterator().next();
				System.out.println("Reached non-final state with cost: " + partialAlignment.getValue());
				replayer.update(null);
				System.out.println(replayer.getCurrentStates());
				alignment = replayer.getCurrentStates().iterator().next();
			}

			System.out.println("Trace: " + Arrays.toString(trace));
			System.out.println("Reached final state with cost: " + alignment.getValue());
			StringBuilder builder = new StringBuilder();
			printAlignment(alignment.getKey(), builder);
			System.out.println(builder.toString());
			System.out.println("-------------------------------------");
		}
	}

	private static <L, M> void printAlignment(HistoryAwareState<L, M> state, StringBuilder builder) {
		if (state.getPredecessor() != null) {
			printAlignment(state.getPredecessor(), builder);

			for (MoveImpl<L, M> move : state.getMovementSequence()) {
				builder.append(move.toString());
				builder.append(",");
			}
			builder.append(" -> ");
			builder.append(state.toString());
			builder.append(" -> ");

		} else {
			builder.append(state.toString());
			builder.append(" -> ");
		}
	}
}
