package org.processmining.onlineconformance.models.hipster4j;

import org.processmining.onlineconformance.models.PartialAlignment;
import org.processmining.onlineconformance.models.PartialAlignment.State;

import es.usc.citius.hipster.model.Transition;
import es.usc.citius.hipster.model.function.CostFunction;

public class PartialAlignmentCostFunctionImpl<S, L, T>
		implements CostFunction<Void, PartialAlignment.State<S, L, T>, Double> {

	public Double evaluate(Transition<Void, State<S, L, T>> transition) {
		return transition.getState().getParentMove().getCost();
	}

}
