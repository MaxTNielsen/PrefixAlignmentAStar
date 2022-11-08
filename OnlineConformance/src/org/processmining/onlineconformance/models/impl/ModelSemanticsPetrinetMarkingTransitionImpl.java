package org.processmining.onlineconformance.models.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.EfficientPetrinetSemantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientPetrinetSemanticsImpl;
import org.processmining.onlineconformance.models.ModelSemanticsPetrinet;

/**
 * wrapper around efficient petrinet semenatics, implements model-semantics
 * provider
 * 
 * @author svzelst
 *
 */
public class ModelSemanticsPetrinetMarkingTransitionImpl implements ModelSemanticsPetrinet<Marking> {

	private final EfficientPetrinetSemantics semantics;
	private final Petrinet model;
	private final Object mutex = new Object();
	private final Map<Marking, byte[]> cache = new HashMap<>();

	public ModelSemanticsPetrinetMarkingTransitionImpl(Petrinet m) {
		model = m;
		semantics = new EfficientPetrinetSemanticsImpl(m);
	}

	public Petrinet getModel() {
		return model;
	}

	private byte[] convert(Marking m) {
		if (cache.containsKey(m)) {
			return cache.get(m);
		} else {
			byte[] res = semantics.convert(m);
			cache.put(m, res);
			return res;
		}
	}

	public Marking execute(Marking m, Transition t) {
		synchronized (mutex) {
			byte[] s = convert(m);
			semantics.setState(s);
			semantics.directExecuteExecutableTransition(t);
			return semantics.convert(semantics.getState());
		}
	}

	public Collection<Transition> getEnabledTransitions(Marking m) {
		synchronized (mutex) {
			byte[] s = convert(m);
			semantics.setState(s);
			return semantics.getExecutableTransitions();
		}
	}

	public boolean isEnabled(Transition t, Marking m) {
		synchronized (mutex) {
			byte[] s = convert(m);
			semantics.setState(s);
			return semantics.isEnabled(t);
		}
	}

	public boolean isMarked(Place p, Marking state) {
		synchronized (mutex) {
			return semantics.isMarked(convert(state), p);
		}

	}
}
