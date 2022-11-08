package org.processmining.onlineconformance.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.EfficientPetrinetSemantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientPetrinetSemanticsImpl;
import org.processmining.onlineconformance.algorithms.IncrementalReplayer;
import org.processmining.onlineconformance.models.IncrementalReplayResult;
import org.processmining.onlineconformance.models.MeasurementAwarePartialAlignment;
import org.processmining.onlineconformance.models.ModelSemanticsPetrinet;
import org.processmining.onlineconformance.models.Move;
import org.processmining.onlineconformance.models.PartialAlignment;
import org.processmining.onlineconformance.parameters.IncrementalReplayerParametersImpl;
import org.processmining.onlineconformance.parameters.IncrementalRevBasedReplayerParametersImpl;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

@Plugin(name = "Compute Prefix Alignments Incrementally", parameterLabels = { "Petri Net", "Initial Marking",
		"Final Marking", "Event Data",
		"Parameters" }, returnLabels = { "Replay Result" }, returnTypes = { IncrementalReplayResult.class })
public class IncrementalAlignmentPlugin {

	private final Map<Transition, String> modelElementsToLabelMap = new HashMap<>();
	private final Map<String, Collection<Transition>> labelsToModelElementsMap = new HashMap<>();
	private final TObjectDoubleMap<Transition> modelMoveCosts = new TObjectDoubleHashMap<>();
	private final TObjectDoubleMap<String> labelMoveCosts = new TObjectDoubleHashMap<>();

	@UITopiaVariant(author = "S.J. van Zelst", email = "s.j.v.zelst@tue.nl", affiliation = "Eindhoven University of Technology")
	@PluginVariant(variantLabel = "Compute Prefix Alignments Incrementally", requiredParameterLabels = { 0, 1, 2, 3 })
	public IncrementalReplayResult<String, String, Transition, Marking, ? extends PartialAlignment<String, Transition, Marking>> apply(
			final UIPluginContext context, final Petrinet net, Marking initialMarking, Marking finalMarking,
			final XLog log) {
		setupLabelMap(net);
		setupModelMoveCosts(net);
		IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition> parameters = new IncrementalRevBasedReplayerParametersImpl<>();
		parameters.setUseMultiThreading(false);
		parameters.setLabelMoveCosts(labelMoveCosts);
		parameters.setLabelToModelElementsMap(labelsToModelElementsMap);
		parameters.setModelMoveCosts(modelMoveCosts);
		parameters.setModelElementsToLabelMap(modelElementsToLabelMap);
		parameters.setSearchAlgorithm(IncrementalReplayer.SearchAlgorithm.A_STAR);
		parameters.setUseSolutionUpperBound(true);
		parameters.setLookBackWindow(2);
		parameters.setExperiment(true);
		if (parameters.isExperiment()) {
			return applyMeasurementAware(context, net, initialMarking, finalMarking, log, parameters);
		} else {
			return applyGeneric(context, net, initialMarking, finalMarking, log, parameters);
		}
	}

	@UITopiaVariant(author = "S.J. van Zelst", email = "s.j.v.zelst@tue.nl", affiliation = "Eindhoven University of Technology")
	@PluginVariant(variantLabel = "Compute Prefix Alignments Incrementally", requiredParameterLabels = { 0, 3 })
	public IncrementalReplayResult<String, String, Transition, Marking, ? extends PartialAlignment<String, Transition, Marking>> apply(
			final UIPluginContext context, final AcceptingPetriNet accNet, final XLog log) {
		Petrinet net = accNet.getNet();
		Marking initialMarking = accNet.getInitialMarking();
		Marking finalMarking = new Marking(); // empty marking is default
		for (Marking m : accNet.getFinalMarkings()) { //currently picks the first final marking
			finalMarking = m;
			break;
		}
		return apply(context, net, initialMarking, finalMarking, log);
	}

	public IncrementalReplayResult<String, String, Transition, Marking, PartialAlignment<String, Transition, Marking>> apply(
			final PluginContext context, final Petrinet net, final Marking initialMarking, final Marking finalMarking,
			final XLog log, final IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition> parameters) {
		return applyGeneric(context, net, initialMarking, finalMarking, log, parameters);
	}

