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
package au.rmit.agtgrp.pplib.csp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.csp.solver.CspSolutionSet;
import au.rmit.agtgrp.pplib.csp.solver.output.CspOutputSet;
import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.expression.Expression;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Atom;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.predicate.Relation;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.BinaryRelationUtils;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;

public class PartitionedExpressionCsp extends ExpressionCsp {

	private static final long serialVersionUID = 1L;

	public static PartitionedExpressionCsp partition(ExpressionCsp csp) {
		return partition(csp, buildEqualityRelation(csp));
	}

	public static PartitionedExpressionCsp partition(ExpressionCsp csp, Relation<Variable> eqRelation) {
		return partition(csp, buildMapping(csp.getVariables(), eqRelation));
	}

	public static PartitionedExpressionCsp partition(ExpressionCsp csp, DirectedGraph<Variable> eqRelation) {
		eqRelation.addVertices(csp.getVariables());
		return partition(csp, buildMapping(eqRelation));
	}

	public static PartitionedExpressionCsp partition(ExpressionCsp csp, Substitution<Variable> sub) {
		Substitution<Variable> s2 = sub;
		if (csp instanceof PartitionedExpressionCsp) {
			PartitionedExpressionCsp pcsp = (PartitionedExpressionCsp) csp;
			Substitution<Variable> origsub = pcsp.partitionSub;

			Map<Variable, Variable> compMap = new HashMap<Variable, Variable>();
			for (Variable var : origsub.getVariables())
				compMap.put(var, sub.apply(origsub.apply(var)));

			s2 = new Substitution<Variable>(compMap);

		}
		
		PartitionedExpressionCsp pcsp = new PartitionedExpressionCsp(s2);

		pcsp.addVariables(sub.getDomain());

		Map<Variable, Set<Constant>> domains = buildDomains(csp, sub);

		for (Variable var : domains.keySet())
			pcsp.addDomainValues(var, domains.get(var));

		Substitution<Variable> id = Substitution.identity(sub.getDomain());
		for (List<Variable> domain : csp.getConstraints().keySet()) {
			Iterator<Expression<Variable>> consIt = csp.getConstraints().get(domain).iterator();

			while (consIt.hasNext()) {
				Expression<Variable> cons = consIt.next(); // remove to save memory
				consIt.remove();
				cons = cons.resetVariables(sub).applySubstitution(id);
				pcsp.addConstraint(cons);
			}
		}

		return pcsp;
	}

	public static <W extends Term> Substitution<W> departition(Substitution<W> sub, Substitution<Variable> partitionSub) {
		Map<Variable, W> depMap = new HashMap<Variable, W>();

		for (Variable var : partitionSub.getVariables())
			depMap.put(var, sub.apply(partitionSub.apply(var)));

		return new Substitution<W>(depMap);
	}

	public <W extends Term> List<Substitution<W>> departitionAll(Collection<Substitution<W>> subs, Substitution<Variable> partitionSub) {
		List<Substitution<W>> deps = new ArrayList<Substitution<W>>();

		for (Substitution<W> sub : subs)
			deps.add(departition(sub, partitionSub));

		return deps;
	}

	private static Relation<Variable> buildEqualityRelation(ExpressionCsp csp) {
		// build relation
		Relation<Variable> eqRelation = new Relation<Variable>(Predicate.EQUALS);
		for (Variable v1 : csp.vars) {
			for (Variable v2 : csp.vars) {
				if (!v1.equals(v2)) {
					Expression<Variable> eq = Expression.buildLiteral(Literal.equals(v1, v2, true));

					List<Variable> key = Arrays.asList(v1, v2);
					Collections.sort(key, Comparators.SYMBOL_COMPARATOR);
					List<Expression<Variable>> cons = csp.constraints.get(key);
					boolean consEq = (cons != null) && (cons.contains(eq));

					Set<Constant> d1 = csp.getDomain(v1);
					Set<Constant> d2 = csp.getDomain(v2);
					boolean domEq = d1.size() == 1 && d1.equals(d2);

					if (consEq || domEq) {
						eqRelation.add(eq.getLiteral().getAtom());
					}
				}
			}
		}

		BinaryRelationUtils.closeSymmetric(eqRelation);
		BinaryRelationUtils.closeTransitive(eqRelation);

		return eqRelation;
	}

	private static Substitution<Variable> buildMapping(Collection<Variable> vars, Relation<Variable> eqRelation) {
	
		int np = 1;
		Map<Integer, Set<Variable>> partitions = new HashMap<Integer, Set<Variable>>();

		// find equal vertices
		for (Variable v1 : vars) {
			boolean found = false;
			for (int p : partitions.keySet()) {
				Variable v2 = partitions.get(p).iterator().next();

				if (eqRelation.contains(Atom.equals(v1, v2, v1, v2))) {
					found = true;
					partitions.get(p).add(v1);
					break;
				}

			}
			if (!found) {
				Set<Variable> part = new HashSet<Variable>();
				part.add(v1);
				partitions.put(np++, part);
			}
		}

		Map<Variable, Variable> mapping = new HashMap<Variable, Variable>();

		for (int p : partitions.keySet()) {
			Type type = null;
			for (Variable var : partitions.get(p)) {
				if (type == null || type.hasSubtype(var.getType())) {
					type = var.getType();
				}
			}
			Variable partitionVar = new Variable(type, "p_" + p).intern();
			for (Variable var : partitions.get(p)) {
				mapping.put(var, partitionVar);
			}
		}

		// init substitution
		return new Substitution<Variable>(mapping);

	}

