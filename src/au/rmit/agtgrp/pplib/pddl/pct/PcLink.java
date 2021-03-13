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
package au.rmit.agtgrp.pplib.pddl.pct;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class PcLink implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final WeakHashMap<PcLink, WeakReference<PcLink>> CACHE = new WeakHashMap<PcLink, WeakReference<PcLink>>();

	public static PcLink getCached(PcLink pcLink) {
		WeakReference<PcLink> cached = (WeakReference<PcLink>) CACHE.get(pcLink);
		if (cached != null) {
			PcLink cachedPc = (PcLink) cached.get();
			if (cachedPc != null) {
				return cachedPc;
			}
		}

		CACHE.put(pcLink, new WeakReference<PcLink>(pcLink));
		return pcLink;
	}
	
	private final Producer first;
	private final Consumer second;
	private final int hashcode;
	
	public PcLink(Producer first, Consumer second) {
		this.first = first;
		this.second = second;
		hashcode = computeHashCode();
	}

	public Producer getProducer() {
		return first;
	}

	public Consumer getConsumer() {
		return second;
	}

	public PcLink intern() {
		return getCached(this);
	}
	
	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public int hashCode() {
		return hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PcLink other = (PcLink) obj;
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

	@Override
	public String toString() {
		return first.toString() + " -> " + second.toString();
	}

}