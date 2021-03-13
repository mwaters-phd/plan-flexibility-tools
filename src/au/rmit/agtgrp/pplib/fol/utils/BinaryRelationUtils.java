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
package au.rmit.agtgrp.pplib.fol.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.predicate.Atom;
import au.rmit.agtgrp.pplib.fol.predicate.Relation;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;
import au.rmit.agtgrp.pplib.utils.collections.graph.GraphUtils;

public class BinaryRelationUtils {

	
	
	
	public static <V extends Term> DirectedGraph<V> toGraph(Relation<V> relation) {
		DirectedGraph<V> graph = new DirectedGraph<V>();
		for (V var : relation.getDomain())
			graph.addVertex(var);

		for (Atom<V> atom : relation)
			graph.addEdge(atom.getParameters().get(0), atom.getParameters().get(1));

		return graph;
	}

	public static <V extends Term> void transitiveReduction(Relation<V> relation, Relation<V> closed) {
		reflexiveReduction(relation);

		DirectedGraph<V> graph = toGraph(closed);
		for (V v1 : graph.getVertices()) {
			for (V v2 : graph.getEdgesTo(v1)) { // v2 --> v1
				for (V v3 : graph.getEdgesFrom(v1)) { // v1 --> v3
					if (relation.contains(Arrays.asList(v2, v1)) && relation.contains(Arrays.asList(v1, v3)))
						relation.remove(Arrays.asList(v2, v3));
				}
			}
		}

		if (!isIntransitive(relation)) {
			throw new RuntimeException();
		}

	}

	public static <V extends Term> void transitiveReduction(Relation<V> relation) {
		reflexiveReduction(relation);

		for (V root : relation.getDomain()) {
			Set<V> visited = new HashSet<V>();
			for (V child : relation.getDomain()) {
				if (relation.contains(Arrays.asList(root, child))) {
					dfsReduction(relation, root, child, visited);
				}
			}
		}
	}

	private static <V extends Term> void dfsReduction(Relation<V> relation, V root, V vertex, Set<V> visited) {
		if (visited.contains(vertex))
			return;

		visited.add(vertex);

		for (V child : relation.getDomain()) {
			if (relation.contains(Arrays.asList(vertex, child))) {
				relation.remove(Arrays.asList(root, child));
				dfsReduction(relation, root, child, visited);
			}
		}
	}

	public static <V extends Term> void reflexiveReduction(Relation<V> relation) {
		for (V var : relation.getDomain())
			relation.remove(Arrays.asList(var, var));
	}

	public static <V extends Term> Set<Atom<V>> addAndCloseTransitive(Relation<V> relation, Atom<V> atom) {

		Set<Atom<V>> newAtoms = new HashSet<Atom<V>>();
		newAtoms.add(atom);
		relation.add(atom);

		DirectedGraph<V> graph = toGraph(relation);

		for (V v1 : graph.getVertices()) {
			if (v1.equals(atom.getParameters().get(0))
					|| relation.contains(Arrays.asList(v1, atom.getParameters().get(0)))) {

				for (V v2 : graph.getEdgesFrom(atom.getParameters().get(1))) {

					if (!graph.containsEdge(v1, v2)) {
						Atom<V> a = new Atom<V>(relation.getPredicateSymbol(), 
								Variable.buildVariables(Term.getTypes(v1, v2)), 
								Arrays.asList(v1, v2), false).intern();
						graph.addEdge(v1, v2);
						newAtoms.add(a);
					}
				}

				if (!graph.containsEdge(v1, atom.getParameters().get(1))) {
					Atom<V> a = new Atom<V>(relation.getPredicateSymbol(),
							Variable.buildVariables(Term.getTypes(v1, atom.getParameters().get(1))),
							Arrays.asList(v1, atom.getParameters().get(1)), false).intern();
					graph.addEdge(v1, atom.getParameters().get(1));
					newAtoms.add(a);
				}
			}
		}

		relation.addAll(newAtoms);

		return newAtoms;

	}

	public static <V extends Term> Set<Atom<V>> addAndCloseTransitiveSymmetric(Relation<V> relation, Atom<V> atom) {

		relation.add(atom);

		Set<Atom<V>> newAtoms = new HashSet<Atom<V>>();
		newAtoms.addAll(closeReflexive(relation));
		newAtoms.addAll(closeSymmetric(relation));

		DirectedGraph<V> graph = toGraph(relation);

		Set<V> edgesTo = new HashSet<V>(graph.getEdgesTo(atom.getParameters().get(0)));
		for (V v1 : edgesTo) {
			Set<V> edgesFrom = new HashSet<V>(graph.getEdgesFrom(atom.getParameters().get(1)));
			for (V v2 : edgesFrom) {
				Atom<V> a1 = new Atom<V>(relation.getPredicateSymbol(), 
						Variable.buildVariables(Term.getTypes(v1, v2)), 
						Arrays.asList(v1, v2), false).intern();
				Atom<V> a2 = new Atom<V>(relation.getPredicateSymbol(), 
						Variable.buildVariables(Term.getTypes(v2, v1)), 
						Arrays.asList(v2, v1), false).intern();

				graph.addEdge(v1, v2);
				graph.addEdge(v2, v1);

				relation.add(a1);
				relation.add(a2);
				newAtoms.add(a1);
				newAtoms.add(a2);
			}

		}

		return newAtoms;
	}

