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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.csp.alldiff.AllDifferent;
import au.rmit.agtgrp.pplib.fol.expression.Expression;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;
import au.rmit.agtgrp.pplib.utils.collections.graph.UndirectedGraph;

//TODO: change to implements Csp<Expression<Term>> to allow for 
// arbitrary constraints between variables and constants
public class ExpressionCsp implements Csp<Expression<Variable>> {


	private static final long serialVersionUID = 1L;

	protected List<Variable> vars;
	protected List<Constant> domain;
	protected Map<Variable, Set<Constant>> varDomains;
	protected Map<List<Variable>, List<Expression<Variable>>> constraints;

	public ExpressionCsp() {
		vars = new ArrayList<Variable>();
		domain = new ArrayList<Constant>();
		varDomains = new HashMap<Variable, Set<Constant>>();
		constraints = new HashMap<List<Variable>, List<Expression<Variable>>>();
	}

	@Override
	public void addVariable(Variable v) {
		if (v == null)
			throw new NullPointerException();
		
		if (varDomains.get(v) == null)
			varDomains.put(v, new HashSet<Constant>());

		if (!vars.contains(v))
			vars.add(v);
	}

	@Override
	public void addVariables(Collection<Variable> vs) {
		for (Variable var : vs)
			addVariable(var);
	}

	@Override
	public List<Variable> getVariables() {
		return vars;
	}

	@Override
	public void addConstraint(Expression<Variable> con) {
		if (con == null)
			throw new NullPointerException();
		
		List<Variable> domain = new ArrayList<Variable>(con.getDomain());

		if (!vars.containsAll(domain)) {
			domain.removeAll(vars);
			throw new IllegalArgumentException("Unknown variable in constraint!\n Constraint: " + con + "\nVars: " + domain);

		}
		Collections.sort(domain, Comparators.SYMBOL_COMPARATOR);
		List<Expression<Variable>> cons = constraints.get(domain);

		if (cons == null) {
			cons = new ArrayList<Expression<Variable>>();
			constraints.put(domain, cons);
		}

		cons.add(con);

	}

	@Override
	public void addConstraint(List<Variable> vars, Expression<Variable> con) {
		if (vars == null || con == null)
			throw new NullPointerException();
		
		List<Expression<Variable>> list = new ArrayList<Expression<Variable>>();
		list.add(con);

		addConstraints(vars, list);
	}

	@Override
	public void addConstraints(List<Variable> vars, Collection<Expression<Variable>> cons) {
		if (vars == null || cons == null)
			throw new NullPointerException();
		
		for (Variable v : vars)
			if (!this.vars.contains(v))
				throw new IllegalArgumentException("Unknown variable: " + v);

		vars = new ArrayList<Variable>(vars);
		Collections.sort(vars, Comparators.SYMBOL_COMPARATOR);

		if (!constraints.containsKey(vars))
			constraints.put(vars, new ArrayList<Expression<Variable>>());

		for (Expression<Variable> con : cons) {
			Set<Variable> domain = con.getDomain();
			if (!domain.containsAll(vars) || domain.size() != vars.size())
				throw new IllegalArgumentException("Variables != domain of constraint");

			if (!constraints.get(vars).contains(con))
				constraints.get(vars).add(con);
		}
	}

	@Override
	public Map<List<Variable>, List<Expression<Variable>>> getConstraints() {
		return constraints;
	}

	@Override
	public List<Expression<Variable>> getConstraints(List<Variable> vars) {
		vars = new ArrayList<Variable>(vars);
		Collections.sort(vars, Comparators.SYMBOL_COMPARATOR);
		return constraints.get(vars);
	}

	@Override
	public void addDomainValue(Constant dv) {
		if (dv == null)
			throw new NullPointerException("Cannot be null");
		
		if (!domain.contains(dv))
			domain.add(dv);
	}

	@Override
	public void addDomainValues(Collection<Constant> dvs) {
		for (Constant dv : dvs)
			addDomainValue(dv);
	}

