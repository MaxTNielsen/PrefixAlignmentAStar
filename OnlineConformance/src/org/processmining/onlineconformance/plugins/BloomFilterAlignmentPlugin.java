package org.processmining.onlineconformance.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.onlineconformance.algorithms.BloomFilterBasedAlignmentCalculatorImpl;
import org.processmining.onlineconformance.models.ModelSemantics;
import org.processmining.onlineconformance.models.PartialAlignment;
import org.processmining.onlineconformance.models.PriceList;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

@Plugin(name = "Compute Alignments using Bloom Filters", parameterLabels = { "Petri Net", "Initial Marking",
		"Final Marking", "Event Data",
		"Parameters" }, returnLabels = { "Replay Result" }, returnTypes = { Object.class })
public class BloomFilterAlignmentPlugin {

	private final Map<Transition, String> modelElementsToLabelMap = new HashMap<>();
	private final Map<String, Collection<Transition>> labelsToModelElementsMap = new HashMap<>();
	private final TObjectDoubleMap<Transition> modelMoveCosts = new TObjectDoubleHashMap<>();
	private final TObjectDoubleMap<String> labelMoveCosts = new TObjectDoubleHashMap<>();

	@UITopiaVariant(author = "S.J. van Zelst", email = "s.j.v.zelst@tue.nl", affiliation = "Eindhoven University of Technology")
	@PluginVariant(variantLabel = "Compute Alignments using Bloom Filters", requiredParameterLabels = { 0, 1, 2, 3 })
	public Object apply(final UIPluginContext context, final Petrinet net, Marking initialMarking, Marking finalMarking,
			final XLog log) {
		setupLabelMap(net);
		setupModelMoveCosts(net);
		PriceList<String, Transition> priceList = PriceList.Factory.construct(labelMoveCosts, modelMoveCosts, 1d, 0d,
				modelElementsToLabelMap);
		return replay(net, initialMarking, finalMarking, log, priceList);
	}

	@UITopiaVariant(author = "S.J. van Zelst", email = "s.j.v.zelst@tue.nl", affiliation = "Eindhoven University of Technology")
	@PluginVariant(variantLabel = "Compute Alignments using Bloom Filters", requiredParameterLabels = { 0, 3 })
	public Object apply(final UIPluginContext context, final AcceptingPetriNet accNet, final XLog log) {
		Petrinet net = accNet.getNet();
		Marking initialMarking = accNet.getInitialMarking();
		Marking finalMarking = new Marking(); // empty marking is default
		for (Marking m : accNet.getFinalMarkings()) { //currently picks the first final marking
			finalMarking = m;
			break;
		}
		return apply(context, net, initialMarking, finalMarking, log);
	}

	private Object replay(final Petrinet net, Marking iniMarking, Marking finalMarking, final XLog log,
			PriceList<String, Transition> priceList) {
		XEventClassifier classifier = new XEventNameClassifier();
		ModelSemantics<Petrinet, Marking, Transition> semantics = ModelSemantics.Factory.construct(net);
		BloomFilterBasedAlignmentCalculatorImpl<Petrinet, String, Transition, Marking> replayer = new BloomFilterBasedAlignmentCalculatorImpl<Petrinet, String, Transition, Marking>(
				iniMarking, finalMarking, semantics, priceList);
		Collection<List<String>> tracesReplayed = new HashSet<>();
		for (XTrace t : log) {
			List<String> trace = new ArrayList<>();
			for (XEvent e : t) {
				trace.add(classifier.getClassIdentity(e));
			}
			if (!tracesReplayed.contains(trace)) {
				PartialAlignment<String, Transition, Marking> alignment = replayer.align(trace);
				System.out.println(alignment);
				tracesReplayed.add(trace);
			}				
		}
		return new Object();
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

}
