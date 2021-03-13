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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.symbol.Symbol;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class Relation<T extends Term> implements Iterable<Atom<T>>, Symbol {

	private static final long serialVersionUID = 1L;
	
	protected final Predicate predicateSymbol;
	private final Set<Atom<T>> contents;
	private Set<T> domain;

	public Relation(Predicate predicateSymbol) {
		this.predicateSymbol = predicateSymbol;
		this.contents = new HashSet<Atom<T>>();
		domain = new HashSet<T>();
	}

	public Predicate getPredicateSymbol() {
		return predicateSymbol;
	}
	
	public void add(List<T> vars) {
		contents.add(new Atom<T>(predicateSymbol, Variable.buildVariables(Term.getTypes(vars)), vars, false).intern());
		if (domain == null)
			domain = new HashSet<T>();

		domain.addAll(vars);
	}

	public void add(Atom<T> atom) {
		if (!atom.getSymbol().equals(this.predicateSymbol))
			throw new IllegalArgumentException("Wrong predicate type: " + atom.getSymbol());

		add(atom.getParameters());
	}

	public void addAll(Collection<Atom<T>> atoms) {
		for (Atom<T> atom : atoms)
			add(atom);
	}

	public void remove(Atom<T> atom) {
		if (!atom.getSymbol().equals(this.predicateSymbol))
			throw new IllegalArgumentException("Wrong predicate type: " + atom.getSymbol());
		remove(atom.getParameters());
	}

	public void remove(List<T> vars) {
		contents.remove(new Atom<T>(predicateSymbol, Variable.buildVariables(Term.getTypes(vars)), vars, false).intern());
		domain = null; //set null then rebuild when getDomain() is called
	}

	public void removeAll(Collection<Atom<T>> atoms) {
		for (Atom<T> atom : atoms)
			remove(atom);
	}

	public boolean contains(Atom<T> atom) {
		if (!atom.getSymbol().equals(this.predicateSymbol))
			throw new IllegalArgumentException("Wrong predicate type: " + atom.getSymbol());
		
		return contains(atom.getParameters());
	}

	public boolean contains(List<T> vars) {
		return contents.contains(new Atom<T>(predicateSymbol, Variable.buildVariables(Term.getTypes(vars)), vars, false).intern());
	}

	public int size() {
		return contents.size();
	}

	public void clear() {
		contents.clear();
	}

	public Set<T> getDomain() {
		if (domain == null) {
			domain = new HashSet<T>();
			for (Atom<T> atom : contents)
				domain.addAll(atom.getParameters());
		}
		return domain;
	}

	public Set<Atom<T>> getContents() {
		return Collections.unmodifiableSet(contents);
	}

	@Override
	public Iterator<Atom<T>> iterator() {
		return contents.iterator();
	}

	public String getName() {
		return predicateSymbol.getName();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(predicateSymbol.toString() + ", size: " + size());

		if (!contents.isEmpty())
			sb.append("\n" + FormattingUtils.toString(contents, "\n"));

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contents == null) ? 0 : contents.hashCode());
		result = prime * result + ((predicateSymbol == null) ? 0 : predicateSymbol.hashCode());
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
		Relation<?> other = (Relation<?>) obj;
		if (contents == null) {
			if (other.contents != null)
				return false;
		} else if (!contents.equals(other.contents))
			return false;
		if (predicateSymbol == null) {
			if (other.predicateSymbol != null)
				return false;
		} else if (!predicateSymbol.equals(other.predicateSymbol))
			return false;
		return true;
	}

}
