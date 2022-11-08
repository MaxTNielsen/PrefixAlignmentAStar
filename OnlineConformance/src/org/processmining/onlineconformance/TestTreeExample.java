package org.processmining.onlineconformance;

import gnu.trove.iterator.TShortIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectShortHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TShortHashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import nl.tue.astar.Trace;

import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.MovementSequence;
import org.processmining.onlineconformance.oracle.treebased.AbstractNAryTreeOracle;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeHistoryAwareMovementSequence;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeHistoryAwareOracle;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeHistoryAwareState;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeLabel;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeNode;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeOracle;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeSimpleState;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeState;
import org.processmining.onlineconformance.replayer.OracleBasedReplayer;
import org.processmining.onlineconformance.replayer.OracleBasedReplayerWithHistory;
import org.processmining.plugins.etm.model.narytree.Configuration;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.model.narytree.Simulator;
import org.processmining.plugins.etm.model.narytree.StateBuilder;
import org.processmining.plugins.etm.model.narytree.StateSpace;
import org.processmining.plugins.etm.model.narytree.StateSpace.Edge;
import org.processmining.plugins.etm.model.narytree.TreeUtils;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;

public class TestTreeExample {

	public static final int SCALING = 10000;

	private static class RealReplayResult {
		public final NAryTreeHistoryAwareMovementSequence<?> moves;
		public final boolean reliable;
		public final int cacheSize;

		public RealReplayResult(NAryTreeHistoryAwareMovementSequence<?> moves, boolean reliable, int cacheSize) {
			this.moves = moves;
			this.reliable = reliable;
			this.cacheSize = cacheSize;

		}

		public int cost() {
			return moves.getTotalCost();
		}
	}

	public static void main(String[] args) throws Exception {
		long start, end;
		//  -XX:+UseConcMarkSweepGC
		//		example.doMaxDepthExperiment(10);

		TestTreeExample example = new TestTreeExample();
		start = System.nanoTime();
		example.doSingleTest(true);
		end = System.nanoTime();
		System.out.println("Time: " + (end - start) / 1.0E9);
		System.exit(0);

		if (args.length == 0) {
			float[] times = new float[8];
			int t = 0;
			int treeNumber = 0;
			for (int treeOffset = 10; treeOffset < 101; treeOffset += 10) {
				int treeStart = treeNumber;
				for (int mm = 0; mm < 2; mm++) {
					for (int lm = 0; lm < 2; lm++) {
						if (lm == mm && lm > 0) {
							continue;
						}
						String folder = "D:\\temp\\onlineconformance\\LM" + (lm * 10 + (lm == 0 ? 1 : 0)) + "MM"
								+ (mm * 10 + (mm == 0 ? 1 : 0) + "T" + treeOffset);
						if (!new File(folder).exists()) {
							new File(folder).mkdir();
						}
						System.gc();
						TestTreeExample ex = new TestTreeExample();
						ex.maxNoise = 10;
						start = System.currentTimeMillis();
						treeNumber = ex.doFullExperiment2(folder, false, 1, ex.maxNoise, lm * 10 + (lm == 0 ? 1 : 0),
								mm * 10 + (mm == 0 ? 1 : 0), treeOffset, treeStart);
						//						treeNumber = ex.findMaxQueue(folder, false, 1, ex.maxNoise, lm * 10 + (lm == 0 ? 1 : 0), mm
						//								* 10 + (mm == 0 ? 1 : 0), treeOffset, treeStart);
						end = System.currentTimeMillis();
						times[t++] = (end - start) / 1000.0f;
					}
				}
			}
			System.out.println("Experiment times: " + Arrays.toString(times));
		} else {
			TestTreeExample ex = new TestTreeExample();
			ex.maxNoise = 0;
			System.out.println(args[0] + ";" + args[1] + ";" + args[2]);
			ex.maxNoise = Integer.parseInt(args[2]);
			ex.doFullExperiment2(args[0], true, Integer.parseInt(args[1]), ex.maxNoise, 1, 1, 0, 0);

		}
	}

	int numTrees = 50;
	int numTraces = 500;
	int maxNoise;

	public void doSingleTest(boolean keepHistory) throws Exception {

		String[] activities = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
				"P", "Q", "R", "S", "T","A1", "B1", "C1", "D1", "E1", "F1", "G1", "H1", "I1", "J1", "K1", "L1", "M1", "N1", "O1",
				"P1", "Q1", "R1", "S1", "T1" };
		NAryTreeLabel[] labels = new NAryTreeLabel[activities.length];

		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
		// initialize a tree with all node costs 5 for leafs and 0 otherwise.
		for (short i = 0; i < activities.length; i++) {
			map.put(activities[i], i);
			labels[i] = new NAryTreeLabel(i);
		}

		TObjectIntMap<NAryTreeLabel> logMoveCost = new TObjectIntHashMap<>(10);
		for (NAryTreeLabel lm : labels) {
			logMoveCost.put(lm, 1);
		}

		String trace;
		// MAXDEPTH EXAMPLE:
		//      MODELCOST = 1 LOGCOST = 5
		//		trace  = "[B ]  ";
		//		NAryTree tree = TreeUtils.fromString(
		//				"SEQ( LEAF: A , LEAF: A , LEAF: A , LEAF: A , LEAF: B ) [[-, -, -, -, -, -] ]", map);

		NAryTree tree;
		tree = TreeUtils.randomTree(map.keySet().size(), 0.6, 300, 500, new Random(3142));
		tree = TreeUtils.flatten(tree);
		boolean[] b = new boolean[tree.size()];
		boolean[] h = new boolean[tree.size()];
		Configuration c = new Configuration(b, h);
		tree.addConfiguration(c);

		// MAXQUEUE EXAMPLE
		//     MOVEMODELCOST G = 1000, ALL OTHER 1. LOGMOVECOST 1
		//		trace = "[A, B, C, D, E, F, G] ";
		//		tree = TreeUtils
		//				.fromString(
		//						"XOR( AND( LEAF: A , LEAF: B , LEAF: C , LEAF: D , LEAF: E , LEAF: F ) , LEAF: G ) [[-, -, -, -, -, -, -, -, -] ]",
		//						map);

		//		trace = "[A, B, C, F] ";
		//		tree = TreeUtils
		//				.fromString(
		//						"XOR( SEQ( AND( LEAF: A , LEAF: B , LEAF: C ) , SEQ( LEAF: D , LEAF: E , LEAF: F ) ) , LEAF: F ) [[-, -, -, -, -, -, -, -, -, -, -] ]",
		//						map);

		//		tree = TreeUtils
		//				.fromString(
		//						"AND( LEAF: tau , LEAF: tau , OR( LEAF: 2 , AND( LEAF: 6 , LEAF: 5 , LEAF: 3 , OR( SEQ( LEAF: 5 , LEAF: 6 , LEAF: 4 ) , LEAF: 3 , LEAF: 2 ) ) , LEAF: tau ) , LEAF: 3 ) [[-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -] ]",
		//						map);
		//		trace = "[C, G, E, G, F, A, B, F, G, D] ";

		//		tree = TreeUtils
		//				.fromString(
		//						"LOOP( LEAF: 19 , AND( OR( LEAF: 18 , LEAF: 17 , LEAF: 16 , LEAF: 15 ) , LEAF: 11 , OR( LEAF: 14 , LEAF: 13 , LEAF: 12 , LEAF: 11 ) , LEAF: 10 ) , XOR( REVSEQ( LEAF: 9 , LEAF: 8 , LEAF: 7 ) , OR( LEAF: 6 , LEAF: 5 , LEAF: 4 ) , AND( LEAF: 3 , LEAF: 2 , LEAF: 1 ) ) ) [[-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -] ]",
		//						map);
		//		String[] trs = new String[] { "[T, S, D, F, P, E, L, L, M, N, K, T, F, N, N, Q, O, K, R, H, P, L, M, F, M, T, L, Q, K, M, N, R, O, S, I, T, N, M, L, C, O, R, S, L, N, K, Q, T, N, T, C, B, D] " };