	private static Substitution<Variable> buildMapping(DirectedGraph<Variable> eqRelation) {
		int np = 1;
		Map<Integer, Set<Variable>> partitions = new HashMap<Integer, Set<Variable>>();

		// find equal vertices
		for (Variable v1 : eqRelation.getVertices()) {
			boolean found = false;
			for (int p : partitions.keySet()) {
				Variable v2 = partitions.get(p).iterator().next();

				if (eqRelation.containsEdge(v1, v2) || eqRelation.containsEdge(v2, v1)) {
					found = true;
					partitions.get(p).add(v1);
					break;
				}

			}
			if (!found) {
				Set<Variable> part = new HashSet<Variable>();
				part.add(v1);
				partitions.put(np++, part);
			}
		}

		Map<Variable, Variable> mapping = new HashMap<Variable, Variable>();

		for (int p : partitions.keySet()) {
			Type type = null;
			for (Variable var : partitions.get(p)) {
				if (type == null || type.hasSubtype(var.getType())) {
					type = var.getType();
				}
			}
			Variable partitionVar = new Variable(type, "p_" + p).intern();
			for (Variable var : partitions.get(p)) {
				mapping.put(var, partitionVar);
			}
		}

		// init substitution
		return new Substitution<Variable>(mapping);

	}

	private static Map<Variable, Set<Constant>> buildDomains(ExpressionCsp csp, Substitution<Variable> sub) {
		Map<Variable, Set<Variable>> partitionMapping = new HashMap<Variable, Set<Variable>>();

		// build partitions
		for (Variable var : sub.getVariables()) {
			Variable part = sub.apply(var);
			Set<Variable> partition = partitionMapping.get(part);
			if (partition == null) {
				partition = new HashSet<Variable>();
				partitionMapping.put(part, partition);
			}

			partition.add(var);
		}

		Map<Variable, Set<Constant>> newDomains = new HashMap<Variable, Set<Constant>>();

		// build domains -- intersection of every var in partition
		for (Variable part : partitionMapping.keySet()) {
			Set<Variable> partition = partitionMapping.get(part);
			Set<Constant> domain = new HashSet<Constant>(csp.domain);
			for (Variable var : partition) {
				domain.retainAll(csp.getDomain(var));
			}
			newDomains.put(part, domain);
		}
		
		return newDomains;
	}

	protected Substitution<Variable> partitionSub;

	public PartitionedExpressionCsp(Substitution<Variable> sub) {
		this.partitionSub = sub;
	}

	public Substitution<Variable> getMapping() {
		return partitionSub;
	}

	public CspSolutionSet departitionSolutions(CspSolutionSet partitionedSolutions) {
		return new PartitionedCspSolutionSet(partitionedSolutions, partitionSub);
	}

	public ExpressionCsp departition() {
		ExpressionCsp dep = new ExpressionCsp();

		// variables
		dep.addVariables(getMapping().getVariables());

		// domains
		for (Variable var : dep.getVariables()) 
			dep.addDomainValues(var, getDomain(getMapping().apply(var)));

		// build partition
		Map<Variable, List<Variable>> partition = new HashMap<Variable, List<Variable>>();
		for (Variable partVar : getVariables())
			partition.put(partVar, new ArrayList<Variable>());

		for (Variable var : dep.getVariables())
			partition.get(getMapping().apply(var)).add(var);

		// build equality constraints over partitioned variables
		for (Variable part : getVariables()) {
			if (getDomain(part).size() > 1) {
				List<Variable> pvars = partition.get(part);
				for (int i = 0; i < pvars.size()-1; i++)
					dep.addConstraint(Expression.buildLiteral(Literal.equals(pvars.get(i), pvars.get(i+1), true)));

			}		
		}

		// translate constraints back
		for (List<Variable> domain : getConstraints().keySet()) {
			for (Expression<Variable> cons : getConstraints(domain)) {
				List<Variable> newParams = new ArrayList<Variable>();
				for (Variable pv : domain)
					newParams.add(partition.get(pv).get(0));

				dep.addConstraint(cons.applySubstitution(Substitution.build(domain, newParams)));

			}
		}

		return dep;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\nPartition: " + partitionSub);
		return sb.toString();
	}

	public static class PartitionedCspSolutionSet extends CspSolutionSet {

		private final Substitution<Variable> partitionSub;

		public PartitionedCspSolutionSet(CspOutputSet cspSols, Map<Integer, Constant> constantMap,
				Substitution<Variable> partitionSub) {
			super(cspSols, new ArrayList<Variable>(partitionSub.getDomain()), constantMap);

			this.partitionSub = partitionSub;
		}

		public PartitionedCspSolutionSet(CspSolutionSet unpartitioned, Substitution<Variable> partitionSub) {
			super(unpartitioned.getCspOutputSet(), unpartitioned.getVariables(), unpartitioned.getConstantMap());

			this.partitionSub = partitionSub;
		}

		@Override
		public Iterator<Substitution<Constant>> iterator() {
			return new PartitionedCspSolutionIterator(super.iterator(), partitionSub);
		}

	}

	public static class PartitionedCspSolutionIterator implements Iterator<Substitution<Constant>> {

		private final Iterator<Substitution<Constant>> solnIterator;
		private final Substitution<Variable> partitionSub;

		public PartitionedCspSolutionIterator(Iterator<Substitution<Constant>> solnIterator,
				Substitution<Variable> partitionSub) {
			this.solnIterator = solnIterator;
			this.partitionSub = partitionSub;
		}

		@Override
		public boolean hasNext() {
			return solnIterator.hasNext();
		}

		@Override
		public Substitution<Constant> next() {
			return departition(solnIterator.next(), partitionSub);
		}

	}

}
