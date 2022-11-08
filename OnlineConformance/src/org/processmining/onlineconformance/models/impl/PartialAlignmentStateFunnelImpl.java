package org.processmining.onlineconformance.models.impl;

import org.processmining.onlineconformance.models.PartialAlignment;
import org.processmining.onlineconformance.models.PartialAlignment.State;

import com.google.common.base.Charsets;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

public class PartialAlignmentStateFunnelImpl<S, L, T> implements Funnel<PartialAlignment.State<S, L, T>> {

	private static final long serialVersionUID = -7874902836560341060L;

	public void funnel(State<S, L, T> state, PrimitiveSink into) {
		into.putString(state.getStateInModel().toString(), Charsets.UTF_8).putInt(state.getNumLabelsExplained());

	}

}
