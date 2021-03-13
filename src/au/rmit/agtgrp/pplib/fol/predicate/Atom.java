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
package au.rmit.agtgrp.pplib.fol.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.symbol.SymbolInstance;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;
import au.rmit.agtgrp.pplib.utils.collections.ObjectCache;

public class Atom<T extends Term> extends SymbolInstance<Predicate, T> {

	private static final long serialVersionUID = 1L;

	private static ObjectCache<Atom<?>> CACHE = new ObjectCache<Atom<?>>();
	
	
	public static <T extends Term> Atom<T> equals(Variable v1, Variable v2, T p1, T p2) {
		List<Variable> vars = Arrays.asList(v1, v2);
		List<T> params = Arrays.asList(p1, p2);
		return new Atom<T>(Predicate.EQUALS, vars, params).intern();
	}

	public static <T extends Term> Atom<T> prec(Variable v1, Variable v2, T p1, T p2) {
		List<Variable> vars = Arrays.asList(v1, v2);
		List<T> params = Arrays.asList(p1, p2);
		return new Atom<T>(Predicate.PREC, vars, params).intern();
	}

	private static <T extends Term> List<Variable> normalise(Predicate symbol, List<Variable> variables, Substitution<T> sub) {
		if (symbol == null)
			throw new NullPointerException("Symbol cannot be null");
		
		if (symbol.isReflexive()) {
			List<Variable> sortedVars = new ArrayList<Variable>(variables);
			Collections.sort(sortedVars, Comparators.SYMBOL_COMPARATOR);
		}
		return variables;
	}
	
	public Atom(Predicate symbol, List<Variable> variables, Substitution<T> substitution) {
		this(symbol, variables, substitution, true);
	}
	
	public Atom(Predicate symbol, List<Variable> variables, Substitution<T> substitution, boolean normalise) {
		super(symbol, normalise ? normalise(symbol, variables, substitution) : variables, substitution);
	}
	
	public Atom(Predicate symbol, List<Variable> variables, List<? extends T> params) {
		this(symbol, variables, params, true);
	}
	
	public Atom(Predicate symbol, List<Variable> variables, List<? extends T> params, boolean normalise) {
		super(symbol, 
			normalise ? normalise(symbol, variables, Substitution.build(variables, params)) : variables, 
			Substitution.build(variables, params));
	}

	@Override
	public Atom<T> rename(String newName) {
		return new Atom<T>(symbol.rename(newName), super.variables, super.parameters).intern();
	}
	
	@Override
	public <W extends Term> Atom<W> rename(String newName, List<W> newParams) {
		return new Atom<W>(symbol.rename(newName), variables, newParams).intern();
	}

	@Override
	public <W extends Term> Atom<W> rebind(List<W> newBindings) {
		return new Atom<W>(symbol, variables, newBindings).intern();
	}

	@Override
	public <W extends Term> Atom<W> applySubstitution(Substitution<W> sub) {
		return new Atom<W>(symbol, variables, sub.apply(variables)).intern();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Atom<T> resetVariables(List<Variable> variables) {
		Substitution<Variable> sub = Substitution.build(super.variables, variables);
		List<T> newParams = new ArrayList<T>();
		for (T param : super.parameters) {
			if (param instanceof Variable) {
				Variable subbedVar = sub.apply((Variable) param);
				if (subbedVar == null)
					subbedVar = (Variable) param;
				
				newParams.add((T) subbedVar);
			}
			else
				newParams.add(param);
		}
	
		return new Atom<T>(super.symbol, variables, newParams).intern();
	}
	
	public Atom<T> normalise() {
		return new Atom<T>(symbol, variables, new ArrayList<T>(parameters), true).intern();
	}

	public Atom<T> intern() {
		return CACHE.get(this);
	}


}
