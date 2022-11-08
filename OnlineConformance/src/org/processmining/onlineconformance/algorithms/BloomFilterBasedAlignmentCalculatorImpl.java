package org.processmining.onlineconformance.algorithms;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.processmining.onlineconformance.models.ModelSemantics;
import org.processmining.onlineconformance.models.Move;
import org.processmining.onlineconformance.models.PartialAlignment;
import org.processmining.onlineconformance.models.PartialAlignment.State;
import org.processmining.onlineconformance.models.PriceList;
import org.processmining.onlineconformance.models.impl.PartialAlignmentStateFunnelImpl;

import com.google.common.hash.BloomFilter;

public class BloomFilterBasedAlignmentCalculatorImpl<M, L, T, S> {

	private final S initialStateInModel;
	private final S finalStateInModel;
	private final ModelSemantics<M, S, T> semantics;
	private final PriceList<L, T> priceList;

	public BloomFilterBasedAlignmentCalculatorImpl(S initialStateInModel, S finalStateInModel,
			ModelSemantics<M, S, T> semantics, PriceList<L, T> priceList) {
		super();
		this.initialStateInModel = initialStateInModel;
		this.finalStateInModel = finalStateInModel;
		this.semantics = semantics;
		this.priceList = priceList;
	}

	public PartialAlignment<L, T, S> align(List<L> trace) {
		BloomFilter<PartialAlignment.State<S, L, T>> bloomFilter = BloomFilter
				.create(new PartialAlignmentStateFunnelImpl<S, L, T>(), 10000, 0.01);
		return searchStateSpace(trace, bloomFilter);
	}

	private PartialAlignment.State<S, L, T> getInitialStateInSynProduct() {
		return PartialAlignment.State.Factory.construct(initialStateInModel, 0, null, null);
	}

	private boolean isFinal(final PartialAlignment.State<S, L, T> state, final int traceLength) {
		return state.getNumLabelsExplained() == traceLength && state.getStateInModel().equals(finalStateInModel);
	}

	public double fetchCosts(final PartialAlignment.State<S, L, T> state) {
		double costs = 0;
		PartialAlignment.State<S, L, T> parent = state.getParentState();
		while (parent != null) {
			costs += state.getParentMove().getCost();
			parent = parent.getParentState();
		}
		return costs;
	}

	public PartialAlignment<L, T, S> searchStateSpace(List<L> trace,
			BloomFilter<PartialAlignment.State<S, L, T>> bloomFilter) {
		//		Stack<PartialAlignment.State<S, L, T>> globalStack = new Stack<>();
		Queue<PartialAlignment.State<S, L, T>> globalQueue = new PriorityQueue<>(10, // HV: Initial capacity added to get it to compile.
				new Comparator<PartialAlignment.State<S, L, T>>() {
					public int compare(State<S, L, T> o1, State<S, L, T> o2) {
						return new Double(fetchCosts(o1)).compareTo(fetchCosts(o2));
					}
				});
		Queue<PartialAlignment.State<S, L, T>> syncStack = new LinkedBlockingQueue<>();
		PartialAlignment.State<S, L, T> currentState = getInitialStateInSynProduct();
		while (currentState != null && !isFinal(currentState, trace.size())) {
			if (!bloomFilter.mightContain(currentState)) {
				bloomFilter.put(currentState);
				int labelToSynchronize = currentState.getNumLabelsExplained();
				for (T t : semantics.getEnabledTransitions(currentState.getStateInModel())) {
					final S newStateInModel = semantics.execute(currentState.getStateInModel(), t);
					if (labelToSynchronize < trace.size()) { // we should be able to do synchronous moves...
						if (priceList.getTransitionToLabelsMap().containsKey(t)
								&& priceList.getTransitionToLabelsMap().get(t).equals(trace.get(labelToSynchronize))) {
							PartialAlignment.State<S, L, T> newState = PartialAlignment.State.Factory.construct(
									newStateInModel, labelToSynchronize + 1, currentState, Move.Factory.construct(
											trace.get(labelToSynchronize), t, priceList.getPriceOfSynchronous()));
							if (!bloomFilter.mightContain(newState)) {
								syncStack.add(newState);
							}
						}
					} // model moves
					PartialAlignment.State<S, L, T> newState = PartialAlignment.State.Factory.construct(newStateInModel,
							labelToSynchronize, currentState,
							Move.Factory.construct((L) null, t, priceList.getPriceOfTransition(t)));
					if (!bloomFilter.mightContain(newState)) {
						globalQueue.add(newState);
					}
				}
				if (labelToSynchronize < trace.size()) { // we should be able to do synchronous moves...
					// labelMove:
					PartialAlignment.State<S, L, T> newState = PartialAlignment.State.Factory.construct(
							currentState.getStateInModel(), labelToSynchronize + 1, currentState,
							Move.Factory.construct(trace.get(labelToSynchronize), (T) null,
									priceList.getPriceOfLabel(trace.get(labelToSynchronize))));
					if (!bloomFilter.mightContain(newState)) {
						globalQueue.add(newState);
					}
				}
			}
			// next state selection
			if (!syncStack.isEmpty()) {
				currentState = syncStack.poll();
			} else if (!globalQueue.isEmpty()) {
				currentState = globalQueue.poll();
			} else {
				currentState = null;
			}
		}
		return PartialAlignment.Factory.construct(currentState);
	}

}
