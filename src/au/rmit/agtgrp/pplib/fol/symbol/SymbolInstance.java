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
package au.rmit.agtgrp.pplib.fol.symbol;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public abstract class SymbolInstance<S extends ParameterisedSymbol, T extends Term> implements Rebindable<S, T>, Symbol, Serializable {

	private static final long serialVersionUID = 1L;

	protected final S symbol;	
	protected final List<Variable> variables;
	protected final List<T> parameters;
	protected final Substitution<T> substitution;

	private final int hashCode;

	public SymbolInstance(S symbol, List<Variable> variables, Substitution<T> substitution) {

		if (symbol == null || variables == null || substitution == null)
			throw new NullPointerException();

		if (!substitution.getVariables().containsAll(variables))
			throw new IllegalArgumentException("Substitution is not complete with respect to variables");

		this.symbol = symbol;
		this.variables = Collections.unmodifiableList(variables);
		this.parameters = substitution.apply(variables);
		this.substitution = substitution;

		checkArityAndTypes(symbol, variables);
		checkArityAndTypes(symbol, parameters);

		hashCode = computeHashCode();

	}

	public SymbolInstance(S symbol, List<Variable> variables, List<? extends T> params) {

		if (symbol == null || variables == null || params == null)
			throw new NullPointerException();

		checkArityAndTypes(symbol, variables);
		checkArityAndTypes(symbol, params);

		this.symbol = symbol;
		this.variables = Collections.unmodifiableList(variables);
		this.parameters = Collections.unmodifiableList(params);
		this.substitution = Substitution.build(variables, parameters);

		hashCode = computeHashCode();

	}

	private void checkArityAndTypes(S symbol, List<? extends Term> terms) {
		if (terms.size() != symbol.getArity())
			throw new IllegalArgumentException(
					"Illegal number of variables/parameters for symbol " + symbol + 
					": " + FormattingUtils.toString(terms));

		for (int i = 0; i < terms.size(); i++) {
			Term var = terms.get(i);
			if (var == null)
				throw new IllegalArgumentException("Variables/parameters cannot be null:\n " + symbol + " " + terms);

			if (!var.getType().hasSupertype(symbol.getTypeAt(i)))
				throw new IllegalArgumentException("Illegal types in variables/parameters in Symbol " + symbol.name + ". Expected: " + symbol.types + ", actual:" + Term.getTypes(terms) );
		
		}
	}

	public S getSymbol() {
		return symbol;
	}

	public String getName() {
		return symbol.getName();
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public List<T> getParameters() {
		return parameters;
	}

	public Substitution<T> getSubstitution() {
		return substitution;
	}
	
	public String formatParameters() {
		StringBuilder sb = new StringBuilder();
		sb.append(symbol.getName() + "(");
		int i = 0;
		for (Term param : parameters) {
			sb.append(param.getName());
			if (i++ < parameters.size()-1)
				sb.append(" ");
		}
		
		sb.append(")");
		return sb.toString();
				
	}

	@Override
	public String toString() {
		return symbol.getName() + "(" + FormattingUtils.toString(parameters) + ")";
	}

	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		result = prime * result + ((variables == null) ? 0 : variables.hashCode());
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
		SymbolInstance<?, ?> other = (SymbolInstance<?, ?>) obj;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!parameters.equals(other.parameters))
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		if (variables == null) {
			if (other.variables != null)
				return false;
		} else if (!variables.equals(other.variables))
			return false;
		return true;
	}





}
