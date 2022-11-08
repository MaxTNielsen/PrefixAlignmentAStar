package org.processmining.onlineconformance.util;

import java.util.Collection;
import java.util.List;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ListUtils {

	public static <T> TObjectIntMap<T> parikhVector(List<T> l) {
		TObjectIntMap<T> result = new TObjectIntHashMap<>();
		for (T t : l) {
			result.adjustOrPutValue(t, 1, 1);
		}
		return result;
	}

	public static <T> boolean contains(List<Collection<T>> list, T t) {
		for (Collection<T> l : list) {
			if (l.contains(t)) {
				return true;
			}
		}
		return false;
	}

}
