package org.processmining.onlineconformance.models;

import java.util.Map;

import gnu.trove.map.TObjectDoubleMap;

/**
 * Defines prices for alignment moves
 * 
 * @author svzelst
 *
 * @param <L>
 *            type of label
 * @param <T>
 *            type of transition
 */
public interface PriceList<L, T> {

	double getPriceOfLabel(L l);

	double getPriceOfTransition(T t);

	double getPriceOfSynchronous();

	Map<T, L> getTransitionToLabelsMap();

	public class Factory {

		public static <L, T> PriceList<L, T> construct(final TObjectDoubleMap<L> labelMoveCosts,
				final TObjectDoubleMap<T> modelMoveCosts, final double unknownLabelCost, final double syncMoveCost,
				final Map<T, L> t2lMap) {
			return new NaiveImpl<>(labelMoveCosts, modelMoveCosts, unknownLabelCost, syncMoveCost, t2lMap);
		}

	}

	public class NaiveImpl<L, T> implements PriceList<L, T> {
		private final TObjectDoubleMap<L> labelMoveCosts;
		private final TObjectDoubleMap<T> modelMoveCosts;
		private final double syncMovecost;
		private final double unknownLabelCost;
		private final Map<T, L> t2lMap;

		public NaiveImpl(final TObjectDoubleMap<L> labelMoveCosts, final TObjectDoubleMap<T> modelMoveCosts,
				final double unknownLabelCost, final double syncMoveCost, final Map<T, L> t2lMap) {
			this.labelMoveCosts = labelMoveCosts;
			this.modelMoveCosts = modelMoveCosts;
			this.syncMovecost = syncMoveCost;
			this.unknownLabelCost = unknownLabelCost;
			this.t2lMap = t2lMap;

		}

		public double getPriceOfLabel(L l) {
			return labelMoveCosts.containsKey(l) ? labelMoveCosts.get(l) : unknownLabelCost;
		}

		public double getPriceOfTransition(T t) {
			return modelMoveCosts.containsKey(t) ? modelMoveCosts.get(t) : Integer.MAX_VALUE;
		}

		public double getPriceOfSynchronous() {
			return syncMovecost;
		}

		public Map<T, L> getTransitionToLabelsMap() {
			return t2lMap;
		}
	}

}
