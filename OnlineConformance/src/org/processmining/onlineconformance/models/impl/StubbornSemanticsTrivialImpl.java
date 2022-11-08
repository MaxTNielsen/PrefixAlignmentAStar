package org.processmining.onlineconformance.models.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.processmining.onlineconformance.models.StubbornSemantics;

/**
 * 
 * @author svzelst
 *
 * @param <M>
 * @param <S>
 * @param <T>
 */
public class StubbornSemanticsTrivialImpl<M, S, T> implements StubbornSemantics<M, S, T> {

	public List<Collection<T>> getStubbornSets(S state) {
		return new ArrayList<>();
	}

	public List<Collection<T>> getStubbornSets(List<Collection<T>> known, S state, List<Collection<T>> ignore) {
		return new ArrayList<>();
	}

	public boolean isStubborn(Collection<T> set, S state) {
		return true;
	}

	public List<Collection<T>> retain(List<Collection<T>> sets, T action) {
		return sets;
	}

	public List<Collection<T>> updateIgnoreSets(List<Collection<T>> ignore, S state) {
		return ignore;
	}

}
