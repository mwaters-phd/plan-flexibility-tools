/*******************************************************************************
 * MKTR - Minimal k-Treewidth Relaxation
 *
 * Copyright (C) 2018 
 * Max Waters (max.waters@rmit.edu.au)
 * RMIT University, Melbourne VIC 3000
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package au.rmit.agtgrp.pplib.utils.collections;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class Pair<F, S> implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final WeakHashMap<Pair<?,?>, WeakReference<? extends Pair<?,?>>> CACHE = new WeakHashMap<Pair<?,?>, WeakReference<? extends Pair<?,?>>>();

	public static <T extends Pair<?,?>> T getCached(T pair) {
		@SuppressWarnings("unchecked")
		WeakReference<T> cached = (WeakReference<T>) CACHE.get(pair);
		if (cached != null) {
			T cachedPc = (T) cached.get();
			if (cachedPc != null) {
				return cachedPc;
			}
		}

		CACHE.put(pair, new WeakReference<T>(pair));

		return pair;
	}
	
	
	public static <F, S> Pair<F, S> instance(F first, S second) {
		return new Pair<F, S>(first, second);
	}

	private F first;
	private S second;

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public void setFirst(F first) {
		this.first = first;
	}

	public S getSecond() {
		return second;
	}

	public void setSecond(S second) {
		this.second = second;
	}
	
	public Pair<F, S> intern() {
		return getCached(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}
	
	public String toString() {
		return "<" + first.toString() + ", " + second.toString() + ">";
	}

}
