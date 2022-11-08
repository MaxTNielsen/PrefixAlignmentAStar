package org.processmining.onlineconformance.models.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.onlineconformance.models.ModelSemanticsPetrinet;
import org.processmining.onlineconformance.models.StubbornSemantics;
import org.processmining.onlineconformance.util.ListUtils;

public class StubbornSemanticsPetrinetImpl<S> implements StubbornSemantics<Petrinet, S, Transition> {

	private final ModelSemanticsPetrinet<S> semantics;
	private final Object mutex = new Object();

	public StubbornSemanticsPetrinetImpl(final ModelSemanticsPetrinet<S> semantics) {
		this.semantics = semantics;
	}

	public List<Collection<Transition>> getStubbornSets(S state) {
		synchronized (mutex) {
			return getStubbornSets(new ArrayList<Collection<Transition>>(), state,
					new ArrayList<Collection<Transition>>());
		}

	}

	public List<Collection<Transition>> getStubbornSets(List<Collection<Transition>> known, S state,
			List<Collection<Transition>> ignore) {
		synchronized (mutex) {
			List<Collection<Transition>> stubbornSets = new ArrayList<>(known);
			for (Transition t : semantics.getEnabledTransitions(state)) {
				if (!ListUtils.contains(stubbornSets, t) && !ListUtils.contains(ignore, t)) {
					stubbornSets.add(constructStubbornSet(state, t));
				}
			}
			return stubbornSets;
		}
	}

	private Collection<Transition> constructStubbornSet(S state, Transition transition) {
		Collection<Transition> enabled = semantics.getEnabledTransitions(state);
		Collection<Transition> stubbornSet = new ArrayList<>();
		stubbornSet.add(transition);
		boolean isStubborn = false;
		while (!isStubborn) {
			isStubborn = true;
			isStubborn &= disabledTransitionRule(state, stubbornSet, enabled);
			if (!isStubborn) {
				stubbornSet = disabledTransitionExpansion(state, stubbornSet, enabled);
			} else {
				isStubborn &= enabledTransitionRule(stubbornSet, enabled);
				if (!isStubborn) {
					stubbornSet = enabledTransitionExpansion(stubbornSet, enabled);
				} else {
					isStubborn &= containsEnabled(stubbornSet, enabled);
				}
			}
		}
		return stubbornSet;
	}

	private Collection<Transition> disabledTransitionExpansion(final S state, Collection<Transition> stubb,
			final Collection<Transition> enabled) {
		for (Transition t : stubb) {
			if (!enabled.contains(t)) {
				if (!disabledTransitionRule(state, t, stubb)) {
					stubb = disabledTransitionExpansion(state, t, stubb);
				}
			}
		}
		return stubb;
	}

	private Collection<Transition> disabledTransitionExpansion(final S state, final Transition disabled,
			Collection<Transition> stubb) {
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArc : getSemantics().getModel()
				.getInEdges(disabled)) {
			Place inPlace = (Place) inArc.getSource();
			if (!semantics.isMarked(inPlace, state)) {
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArcOfPlace : getSemantics()
						.getModel().getInEdges(inPlace)) {
					Transition inTrans = (Transition) inArcOfPlace.getSource();
					if (!stubb.contains(inTrans)) {
						stubb.add(inTrans);

					}
				}
			}
		}
		return stubb;
	}

	/**
	 * implementation of "rule 1" as described in "State of the Art Report:
	 * STUBBORN SETS"; Antti Valmari, April 1994. implements: "(1) every
	 * disabled transition in Ts has an empty input place p such that all
	 * transitions in pre(p)} are in Ts"
	 * 
	 * @param stubb
	 * @param enabled
	 * @return
	 */
	private boolean disabledTransitionRule(final S state, final Collection<Transition> stubb,
			final Collection<Transition> enabled) {
		boolean result = true;
		for (Transition t : stubb) {
			if (!enabled.contains(t)) {
				if (!disabledTransitionRule(state, t, stubb)) {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	private boolean disabledTransitionRule(final S state, final Transition disabled,
			final Collection<Transition> stubb) {
		boolean result = false;
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArc : semantics.getModel()
				.getInEdges(disabled)) {
			Place inPlace = (Place) inArc.getSource();
			if (!semantics.isMarked(inPlace, state)) {
				if (disabledTransitionRule(disabled, inPlace, stubb)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	private boolean disabledTransitionRule(final Transition disabled, final Place p,
			final Collection<Transition> stubb) {
		boolean result = true;
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArcOfPlace : semantics.getModel()
				.getInEdges(p)) {
			Transition inTrans = (Transition) inArcOfPlace.getSource();
			if (!stubb.contains(inTrans)) {
				result = false;
				break;
			}
		}
		return result;
	}

	/**
	 * implementation of "rule 2" as described in "State of the Art Report:
	 * STUBBORN SETS"; Antti Valmari, April 1994. implements: "(2) no enabled
	 * transition in Ts has an input place in common with any transition outside
	 * Ts""
	 * 
	 * @param stubb
	 * @param enabled
	 * @return
	 */
	private boolean enabledTransitionRule(Collection<Transition> stubb, Collection<Transition> enabled) {
		for (Transition t : stubb) {
			if (enabled.contains(t)) {
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArc : semantics.getModel()
						.getInEdges(t)) {
					Place inPlace = (Place) inArc.getSource();
					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArcOfPlace : getSemantics()
							.getModel().getOutEdges(inPlace)) {
						Transition outTrans = (Transition) inArcOfPlace.getTarget();
						if (!stubb.contains(outTrans)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public ModelSemanticsPetrinet<S> getSemantics() {
		return semantics;
	}

	private Collection<Transition> enabledTransitionExpansion(Collection<Transition> stubb,
			Collection<Transition> enabled) {
		for (Transition t : stubb) {
			if (enabled.contains(t)) {
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArc : getSemantics().getModel()
						.getInEdges(t)) {
					Place inPlace = (Place) inArc.getSource();
					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inArcOfPlace : getSemantics()
							.getModel().getOutEdges(inPlace)) {
						Transition outTrans = (Transition) inArcOfPlace.getTarget();
						if (stubb.contains(outTrans)) {
							stubb.add(outTrans);
						}
					}
				}
			}
		}
		return stubb;
	}

	private boolean containsEnabled(Collection<Transition> stubb, Collection<Transition> enabled) {
		for (Transition t : stubb) {
			if (enabled.contains(t)) {
				return true;
			}
		}
		return false;
	}

	public boolean isStubborn(Collection<Transition> set, S state) {
		synchronized (mutex) {
			Collection<Transition> enabled = getSemantics().getEnabledTransitions(state);
			return disabledTransitionRule(state, set, enabled) && enabledTransitionRule(set, enabled)
					&& containsEnabled(set, enabled);
		}
	}

	public List<Collection<Transition>> retain(List<Collection<Transition>> sets, Transition action) {
		List<Collection<Transition>> newStubb = new ArrayList<>();
		for (Collection<Transition> s : sets) {
			if (!s.contains(action)) {
				newStubb.add(s);
			}
		}
		return newStubb;
	}

	public List<Collection<Transition>> updateIgnoreSets(List<Collection<Transition>> ignore, S state) {
		synchronized (mutex) {
			List<Collection<Transition>> ign = new ArrayList<Collection<Transition>>();
			for (Collection<Transition> tArr : ignore) {
				t: for (Transition t : tArr) {
					if (semantics.isEnabled(t, state)) {
						ign.add(tArr);
						break t;
					}
				}
			}
			return ign;
		}
	}

}