		//		tree = TreeUtils
		//				.fromString(
		//						"ILV( LEAF: 19 , LEAF: 18 , OR( XOR( LEAF: 17 , LEAF: 16 , LEAF: 15 ) , SEQ( LEAF: 14 , LEAF: 13 ) , LEAF: 12 , LEAF: 11 ) , LEAF: 10 ) [[-, -, -, -, -, -, -, -, -, -, -, -, -, -] ]",
		//						map);
		String[] trs = "[[], [S, S, K, H, M], [C, T, L, M, Q, O, O, S], [S, I, J, R, L, N, H, I, M, K, T], [T, L, I, R, M, O, N, S, K], [K, K, S, M, L, Q, O, N, T, E], [K, M, L, P, M, N, N, T], [K, L, T, S], [Q, M, O, N, L, M, T, K], [M, P, T, K], [S, T, M, R, L, F, N, P, K], [S, T, M, Q, O, L, N, K], [P, J, P, L, N, M, T, E, S], [B, O, R, L, M, O, N, S, T, K, J], [L, J, T, S, S], [T, S, H, E, P, O, M, L, I, E], [T, K, R, F, L, M, O, P, N, S], [B, L, E, N, R, M, S, K], [M, O, S, L, N, S, T, K], [K, T, M, L, S, O, Q, N, S], [K, H, P, M, L, O, N, S], [R, C, O, N, M, T, K, S], [T, K, M, L, P, O, G, N, N, J], [S, L, K, T], [L, G, E, P, J, N, L, S, T, Q], [T, K, L, M, Q, O, N, S, S], [S, M, O, N, J, M, T, K, A], [S, K, G, Q, M, O, N, H], [T, S, H, L, M, A, K], [S, M, G, P, O, B, N, H, K], [S, K, K, T, M], [S, K, L, R, M, T], [J, L, S, T], [K, R, M, L, O, E, K, E, D, S], [K, T, S, L], [S, D, O, P, Q, M, T, K], [S, L, O, T, Q, M, T, P, S, S], [T, S, P, M, L, O, N, K], [B, M, R, T, S, J], [S, T, L, M, K], [T, R, O, Q, L, N, M, S, J, T, K, N], [T, K, M, R, L, O, N, S], [M, P, L, O, L, N, F, H, S, K], [K, T, M, N, Q, L, O, N, S], [N, L, R, O, M, L, O, N, S, T, K], [K, T, L, R, M, D, O, L, N, S], [T, K, L, P, M, O, N, L, S], [K, T, P, O, L, N, M, S], [P, S, P, L, O, N, M, K, T], [M, L, M, O, N, L, K, E], [I, M, D, K, E], [T, L, A, N, M, K, S, M], [L, L, Q, O, N, T, K, S], [L, P, T, K, S, J], [T, S, S, Q, L, M, O, N], [S, K, L, O, M, P, N, T], [K, T, S, L, B, M, O, N], [T, S, L, M, O, J, Q, K], [T, S, K, M, J, R, L, S, M], [T, Q, B, S, M, L, P, O, N], [L, S, M, O, N, T, S, K], [K, T, M, O, A, Q, L, B, S], [S, K, Q, O, A, M, N, I, Q, L, T], [K, S, J, M, T], [G, S, T, O, M, N, P, K], [A, M, P, K, L, S, T], [E, H, T, S, O, M, O, L, C, Q], [L, A, T, F, K], [T, K, L, Q, M, O, N, S], [K, F, T, S, D, N, N, L, S, T, F], [S, K, T, P, L, O, N, N, M], [C, G, S, L, M, M, O, N], [K, S, L, O, K, N, R, M, L, P, T], [S, E, D, L, K], [T, K, S, N, O, N], [T, K, L, M, Q, S], [S, L, T, B, J], [M, S, K, B, C], [K, S, T, L, Q], [L, E, O, R, N, K, S, J], [M, R, O, N, S, T, K], [S, L, M, O, N, K, T, K], [S, L, P, T, K], [N, G, T, K, M, L, O, A], [S, K, N, A, O, N, M, T], [C, S, T, L, A, O, M, D, N, R], [K, P, L, O, N, S, T], [S, T, K, G, M, Q, C, L], [O, K, N, P, L, T, G, A], [P, K, Q, T, O, J, L, O, M, O], [K, T, K, P, O, M, B, N, S], [M, S, I, T, K], [K, S, T, N, N, L, J, G, K, P], [K, L, R, H, M, J, N, S, T], [M, L, O, N, K, T, S, K], [S, O, O, N, L, M, P, T, F, K], [M, P, T, S, K], [B, K, S, T, L, M], [S, T, K, M, L, P, O, N, R], [H, S, H, T, S, E], [L, R, M, O, N, K, S, T], [A, L, R, E, F, K, A, T, S], [T, P, Q, K, S], [K, T, I, S, O, C, S, M], [J, M, L, R, O, N, S, P, T, K, H], [M, L, T, S, K], [T, K, S, L, M, R, O, N, D], [T, J, M, R, L, O, K], [L, R, Q, M, S, K, T], [K, M, T, S, R], [T, I, S, Q, O, Q, L, M, N, K], [O, N, R, M, T, K, S], [L, M, R, O, O, K, J, S, T], [K, T, S, T, S, J, O, N], [S, K, N, T, T, J, M, O, P, N], [S, O, C, R, O, N, T], [S, T, M, P, O, L, P, K], [L, M, O, T, N, R, T, S, K], [K, O, N, L, G, S, S, T], [J, L, C, R, M, O, N, T, K, S], [M, L, K, S, S], [K, M, T, S], [S, L, S, T, O, Q, N, K, T], [P, L, M, K, S, T, E], [M, L, R, O, N, T, S, K], [K, B, M, R, L, O, N, S, T], [F, L, R, K, O, R, N, K, T, S], [K, S, T, R], [M, L, Q, O, N, K, R, J], [K, G, Q, O, R, L, T], [A, R, L, M, O, J, N, K, T, K], [K, T, M, L, O, N, R, R, S, F], [S, B, P, M, L, O, N, T, N], [S, O, R, B, N, I, E, T], [T, S, N, R, M, L, O, A, N, K], [K, S, A, T, H, M], [T, M, S, K, L, M, B], [T, L, M, O, P, E, N, K, T, S, I], [S, R, L, M, O, N, M, A, K], [M, P, L, F, S, O, P, N, T, S, T, K], [S, T, K, K, L, Q, O, M, N], [H, L, C, P, K], [K, M, H, S], [I, K, S, T, D, M, G], [J, R, L, M, Q, S, T, S], [I, M, R, O, L, M, N, A, S, T, K], [M, L, K, Q, K, T, S], [O, R, L, M, N, K, S, T], [S, T, L, K], [L, R, M, O, N, K, J, S, Q, T, Q], [L, Q, M, O, N, S, K, T], [S, K, T, E, L, M, F], [O, S, T, K, C, P], [T, K, S, M], [M, Q, K, T, S], [L, G, M, N, K, R, S, K, T], [N, S, M, K, Q, L, O, N, K, T], [S, T, G, M, S], [T, L, Q, K, S], [O, N, E, R, S, K, T], [B, D, H, C, M, O, L, N, T, S, K], [L, E, M, Q, N, K, T, S], [T, S, K, Q, M, R, O, N], [K, R, M, C, P, L, O, T, S, T], [M, L, P, T, N, K, S, T], [L, P, M, Q, O, N, S, S, A, A, K], [S, L, M, F, R, N, T, K], [K, S, T, M, L, K, Q, O, N], [C, K, S, M, L, O, R, E], [E, M, Q, L, N, R, O, N, E, S, K, T], [P, O, R, O, E, N, F, T, K, S], [J, T, S, K, Q, M, O, L, S, P], [M, L, O, P, R, O, K, T], [N, L, P, M, O, N, K, S], [J, L, R, F, M, A, O, N, K, S, T], [C, Q, T, S, K, K], [E, S, R, O, L, N, M, E], [S, M, R, C, O, M, N, B, J, K, L, Q], [T, K, H, L, M, S], [K, S, L, M, T], [Q, K, L, T, S, D], [K, T, L, M, O, S, R, N, S], [S, K, T, L, Q, M, O, N], [K, S, M, I, T], [L, M, H, T, T, K], [Q, T, S, K, M, R, L, O, N], [T, H, M, B, K], [M, Q, S, T, A], [K, S, T, R, P], [M, T, S, P, P, K], [S, K, T, M, L, R], [K, B, O, L, N, M, R, S, T], [T, L, S, K], [P, L, M, O, N, T, S, K], [K, S, T, S, Q, M, P, L], [K, T, S, L, O, N], [K, B, M, N, S, T], [T, M, O, N, Q, L, S, K, R], [T, M, O, C, N, L, G, S, K], [S, M, E, K], [K, T, L, P, M, O, N, S], [K, R, O, N, T, J], [Q, K, T, L, I, B, N, S], [M, S, T, Q, O, L, M, M, H, N, K], [O, L, N, D, M, Q, T, K, S], [N, O, S, T, J], [T, S, K, K, O, L, R, N, M], [S, T, K, M, C, M, N, C, O, N, P, L, S], [K, M, O, N, P, B, M], [K, T, B, M, O, L, Q, G, N, S], [T, R, J, M, O, N, Q, L, S, K], [D, C, L, O, N, P, K, S], [T, K, S, L, J, M, R, O, N], [N, P, O, M, N, S, T, K], [S, M, T, H, R, M, S], [M, L, Q, O, N, P, S, T], [M, Q, O, P, N, K, T, S], [F, Q, L, K, S, T], [M, A, T, S, K], [T, M, O, D, N, P, S, A, K], [Q, T, N, O, C, N, P, P, M, S], [S, K, E, L, M, Q, O, T, T], [K, T, S, M, L, T, P, M, N], [T, L, K, M, O, N, S, K, N], [S, K, T, O, P, L], [T, S, R, T, P, Q, M, O, K], [B, K, T, M, C, S], [K, T, S, M, O, J, N, I, R], [K, F, S, T, P, L, M, O, H, N, M], [O, N, M, F, L, P, S, K, T, T], [S, P, L, Q, O, P, N, M, K, T], [K, T, L, M, F, Q, O, N, S], [K, T, S, M, P, L, O, N], [L, M, Q, T, S, K, T], [T, S, K, L, R], [M, D, O, P, M, C, L, O, N, S, S], [B, T, K, S, L, P, P, A, O, M, L, N], [M, I, L, O, N, P, T, K, S], [S, G, M, L, P, O, N, T, K], [M, L, Q, O, B, K, K, S, T], [H, T, B, M, R, K], [K, T, L, R, M, O, N, S], [T, S, K, G, R], [K, S, P, L, M, T, T], [S, K, M, J, N, N, T], [S, T, K, H, I, F, I, L, M, K, P], [R, S, P, L, M, O, F, T, K], [T, R, M, L, R, O, N, K, K, F], [T, L, M, O, N, S, K], [K, G, M, M, L, T, S], [T, M, L, P, S, O, O, N, S, P], [K, L, O, P, M, N, S, T], [K, L, E, R, M, E, O, N, S, T, K], [T, M, O, K, S], [K, T, S, S, L, Q, O, N], [T, K, K, L, L, T, R], [T, K, S, R, O, L, M, Q, N], [S, O, L, M, Q, O, N, K, T], [M, Q, O, L, Q, N, K, S, P], [S, A, Q, M, O, P, J, K, T], [M, L, J, Q, N, S, T, K], [O, M, P, L, N, T, K, S], [S, R, O, D, L, R, M, P, N, K, T], [K, S, D, Q, O, N, T, H], [E, B, K, L], [M, L, O, N, S, T, K], [B, L, O, N, P, M, S, T], [M, O, N, P, E, T, J, L], [D, K, Q, L, M, O, N, T], [S, T, K, P, Q, L, N], [S, T, B, M, F, R, L, G, O, L, N, K, S], [K, T, S, O, P, L, N, M], [T, K, S, J, P, R, E, O, L, Q, M], [L, M, S, O, G, P, N, K, T, R, E], [T, L, E, L, S, G, M, O, L, N, R], [F, T, S, K, M, O, A, O, R], [S, R, L, M, O, N, K, T], [S, Q, L, O, Q, N, P, K, T], [I, T, S, M, K], [S, T, L, B, K], [K, T, L, M, M, P, S], [T, M, Q, L, S, N, M, S, K], [K, T, T, L, R, B, M, S, J], [K, Q, O, L, M, N, G, T, S], [L, G, O, S, E], [M, L, P, O, H, N, T, S, K], [G, K, R, M, P, L, O, N, S], [T, S, K, T, K, O, L, N, R], [S, T, K, O, P, P, L, N], [T, Q, L, R, S, K, S], [K, F, T, S, M], [S, K, T, M, L, K, R, F, G, E, P, S, M], [T, M, G, R, J, O, N, J, S, K], [A, T, S, K, P, L, O, N, G, N], [K, M, Q, T, S], [T, S, T, Q, M, R, J, L, M, K], [T, S, F, L, T, F, K], [K, H, S, L, M, P, O, N, T], [I, K, S, M, T], [R, S, M, P, M, O, N, L, K], [S, K, M, C, R, O, L, T], [S, R, L, R, O, N, K, K, T], [L, O, G, N, L, S, G], [M, K, T, T], [T, D, K, S, A, M, L, O, R, N], [K, O, L, D, M, N, T, S], [K, T, O, O, M, L, P, N, S], [Q, M, Q, P, L, O, N, T, K, S], [T, S, K, M], [N, P, L, M, H, R, M, O, N, S, K], [S, K, L, E, P, T], [F, L, S, T, M, M], [T, S, T, L, P, O, M, N, I, K], [K, S, M, Q, T], [K, T, S, M, O, P, L, N], [L, P, T, N, O, N, T, K, S], [H, K, S, C, M, R, T], [D, L, Q, L, O, N, T, S, K], [L, A, A, T, M, B, M, O, O, N, B, R], [K, S, L, M, Q, O, L, T], [K, T, M, L, R, M, O, N], [T, R, K, S, R, L, O, H, M, N], [S, T, M, L, E, O, N, R, K], [I, M, S, S, K], [K, M, L, Q, O, P, N, S, T], [S, K, T, H, L, B, O, R, N], [K, T, S, O, M, L, R, N], [K, S, M, B, L, R, A, O, N, T], [G, M, L, R, O, N, S, K, Q], [K, S, A, T, J, C], [K, T, L, S], [B, K, T, M, L, C, N], [Q, L, O, M, O, S, T, S, K], [D, M, L, O, C, N, S, I], [K, T, R, E, L, O, N, S, T], [K, T, L, C, M, T, S], [E, J, M, L, Q, T, K], [G, R, N, L, M, R, N, K, T, H], [S, K, T, P], [O, K, T, L, G, Q, T, O, C, N, S, J], [T, L, L, O, P, H, R, K, S], [Q, M, T, K, S], [L, Q, T, B, S], [T, S, M, E, M, O, F, P, N, K, S], [F, K, F, Q, L, O, M, L, N, R, O], [R, M, T, K, Q], [S, M, D, J, R, L, O, N, T, D], [T, S, K, N, O, M, L, R, O, N], [T, S, P, L, O, N, K], [L, P, O, N, E, S, T, K], [L, S, P, K, T], [T, K, E, R, L, K, S], [K, R, S, M, L, C, R, O, N], [S, M, K, F, T], [K, T, L, M, M, O, Q, N, R], [S, K, M, Q, L, O, N, T], [Q, L, S, T, K], [K, T, P, L, M, K, N, O], [K, S, D, M, Q, N, T], [S, T, K, P, M, L, O, N], [C, M, N, T, Q, S], [T, L, Q, M, O, N, S, K], [Q, S, T, Q, L, G, N, T, K], [S, K, F, T], [M, L, F, S, T, K], [K, S, L, M, R, T], [S, M, L, P, O, F, N, T, H], [B, G, L, M, B, R, O, K, S], [K, S, L, M, O, M, R, N, T], [S, K, M, T], [T, Q, K, M, T], [K, S, M, L, P, T], [T, S, K, T, P, O, F, Q, R, N], [T, K, S, L, M, O, N, R], [F, T, M, D, R, O, L, N, S], [K, T, Q, J, T, B], [J, Q, T, K, S, G], [P, M, T, P, N, T, S, K], [K, T, M, L, R, O, N, S], [Q, L, T, C, K, T], [S, K, L, T], [T, L, M, O, N, S, K], [K, S, H, H, A, R, O, N, T], [M, R, O, B, L, N, S, K, T], [T, M, A, Q, K, B], [K, T, O, M, K, R, S], [C, M, O, L, R, N, S, T, K], [A, M, T, S, K], [O, O, P, N, L, M, S, B], [K, M, H, O, N, N, P, S, E], [K, M, S, N, I, T], [S, M, P, K, E], [F, K, K, S, L], [O, L, M, L, N, E, S, T, K], [T, S, K, M, L, R, O, N], [S, L, T, K, O, R, H, A], [T, L, O, N, P, M, N, D, K, K], [T, R, K, S, M, O, L, N, Q], [K, L, B, M, O, N, R, T, S], [K, I, O, N, J, P, K, F, T], [S, K, T, A, M, O, L, P, N], [P, L, P, O, F, N, K, T, S], [K, S, N, D, L, O, M, Q, S, N], [K, T, L, M, B, C, R, N, S], [O, E, M, L, P, O, N, S, T], [F, L, T, L, S], [S, K, O, O, T, C], [P, M, F, O, M, K, K, T, S, S, E], [T, K, Q, Q, M, L, Q, N], [S, T, J, K], [K, T, S, S, L, M, R, H, O, N], [L, M, O, A, N, K, T, S], [K, M, Q, O, Q, N, L, T, S], [K, M, P, L, T, S, F], [K, T, S, D, L, M, O, Q, N, G], [T, S, R, L, K], [T, O, P, M, O, N, K, H], [K, H, S, T, M, O, R, L, N], [L, M, S, R, O, N, T, S, F, K], [T, S, K, T, L, M, O, R, N], [S, R, K, T, T], [D, K, M, S, C, P, R, L, T], [G, T, O, L, K, N, C, H, S, E, K], [S, T, M, Q, E, K], [P, B, M, H, T, K, S], [A, T, M, T, S], [M, N, L, J, O, N, T, S, K], [J, M, L, O, G, R, N, M, K, T], [E, M, T, K, N, J], [M, O, L, Q, N, S, K, T], [I, S, L, K], [S, M, L, M, T], [R, L, I, O, N, T, K, S], [N, K, S, T, M], [Q, L, M, S, C, K, T], [T, M, R, L, K, S], [K, S, N, T, L], [S, T, K, M], [Q, K, S, M, P, O, L, N, T], [K, R, M, O, E, N, T, R, S], [L, M, K, T, S], [T, K, S, H, O, M, R, N, L], [R, K, T, S, M, R, O, D, L], [S, P, O, O, M, N, K, T], [M, P, M, O, N, L, K, S, T], [P, S, T, D, K, T, L, R], [S, M, T, O, N, K, E], [Q, M, L, C, N, I, K, O, T, J, S], [L, M, S, T, K], [K, L, M, Q, S, T], [K, S, L, O, N, P, M, R], [S, K, T, E, P], [S, L, O, M, P, N, K, T], [K, T, S, Q, O, L, E, M, N], [T, M, G, B, O, N, S, Q, E, G, K], [M, L, Q, O, N, R, T, S, D], [G, S, K, M, L, O, N, R], [S, K, L, T, M], [M, L, R, K, T, S], [K, L, M, L, P, O, N, S, L], [K, S, T, S, L, M, B, P, P], [K, M, N, N, L, Q, S, F, N], [M, Q, T, K, S], [T, L, Q, K, M, A, N, S, T, K], [I, K, T, N, M, L, O, N, R], [S, M, K, T], [O, P, H, L, O, N, T, S, T], [Q, R, O, M, G, N, K, S, T], [H, K, S, T, M, Q, L, O, N], [T, K, S, M, O, P, L, F, E], [L, M, Q, O, N, F, K, S], [R, S, T, K, Q, Q, M, O, N], [T, A, K, O, N, S], [T, K, S, O, M, L, N, R], [K, N, N, M, P, O, L, N], [T, K, S, J, M, L, O, N, P], [L, M, Q, O, N, S, K, T], [L, M, R, P, K, T], [M, P, K, T, H], [K, G, T, R, M, N, L, N, R], [S, L, B, N, K, T], [R, L, G, O, M, O, N, T, K, I, S], [M, O, R, L, N, S, M, T], [S, M, L, K, L, Q, M, L, O, N], [T, K, L, E, M, L, F, S, E, K], [T, L, S, S, K], [B, S, M, L, A, Q, O, N, T, K, D], [B, K, J, S, T, J, R], [K, L, L, O, N, S, T], [K, H, S, T, T, C, L, M, F, M], [K, T, Q, L, M, O, S, N, M, S], [L, O, N, M, K, S, T], [S, T, N, K, Q, O, P, N, L], [T, N, Q, L, M, S, K], [M, S, S, K, T], [T, S, K, L], [K, J, S, H, F, S, M, Q, L, O, N], [S, K, S, D, R], [K, S, T, T, L, D, G, O, N, R], [K, K, M, L, O, N, P, G, T, S]] "
				.split("], ");

