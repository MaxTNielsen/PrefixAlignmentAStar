package org.processmining.onlineconformance.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.eventstream.core.interfaces.XSEvent;
import org.processmining.eventstream.readers.abstr.AbstractXSEventReader;
import org.processmining.onlineconformance.models.impl.MoveImpl;
import org.processmining.onlineconformance.oracle.MovementSequence;
import org.processmining.onlineconformance.oracle.treebased.AbstractNAryTreeOracle;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeHistoryAwareOracle;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeHistoryAwareState;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeLabel;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeNode;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeOracle;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeSimpleState;
import org.processmining.onlineconformance.oracle.treebased.NAryTreeState;
import org.processmining.onlineconformance.replayer.OracleBasedReplayer;
import org.processmining.onlineconformance.replayer.OracleBasedReplayerWithHistory;
import org.processmining.plugins.etm.model.narytree.NAryTree;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

// TODO: The second argument, i.e., Object refers to the actual artifact that
// needs to be visualized based on this algorithm
public class OnlineReplayerAlgorithm extends
		AbstractXSEventReader<Map<String, Entry<? extends NAryTreeState, Integer>>, Object, OnlineReplayerParameters> {

	private static final int SCALING = 10000;

	private boolean keepHistory;
	private TObjectIntMap<NAryTreeLabel> logMoveCost;
	private int maxQueueSize;
	private TIntIntHashMap modelMoveCost;

	private TObjectShortMap<String> labelMap;
	private short nextActivity;

	public OnlineReplayerAlgorithm(NAryTree tree, TObjectShortMap<String> labelMap) {
		super("Online Replayer", null, new OnlineReplayerParameters());

		// TODO: Parameters in parameter object
		keepHistory = true;

		modelMoveCost = new TIntIntHashMap(10);
		for (int i = 0; i < tree.size(); i++) {
			if (tree.getTypeFast(i) >= 0 && tree.getTypeFast(i) != NAryTree.TAU) {
				modelMoveCost.put(i, 1);
			} else {
				modelMoveCost.put(i, 0);
			}
		}

		logMoveCost = new TObjectIntHashMap<NAryTreeLabel>();
		for (short s : labelMap.values()) {
			logMoveCost.put(new NAryTreeLabel(s), 1);
		}

		this.labelMap = labelMap;
		this.maxQueueSize = 10;

		nextActivity = 0;

		oracle = createOracle(true, true, tree, 0, modelMoveCost, 5);

	}

	public Class<XSEvent> getTopic() {
		return XSEvent.class;
	}

	Map<String, OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode>> replayerMap = new HashMap<>();

	private final AbstractNAryTreeOracle<?, ?> oracle;

	protected Map<String, Entry<? extends NAryTreeState, Integer>> computeCurrentResult() {
		// return list of peek() of oracles
		Map<String, Entry<? extends NAryTreeState, Integer>> result = new HashMap<>();
		for (String caseID : replayerMap.keySet()) {
			OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> replayer = replayerMap
					.get(caseID);
			Entry<? extends NAryTreeState, Integer> partialAlignment = replayer.getCurrentStates().get(0);
			result.put(caseID, partialAlignment);
		}

		return result;
	}

	public void processNewXSEvent(String caseId, XEventClass activity) {

		OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> replayer = replayerMap.get(caseId);
		if (replayer == null) {
			replayer = createReplayer(keepHistory, logMoveCost, maxQueueSize, oracle);
			replayerMap.put(caseId, replayer);

		}

		NAryTreeLabel label;
		if (labelMap.containsKey(activity.toString())) {
			label = new NAryTreeLabel(labelMap.get(activity.toString()));
		} else {
			label = new NAryTreeLabel((short) labelMap.size());
			labelMap.put(activity.toString(), label.getLabel());
			logMoveCost.put(label, 1);
		}

		replayer.update(label);

		System.out.println(computeCurrentResult());

	}

	private AbstractNAryTreeOracle<?, ?> createOracle(boolean keepHistory, boolean useCache, NAryTree tree,
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

	private OracleBasedReplayer<? extends NAryTreeState, NAryTreeLabel, NAryTreeNode> createReplayer(
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

}
