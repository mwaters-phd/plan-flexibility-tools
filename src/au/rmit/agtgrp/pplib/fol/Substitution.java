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
package au.rmit.agtgrp.pplib.fol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;

public class Substitution<V extends Term> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static String MAPS_SYMBOL = "<-";

	@SuppressWarnings("unchecked")
	public static <T extends Term, S extends Term> List<T> apply(Substitution<T> sub, List<S> vars) {

		List<T> renamed = new ArrayList<T>();
		try {
			for (S var : vars) {
				if (var instanceof Constant)
					renamed.add((T) var);
				else {
					renamed.add(sub.apply((Variable) var));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return renamed;
	}

	public static final <T extends Term> Substitution<T> empty() {
		return new Substitution<T>();
	}

	public static final <T extends Term> Substitution<T> subset(Substitution<T> s, Collection<Variable> vars) {
		List<Variable> variables = new ArrayList<Variable>(vars);
		List<T> vals = new ArrayList<T>();
		for (Variable var : variables) {
			vals.add(s.apply(var));
		}

		return Substitution.build(variables, vals);

	}

	public static <T extends Term, S extends Term> Substitution<S> composition(Substitution<T> s1, Substitution<S> s2) {
		Substitution<S> composed = new Substitution<S>();

		for (Variable var : s1.getVariables()) {
			T s1val = s1.apply(var);
			S s2val = s2.apply((Variable) s1val);

			if (s2val != s1val)
				composed.setSubstitition(var, s2val);
		}

		for (Variable var : s2.getVariables()) {
			if (!s1.getVariables().contains(var))
				composed.setSubstitition(var, s2.apply(var));
		}

		return composed;
	}

	public static <T extends Term> Substitution<T> merge(Substitution<T> s1, Substitution<T> s2) {

		Substitution<T> merged = new Substitution<T>();

		for (Variable var : s1.getVariables())
			merged.setSubstitition(var, s1.apply(var));

		for (Variable var : s2.getVariables()) {
			if (!s1.getVariables().contains(var))
				merged.setSubstitition(var, s2.apply(var));
		}

		return merged;

	}

	public static Substitution<Variable> identity(Collection<Variable> vars) {
		List<Variable> list = new ArrayList<Variable>(vars);
		return build(list, list);
	}

	public static <T extends Term> Substitution<T> buildFromValues(List<Variable> freeVars, List<T> constants) {
		Substitution<T> subs = new Substitution<T>();
		if (freeVars.size() != constants.size())
			throw new IllegalArgumentException();

		for (int i = 0; i < constants.size(); i++)
			subs.setSubstitition(freeVars.get(i), constants.get(i));

		return subs;
	}

	public static <T extends Term> Substitution<T> build(List<Variable> freeVars, List<? extends T> vals) {

		Substitution<T> subs = new Substitution<T>();
		if (freeVars.size() != vals.size())
			throw new IllegalArgumentException("Different number of variables and constants");

		for (int i = 0; i < vals.size(); i++)
			subs.setSubstitition(freeVars.get(i), vals.get(i));

		return subs;
	}

	public static <T extends Term> Substitution<T> trim(Substitution<T> sub, Collection<Variable> keep) {
		Map<Variable, T> subMap = new HashMap<Variable, T>();
		for (Variable var : keep)
			subMap.put(var, sub.apply(var));

		return new Substitution<T>(subMap);
	}

	private final Map<Variable, V> subMap;

	public Substitution(Map<Variable, V> mapping) {
		subMap = mapping;
	}

	public Substitution() {
		subMap = new HashMap<Variable, V>();
	}

	private void setSubstitition(Variable var, V val) {
		subMap.put(var, val);
	}

	public V apply(Variable var) {
		return subMap.get(var);
	}

	public Set<Variable> getVariables() {
		return subMap.keySet();
	}

	public Set<V> getDomain() {
		return new HashSet<V>(subMap.values());
	}

	public List<V> apply(List<Variable> vars) {
		ArrayList<V> vals = new ArrayList<V>();

		for (Variable var : vars)
			vals.add(apply(var));

		return vals;
	}
	
	public Map<Variable, V> getMap() {
		return Collections.unmodifiableMap(subMap);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<Variable> it = subMap.keySet().iterator();
		while (it.hasNext()) {
			Variable var = it.next();
			sb.append(var + MAPS_SYMBOL + subMap.get(var));
			if (it.hasNext())
				sb.append(", ");
		}
		return sb.toString();

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subMap == null) ? 0 : subMap.hashCode());
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
		Substitution<?> other = (Substitution<?>) obj;
		if (subMap == null) {
			if (other.subMap != null)
				return false;
		} else if (!subMap.equals(other.subMap))
			return false;
		return true;
	}
	
	

}
