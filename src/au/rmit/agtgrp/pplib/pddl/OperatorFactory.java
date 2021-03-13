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
package au.rmit.agtgrp.pplib.pddl;

import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;

public class OperatorFactory<T extends Term> {

	private final String name;
	private final List<Variable> variables;
	private final List<T> parameters;
	private final List<Literal<T>> preconditions;
	private final List<Literal<T>> postconditions;

	public OperatorFactory(String name) {
		this.name = name;
		variables = new ArrayList<Variable>();
		parameters = new ArrayList<T>();
		preconditions = new ArrayList<Literal<T>>();
		postconditions = new ArrayList<Literal<T>>();
	}

	public OperatorFactory<T> addVariable(Variable var) {
		variables.add(var);
		return this;
	}
	
	public OperatorFactory<T> addVariables(List<Variable> vars) {
		variables.addAll(vars);
		return this;
	}
	
	public OperatorFactory<T> addPrecondition(Predicate predicate, boolean value, int ... indexes) {
		List<Variable> vars = new ArrayList<Variable>();
		List<T> params = new ArrayList<T>();
		for (int i : indexes) {
			vars.add(variables.get(i));
			params.add(parameters.get(i));
		}
			
		preconditions.add(new Literal<T>(predicate, vars, params, value));
		
		return this;
	}
	
	public OperatorFactory<T> addPrecondition(Literal<T> precon) {
		preconditions.add(substituteTerms(precon));
		return this;
	}
	
	public OperatorFactory<T> addPreconditions(List<Literal<T>> precons) {
		for (Literal<T> precon : precons)
			addPrecondition(precon);
		return this;
	}
	
	
	public OperatorFactory<T> addPostcondition(Predicate predicate, boolean value, int ... indexes) {
		List<Variable> vars = new ArrayList<Variable>();
		List<T> params = new ArrayList<T>();
		for (int i : indexes) {
			vars.add(variables.get(i));
			params.add(parameters.get(i));
		}
			
		postconditions.add(new Literal<T>(predicate, vars, params, value));
		
		return this;
	}
	
	public OperatorFactory<T> addPostcondition(Literal<T> postcon) {
		postconditions.add(substituteTerms(postcon));
		return this;
	}
	
	public OperatorFactory<T> addPostconditions(List<Literal<T>> postcons) {
		for (Literal<T> postcon : postcons)
			addPostcondition(postcon);
		return this;
	}
	
	public OperatorFactory<T> addParameter(T param) {
		parameters.add(param);
		return this;
	}
	
	public OperatorFactory<T> addParameters(List<T> params) {
		this.parameters.addAll(params);
		return this;
	}
	
	public Operator<T> getOperator() {
		return new Operator<T>(name, variables, parameters, preconditions, postconditions);
	}
	
	
	private Literal<T> substituteTerms(Literal<T> literal) {
		List<Variable> subbedVars = sub(variables, literal.getAtom().getVariables());
		List<T> subbedParams = sub(parameters, literal.getAtom().getParameters());
			
		return new Literal<T>(literal.getAtom().getSymbol(), subbedVars, subbedParams, literal.getValue());
	}
	
	private <S extends Term> List<S> sub(List<S> opValues, List<S> litValues) {
		List<S> subbed = new ArrayList<S>();
		for (S param : litValues) {
			boolean found = false;
			for (S opParam : opValues) {
				if (opParam.getName().equals(param.getName()) && 
					param.getType().hasSubtype(opParam.getType())) {
					
					subbed.add(opParam);
					found = true;
					break;
				}
			}
			
			if (!found) {
				throw new IllegalArgumentException("Illegal symbol " +  param + ", no compatible option in " + opValues);
			}
		}
		
		return subbed;
	}
	
}
