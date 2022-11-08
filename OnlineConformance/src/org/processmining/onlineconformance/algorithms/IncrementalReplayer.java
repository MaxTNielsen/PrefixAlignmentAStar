
package org.processmining.onlineconformance.algorithms;

import java.util.Map;

import org.processmining.onlineconformance.algorithms.impl.IncrementalRevBasedReplayerImpl;
import org.processmining.onlineconformance.models.ModelSemantics;
import org.processmining.onlineconformance.models.PartialAlignment;
import org.processmining.onlineconformance.parameters.IncrementalReplayerParametersImpl;
import org.processmining.onlineconformance.parameters.IncrementalRevBasedReplayerParametersImpl;

public interface IncrementalReplayer<M, C, S, T, L, A extends PartialAlignment<L, T, S>, P extends IncrementalReplayerParametersImpl<M, L, T>> {

	public class Factory {

		public static <M, C, S, T, L, A extends PartialAlignment<L, T, S>, P extends IncrementalReplayerParametersImpl<M, L, T>> IncrementalReplayer<M, C, S, T, L, A, P> construct(
				final S initialStateInModel, final S finalStateInModel, final Map<C, A> dataStore,
				final ModelSemantics<M, S, T> modelSemantics, final P parameters, final Map<T, L> labelmap,
				final Strategy strategy) throws IllegalArgumentException {
			switch (strategy) {
				case REVERT_BASED :
				default :
					throw new IllegalArgumentException("Parameters do not match chosen strategy");
			}
		}

		public static <M, C, S, T, L, A extends PartialAlignment<L, T, S>, P extends IncrementalRevBasedReplayerParametersImpl<M, L, T>> IncrementalReplayer<M, C, S, T, L, A, P> construct(
				final S initialStateInModel, final S finalStateInModel, final Map<C, A> dataStore,
				final ModelSemantics<M, S, T> modelSemantics, final P parameters, final Map<T, L> labelmap,
				final Strategy strategy) {
			switch (strategy) {
				case REVERT_BASED :
				default :
					return new IncrementalRevBasedReplayerImpl<M, C, S, T, L, A, P>(initialStateInModel,
							finalStateInModel, dataStore, modelSemantics, parameters, labelmap);
			}
		}
		
		
	}

	public enum SearchAlgorithm {
		A_STAR, IDA_STAR;
	}

	public enum Strategy {
		REVERT_BASED;
	}

	Map<C, A> getDataStore();

	S getFinalStateInModel();

	S getInitialStateInModel();

	A processEvent(C c, L l);

	PartialAlignment.State<S, L, T> getInitialState();

	P getParameters();

	Strategy getStrategy();

}
