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

import au.rmit.agtgrp.pplib.utils.collections.Pair;

public class DirectedGraph<V> {

	protected Map<V, Set<V>> linksTo;
	protected Map<V, Set<V>> linksFrom;

	public DirectedGraph() {
		linksFrom = new HashMap<V, Set<V>>();
		linksTo = new HashMap<V, Set<V>>();
	}

	public Set<V> getVertices() {
		return linksFrom.keySet();
	}

	public void addVertex(V vertex) {
		if (vertex == null)
			throw new NullPointerException("Vertex cannot be null");
		
		if (!linksFrom.containsKey(vertex)) {
			linksFrom.put(vertex, new HashSet<V>());
			linksTo.put(vertex, new HashSet<V>());
		}
	}

	public void addVertices(Collection<V> vertices) {
		for (V vertex : vertices)
			addVertex(vertex);
	}

	public void addEdge(V source, V dest) {
		addVertex(source);
		addVertex(dest);
		linksFrom.get(source).add(dest);
		linksTo.get(dest).add(source);
	}

	public void removeEdge(V source, V dest) {
		if (source == null || dest == null)
			throw new NullPointerException("Vertices cannot be null");
		
		if (linksFrom.containsKey(source))
			linksFrom.get(source).remove(dest);

		if (linksTo.containsKey(dest))
			linksTo.get(dest).remove(source);
	}

	public boolean containsEdge(V source, V dest) {
		if (source == null || dest == null)
			throw new NullPointerException("Vertices cannot be null");
		
		return linksFrom.containsKey(source) && linksFrom.get(source).contains(dest);
	}

	public Set<V> getEdgesTo(V dest) {
		if (dest == null)
			throw new NullPointerException("Vertices cannot be null");
		
		if (linksTo.containsKey(dest))
			return linksTo.get(dest);

		return Collections.emptySet();
	}

	public Set<V> getEdgesFrom(V source) {
		if (source == null)
			throw new NullPointerException("Vertices cannot be null");
		
		if (linksFrom.containsKey(source))
			return linksFrom.get(source);

		return Collections.emptySet();
	}

	public Set<Pair<V, V>> getAllEdges() {
		Set<Pair<V, V>> all = new HashSet<Pair<V, V>>();
		for (V vertex : linksTo.keySet()) {
			for (V other : linksTo.get(vertex))
				all.add(Pair.instance(other, vertex));
		}

		return all;
	}

	public int getSize() {
		return getAllEdges().size();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (V v : linksFrom.keySet())
			sb.append(v + " -> " + linksFrom.get(v) + "\n");

		return sb.toString();
	}

}
