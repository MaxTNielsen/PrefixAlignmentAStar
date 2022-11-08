package org.processmining.onlineconformance.models;

import java.util.Collection;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.onlineconformance.models.impl.ModelSemanticsPetrinetMarkingTransitionImpl;

/**
 * provides common semantic utilities for a given model
 * 
 * @author svzelst
 *
 * @param <M>
 *            type of model
 * @param <S>
 *            type of state in model
 * @param <T>
 *            type of transition from state to state in model
 */
public interface ModelSemantics<M, S, T> {

	M getModel();

	S execute(S s, T t);

	Collection<T> getEnabledTransitions(S s);

	boolean isEnabled(T t, S s);

	public class Factory {

		public static ModelSemantics<Petrinet, Marking, Transition> construct(Petrinet net) {
			return new ModelSemanticsPetrinetMarkingTransitionImpl(net);
		}

	}

}
