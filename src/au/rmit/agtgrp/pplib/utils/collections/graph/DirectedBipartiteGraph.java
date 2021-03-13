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
package au.rmit.agtgrp.pplib.utils.collections.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DirectedBipartiteGraph<V, W> {

	protected Map<V, Set<W>> linksFrom;
	protected Map<W, Set<V>> linksTo;

	public DirectedBipartiteGraph() {
		linksFrom = new HashMap<V, Set<W>>();
		linksTo = new HashMap<W, Set<V>>();
	}

	public Set<V> getSourceVertices() {
		return linksFrom.keySet();
	}

	public Set<W> getDestinationVertices() {
		return linksTo.keySet();
	}

	public void addSourceVertex(V vertex) {
		if (!linksFrom.containsKey(vertex)) {
			linksFrom.put(vertex, new HashSet<W>());
		}
	}

	public void addSourceVertices(Collection<V> vertices) {
		for (V vertex : vertices)
			addSourceVertex(vertex);
	}

	public void addDestinationVertex(W vertex) {
		if (!linksTo.containsKey(vertex)) {
			linksTo.put(vertex, new HashSet<V>());
		}
	}

	public void addDestinationVertices(Collection<W> vertices) {
		for (W vertex : vertices)
			addDestinationVertex(vertex);
	}

	public void addEdge(V source, W dest) {
		addSourceVertex(source);
		addDestinationVertex(dest);
		linksFrom.get(source).add(dest);
		linksTo.get(dest).add(source);
	}

	public void removeEdge(V source, W dest) {
		if (linksFrom.containsKey(source))
			linksFrom.get(source).remove(dest);

		if (linksTo.containsKey(dest))
			linksTo.get(dest).remove(source);
	}

	public boolean containsEdge(V source, W dest) {
		return linksFrom.containsKey(source) && linksFrom.get(source).contains(dest);
	}

	public Set<V> getEdgesTo(W dest) {
		if (linksTo.containsKey(dest))
			return linksTo.get(dest);

		return Collections.emptySet();
	}

	public Set<W> getEdgesFrom(V source) {
		if (linksFrom.containsKey(source))
			return linksFrom.get(source);

		return Collections.emptySet();
	}

	public int getSize() {
		int size = 0;
		for (V source : linksFrom.keySet())
			size += linksFrom.get(source).size();

		return size;
	}
	
	public void clear() {
		linksFrom.clear();
		linksTo.clear();
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (V v : linksFrom.keySet())
			sb.append(v + " -> " + linksFrom.get(v) + "\n");

		return sb.toString();
	}

}
