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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.symbol.Rebindable;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.ObjectCache;

public class Literal<T extends Term> implements Rebindable<Predicate, T>, Serializable {

	private static final long serialVersionUID = 1L;

	private static ObjectCache<Literal<?>> CACHE = new ObjectCache<Literal<?>>();
	
	public static final Literal<Variable> TRUE = new Literal<Variable>(Predicate.TRUE, 
									new ArrayList<Variable>(), new ArrayList<Variable>(), true);
	
	public static final Literal<Variable> FALSE = new Literal<Variable>(Predicate.TRUE,
									new ArrayList<Variable>(), new ArrayList<Variable>(), false);

	public static <T extends Term> Literal<T> equals(Variable v1, Variable v2, T p1, T p2, boolean value) {
		return new Literal<T>(Atom.equals(v1, v2, p1, p2), value).intern();
	}
	
	public static Literal<Variable> equals(Variable v1, Variable v2, boolean value) {
		return new Literal<Variable>(Atom.equals(v1, v2, v1, v2), value).intern();
	}

	public static <T extends Term> Literal<T> prec(Variable v1, Variable v2, T p1, T p2) {
		return new Literal<T>(Atom.prec(v1, v2, p1, p2), true).intern();
	}
	
	public static Literal<Variable> prec(Variable v1, Variable v2) {
		return new Literal<Variable>(Atom.prec(v1, v2, v1, v2), true).intern();
	}
		
	public static <T extends Term> Literal<T> prec(Variable v1, Variable v2, T p1, T p2, boolean value) {
		return new Literal<T>(Atom.prec(v1, v2, p1, p2), value).intern();
	}

	
	private final Atom<T> atom;
	private final boolean value;

	private final int hashCode;
	
	public Literal(Predicate predicateSymbol, List<Variable> variables, Substitution<T> substitution, boolean value) {
		this(new Atom<T>(predicateSymbol, variables, substitution).intern(), value);
	}
	
	//public Literal(Predicate predicateSymbol, List<T> params, boolean value) {
	//	this(new Atom<T>(predicateSymbol, params).intern(), value);
	//}
	
	public Literal(Predicate predicateSymbol, List<Variable> variables, List<? extends T> params, boolean value) {
		this(new Atom<T>(predicateSymbol, variables, params).intern(), value);
	}

	public Literal(Atom<T> atom, boolean value) {
		this.atom = atom.intern();
		this.value = value;

		hashCode = computeHashCode();
	}

	public boolean getValue() {
		return value;
	}

	public Atom<T> getAtom() {
		return atom;
	}
	
	public String getName() {
		return atom.getName();
	}

	public Literal<T> getNegated() {
		return new Literal<T>(atom, !value).intern();
	}

	@Override
	public Literal<T> rename(String newName) {
		return new Literal<T>(atom.rename(newName), value).intern();
	}

	@Override
	public <W extends Term> Literal<W> rename(String newName, List<W> newParams) {
		return new Literal<W>(atom.rename(newName, newParams), value).intern();
	}

	@Override
	public Literal<T> resetVariables(List<Variable> variables) {
		return new Literal<T>(atom.resetVariables(variables), value).intern();
	}
	
	@Override
	public <W extends Term> Literal<W> rebind(List<W> newBindings) {
		return new Literal<W>(atom.rebind(newBindings), value).intern();
	}

	@Override
	public <W extends Term> Literal<W> applySubstitution(Substitution<W> sub) {
		return new Literal<W>(atom.applySubstitution(sub), value).intern();
	}

	public Literal<T> intern() {
		return CACHE.get(this);
	}

	@Override
	public String toString() {
		if (value)
			return atom.toString();
		else
			return "!" + atom.toString();
	}

	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((atom == null) ? 0 : atom.hashCode());
		result = prime * result + (value ? 1231 : 1237);
		return result;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Literal<?> other = (Literal<?>) obj;
		if (atom == null) {
			if (other.atom != null)
				return false;
		} else if (!atom.equals(other.atom))
			return false;
		if (value != other.value)
			return false;
		return true;
	}

	

}
