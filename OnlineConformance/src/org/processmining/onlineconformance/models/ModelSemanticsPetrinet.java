package org.processmining.onlineconformance.models;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.onlineconformance.models.impl.ModelSemanticsPetrinetMarkingTransitionImpl;

public interface ModelSemanticsPetrinet<S> extends ModelSemantics<Petrinet, S, Transition> {

	boolean isMarked(Place p, S state);

	public class Factory {

		public static ModelSemanticsPetrinet<Marking> construct(Petrinet net) {
			return new ModelSemanticsPetrinetMarkingTransitionImpl(net);
		}

	}

}