	public void addDomainValue(Variable var, Constant dv) {
		addDomainValue(dv);
		addVariable(var);
		if (!varDomains.get(var).contains(dv))
			varDomains.get(var).add(dv);
	}

	public void addDomainValues(Variable var, Collection<Constant> dvs) {
		addDomainValues(dvs);
		addVariable(var);
		for (Constant dv : dvs) {
			if (!varDomains.get(var).contains(dv))
				varDomains.get(var).add(dv);
		}
	}

	@Override
	public List<Constant> getDomain() {
		return domain;
	}

	@Override
	public Set<Constant> getDomain(Variable v) {
		return varDomains.get(v);
	}

	@Override
	public Map<Variable, Set<Constant>> getDomains() {
		return varDomains;
	}

	@Override
	public UndirectedGraph<Variable> getPrimalGraph() {
		UndirectedGraph<Variable> primalGraph = new UndirectedGraph<Variable>();

		// all vars with domain size > 1
		Set<Variable> usableVars = new HashSet<Variable>();
		for (Variable var : vars) {
			if (varDomains.get(var).size() > 1)
				usableVars.add(var);
		}

		for (List<Variable> domain : constraints.keySet()) {
			// just in case
			List<Expression<Variable>> cons = constraints.get(domain);
			if (cons.size() == 1 && 
				cons.get(0).isLiteral() && 
				AllDifferent.isAllDifferentLiteral(cons.get(0).getLiteral())) {
				continue;
			}
			
			if (!cons.isEmpty()) {
				// filter those with domain size <= 1
				List<Variable> filtered = new ArrayList<Variable>(domain);
				filtered.retainAll(usableVars);

				for (int i = 0; i < filtered.size(); i++) {
					for (int j = i + 1; j < filtered.size(); j++) {

						Variable v1 = filtered.get(i);
						Variable v2 = filtered.get(j);

						primalGraph.addEdge(v1, v2);
					}
				}
			}
		}
		return primalGraph;
	}

	public UndirectedGraph<List<Variable>> getDualGraph() {

		UndirectedGraph<List<Variable>> dualGraph = new UndirectedGraph<List<Variable>>();

		for (List<Variable> vars1 : constraints.keySet()) {
			if (!constraints.get(vars1).isEmpty()) {
				List<Variable> filtered1 = new ArrayList<Variable>();
				for (Variable var : vars1)
					if (varDomains.get(var).size() > 1)
						filtered1.add(var);

				for (List<Variable> vars2 : constraints.keySet()) {
					if (!constraints.get(vars2).isEmpty()) {

						List<Variable> filtered2 = new ArrayList<Variable>();
						for (Variable var : vars2)
							if (varDomains.get(var).size() > 1)
								filtered2.add(var);

						if (!filtered1.equals(filtered2) && !dualGraph.containsEdge(filtered1, filtered2)) {

							List<Variable> intersection = new ArrayList<Variable>(filtered1);
							intersection.retainAll(filtered2);
							if (!intersection.isEmpty())
								dualGraph.addEdge(filtered1, filtered2);
						}
					}

				}
			}

		}

		return dualGraph;
	}

	public UndirectedGraph<Object> getIncidenceGraph() {
		UndirectedGraph<Object> incGraph = new UndirectedGraph<Object>();

		for (List<Variable> cons : constraints.keySet()) {
			if (!constraints.get(cons).isEmpty()) {
				for (Variable var : cons) {
					if (!incGraph.containsEdge(var, cons) && varDomains.get(var).size() > 1) {
						incGraph.addEdge(var, cons);
					}
				}
			}
		}

		return incGraph;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Variables/domains:\n");
		for (Variable var : vars)
			sb.append(var + " = " + varDomains.get(var) + "\n");
		sb.append("Constraints:");
		for (List<Variable> domain : constraints.keySet()) {
			for (Expression<Variable> cons : constraints.get(domain))
				sb.append("\n" + cons);
		}
		return sb.toString();
	}

}
