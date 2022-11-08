package org.processmining.onlineconformance.models;

import java.util.Collection;
import java.util.List;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.onlineconformance.models.impl.StubbornSemanticsPetrinetImpl;
import org.processmining.onlineconformance.models.impl.StubbornSemanticsTrivialImpl;

/**
 * given a model type <M>, state in model type <S> and state-state transition
 * type <T> this provider allows us to answer questions regarding the
 * 
 * @author svzelst
 *
 * @param <M>
 * @param <S>
 * @param <T>
 */
public interface StubbornSemantics<M, S, T> {

	public class Factory {

		public static <M, S, T> StubbornSemantics<M, S, T> construct(ModelSemantics<M, S, T> semantics) {
			return new StubbornSemanticsTrivialImpl<>();
		}

		public static <S> StubbornSemantics<Petrinet, S, Transition> consturct(
				ModelSemantics<Petrinet, S, Transition> semantics) {
			if (semantics instanceof ModelSemanticsPetrinet) {
				return construct((ModelSemanticsPetrinet<S>) semantics);
			}
			return new StubbornSemanticsTrivialImpl<>();
		}

		public static <S> StubbornSemantics<Petrinet, S, Transition> construct(ModelSemanticsPetrinet<S> semantics) {
			return new StubbornSemanticsPetrinetImpl<>(semantics);
		}

	}

	/**
	 * returns all stubborn sets for a given state <S>
	 * 
	 * @param state
	 * @return
	 */
	public List<Collection<T>> getStubbornSets(S state);

	/**
	 * returns all stubborn sets for a given state <S> given that we know that
	 * there are some existing sets <T>[][] known, and, a set of <T>[][] known
	 * stubborn sets that we wish to ignore
	 * 
	 * @param known
	 * @param state
	 * @param ignore
	 * @return
	 */
	public List<Collection<T>> getStubbornSets(List<Collection<T>> known, S state, List<Collection<T>> ignore);

	/**
	 * check whether the given set of transitions is stubborn given the state
	 * 
	 * @param set
	 *            potential stubborn set
	 * @param state
	 *            in model
	 * @return true iff set is stubborn in state
	 */
	public boolean isStubborn(Collection<T> set, S state);

	/**
	 * retains all sets that do not contain the action, i.e., property of
	 * stubborn sets: a stubborn set remains stubborn if a transition outside of
	 * it is performed.
	 * 
	 * @param sets
	 * @param action
	 * @return
	 */
	public List<Collection<T>> retain(List<Collection<T>> sets, T action);

	/**
	 * will return those ignore sets that at least contain an enabled
	 * transition.
	 * 
	 * @param ignore
	 * @param state
	 * @return
	 */
	public List<Collection<T>> updateIgnoreSets(List<Collection<T>> ignore, S state);

}
