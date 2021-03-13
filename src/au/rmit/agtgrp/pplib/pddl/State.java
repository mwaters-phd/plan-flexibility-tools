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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Atom;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class State<V extends Term> implements Iterable<Literal<V>> {

	private Set<Atom<V>> facts;

	public State(Collection<Literal<V>> facts) {
		this();
		for (Literal<V> fact : facts) {
			if (fact.getValue())
				add(fact.resetVariables(Variable.buildVariables(fact.getAtom().getSymbol().getTypes())));
		}
	}
	
	public State() {
		facts = new HashSet<Atom<V>>();
	}

	public Set<Literal<V>> getFacts() {
		Set<Literal<V>> litFacts = new HashSet<Literal<V>>();
		for (Atom<V> fact : facts) 
			litFacts.add(new Literal<V>(fact, true).intern());

		return litFacts;
	}

	private void add(Literal<V> literal) {
		if (literal.getValue())
			add(literal.getAtom());
		else
			remove(literal.getAtom());	
	}
	
	private void add(Atom<V> atom) {
		atom = atom.resetVariables(Variable.buildVariables(atom.getSymbol().getTypes()));
		facts.add(atom);
	}
	
	@SuppressWarnings("unused")
	private void remove(Literal<V> literal) {
		if (literal.getValue())
			remove(literal.getAtom());
		else
			add(literal.getAtom());
	}
	
	private void remove(Atom<V> fact) {
		fact = fact.resetVariables(Variable.buildVariables(fact.getSymbol().getTypes()));
		facts.remove(fact);
	}

	public boolean isSuperState(State<V> s) {
		return facts.containsAll(s.facts);
	}

	public boolean isTrue(Atom<Constant> fact) {
		fact = fact.resetVariables(Variable.buildVariables(fact.getSymbol().getTypes()));
		return facts.contains(fact);
	}

	public boolean isTrue(Literal<V> literal) {
		literal = literal.resetVariables(Variable.buildVariables(literal.getAtom().getSymbol().getTypes()));
		return facts.contains(literal.getAtom()) == literal.getValue();
	}

	public boolean satisfiesPreconditions(Operator<V> op) {
		State<V> preconState = new State<V>(op.preconditions);
		return this.isSuperState(preconState);
	}

	public State<V> applyOperator(Operator<V> op) {
		if (!this.satisfiesPreconditions(op))
			throw new IllegalArgumentException("Cannot execute action in this state");

		State<V> ns = new State<V>();
		ns.facts.addAll(facts);

		for (Literal<V> eff : op.getPostconditions()) {
			if (!eff.getValue())
				ns.add(eff);
		}

		for (Literal<V> eff : op.getPostconditions()) {
			if (eff.getValue())
				ns.add(eff);
		}
	
		return ns;

	}

	@Override
	public Iterator<Literal<V>> iterator() {
		return new StateIterator(facts.iterator());
	}

	@Override
	public String toString() {
		return FormattingUtils.toString(facts);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((facts == null) ? 0 : facts.hashCode());
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
		State<?> other = (State<?>) obj;
		if (facts == null) {
			if (other.facts != null)
				return false;
		} else if (!facts.equals(other.facts))
			return false;
		return true;
	}
	
	private class StateIterator implements Iterator<Literal<V>> {

		private Iterator<Atom<V>> atomIterator;
		
		public StateIterator(Iterator<Atom<V>> atomIterator) {
			this.atomIterator = atomIterator;
		}

		@Override
		public boolean hasNext() {
			return atomIterator.hasNext();
		}

		@Override
		public Literal<V> next() {
			return new Literal<V>(atomIterator.next(), true).intern();
		}
		
	}

}
