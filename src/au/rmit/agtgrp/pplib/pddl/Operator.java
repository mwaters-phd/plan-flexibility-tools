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
import java.util.Collections;
import java.util.List;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.SymbolInstance;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class Operator<T extends Term> extends SymbolInstance<OperatorSymbol, T> {

	private static final long serialVersionUID = 1L;

	protected final List<Literal<T>> preconditions;
	protected final List<Literal<T>> postconditions;

	protected final int hashCode;

	public Operator(String name, List<Variable> variables, List<? extends T> parameters, 
			List<Literal<T>> preconditions, List<Literal<T>> postconditions) {

		super(new OperatorSymbol(name, Term.getTypes(variables)), variables, parameters);

		if (variables == null || parameters == null || preconditions == null || postconditions == null)
			throw new NullPointerException();

		if (variables.size() != parameters.size())
			throw new IllegalArgumentException("Variables/parameters mismatch");

		checkVariables(preconditions);
		this.preconditions = Collections.unmodifiableList(preconditions);

		checkVariables(postconditions);
		this.postconditions = Collections.unmodifiableList(postconditions);

		hashCode = computeHashCode();

	}


	private void checkVariables(List<Literal<T>> literals) {
		for (Literal<T> literal : literals) {
			for (Variable param : literal.getAtom().getVariables()) {
				if (!variables.contains(param))
					throw new IllegalArgumentException("Unknown variable " + param + " in literal " + literal + 
							"\nVariables = " + variables);
			}
		}
	}


	public List<Literal<T>> getPreconditions() {
		return preconditions;
	}

	public List<Literal<T>> getPostconditions() {
		return postconditions;
	}

	public boolean isUndone(Literal<T> effect) {
		// assume negative effects are applied first
		if (effect.getValue())
			return false;

		// negative effect may be undone by positive effect
		if (postconditions.contains(effect)) {
			for (Literal<T> otherEff : postconditions) {
				if (otherEff.getValue() && 
						otherEff.getAtom().getSymbol().equals(effect.getAtom().getSymbol()) &&
						otherEff.getAtom().getParameters().equals(effect.getAtom().getParameters())) {

					return true;
				}
			}
		}

		return false;
	}

	public Literal<T> getUndoing(Literal<T> effect) {
		// assume negative effects are applied first
		if (effect.getValue() || !postconditions.contains(effect))
			return null;

		// negative effect may be undone by positive effect
		for (Literal<T> otherEff : postconditions) {
			if (otherEff.getValue() && otherEff.getAtom().getSymbol().equals(effect.getAtom().getSymbol())) {
				return otherEff;
			}
		}

		return null;
	}



	@Override
	public <W extends Term> Operator<W> rename(String newName, List<W> newParams) {
		Substitution<W> sub = Substitution.build(variables, newParams);
		List<Literal<W>> renamedPrecons = new ArrayList<Literal<W>>();
		for (Literal<T> precon : this.preconditions)
			renamedPrecons.add(precon.applySubstitution(sub));

		List<Literal<W>> renamedPostcons = new ArrayList<Literal<W>>();
		for (Literal<T> postcon : this.postconditions)
			renamedPostcons.add(postcon.applySubstitution(sub));

		return new Operator<W>(newName, super.variables, newParams, renamedPrecons, renamedPostcons);
	}

	@Override
	public <W extends Term> Operator<W> rebind(List<W> newParams) {
		Substitution<W> sub = Substitution.build(variables, newParams);
		List<Literal<W>> renamedPrecons = new ArrayList<Literal<W>>();
		for (Literal<T> precon : this.preconditions)
			renamedPrecons.add(precon.applySubstitution(sub));

		List<Literal<W>> renamedPostcons = new ArrayList<Literal<W>>();
		for (Literal<T> postcon : this.postconditions)
			renamedPostcons.add(postcon.applySubstitution(sub));

		return new Operator<W>(super.symbol.getName(), super.variables, newParams, renamedPrecons, renamedPostcons);
	}

	@Override
	public Operator<T> rename(String newName) {
		return new Operator<T>(newName, super.variables, super.parameters, this.preconditions, this.postconditions);
	}

	@Override
	public Operator<T> resetVariables(List<Variable> newVariables) {
		Substitution<Variable> sub = Substitution.build(variables, newVariables);

		List<Literal<T>> renamedPrecons = new ArrayList<Literal<T>>();
		for (Literal<T> precon : this.preconditions)
			renamedPrecons.add(precon.resetVariables(sub.apply(precon.getAtom().getVariables())));

		List<Literal<T>> renamedPostcons = new ArrayList<Literal<T>>();
		for (Literal<T> postcon : this.postconditions)
			renamedPostcons.add(postcon.resetVariables(sub.apply(postcon.getAtom().getVariables())));

		return new Operator<T>(super.symbol.getName(), newVariables, super.parameters, renamedPrecons, renamedPostcons);
	}

	@Override
	public <W extends Term> Operator<W> applySubstitution(Substitution<W> sub) {
		List<Literal<W>> renamedPrecons = new ArrayList<Literal<W>>();
		for (Literal<T> precon : this.preconditions)
			renamedPrecons.add(precon.applySubstitution(sub));

		List<Literal<W>> renamedPostcons = new ArrayList<Literal<W>>();
		for (Literal<T> postcon : this.postconditions)
			renamedPostcons.add(postcon.applySubstitution(sub));

		return new Operator<W>(super.symbol.getName(), super.variables, Substitution.apply(sub, super.parameters), renamedPrecons, renamedPostcons);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("operator = " + super.toString() + "\n");
		sb.append("pre      = " + FormattingUtils.toString(preconditions) + "\n");
		sb.append("post     = " + FormattingUtils.toString(postconditions));

		return sb.toString();
	}

	public int computeHashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((postconditions == null) ? 0 : postconditions.hashCode());
		result = prime * result + ((preconditions == null) ? 0 : preconditions.hashCode());
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
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Operator<?> other = (Operator<?>) obj;
		if (postconditions == null) {
			if (other.postconditions != null)
				return false;
		} else if (!postconditions.equals(other.postconditions))
			return false;
		if (preconditions == null) {
			if (other.preconditions != null)
				return false;
		} else if (!preconditions.equals(other.preconditions))
			return false;
		return true;
	}



}