		TIntIntMap modelMoveCost = new TIntIntHashMap(10);
		for (int i = 0; i < tree.size(); i++) {
			if (tree.getTypeFast(i) >= 0 && tree.getTypeFast(i) != NAryTree.TAU) {
				modelMoveCost.put(i, 1);
			} else {
				modelMoveCost.put(i, 0);
			}
		}

		System.out.println(TreeUtils.toString(tree));
		System.out.println("MaxDepth = " + getMaxDepth(tree));
		StateBuilder sb = new StateBuilder(tree, 0);
		sb.setPushDownUnderAND(true);

		//		StateSpace statespace = sb.buildStateSpace(true);

		//		OutputStreamWriter out = new FileWriter(new File("d:\\temp\\onlineconformance\\statespace.dot"));
		//		TreeUtils.writeBehaviorToDot(out, statespace, sb);
		//		out.close();

		//		Simulator sim = new Simulator(tree, 0, new Random(34533));
		//		String[][] traces = getTraces(sim, 10, activities, 0.3);

		Random logRandom = new Random(345343);
		Simulator sim = new Simulator(tree, 0, logRandom);
		String[][] traces = getTraces(sim, 2, activities, 0);

		//		String[][] traces = new String[trs.length][];
		//		Random logRandom = new Random(345343);
		//		Simulator sim = new Simulator(tree, 0, logRandom);
		//		 traces = getTraces(sim, 10, activities, 0.3);
		//		String trace = "[G, C, E, D, F, C, C, G, G, A, B, F, F, E, B, C, C, C, B, C, F, G, A, A, E, A, E, C, C, G, A, E, A, D, F, C, B, B, C, B, G, D, C, G, D, F, E, A, E, G, D, E, B, A, C, A, D, F, B, A, A, A, D, A, A, D, E, B, F, G, C, D, C, A, G, D, C, G, E, F, F, E, C, E, A, B, C, G, D, B, F, A, E, B, C, A, G, A, A, B, F, G, E, G, G, G, C, D, D, G, F, A, G, E, B, B, G, C, C, G, F, A, A, A, E, A, C, A, C, E, D, F, A, G, F, F, F, G, G, A, A, G, B, A, B, D]  ";
		//		for (int i = 0; i < trs.length; i++) {
		//			trace = trs[i].replaceAll("[\\[\\]]", "").trim();
		//			traces[i] = trace.split("[, ]+");
		//			if (traces[i].length == 1 && traces[i][0].equals("")) {
		//				traces[i] = new String[0];
		//			}
		//		}
		//		for (int i = 0; i < traces.length; i++) {
		//			System.out.println("Max Queue trace " + i + ": " + getQueueBound(tree, traces[i], traces[i].length, map));
		//		}

		//		OfflineAlignmentCalculator calculator = new OfflineAlignmentCalculator(tree, activities, traces, 30.0,
		//				logMoveCost, modelMoveCost, false);

		int maxDepth = 1; //getMaxDepth(tree);
		int maxQueueSize = 5;// statespace.size();

		AbstractNAryTreeOracle<?, ?> oracle = createOracle(keepHistory, true, tree, 0, modelMoveCost, maxDepth);

		double offLineTime = 0;
		double onLineTime = 0;