	public IncrementalReplayResult<String, String, Transition, Marking, MeasurementAwarePartialAlignment<String, Transition, Marking>> applyMeasurementAware(
			final PluginContext context, final Petrinet net, final Marking initialMarking, final Marking finalMarking,
			final XLog log, final IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition> parameters) {
		return applyGeneric(context, net, initialMarking, finalMarking, log, parameters);
	}

	@SuppressWarnings("unchecked")
	public <A extends PartialAlignment<String, Transition, Marking>> IncrementalReplayResult<String, String, Transition, Marking, A> applyGeneric(
			final PluginContext context, final Petrinet net, final Marking initialMarking, final Marking finalMarking,
			final XLog log, final IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition> parameters) {
		ModelSemanticsPetrinet<Marking> modelSemantics = ModelSemanticsPetrinet.Factory.construct(net);
		Map<Transition, String> labelsInPN = new HashMap<Transition, String>();
		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible()) {
				labelsInPN.put(t, t.getLabel());
			}
		}
		if (parameters.isExperiment()) {
			Map<String, MeasurementAwarePartialAlignment<String, Transition, Marking>> store = new HashMap<>();
			IncrementalReplayer<Petrinet, String, Marking, Transition, String, MeasurementAwarePartialAlignment<String, Transition, Marking>, IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition>> replayer = IncrementalReplayer.Factory
					.construct(initialMarking, finalMarking, store, modelSemantics, parameters, labelsInPN,
							IncrementalReplayer.Strategy.REVERT_BASED);
			return (IncrementalReplayResult<String, String, Transition, Marking, A>) processXLog(log, net,
					initialMarking, replayer);
		} else {
			Map<String, PartialAlignment<String, Transition, Marking>> store = new HashMap<>();
			IncrementalReplayer<Petrinet, String, Marking, Transition, String, PartialAlignment<String, Transition, Marking>, IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition>> replayer = IncrementalReplayer.Factory
					.construct(initialMarking, finalMarking, store, modelSemantics, parameters, labelsInPN,
							IncrementalReplayer.Strategy.REVERT_BASED);
			return (IncrementalReplayResult<String, String, Transition, Marking, A>) processXLog(log, net,
					initialMarking, replayer);
		}
	}

	private boolean isFeasible(String caseId, List<Move<String, Transition>> moves, List<String> trace, Petrinet net,
			Marking iMarking) {
		boolean res = true;
		EfficientPetrinetSemantics semantics = new EfficientPetrinetSemanticsImpl(net);
		semantics.setState(semantics.convert(iMarking));
		int i = 0;
		for (Move<String, Transition> move : moves) {
			if (move.getTransition() != null) {
				res &= semantics.isEnabled(move.getTransition());
				if (!res) {
					System.out.println("Violation for case " + caseId + ", " + "move " + move.toString() + ", at: "
							+ semantics.getStateAsMarking().toString());
				}
				semantics.directExecuteExecutableTransition(move.getTransition());
			}
			if (move.getEventLabel() != null) {
				//				res &= move.getEventLabel().equals(trace.get(i).toString() + "+complete");
				res &= move.getEventLabel().equals(trace.get(i).toString());
				if (!res) {
					System.out.println("Violation for case " + caseId + " on label part. original: " + trace.toString()
							+ ", moves: " + moves.toString());
				}
				i++;
			}
			if (!res)
				break;
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	private <A extends PartialAlignment<String, Transition, Marking>> IncrementalReplayResult<String, String, Transition, Marking, A> processXLog(
			XLog log, Petrinet net, Marking iMarking,
			IncrementalReplayer<Petrinet, String, Marking, Transition, String, A, ? extends IncrementalReplayerParametersImpl<Petrinet, String, Transition>> replayer) {
		XEventClasses classes = XEventClasses.deriveEventClasses(new XEventNameClassifier(), log);
		final TObjectDoubleMap<List<String>> costPerTrace = new TObjectDoubleHashMap<>();
		final TObjectIntMap<List<String>> count = new TObjectIntHashMap<>();
		IncrementalReplayResult<String, String, Transition, Marking, A> pluginResult = IncrementalReplayResult.Factory
				.construct(IncrementalReplayResult.Impl.HASH_MAP);
		for (XTrace t : log) {
			List<String> traceStrLst = toStringList(t, classes);
			String traceStr = StringUtils.join(traceStrLst, ",");
			String caseId = XConceptExtension.instance().extractName(t);
			if (!costPerTrace.containsKey(traceStrLst)) {
				pluginResult.put(traceStr, new ArrayList<A>());
				PartialAlignment<String, Transition, Marking> partialAlignment = null;
				for (String e : traceStrLst) {
					partialAlignment = replayer.processEvent(caseId, e.toString());
					pluginResult.get(traceStr).add((A) partialAlignment);
				}
				if (partialAlignment != null) {
					assert (isFeasible(caseId, partialAlignment, traceStrLst, net, iMarking));
				} else {
					assert (false);
				}
				costPerTrace.put(traceStrLst, partialAlignment.getCost());
				count.put(traceStrLst, 1);
			} else {
				count.adjustOrPutValue(traceStrLst, 1, 1);
			}
		}
		int totalCost = 0;
		for (List<String> t : costPerTrace.keySet()) {
			totalCost += count.get(t) * costPerTrace.get(t);
		}
		System.out.println("total costs: " + totalCost);
		return pluginResult;
	}

	public static <A extends PartialAlignment<String, Transition, Marking>> A processEventUsingReplayer(String caseId,
			String event,
			IncrementalReplayer<Petrinet, String, Marking, Transition, String, A, ? extends IncrementalReplayerParametersImpl<Petrinet, String, Transition>> replayer) {
		return replayer.processEvent(caseId, event);
	}

	private List<String> toStringList(XTrace trace, XEventClasses classes) {
		List<String> l = new ArrayList<>(trace.size());
		for (int i = 0; i < trace.size(); i++) {
			l.add(i, classes.getByIdentity(XConceptExtension.instance().extractName(trace.get(i))).toString());
		}
		return l;
	}

	private void setupLabelMap(final Petrinet net) {
		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible()) {
				String label = t.getLabel();
				modelElementsToLabelMap.put(t, label);
				if (!labelsToModelElementsMap.containsKey(label)) {
					labelsToModelElementsMap.put(label, Collections.singleton(t));
				} else {
					labelsToModelElementsMap.get(label).add(t);
				}
			}
		}
	}

	//TODO: needs a parameter object
	private void setupModelMoveCosts(final Petrinet net) {
		for (Transition t : net.getTransitions()) {
			if (t.isInvisible()) {
				modelMoveCosts.put(t, (short) 0);
			} else {
				modelMoveCosts.put(t, (short) 1);
				labelMoveCosts.put(t.getLabel(), (short) 1);
			}
		}
	}

	//	@UITopiaVariant(author = "S.J. van Zelst", email = "s.j.v.zelst@tue.nl", affiliation = "Eindhoven University of Technology")
	//	@PluginVariant(variantLabel = "Incrementally Compute Prefix Alignments", requiredParameterLabels = { 0, 1, 2, 3 })
	//	public static Object apply(final UIPluginContext context, final Petrinet net, Marking initialMarking,
	//			Marking finalMarking, final XSEventStream stream) {
	//		GreedyAStarStateEquationOpitmalAlignmentReplayer algo = new GreedyAStarStateEquationOpitmalAlignmentReplayer(
	//				net, initialMarking, finalMarking);
	//		algo.start();
	//		stream.connect(algo);
	//		return algo;
	//	}
	//
	//	@UITopiaVariant(author = "S.J. van Zelst", email = "s.j.v.zelst@tue.nl", affiliation = "Eindhoven University of Technology")
	//	@PluginVariant(variantLabel = "Incrementally Compute Prefix Alignments", requiredParameterLabels = { 0, 3 })
	//	public static Object apply(final UIPluginContext context, final AcceptingPetriNet accNet,
	//			final XSEventStream stream) {
	//		Petrinet net = accNet.getNet();
	//		Marking initialMarking = accNet.getInitialMarking();
	//		Marking finalMarking = new Marking(); // empty marking is default
	//		for (Marking m : accNet.getFinalMarkings()) {
	//			finalMarking = m;
	//			break;
	//		}
	//		GreedyAStarStateEquationOpitmalAlignmentReplayer algo = new GreedyAStarStateEquationOpitmalAlignmentReplayer(
	//				net, initialMarking, finalMarking);
	//		algo.start();
	//		stream.connect(algo);
	//		return algo;
	//	}

}
