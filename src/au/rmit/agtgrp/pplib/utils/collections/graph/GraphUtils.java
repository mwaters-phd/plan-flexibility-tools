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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.rmit.agtgrp.pplib.utils.collections.Pair;

public class GraphUtils {

	public static <V> Set<Pair<V, V>> addAndCloseTransitive(DirectedGraph<V> graph, V source, V dest) {

		Set<Pair<V, V>> newLinks = new HashSet<Pair<V, V>>();
		newLinks.add(Pair.instance(source, dest));

		graph.addEdge(source, dest);

		Set<V> linksTo = new HashSet<V>(graph.getEdgesTo(source));
		linksTo.add(source);

		Set<V> linksFrom = new HashSet<V>(graph.getEdgesFrom(dest));
		linksFrom.add(dest);

		for (V v1 : linksTo) {
			for (V v2 : linksFrom) {
				if (!graph.containsEdge(v1, v2)) {
					graph.addEdge(v1, v2);
					newLinks.add(Pair.instance(v1, v2));
				}
			}
		}

		return newLinks;

	}

	public static <V> boolean isTransitive(DirectedGraph<V> graph) {
		for (V v1 : graph.getVertices()) {
			for (V v2 : graph.getEdgesFrom(v1)) {
				for (V v3 : graph.getEdgesFrom(v2)) {
					if (!graph.containsEdge(v1, v3)) {
						System.out.println(v1 + " -> " + v2 + " -> " + v3);
						return false;
					}
				}
			}
		}

		return true;
	}

	public static <V> void transitiveReduction(DirectedGraph<V> graph, DirectedGraph<V> closed) {

		reflexiveReduction(graph);

		for (V v1 : closed.getVertices()) {
			for (V v2 : closed.getEdgesTo(v1)) { // v2 --> v1
				for (V v3 : closed.getEdgesFrom(v1)) { // v1 --> v3
					if (graph.containsEdge(v2, v1) && graph.containsEdge(v1, v3))
						graph.removeEdge(v2, v3);
				}
			}
		}
	}

	public static <V> void reflexiveReduction(DirectedGraph<V> graph) {
		for (V var : new HashSet<V>(graph.getVertices()))
			graph.removeEdge(var, var);
	}

	public static <U> List<U> getLinearExtension(DirectedGraph<U> graph) {
		Set<U> verts = new HashSet<U>(graph.getVertices());
		Set<U> mins = new HashSet<U>();

		List<U> sort = new ArrayList<U>();

		while (!verts.isEmpty()) {
			boolean found = false;
			for (U vert : verts) {
				Collection<U> edgesTo = new ArrayList<U>(graph.getEdgesTo(vert));
				edgesTo.removeAll(mins);
				if (edgesTo.isEmpty()) {
					sort.add(vert);
					verts.remove(vert);
					mins.add(vert);
					found = true;
					break;
				}
			}

			if (!found)
				throw new IllegalArgumentException("Cyclic graph detected!");
		}

		return sort;
	}
	
	
	private GraphUtils() { }

}