		for (int tr = 0; tr < traces.length; tr++) {
			if (traces[tr].length == 0) {
				continue;
			}

			//			long replayerStart = System.nanoTime();
			//			Trace traceForReplayer = calculator.getTrace(traces[tr]);
			//			TreeRecord record = calculator.replayTrace(traceForReplayer);
			//			long replayerEnd = System.nanoTime();
			//			NAryTreeHistoryAwareMovementSequence<?> realAlignment = calculator
			//					.createMovements(record, traceForReplayer);
			//
			//			System.out.print("Offline : ");
			//			StringBuilder builder = new StringBuilder();
			//			printAlignment(builder, realAlignment, activities);
			//			System.out.println(builder.toString());
			//			System.out.print(" c:" + realAlignment.getTotalCost() / SCALING);
			//			System.out.print(" r:" + calculator.wasReliable());
			//			System.out.println(" t:" + (replayerEnd - replayerStart) / 1000000000.0 + " s");
			//			offLineTime += (replayerEnd - replayerStart) / 1.0E9;
			//
			//			List<Move<NAryTreeLabel, NAryTreeNode>> movementSequence = realAlignment.getMovementSequence();
			//
			StateBuilder stateBuilder = new StateBuilder(tree, 0, true);
			//			if (calculator.wasReliable()) {
			//				testAlignmentLog(movementSequence, traces[tr], map);
			//				testAlignmentModel(movementSequence, stateBuilder);
			//			}

			OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> replayer;
			replayer = createReplayer(keepHistory, logMoveCost, maxQueueSize, oracle);

			List<?> queue = replayer.getCurrentStates();
			//			for (Map.Entry<NAryTreeState, Integer> entry : queue.entrySet()) {
			//				System.out.print("(" + sb.toString(entry.getKey().getState()) + "," + entry.getValue() + "), ");
			//			}
			//			System.out.println();

			Entry<? extends NAryTreeState, Integer> alignment = null;
			long start = System.nanoTime();
			long mid = start;
			int maxDepthNeeded = 0;
			for (int i = 0; i <= traces[tr].length; i++) {

				oracle.resetHitMiss();
				if (i == traces[tr].length) {
					mid = System.nanoTime();
					//					for (Object a : queue) {
					//						System.out.println("(p:"
					//								+ getMaxQueueTrace((Map.Entry<? extends NAryTreeState, Integer>) a, keepHistory)
					//								+ ",c:" + ((Map.Entry<? extends NAryTreeState, Integer>) a).getValue() + ")");
					//					}
					replayer.update(null);
					//					System.out.print("-: ");
				} else {
					replayer.update(labels[map.get(traces[tr][i])]);
					//					System.out.print(traces[tr][i] + ": ");
				}
				queue = replayer.getCurrentStates();
				alignment = (Map.Entry<? extends NAryTreeState, Integer>) queue.iterator().next();
				maxDepthNeeded = Math.max(maxDepthNeeded, oracle.getDepth());

				//				System.out.print(" P:" + getMaxQueueTrace(alignment, keepHistory));
				//				if (keepHistory) {
				//					builder = new StringBuilder();
				//					printAlignment(stateBuilder, (NAryTreeHistoryAwareState) alignment.getKey(), builder, false,
				//							activities);
				//					System.out.print(builder.toString());
				//				}
				//				System.out.println();

				//				if (!keepHistory) {
				//					NAryTreeSimpleState key = (NAryTreeSimpleState) alignment.getKey();
				//					System.out.print(stateBuilder.toString(key.getState()) + ", ");
				//				}

			}
			long end = System.nanoTime();
			//			System.out.println();
			System.out.println("Max depth needed: " + maxDepthNeeded);
			System.out.println();
			onLineTime += (end - start) / 1.0E9;
			alignment = (Map.Entry<? extends NAryTreeState, Integer>) queue.iterator().next();

			StringBuilder builder = new StringBuilder();
			if (keepHistory) {
				System.out.print("Online  : ");
				printAlignment(stateBuilder, (NAryTreeHistoryAwareState) alignment.getKey(), builder, false, activities);
				System.out.println(builder.toString());
			} else {
				NAryTreeSimpleState key = (NAryTreeSimpleState) alignment.getKey();
				System.out.println(stateBuilder.toString(key.getState()) + ", ");
				System.out.println("Online  : ");
			}
			System.out.print(" c:" + alignment.getValue() / SCALING);
			System.out.print(" p:" + getMaxQueueTrace(alignment, keepHistory));
			System.out.println(" t:" + (end - start) / 1000000000.0 + " s");
			System.out.println(" t':" + (mid - start) / 1000000000.0 + " s");
			System.out.println("Reached final state with cost: " + alignment.getValue());
			System.out.println("-------------------------------------");

			//			if (realAlignment.getTotalCost() / SCALING != alignment.getValue() / SCALING) {
			//				System.err.println(Arrays.toString(traces[tr]));
			//				System.in.read();
			//			}
			//			if (keepHistory) {
			//				testAlignmentLog((NAryTreeHistoryAwareState) alignment.getKey(), traces[tr], map);
			//				testAlignmentModel((NAryTreeHistoryAwareState) alignment.getKey(), stateBuilder);
			//			}

		}
		//		calculator.deleteLPs();