	public static <V extends Term> boolean isSymmetric(Relation<V> relation) {

		for (V v1 : relation.getDomain()) {
			for (V v2 : relation.getDomain()) {
				if (relation.contains(Arrays.asList(v1, v2)) && !relation.contains(Arrays.asList(v2, v1))) {
					return false;
				}
			}
		}

		return true;
	}

	public static <V extends Term> boolean isTransitive(Relation<V> relation) {

		for (V v1 : relation.getDomain()) {
			for (V v2 : relation.getDomain()) {
				if (relation.contains(Arrays.asList(v1, v2))) {
					for (V v3 : relation.getDomain()) {
						if (relation.contains(Arrays.asList(v2, v3)) && !relation.contains(Arrays.asList(v1, v3))) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}

	public static <V extends Term> boolean isIntransitive(Relation<V> relation) {

		for (V v1 : relation.getDomain()) {
			for (V v2 : relation.getDomain()) {
				if (relation.contains(Arrays.asList(v1, v2))) {
					for (V v3 : relation.getDomain()) {
						if (relation.contains(Arrays.asList(v1, v3)) && relation.contains(Arrays.asList(v3, v2))) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}

	public static <V extends Term> Set<Atom<V>> closeTransitive(Relation<V> relation) {

		Set<Atom<V>> newAtoms = new HashSet<Atom<V>>();

		DirectedGraph<V> graph = toGraph(relation);

		for (V v1 : graph.getVertices()) {
			Set<V> edgesTo = new HashSet<V>(graph.getEdgesTo(v1));
			for (V v2 : edgesTo) { // v2 --> v1
				Set<V> edgesFrom = new HashSet<V>(graph.getEdgesFrom(v1));
				for (V v3 : edgesFrom) {// v1 --> v3
					if (!graph.containsEdge(v2, v3)) {
						Atom<V> a = new Atom<V>(relation.getPredicateSymbol(), 
								Variable.buildVariables(Term.getTypes(v2, v3)), 
								Arrays.asList(v2, v3), false).intern();
						graph.addEdge(v2, v3);
						newAtoms.add(a);
					}
				}
			}
		}

		relation.addAll(newAtoms);

		return newAtoms;
	}

	public static <V extends Term> Set<Atom<V>> closeTransitiveDAG(Relation<V> relation) {

		Set<Atom<V>> newAtoms = new HashSet<Atom<V>>();

		DirectedGraph<V> graph = toGraph(relation);

		List<V> topSort = GraphUtils.getLinearExtension(graph);

		Map<V, Set<V>> trcs = new HashMap<V, Set<V>>();
		for (V var : graph.getVertices())
			trcs.put(var, new HashSet<V>());

		for (int i = topSort.size() - 1; i >= 0; i--) {
			V var = topSort.get(i);
			Set<V> trClos = trcs.get(var);
			// trClos.add(var); //DAG! do not link node to itself

			for (V v : graph.getEdgesTo(var)) {
				trcs.get(v).add(var);
				trcs.get(v).addAll(trClos);
			}

		}

		for (V v1 : trcs.keySet()) {
			for (V v2 : trcs.get(v1)) {
				Atom<V> a = new Atom<V>(relation.getPredicateSymbol(),
						Variable.buildVariables(Term.getTypes(v1, v2)), 
						Arrays.asList(v1, v2), false).intern();
				if (!relation.contains(a)) {
					relation.add(a);
					newAtoms.add(a);
				}
			}
		}

		return newAtoms;

	}

	public static <V extends Term> Set<Atom<V>> closeSymmetric(Relation<V> relation) {

		Set<Atom<V>> symmetric = new HashSet<Atom<V>>();

		for (Atom<V> atom : relation.getContents()) {
			Atom<V> symm = new Atom<V>(atom.getSymbol(),
					Variable.buildVariables(Term.getTypes(atom.getParameters().get(1), atom.getParameters().get(0))), 
					Arrays.asList(atom.getParameters().get(1), atom.getParameters().get(0)), false).intern();

			if (!relation.contains(symm))
				symmetric.add(symm);
		}

		relation.addAll(symmetric);

		return symmetric;
	}

	public static <V extends Term> Set<Atom<V>> closeReflexive(Relation<V> relation) {

		Set<Atom<V>> reflexive = new HashSet<Atom<V>>();

		for (V var : relation.getDomain()) {
			Atom<V> refl = new Atom<V>(relation.getPredicateSymbol(), 
					Variable.buildVariables(Term.getTypes(var, var)),
					Arrays.asList(var, var), false).intern();
			if (!relation.contains(refl)) {
				relation.add(refl);
				reflexive.add(refl);
			}
		}

		return reflexive;
	}
	
	private BinaryRelationUtils() { }

}