		//		System.out.println("Number states: " + statespace.size());
		//		System.out.println("Number edges: " + statespace.numEdges());
		System.out.println(" t online: " + onLineTime + " s");
		System.out.println(" t offline:" + offLineTime + " s");

	}

	public int getQueueBound(NAryTree tree, String[] trace, int l, TObjectShortMap<String> map) {
		if (l == 0) {
			return 1;
		}
		l--;
		int result = (tree.countNodes(map.get(trace[l])) + 1);
		result *= getQueueBound(tree, trace, l, map);
		return result;
	}

	public static int log2(int n) {
		return 31 - Integer.numberOfLeadingZeros(n);
	}

	public static int log(int n, double base) {
		return (int) Math.ceil((Math.log(n) / Math.log(base)));
	}

	public void doMaxDepthExperiment(int treeOffset) {
		String[] activities = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
				"P", "Q", "R", "S", "T" };
		NAryTreeLabel[] labels = new NAryTreeLabel[activities.length];

		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
		// initialize a tree with all node costs 5 for leafs and 0 otherwise.
		for (short i = 0; i < activities.length; i++) {
			map.put(activities[i], i);
			labels[i] = new NAryTreeLabel(i);
		}

		Random treeRandom = new Random(345653);
		Random doubleLabelRandom = new Random(23423);

		for (int test = 0; test < numTrees; test++) {
			NAryTree tree = TreeUtils.randomTree(map.keySet().size(), 0.9, test / 5 + treeOffset, test / 5 + 30
					+ treeOffset, treeRandom);
			tree = TreeUtils.flatten(tree);

			TShortSet labelSet = new TShortHashSet();
			labelSet.addAll(map.values());
			TShortIterator it = labelSet.iterator();
			short newLabel = it.next();
			for (int i = 0; i < tree.size(); i++) {
				if (tree.isLeaf(i)) {
					tree.setType(i, newLabel);
					if (doubleLabelRandom.nextDouble() > 0.1) {
						newLabel = it.next();
					} else {
						newLabel -= 4;
						if (newLabel < 0) {
							newLabel = NAryTree.TAU;
						}
					}
				}
				if (!it.hasNext()) {
					labelSet.addAll(map.values());
					it = labelSet.iterator();
				}
			}

			/* IF YOU NEED TO SKIP TREES, DO IT HERE: */

			if (test + 1 < 8) {
				// skip this tree and continue with the next
				continue;
			}
			int maxDepthPredicted = getMaxDepth(tree);
			System.out.println("Tree: t" + (test + 1) + " maxDepth: " + maxDepthPredicted);
		}

	}

	public int doFullExperiment2(String folder, boolean keepHistory, int noisePar, int numNoise, int fixedLogMoveCost,
			int fixedModelMoveCost, int treeOffset, int treeNumber) throws Exception {

		String[] activities = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
				"P", "Q", "R", "S", "T" };
		NAryTreeLabel[] labels = new NAryTreeLabel[activities.length];

		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
		// initialize a tree with all node costs 5 for leafs and 0 otherwise.
		for (short i = 0; i < activities.length; i++) {
			map.put(activities[i], i);
			labels[i] = new NAryTreeLabel(i);
		}

		TObjectIntMap<NAryTreeLabel> logMoveCost = new TObjectIntHashMap<>(10);
		for (NAryTreeLabel lm : labels) {
			logMoveCost.put(lm, fixedLogMoveCost);
		}

		long startTest = System.currentTimeMillis();

		int[] treeStats = new int[numTrees * 2];
		int totalOffline = (noisePar < 0 ? numNoise + 1 : 1) * numTrees;
		int testOffline = 0;

		Random treeRandom = new Random(345653);
		Random doubleLabelRandom = new Random(23423);
		boolean useCache = true;
		double base = 1.5;

		for (int test = 0; test < numTrees; test++) {
			treeNumber++;
			NAryTree tree = TreeUtils.randomTree(map.keySet().size(), 0.9, test / 5 + treeOffset, test / 5 + 30
					+ treeOffset, treeRandom);
			tree = TreeUtils.flatten(tree);

			TShortSet labelSet = new TShortHashSet();
			labelSet.addAll(map.values());
			TShortIterator it = labelSet.iterator();
			short newLabel = it.next();
			for (int i = 0; i < tree.size(); i++) {
				if (tree.isLeaf(i)) {
					tree.setType(i, newLabel);
					if (doubleLabelRandom.nextDouble() > 0.1) {
						newLabel = it.next();
					} else {
						newLabel -= 4;
						if (newLabel < 0) {
							newLabel = NAryTree.TAU;
						}
					}
				}
				if (!it.hasNext()) {
					labelSet.addAll(map.values());
					it = labelSet.iterator();
				}
			}

			/* IF YOU NEED TO SKIP TREES, DO IT HERE: */

			//			if (treeNumber < 29) {
			//				// skip this tree and continue with the next
			//				testOffline += numNoise;
			//				continue;
			//			}
			int maxDepthPredicted = getMaxDepth(tree);

			StateBuilder sb = new StateBuilder(tree, 0, true);
			sb.setPushDownUnderAND(false);

			boolean[] b = new boolean[tree.size()];
			boolean[] h = new boolean[tree.size()];
			Configuration c = new Configuration(b, h);
			tree.addConfiguration(c);
			File treeFile = new File(folder + "/tree t" + treeNumber + ".dot");
			if (!treeFile.exists()) {
				OutputStreamWriter dotFile = new FileWriter(treeFile);
				TreeUtils.writeTreeToDot(tree, 0, dotFile, activities);
				dotFile.close();

				StateSpace stateSpace = sb.buildStateSpace(true);
				int nodes = countNodes(tree, stateSpace);
				int edges = stateSpace.numEdges();
				treeStats[2 * treeNumber - 2] = nodes;
				treeStats[2 * treeNumber - 1] = edges;
				System.out.println("Statespace of tree " + treeNumber + " nodes:" + nodes + " edges: " + edges);
				System.out.println("Maxdepth " + maxDepthPredicted + " / "
						+ (tree.numLeafs() - tree.countNodes(NAryTree.TAU) - 1));

			} else {
				// skip this tree and continue with the next
				testOffline += numNoise;
				continue;
			}

			TIntIntMap modelMoveCost = new TIntIntHashMap(10);
			for (int i = 0; i < tree.size(); i++) {
				if (tree.getTypeFast(i) >= 0 && tree.getTypeFast(i) != NAryTree.TAU) {
					modelMoveCost.put(i, fixedModelMoveCost);
				} else {
					modelMoveCost.put(i, 0);
				}
			}

			for (int noise = numNoise; noise >= 0; noise--) {
				testOffline++;
				if (noisePar >= 0) {
					noise = noisePar;
				}

				double nl = noise;
				nl /= numNoise;

				Random logRandom = new Random(345343);
				Simulator sim = new Simulator(tree, 0, logRandom);

				String[][] traces = getTraces(sim, numTraces, activities, nl);

				long[] offlineTimes = new long[traces.length];

				System.out.print(new Date());

				int maxQueue = treeStats[2 * treeNumber - 2];
				int maxDepth = maxDepthPredicted;//(tree.numLeafs() - tree.countNodes(NAryTree.TAU) - 1);
				int totalOnline = log(maxDepth, base) * log(maxQueue, base);

				String nameBase = " n" + noise + " m" + (test / 5 + treeOffset) + "-" + (test / 5 + 30 + treeOffset)
						+ " t" + treeNumber;
				System.out.println(" starting offline: " + testOffline + "/" + totalOffline + " - 0/" + totalOnline
						+ nameBase);
				int testOnline = 0;

				RealReplayResult[] realAlignments = new RealReplayResult[traces.length];

				OfflineAlignmentCalculator calculator = new OfflineAlignmentCalculator(tree, activities, traces, 30.0,
						logMoveCost, modelMoveCost, true);
				for (int tr = 0; tr < traces.length; tr++) {

					Trace traceForReplayer = calculator.getTrace(traces[tr]);
					long replayerStart, replayerEnd;

					replayerStart = System.nanoTime();
					TreeRecord record = calculator.replayTrace(traceForReplayer);
					replayerEnd = System.nanoTime();

					// store the alignment in the first experiment, or in a later one if we find
					// a reliable result where previously we did not.
					realAlignments[tr] = new RealReplayResult(calculator.createMovements(record, traceForReplayer),
							calculator.wasReliable(), calculator.getCacheSize());

					offlineTimes[tr] = (replayerEnd - replayerStart);
				}
				calculator.deleteLPs();

				//				for (int depth = (numDepth - 1) * ds; depth >= 0; depth -= ds) {
				int depth = -1;
				int maxDepthOverall = 0;
				int dCost = Integer.MAX_VALUE;
				boolean stopDepth = false;
				while (!stopDepth) {
					//				depthLoop: for (int d = 0; depth < maxDepth; d++) {

					if (dCost == 0 && maxDepthOverall < depth) {
						// we're done with all queue sizes at this depth. Stop if dCost is zero and we 
						// didn't need more depth in the last queue.
						// We do one more loop with maxDepthOverall.
						depth = maxDepthOverall;
						stopDepth = true;
					} else {
						if (depth == -1) {
							depth = 0;
						} else {
							depth = (int) Math.max(1, Math.ceil(depth * base));
						}
						if (depth > maxDepth) {
							depth = maxDepth;
							stopDepth = true;
						}
					}

					//					for (int queueSize = (numQueue - 1) * qs + 1; queueSize > 0; queueSize -= qs) {
					//int queueSize = 1;
					int maxQueueNeeded = 0;
					int queueSize = 0;
					dCost = Integer.MAX_VALUE;
					//while (queueSize < maxQueueNeeded) {
					maxDepthOverall = 0;
					boolean stopQueue = false;
					while (!stopQueue) {
						if (dCost == 0 && queueSize > maxQueueNeeded) {
							// we found optimal queue. 
							// do one more loop with maxQueueNeeded
							stopQueue = true;
							queueSize = maxQueueNeeded;
						} else {
							queueSize = (int) Math.max(1, Math.ceil(queueSize * base));
							if (queueSize > maxQueue) {
								queueSize = maxQueue;
								stopQueue = true;
							}

						}

						String name = nameBase + " c" + (useCache ? "1" : "0") + " d" + depth + " q" + queueSize;
						Writer out = createFile(folder + "/statistics" + name + " mid.csv", false);
						testOnline++;
						if (out == null) {
							continue;
						}
						Writer outFinal = createFile(folder + "/statistics" + name + " fin.csv", false);

						System.out.print(new Date());
						System.out.println(" starting online: " + testOffline + "/" + totalOffline + " - " + testOnline
								+ "/" + totalOnline + name);

						long[][] onlineTimes = new long[traces.length][];
						dCost = 0;
						maxQueueNeeded = 0;

						writeHeader(out, outFinal);

						AbstractNAryTreeOracle<?, ?> oracle;
						OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> replayer;

						oracle = createOracle(keepHistory, useCache, tree, 0, modelMoveCost, depth);

						for (int tr = 0; tr < traces.length; tr++) {
							int maxQueueTrace = 0;
							onlineTimes[tr] = new long[traces[tr].length + 1];
							long cumulativeTime = 0;
							String[] trace = new String[traces[tr].length + 1];
							System.arraycopy(traces[tr], 0, trace, 0, traces[tr].length);

							//							replayer = new OracleBasedReplayerWithHistory<>(oracle, queueSize, logMoveCost);
							replayer = createReplayer(keepHistory, logMoveCost, queueSize, oracle);

							int raIndex = 0;
							int raCost = 0;
							int maxDepthNeeded = 0;

							NAryTreeHistoryAwareMovementSequence<?> realAlignment = realAlignments[tr].moves;

							for (int i = 0; i < trace.length; i++) {

								while (raIndex < realAlignment.size()) {
									raCost += realAlignment.get(raIndex).getCost() / SCALING;
									if (realAlignment.get(raIndex).getEventLabel() != null) {
										raIndex++;
										break;
									}
									raIndex++;
								}

								oracle.resetHitMiss();
								long start = System.nanoTime();
								if (trace[i] == null) {
									replayer.update(null);
								} else {
									replayer.update(labels[map.get(trace[i])]);
								}
								long end = System.nanoTime();
								//System.gc();

								//								Map<NAryTreeState, Integer> queue = replayer.getCurrentStates();
								//								System.out.print(queue.size() + ": ");
								//								for (Map.Entry<NAryTreeState, Integer> entry : queue.entrySet()) {
								//									System.out.print("(" + sb.toString(entry.getKey().getState()) + ","
								//											+ entry.getValue() + "), ");
								//								}
								//								System.out.println();

								long eventTime = end - start;
								cumulativeTime += eventTime;

								long offLineTime = offlineTimes[tr];

								if (out != null) {
									if (i == trace.length - 1) {
										Entry<? extends NAryTreeState, Integer> partialAlignment = replayer
												.getCurrentStates().get(0);
										int paCost = partialAlignment.getValue() / SCALING;

										maxQueueTrace = getMaxQueueTrace(partialAlignment, keepHistory);
										for (int a = 1; a < replayer.getCurrentStates().size(); a++) {
											if (replayer.getCurrentStates().get(a).getValue() > paCost) {
												break;
											}
											int mq = getMaxQueueTrace(replayer.getCurrentStates().get(a), keepHistory);
											if (mq < maxQueueTrace) {
												maxQueueTrace = mq;
											}
										}

										writeStats(depth, depth / (double) maxDepth, queueSize, queueSize
												/ (double) maxQueue, realAlignments[tr], realAlignments[0],
												offLineTime, noise, treeNumber, useCache, tree,
												treeStats[2 * treeNumber - 2], treeStats[2 * treeNumber - 1], traces,
												oracle, tr, cumulativeTime, trace, raCost, i, partialAlignment,
												maxDepthNeeded, maxQueueTrace, eventTime, outFinal, fixedLogMoveCost);

										if (realAlignments[tr].reliable) {
											// ignore cost differences in unreliable results.
											dCost += Math.abs(paCost - raCost);
										}
										if (maxQueueTrace > maxQueueNeeded) {
											maxQueueNeeded = maxQueueTrace;
										}
										if (maxDepthNeeded > maxDepthOverall) {
											maxDepthOverall = maxDepthNeeded;
										}
									} else {
										if (oracle.getDepth() > maxDepthNeeded) {
											maxDepthNeeded = oracle.getDepth();
										}

										Entry<? extends NAryTreeState, Integer> partialAlignment = replayer
												.getCurrentStates().get(0);
										writeStats(depth, depth / (double) maxDepth, queueSize, queueSize
												/ (double) maxQueue, realAlignments[tr], realAlignments[0],
												offLineTime, noise, treeNumber, useCache, tree,
												treeStats[2 * treeNumber - 2], treeStats[2 * treeNumber - 1], traces,
												oracle, tr, cumulativeTime, trace, raCost, i, partialAlignment,
												oracle.getDepth(), replayer.getCurrentStates().size(), eventTime, out,
												fixedLogMoveCost);
									}

								}

							}

							//								System.out.println();

							//								System.out.print(realAlignment);
							//								System.out.println(" c:" + raCost);
							//								StringBuilder builder = new StringBuilder();
							//								printAlignment(stateBuilder, alignment.getKey(), builder, false, activities);
							//								System.out.println(builder.toString());
							//								System.out.println("Reached final state with cost: " + alignment.getValue());
							//								System.out.println("-------------------------------------");

							if (keepHistory) {
								Entry<? extends NAryTreeState, Integer> alignment = replayer.getCurrentStates().get(0);
								StateBuilder stateBuilder = new StateBuilder(tree, 0, true);
								testAlignmentLog((NAryTreeHistoryAwareState) alignment.getKey(), trace, map);
								testAlignmentModel((NAryTreeHistoryAwareState) alignment.getKey(), stateBuilder);
							}
						}

						if (out != null) {
							out.close();
							outFinal.close();
						}
					}// queueLoop

				}// depthloop
				if (dCost != 0) {
					System.err.println(tree.toString());
					System.err.println(Arrays.deepToString(traces));
					System.err.flush();
				}
				if (noisePar >= 0) {
					break;
				}
			}
		}
		long endTest = System.currentTimeMillis();
		System.out.println("Test time: " + (endTest - startTest) / 1000.0 + " seconds");
		return treeNumber;
	}

	public int findMaxQueue(String folder, boolean keepHistory, int noisePar, int numNoise, int fixedLogMoveCost,
			int fixedModelMoveCost, int treeOffset, int treeNumber) throws Exception {

		String[] activities = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
				"P", "Q", "R", "S", "T" };
		NAryTreeLabel[] labels = new NAryTreeLabel[activities.length];

		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
		// initialize a tree with all node costs 5 for leafs and 0 otherwise.
		for (short i = 0; i < activities.length; i++) {
			map.put(activities[i], i);
			labels[i] = new NAryTreeLabel(i);
		}

		TObjectIntMap<NAryTreeLabel> logMoveCost = new TObjectIntHashMap<>(10);
		for (NAryTreeLabel lm : labels) {
			logMoveCost.put(lm, fixedLogMoveCost);
		}

		long startTest = System.currentTimeMillis();

		int[] treeStats = new int[numTrees * 2];
		int totalOffline = (noisePar < 0 ? numNoise + 1 : 1) * numTrees;
		int testOffline = 0;

		Random treeRandom = new Random(345653);
		Random doubleLabelRandom = new Random(23423);
		boolean useCache = true;
		double base = 1.5;

		for (int test = 0; test < numTrees; test++) {
			treeNumber++;
			NAryTree tree = TreeUtils.randomTree(map.keySet().size(), 0.9, test / 5 + treeOffset, test / 5 + 30
					+ treeOffset, treeRandom);
			tree = TreeUtils.flatten(tree);

			TShortSet labelSet = new TShortHashSet();
			labelSet.addAll(map.values());
			TShortIterator it = labelSet.iterator();
			short newLabel = it.next();
			for (int i = 0; i < tree.size(); i++) {
				if (tree.isLeaf(i)) {
					tree.setType(i, newLabel);
					if (doubleLabelRandom.nextDouble() > 0.1) {
						newLabel = it.next();
					} else {
						newLabel -= 4;
						if (newLabel < 0) {
							newLabel = NAryTree.TAU;
						}
					}
				}
				if (!it.hasNext()) {
					labelSet.addAll(map.values());
					it = labelSet.iterator();
				}
			}

			/* IF YOU NEED TO SKIP TREES, DO IT HERE: */

			//			if (treeNumber < 29) {
			//				// skip this tree and continue with the next
			//				testOffline += numNoise;
			//				continue;
			//			}
			int maxDepthPredicted = getMaxDepth(tree);

			StateBuilder sb = new StateBuilder(tree, 0, true);
			sb.setPushDownUnderAND(false);

			boolean[] b = new boolean[tree.size()];
			boolean[] h = new boolean[tree.size()];
			Configuration c = new Configuration(b, h);
			tree.addConfiguration(c);
			File treeFile = new File(folder + "/tree t" + treeNumber + ".dot");
			if (!treeFile.exists()) {
				OutputStreamWriter dotFile = new FileWriter(treeFile);
				TreeUtils.writeTreeToDot(tree, 0, dotFile, activities);
				dotFile.close();

				StateSpace stateSpace = sb.buildStateSpace(true);
				int nodes = countNodes(tree, stateSpace);
				int edges = stateSpace.numEdges();
				treeStats[2 * treeNumber - 2] = nodes;
				treeStats[2 * treeNumber - 1] = edges;
				System.out.println("Statespace of tree " + treeNumber + " nodes:" + nodes + " edges: " + edges);
				System.out.println("Maxdepth " + maxDepthPredicted + " / "
						+ (tree.numLeafs() - tree.countNodes(NAryTree.TAU) - 1));

			} else {
				// skip this tree and continue with the next
				testOffline += numNoise;
				continue;
			}

			TIntIntMap modelMoveCost = new TIntIntHashMap(10);
			for (int i = 0; i < tree.size(); i++) {
				if (tree.getTypeFast(i) >= 0 && tree.getTypeFast(i) != NAryTree.TAU) {
					modelMoveCost.put(i, fixedModelMoveCost);
				} else {
					modelMoveCost.put(i, 0);
				}
			}

			for (int noise = numNoise; noise >= 0; noise--) {
				testOffline++;
				if (noisePar >= 0) {
					noise = noisePar;
				}

				double nl = noise;
				nl /= numNoise;

				Random logRandom = new Random(345343);
				Simulator sim = new Simulator(tree, 0, logRandom);

				String[][] traces = getTraces(sim, numTraces, activities, nl);

				long[] offlineTimes = new long[traces.length];

				System.out.print(new Date());

				int maxQueue = treeStats[2 * treeNumber - 2];
				int maxDepth = maxDepthPredicted;//(tree.numLeafs() - tree.countNodes(NAryTree.TAU) - 1);
				int totalOnline = log(maxDepth, base) * log(maxQueue, base);

				String nameBase = " n" + noise + " m" + (test / 5 + treeOffset) + "-" + (test / 5 + 30 + treeOffset)
						+ " t" + treeNumber;
				System.out.println(" starting offline: " + testOffline + "/" + totalOffline + " - 0/" + totalOnline
						+ nameBase);
				int testOnline = 0;

				RealReplayResult[] realAlignments = new RealReplayResult[traces.length];

				OfflineAlignmentCalculator calculator = new OfflineAlignmentCalculator(tree, activities, traces, 30.0,
						logMoveCost, modelMoveCost, true);
				for (int tr = 0; tr < traces.length; tr++) {

					Trace traceForReplayer = calculator.getTrace(traces[tr]);
					long replayerStart, replayerEnd;

					replayerStart = System.nanoTime();
					TreeRecord record = calculator.replayTrace(traceForReplayer);
					replayerEnd = System.nanoTime();

					// store the alignment in the first experiment, or in a later one if we find
					// a reliable result where previously we did not.
					realAlignments[tr] = new RealReplayResult(calculator.createMovements(record, traceForReplayer),
							calculator.wasReliable(), calculator.getCacheSize());

					offlineTimes[tr] = (replayerEnd - replayerStart);
				}
				calculator.deleteLPs();

				//				for (int depth = (numDepth - 1) * ds; depth >= 0; depth -= ds) {
				int depth = maxDepth;
				int dCost = Integer.MAX_VALUE;

				int queueSize = treeStats[2 * treeNumber - 2];
				int maxQueueNeeded = treeStats[2 * treeNumber - 2];

				boolean stopQueue = false;
				while (!stopQueue) {
					if (queueSize > maxQueueNeeded) {
						// we found optimal queue. 
						// do one more loop with maxQueueNeeded
						stopQueue = true;
						queueSize = maxQueueNeeded;
					}
					maxQueueNeeded = 0;

					String name = nameBase + " c" + (useCache ? "1" : "0") + " d" + depth + " q" + queueSize;
					Writer out = createFile(folder + "/statistics" + name + " mid.csv", false);
					testOnline++;
					if (out == null) {
						continue;
					}
					Writer outFinal = createFile(folder + "/statistics" + name + " fin.csv", false);

					System.out.print(new Date());
					System.out.println(" starting online: " + testOffline + "/" + totalOffline + " - " + testOnline
							+ "/" + totalOnline + name);

					long[][] onlineTimes = new long[traces.length][];
					dCost = 0;
					maxQueueNeeded = 0;

					writeHeader(out, outFinal);

					AbstractNAryTreeOracle<?, ?> oracle;
					OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> replayer;

					oracle = createOracle(keepHistory, useCache, tree, 0, modelMoveCost, depth);

					for (int tr = 0; tr < traces.length; tr++) {
						int maxQueueTrace = 0;
						onlineTimes[tr] = new long[traces[tr].length + 1];
						long cumulativeTime = 0;
						String[] trace = new String[traces[tr].length + 1];
						System.arraycopy(traces[tr], 0, trace, 0, traces[tr].length);

						//							replayer = new OracleBasedReplayerWithHistory<>(oracle, queueSize, logMoveCost);
						replayer = createReplayer(keepHistory, logMoveCost, queueSize, oracle);

						int raIndex = 0;
						int raCost = 0;
						int maxDepthNeeded = 0;

						NAryTreeHistoryAwareMovementSequence<?> realAlignment = realAlignments[tr].moves;

						for (int i = 0; i < trace.length; i++) {

							while (raIndex < realAlignment.size()) {
								raCost += realAlignment.get(raIndex).getCost() / SCALING;
								if (realAlignment.get(raIndex).getEventLabel() != null) {
									raIndex++;
									break;
								}
								raIndex++;
							}

							oracle.resetHitMiss();
							long start = System.nanoTime();
							if (trace[i] == null) {
								replayer.update(null);
							} else {
								replayer.update(labels[map.get(trace[i])]);
							}
							long end = System.nanoTime();
							//System.gc();

							//								Map<NAryTreeState, Integer> queue = replayer.getCurrentStates();
							//								System.out.print(queue.size() + ": ");
							//								for (Map.Entry<NAryTreeState, Integer> entry : queue.entrySet()) {
							//									System.out.print("(" + sb.toString(entry.getKey().getState()) + ","
							//											+ entry.getValue() + "), ");
							//								}
							//								System.out.println();

							long eventTime = end - start;
							cumulativeTime += eventTime;

							long offLineTime = offlineTimes[tr];

							if (out != null) {
								if (i == trace.length - 1) {
									Entry<? extends NAryTreeState, Integer> partialAlignment = replayer
											.getCurrentStates().get(0);
									int paCost = partialAlignment.getValue() / SCALING;

									maxQueueTrace = getMaxQueueTrace(partialAlignment, keepHistory);
									for (int a = 1; a < replayer.getCurrentStates().size(); a++) {
										if (replayer.getCurrentStates().get(a).getValue() > paCost) {
											break;
										}
										int mq = getMaxQueueTrace(replayer.getCurrentStates().get(a), keepHistory);
										if (mq < maxQueueTrace) {
											maxQueueTrace = mq;
										}
									}

									writeStats(depth, depth / (double) maxDepth, queueSize, queueSize
											/ (double) maxQueue, realAlignments[tr], realAlignments[0], offLineTime,
											noise, treeNumber, useCache, tree, treeStats[2 * treeNumber - 2],
											treeStats[2 * treeNumber - 1], traces, oracle, tr, cumulativeTime, trace,
											raCost, i, partialAlignment, maxDepthNeeded, maxQueueTrace, eventTime,
											outFinal, fixedLogMoveCost);

									dCost += Math.abs(paCost - raCost);
									if (maxQueueTrace > maxQueueNeeded) {
										maxQueueNeeded = maxQueueTrace;
									}
								} else {
									if (oracle.getDepth() > maxDepthNeeded) {
										maxDepthNeeded = oracle.getDepth();
									}

									Entry<? extends NAryTreeState, Integer> partialAlignment = replayer
											.getCurrentStates().get(0);
									writeStats(depth, depth / (double) maxDepth, queueSize, queueSize
											/ (double) maxQueue, realAlignments[tr], realAlignments[0], offLineTime,
											noise, treeNumber, useCache, tree, treeStats[2 * treeNumber - 2],
											treeStats[2 * treeNumber - 1], traces, oracle, tr, cumulativeTime, trace,
											raCost, i, partialAlignment, oracle.getDepth(), replayer.getCurrentStates()
													.size(), eventTime, out, fixedLogMoveCost);
								}

							}

						}

						//								System.out.println();

						//								System.out.print(realAlignment);
						//								System.out.println(" c:" + raCost);
						//								StringBuilder builder = new StringBuilder();
						//								printAlignment(stateBuilder, alignment.getKey(), builder, false, activities);
						//								System.out.println(builder.toString());
						//								System.out.println("Reached final state with cost: " + alignment.getValue());
						//								System.out.println("-------------------------------------");

						if (keepHistory) {
							Entry<? extends NAryTreeState, Integer> alignment = replayer.getCurrentStates().get(0);
							StateBuilder stateBuilder = new StateBuilder(tree, 0, true);
							testAlignmentLog((NAryTreeHistoryAwareState) alignment.getKey(), trace, map);
							testAlignmentModel((NAryTreeHistoryAwareState) alignment.getKey(), stateBuilder);
						}
					}

					if (out != null) {
						out.close();
						outFinal.close();
					}
				}// queueLoop

				if (dCost != 0) {
					System.err.println(tree.toString());
					System.err.println(Arrays.deepToString(traces));
					System.err.flush();
				}
				if (noisePar >= 0) {
					break;
				}
			}
		}
		long endTest = System.currentTimeMillis();
		System.out.println("Test time: " + (endTest - startTest) / 1000.0 + " seconds");
		return treeNumber;
	}

	public int countNodes(NAryTree tree, StateSpace stateSpace) {
		TIntSet keep = new TIntHashSet();
		Iterator<Edge> it = stateSpace.getEdgeIterator();
		while (it.hasNext()) {
			Edge edge = it.next();
			short type = tree.getType(edge.getLabel());
			if (type >= 0 && type != NAryTree.TAU) {
				// labeled edge going into a state
				keep.add(edge.getTo());
			}
		}
		return keep.size();
	}

	public AbstractNAryTreeOracle<?, ?> createOracle(boolean keepHistory, boolean useCache, NAryTree tree,
			int configurationNumber, TIntIntMap modelMoveCost, int depth) {
		AbstractNAryTreeOracle<?, ?> oracle;
		if (keepHistory) {
			oracle = new NAryTreeHistoryAwareOracle(tree, configurationNumber, modelMoveCost, depth, useCache,
					Integer.MAX_VALUE);
		} else {
			oracle = new NAryTreeOracle(tree, configurationNumber, modelMoveCost, depth, useCache, Integer.MAX_VALUE);
		}
		oracle.setScaling(SCALING);
		return oracle;
	}

	public OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> createReplayer(
			boolean keepHistory, TObjectIntMap<NAryTreeLabel> logMoveCost, int queueSize,
			AbstractNAryTreeOracle<?, ?> oracle) {
		OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> replayer;
		if (keepHistory) {
			replayer = new OracleBasedReplayerWithHistory<NAryTreeHistoryAwareState, NAryTreeLabel, NAryTreeNode>(
					(NAryTreeHistoryAwareOracle) oracle, queueSize, logMoveCost) {

				@Override
				public void update(NAryTreeLabel label) {
					super.update(label);
					// process currently scheduled states to store highest position
					int maxPos = currentStates.size();
					int maxVal = currentStates.get(maxPos - 1).getValue() / SCALING;

					for (int i = currentStates.size(); i-- > 0;) {
						if (currentStates.get(i).getValue() / SCALING < maxVal) {
							maxPos = i + 1;
							maxVal = currentStates.get(i).getValue() / SCALING;
						}
						if (maxPos > currentStates.get(i).getKey().getPosition()) {
							currentStates.get(i).getKey().setPosition(maxPos);
						}
					}
				}

			};
		} else {
			replayer = new OracleBasedReplayer<NAryTreeSimpleState, NAryTreeLabel, NAryTreeNode>(
					(NAryTreeOracle) oracle, queueSize, logMoveCost) {

				@Override
				public void update(NAryTreeLabel label) {
					super.update(label);
					// process currently scheduled states to store highest position
					int maxPos = currentStates.size();
					int maxVal = currentStates.get(maxPos - 1).getValue() / SCALING;

					for (int i = currentStates.size(); i-- > 0;) {
						if (currentStates.get(i).getValue() / SCALING < maxVal) {
							maxPos = i + 1;
							maxVal = currentStates.get(i).getValue() / SCALING;
						}
						if (maxPos > currentStates.get(i).getKey().getPosition()) {
							currentStates.get(i).getKey().setPosition(maxPos);
						}
					}

				}

				protected void followEdge(Map<NAryTreeSimpleState, Integer> newStates, NAryTreeSimpleState fromState,
						NAryTreeSimpleState toState,
						MovementSequence<NAryTreeSimpleState, NAryTreeLabel, NAryTreeNode> moves, int cost) {
					newStates.remove(toState);
					NAryTreeSimpleState copyOfNewState = oracle.createCopy(toState);
					copyOfNewState.setPosition(fromState.getPosition());
					newStates.put(copyOfNewState, cost);
				}

				protected void followEdge(Map<NAryTreeSimpleState, Integer> newStates, NAryTreeSimpleState fromState,
						NAryTreeSimpleState toState, MoveImpl<NAryTreeLabel, NAryTreeNode> move, int cost) {
					newStates.remove(toState);
					NAryTreeSimpleState copyOfNewState = oracle.createCopy(toState);
					copyOfNewState.setPosition(fromState.getPosition());
					newStates.put(copyOfNewState, cost);
				}

			};
		}
		replayer.setScaling(SCALING);
		return replayer;
	}

	private int getMaxQueueTrace(Entry<? extends NAryTreeState, Integer> partialAlignment, boolean keepHistory) {
		int maxQueueTrace = 0;
		if (keepHistory) {
			NAryTreeHistoryAwareState state = (NAryTreeHistoryAwareState) partialAlignment.getKey();
			do {
				if (state.getPosition() > maxQueueTrace) {
					maxQueueTrace = state.getPosition();
				}
				state = state.getPredecessor();
			} while (state != null);
		} else {
			maxQueueTrace = ((NAryTreeSimpleState) partialAlignment.getKey()).getPosition();
		}
		return maxQueueTrace;
	}

	public static void writeStats(int maxDepth, double d, int queueSize, double q, RealReplayResult realReplayResult,
			RealReplayResult emptyTraceRealResult, long realTime, int noise, int treeNumber, boolean useCache,
			NAryTree tree, int statespaceNodes, int statespaceEdges, String[][] traces,
			AbstractNAryTreeOracle<?, ?> oracle, int tr, long cumulativeTime, String[] trace, int raCost, int i,
			Entry<? extends NAryTreeState, Integer> partialAlignment, int depth, int maxQueueNeeded, long eventTime,
			Writer o, int logMoveCost) throws IOException {
		o.write(maxDepth + ";");
		o.write(d + ";");
		o.write(queueSize + ";");
		o.write(q + ";");
		o.write(treeNumber + ";");
		o.write(tree.size() + ";");
		o.write((tree.numLeafs() - tree.countNodes(NAryTree.TAU)) + ";");
		o.write(statespaceNodes + ";");
		o.write(statespaceEdges + ";");
		o.write(tr + ";");
		o.write(noise + ";");
		o.write(eventTime / 1.0E9 + ";");
		double onlineTime = cumulativeTime / 1.0E9;
		o.write(onlineTime + ";");
		double offlineTime = realTime / 1.0E9;
		o.write(offlineTime + ";");
		o.write((offlineTime - onlineTime) + ";");
		o.write((useCache ? getCacheSize(oracle.getCache()) : "0") + ";");
		o.write(realReplayResult.cacheSize + ";");
		o.write(oracle.getHits() + ";");
		o.write(oracle.getMisses() + ";");
		o.write(oracle.getPolls() + ";");
		o.write(partialAlignment.getValue() / SCALING + ";");
		o.write(raCost + ";");
		double off;
		if (raCost == 0) {
			if (partialAlignment.getValue() / SCALING == 0) {
				off = 0.0;
			} else {
				off = -partialAlignment.getValue() / SCALING;
			}
		} else {
			off = (raCost - partialAlignment.getValue() / SCALING);
			off /= raCost;
		}
		o.write(off + ";");
		o.write((raCost - partialAlignment.getValue() / SCALING) * (raCost - partialAlignment.getValue() / SCALING)
				+ ";");
		// fitness
		if (tr == 0) {
			// empty trace prefix
			o.write("0.0;");
		} else {
			double fitness = raCost;
			if (traces[tr].length + emptyTraceRealResult.cost() == 0) {
				// empty trace fits;
				fitness = 0.0;
			} else {

				fitness /= logMoveCost * traces[tr].length + emptyTraceRealResult.cost() / SCALING;
			}
			o.write((1.0 - fitness) + ";");
		}
		o.write(depth + ";");
		o.write(maxQueueNeeded + ";");
		o.write(realReplayResult.reliable + ";");
		if (traces[tr].length == 0) {
			o.write("1;");
			o.write("0;");
		} else {
			o.write(i / (double) traces[tr].length + ";");
			o.write((i + 1) + ";");
		}
		o.write(TreeUtils.toString(tree) + ";");
		o.write("[");
		for (int j = 0; j < i - 1; j++) {
			o.write(trace[j] + ", ");
		}
		if (i == trace.length - 1) {
			if (i > 0) {
				o.write(trace[i - 1] + "] ");
			} else {
				o.write("] ");
			}
		} else {
			if (i > 0) {
				o.write(trace[i - 1] + ", ");
			}
			o.write(trace[i] + " ");

		}
		o.write("\r\n");
	}

	private static int getCacheSize(
			Map<?, ? extends MovementSequence<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode>[]> cache) {
		return cache.size();
		//		int i = 0;
		//		for (MovementSequence<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode>[] o : cache.values()) {
		//			i += o.length;
		//		}
		//		return i;
	}

	private static void writeHeader(Writer... outs) throws IOException {
		for (Writer out : outs) {
			out.write("maxDepth;depthFraction;maxQueueSize;queueFraction;treeNumber;treeSize;nonTauLeafs;statespaceNodes;");
			out.write("statespaceEdges;traceID;noiselevel;");
			out.write("timeLastEvent;cumulativeTime;offlineTime;dTime;");
			out.write("onlineCache;offlineCache;hit;miss;poll;cost;costInReal;dCost;SEdCost;fitness;depth;queueSize;reliable;");
			out.write("tracefraction;traceLength;tree;trace");
			out.write("\r\n");
		}
	}

	private static Writer createFile(String name, boolean append) throws IOException {
		File f = new File(name);
		if (!append && f.exists() && f.length() > 0) {
			return null;
		}
		return new BufferedWriter(new FileWriter(f, append));
	}

	public static String[][] getTraces(Simulator sim, int numTraces, String[] activities, double noise) {

		String[][] traces = new String[numTraces][];
		traces[0] = new String[] {};
		for (int i = 1; i < numTraces; i++) {
			traces[i] = sim.getRandomTrace(activities, noise);
		}
		return traces;
	}

	public static void printAlignment(StateBuilder stateBuilder, NAryTreeHistoryAwareState state,
			StringBuilder builder, boolean states, String[] leafLabels) {
		printAlignment(stateBuilder, state, builder, states, leafLabels, 0);
	}

	private static void printMovementSequence(StringBuilder builder, List<MoveImpl<NAryTreeLabel, NAryTreeNode>> moves,
			String[] leafLabels) {
		Iterator<MoveImpl<NAryTreeLabel, NAryTreeNode>> it = moves.iterator();
		while (it.hasNext()) {
			MoveImpl<NAryTreeLabel, NAryTreeNode> move = it.next();
			NAryTreeNode node = move.getTransition();
			NAryTreeLabel label = move.getEventLabel();
			if (node == null) {
				builder.append("(" + leafLabels[label.getLabel()] + ", _):" + move.getCost() / SCALING);
			} else if (label == null) {
				builder.append("(_," + String.format("%2d", node.getNode()) + "):" + move.getCost() / SCALING);

			} else {
				builder.append("(" + leafLabels[label.getLabel()] + "," + String.format("%2d", node.getNode()) + "):"
						+ move.getCost() / SCALING);

			}
			if (it.hasNext()) {
				builder.append(", ");
			}
		}

	}

	private void printAlignment(StringBuilder builder, NAryTreeHistoryAwareMovementSequence realAlignment,
			String[] leafLabels) {
		builder.append("[");
		printMovementSequence(builder, realAlignment.getMovementSequence(), leafLabels);
		builder.append("]");
	}

	private static void printAlignment(StateBuilder stateBuilder, NAryTreeHistoryAwareState state,
			StringBuilder builder, boolean states, String[] leafLabels, int depth) {
		if (state.getPredecessor() != null) {
			printAlignment(stateBuilder, state.getPredecessor(), builder, states, leafLabels, depth
					+ (state.getMovementSequence().isEmpty() ? 0 : 1));

			printMovementSequence(builder, state.getMovementSequence(), leafLabels);
			if (depth == 0) {
				builder.append("]");
			} else {
				builder.append(", ");
			}
			if (states) {
				builder.append(" -> ");
				builder.append(stateBuilder.toString(state.getState()));
				builder.append(" -> ");
			}

		} else {
			builder.append("[");
			if (states) {
				builder.append(stateBuilder.toString(state.getState()));
				builder.append(" -> ");
			}
		}
	}

	public static <L, M> int testAlignmentLog(NAryTreeHistoryAwareState state, String[] trace,
			TObjectShortMap<String> map) {
		if (state.getPredecessor() != null) {
			int index = testAlignmentLog(state.getPredecessor(), trace, map);
			for (MoveImpl<NAryTreeLabel, NAryTreeNode> move : state.getMovementSequence()) {
				NAryTreeLabel label = move.getEventLabel();
				if (label != null) {
					if (map.get(trace[index]) != label.getLabel()) {
						throw new RuntimeException("ERROR IN LOG-PROJECTION");
					}
					index++;
				}

			}
			return index;
		}
		return 0;
	}

	public static <L, M> byte[] testAlignmentModel(NAryTreeHistoryAwareState state, StateBuilder builder) {
		if (state.getPredecessor() != null) {
			byte[] currentState = testAlignmentModel(state.getPredecessor(), builder);
			for (MoveImpl<NAryTreeLabel, NAryTreeNode> move : state.getMovementSequence()) {
				NAryTreeNode node = move.getTransition();
				if (node != null) {
					int n = node.getNode();
					if (n >= builder.getTree().size()) {
						// explicitly terminating an OR node.
						n -= builder.getTree().size();
					}

					if (!builder.isEnabled(currentState, n)) {
						throw new RuntimeException("ERROR IN MODEL-PROJECTION: " + n + " in state "
								+ builder.toString(currentState));
					}
					currentState = builder.executeAll(currentState, node.getNode());
				}

			}
			return currentState;
		}
		if (!builder.getTree().isLeaf(0)) {
			builder.setPushDownUnderAND(false);
			byte[] initialState = builder.executeAll(builder.initializeState(), 0);
			return initialState;
		} else {
			builder.setPushDownUnderAND(false);
			return builder.initializeState();
		}

	}

	public static <L, M> void testAlignmentLog(List<MoveImpl<NAryTreeLabel, NAryTreeNode>> state, String[] trace,
			TObjectShortMap<String> map) {
		int index = 0;
		for (MoveImpl<NAryTreeLabel, NAryTreeNode> move : state) {
			NAryTreeLabel label = move.getEventLabel();
			if (label != null) {
				if (map.get(trace[index]) != label.getLabel()) {
					throw new RuntimeException("ERROR IN LOG-PROJECTION");
				}
				index++;
			}

		}
		if (index != trace.length) {
			throw new RuntimeException("ERROR IN LOG-PROJECTION");
		}

	}

	public static <L, M> void testAlignmentModel(List<MoveImpl<NAryTreeLabel, NAryTreeNode>> state, StateBuilder builder) {
		builder.setPushDownUnderAND(false);
		byte[] currentState = builder.executeAll(builder.initializeState(), 0);
		for (MoveImpl<NAryTreeLabel, NAryTreeNode> move : state) {
			NAryTreeNode node = move.getTransition();
			if (node != null) {
				int n = node.getNode();
				if (n >= builder.getTree().size()) {
					// explicitly terminating an OR node.
					n -= builder.getTree().size();
				}

				if (!builder.isEnabled(currentState, n)) {
					throw new RuntimeException("ERROR IN MODEL-PROJECTION excuting " + n + " in state "
							+ builder.toString(currentState));
				}
				currentState = builder.executeAll(currentState, node.getNode());
			}

		}
		if (!builder.isFinal(currentState)) {
			throw new RuntimeException("ERROR IN MODEL-PROJECTION");
		}
	}

	public int getMaxDepth(NAryTree tree) {
		// f represents the longest sequence of model moves to finish the
		// subtree under a node
		int[] f = new int[tree.size()];
		// g represents the shortest sequence of model moves to finish the
		// subtree under a node
		int[] g = new int[tree.size()];
		// h represents the maximal number of model moves required to enable
		// any synchronous move under a node.
		int[] h = new int[tree.size()];

		int max = 0;
		for (int n = tree.size(); n-- > 0;) {
			if (tree.isLeaf(n)) {
				f[n] = tree.getTypeFast(n) == NAryTree.TAU ? 0 : 1;
				g[n] = f[n];
				h[n] = 0;
			} else {
				int c = n + 1;
				switch (tree.getTypeFast(n)) {
					case NAryTree.ILV :
					case NAryTree.AND :
					case NAryTree.SEQ :
					case NAryTree.REVSEQ :
						do {
							// all children are needed to complete the node
							f[n] += f[c];
							g[n] += g[c];
							c = tree.getNextFast(c);
						} while (c < tree.size() && tree.getParentFast(c) == n);
						break;
					case NAryTree.XOR :
						g[n] = Integer.MAX_VALUE;
						do {
							// one child is needed to complete the node
							f[n] = Math.max(f[n], f[c]);
							g[n] = Math.min(g[n], g[c]);
							c = tree.getNextFast(c);
						} while (c < tree.size() && tree.getParentFast(c) == n);
						break;
					case NAryTree.OR :
						g[n] = Integer.MAX_VALUE;
						do {
							// at most all children that were started are needed to 
							// complete the node
							f[n] += f[c] - 1;
							// at least one child is needed to complete the node
							g[n] = Math.min(g[n], g[c]);
							c = tree.getNextFast(c);
						} while (c < tree.size() && tree.getParentFast(c) == n);
						// at least the shortest node needs to be completed.
						f[n] = Math.max(f[n] + 1, g[n]);
						break;
					case NAryTree.LOOP :
						// worst case for a loop is redo - do - exit
						// best case is do - exit
						f[n] = f[c];
						g[n] = g[c];
						c = tree.getNextFast(c);
						f[n] += f[c];
						c = tree.getNextFast(c);
						f[n] += f[c];
						g[n] += g[c];
						break;
				}
			}
		}

		for (int n = tree.size(); n-- > 0;) {
			if (f[n] == 0) {
				// correct as otherwise in the next step
				// we always need MAX( f[n]-1, 0)
				f[n] = 1;
			}
		}
		for (int n = tree.size(); n-- > 0;) {
			int c = n + 1;
			int sum;
			switch (tree.getTypeFast(n)) {
				case NAryTree.ILV :
					// worst sequence of model moves to enable any sync move is 
					// the maximum over all children of f[c] - 2, i.e. we started the
					// longest sequence in child c and need the last element, or
					// h[c], i.e. we didn't start the child yet.
					h[n] = 0;
					do {
						h[n] = Math.max(2 * f[c] - 2, h[n]);
						h[n] = Math.max(h[c], h[n]);
						c = tree.getNextFast(c);
					} while (c < tree.size() && tree.getParentFast(c) == n);
					break;
				case NAryTree.OR :
				case NAryTree.AND :
				case NAryTree.XOR :
					// worst sequence of model moves to enable any sync move is 
					// the maximum over all children of f[c] - 2, i.e. we started the
					// longest sequence in child c and need the last element, or
					// h[c], i.e. we didn't start the child yet.
					h[n] = 0;
					do {
						//						h[n] = Math.max(f[c] - 2, h[n]);
						h[n] = Math.max(h[c], h[n]);
						c = tree.getNextFast(c);
					} while (c < tree.size() && tree.getParentFast(c) == n);
					break;
				case NAryTree.SEQ :
				case NAryTree.REVSEQ :
					// worst sequence of model moves to enable any sync move is 
					// the maximum from child j to child i, assuming child j just started 
					// the longest sequence f[j]-1, all children between do shortest
					// sequence g[c] and child i does longest again of which the last
					// is the sync move, f[i]-1.

					// now find max over n+1 <= i <= c < j <maxChild : f[i]-1 + sum g[c] + f[j]-1
					for (int i = n + 1; i < tree.size() && tree.getParentFast(i) == n; i = tree.getNextFast(i)) {
						// reduce sum by g[i];
						sum = 0;
						h[n] = Math.max(h[n], h[i]);
						for (int j = tree.getNextFast(i); j < tree.size() && tree.getParentFast(j) == n; j = tree
								.getNextFast(j)) {
							if (tree.getTypeFast(n) == NAryTree.SEQ) {
								// option 1: we need to complete longest sequence in i and j
								h[n] = Math.max(h[n], f[i] - 1 + sum + h[j]);
								// option 2: we need to start shortest sequence in i
								h[n] = Math.max(h[n], g[i] + sum + h[j]);
							} else {
								// option 1: we need to complete longest sequence in i and j
								h[n] = Math.max(h[n], h[i] + sum + f[j] - 1);
								// option 2: we need to start shortest sequence in j
								h[n] = Math.max(h[n], h[i] + sum + g[j]);
							}
							sum += g[j];
						}
					}
					break;
				case NAryTree.LOOP :
					// worst cases:

					// *need last of do: f[do]-1

					// *need last of redo: g[do] + h[redo]
					// started do, need last of redo: f[do] - 1 + h[redo]

					// *need last of exit: g[do] + h[exit]
					// started do, need last of exit: f[do] - 1 + h[exit]

					// started redo, need last of exit: f[redo]-1 + g[do] + h[exit] 

					int redo = tree.getNextFast(n + 1);
					int exit = tree.getNextFast(redo);
					h[n] = f[n + 1] - 1 + h[redo];
					h[n] = Math.max(h[n], f[n + 1] - 1 + h[exit]);
					h[n] = Math.max(h[n], f[redo] - 1 + g[n + 1] + h[exit]);

					break;
			}
			if (h[n] > max) {
				max = h[n];
			}
		}
		//		System.out.println(Arrays.toString(f));
		//		System.out.println(Arrays.toString(g));
		//		System.out.println(Arrays.toString(h));
		return Math.min(max, (tree.numLeafs() - tree.countNodes(NAryTree.TAU) - 1));
	}

}
