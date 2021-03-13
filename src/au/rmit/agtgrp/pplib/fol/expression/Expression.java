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
package au.rmit.agtgrp.pplib.fol.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.ObjectCache;

public class Expression<T extends Term> implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final ObjectCache<Expression<?>> CACHE = new ObjectCache<Expression<?>>();
	
	public static Expression<Variable> TRUE = Expression.buildLiteral(Literal.TRUE);
	public static Expression<Variable> FALSE = Expression.buildLiteral(Literal.FALSE);

	public static <S extends Term> Expression<S> buildNegation(Expression<S> exp) {
		List<Expression<S>> subexps = new ArrayList<Expression<S>>();
		subexps.add(exp);
		return new Expression<S>(Connective.NOT, null, subexps).intern();
	}

	public static <S extends Term> Expression<S> buildImplicationFromLiterals(Literal<S> prec, Literal<S> ante) {
		return buildImplication(Expression.buildLiteral(prec), Expression.buildLiteral(ante));
	}

	public static <S extends Term> Expression<S> buildImplication(Expression<S> prec, Expression<S> ante) {
		List<Expression<S>> subexps = new ArrayList<Expression<S>>();
		subexps.add(prec);
		subexps.add(ante);
		return new Expression<S>(Connective.IMPL, null, subexps).intern();
	}

	public static <S extends Term> Expression<S> buildExpressionFromLiterals(Connective connective,
			Collection<Literal<S>> literals) {
		List<Expression<S>> subexpressions = new ArrayList<Expression<S>>();
		for (Literal<S> lit : literals)
			subexpressions.add(Expression.buildLiteral(lit));

		return buildExpression(connective, subexpressions);
	}

	public static <S extends Term> Expression<S> buildExpression(Connective connective,
			Collection<Expression<S>> subexpressions) {
		if (subexpressions.size() == 1)
			return subexpressions.iterator().next();

		return new Expression<S>(connective, null, subexpressions).intern();
	}

	public static <S extends Term> Expression<S> buildLiteral(Literal<S> literal) {
		return new Expression<S>(null, literal, new ArrayList<Expression<S>>()).intern();
	}

	public static <S extends Term> Expression<S> negate(Expression<S> exp) {
		if (exp.isLiteral())
			return Expression.buildLiteral(exp.getLiteral().getNegated());

		if (exp.getConnective().equals(Connective.NOT))
			return exp.getSubexpressions().get(0);

		if (exp.getConnective().equals(Connective.IMPL)) {

			List<Expression<S>> conjs = new ArrayList<Expression<S>>();
			conjs.add(exp.getSubexpressions().get(0));
			conjs.add(negate(exp.getSubexpressions().get(1)));

			return Expression.buildExpression(Connective.AND, conjs);
		}

		List<Expression<S>> negSubs = new ArrayList<Expression<S>>();
		for (Expression<S> subExp : exp.getSubexpressions())
			negSubs.add(negate(subExp));

		if (exp.getConnective().equals(Connective.AND))
			return Expression.buildExpression(Connective.OR, negSubs);
		else // or
			return Expression.buildExpression(Connective.AND, negSubs);

	}

	public static <S extends Term> Expression<S> simplifyNesting(Expression<S> exp) {
		if (exp.isLiteral() || exp.getConnective().equals(Connective.IMPL))
			return exp;
		if (exp.getConnective().equals(Connective.NOT))
			return simplifyNesting(negate(exp.getSubexpressions().get(0)));

		// AND, OR
		List<Expression<S>> simpSubs = new ArrayList<Expression<S>>();
		for (Expression<S> sub : exp.getSubexpressions()) {
			sub = simplifyNesting(sub);
			if (sub.isLiteral() || !sub.getConnective().equals(exp.getConnective()))
				simpSubs.add(sub);
			else
				simpSubs.addAll(sub.getSubexpressions());

		}

		return buildExpression(exp.getConnective(), simpSubs);

	}

	private final Connective connective;
	private final Literal<T> literal;
	private final List<Expression<T>> subexpressions;

	private Set<Literal<T>> literals;
	private Set<T> domain;

	private final int hashCode;

	private Expression(Connective connective, Literal<T> literal, Collection<Expression<T>> subexpressions) {
		this.connective = connective;
		this.literal = literal == null ? null : literal.intern();
		this.subexpressions = new ArrayList<Expression<T>>(subexpressions);

		checkConnective();
		
		hashCode = computeHashCode();
	}

	private void checkConnective() {
		if (literal != null && subexpressions.size() != 0)
			throw new IllegalArgumentException("Literal cannot have sub-expressions");

		if (connective == null) {
			if (literal == null)
				throw new IllegalArgumentException("No connective");

		} else {
			if (subexpressions.size() == 0)
				throw new IllegalArgumentException("No subexpressions");

			if (connective.equals(Connective.NOT) && subexpressions.size() > 1)
				throw new IllegalArgumentException(Connective.NOT.name() + " expression with mutliple subexpressions");

			if (connective.equals(Connective.IMPL) && subexpressions.size() != 2)
				throw new IllegalArgumentException(
						Connective.IMPL.name() + " expression with " + subexpressions.size() + " subexpressions");

			if ((connective.equals(Connective.AND) || connective.equals(Connective.OR)) && subexpressions.size() < 2)
				throw new IllegalArgumentException(
						connective.name() + " expression with " + subexpressions.size() + " subexpressions");
		}
	}

	public boolean isLiteral() {
		return literal != null;
	}

	public Connective getConnective() {
		return connective;
	}

	public Literal<T> getLiteral() {
		return literal;
	}

	public List<Expression<T>> getSubexpressions() {
		return subexpressions;
	}

	public Set<T> getDomain() {
		if (domain == null) {
			domain = new HashSet<T>();
			if (this.isLiteral())
				domain.addAll(literal.getAtom().getParameters());
			else {
				for (Expression<T> exp : subexpressions)
					domain.addAll(exp.getDomain());
			}
		}
		return domain;
	}

	public Set<Literal<T>> getLiterals() {
		if (literals == null) {
			literals = new HashSet<Literal<T>>();
			if (literal != null)
				literals.add(literal);
			else {
				for (Expression<T> subexp : subexpressions)
					literals.addAll(subexp.getLiterals());
			}
		}

		return literals;
	}

	public <V extends Term> Expression<V> applySubstitution(Substitution<V> sub) {
		if (this.isLiteral())
			return Expression.buildLiteral(literal.applySubstitution(sub));
		else {
			List<Expression<V>> renamedSubs = new ArrayList<Expression<V>>();
			for (Expression<T> subexp : subexpressions)
				renamedSubs.add(subexp.applySubstitution(sub));

			return Expression.buildExpression(connective, renamedSubs);
		}
	}
	
	public Expression<T> resetVariables(Substitution<Variable> sub) {
		if (this.isLiteral()) {
			List<Variable> newVars = sub.apply(literal.getAtom().getVariables());
			return Expression.buildLiteral(literal.resetVariables(newVars));
		}
		else {
			List<Expression<T>> resetSubs = new ArrayList<Expression<T>>();
			for (Expression<T> subexp : subexpressions)
				resetSubs.add(subexp.resetVariables(sub));

			return Expression.buildExpression(connective, resetSubs);
		}
	}

	public Expression<T> intern() {
		return CACHE.get(this);
	}

	@Override
	public String toString() {
		if (this.isLiteral())
			return literal.toString();
		else if (this.connective.equals(Connective.NOT))
			return connective.name() + "(" + subexpressions.get(0) + ")";
		else if (this.connective.equals(Connective.IMPL))
			return "(" + subexpressions.get(0) + ") -> (" + subexpressions.get(1) + ")";
		else {

			StringBuilder sb = new StringBuilder();
			Iterator<Expression<T>> it = subexpressions.iterator();
			while (it.hasNext()) {
				Expression<T> subexp = it.next();

				if (!subexp.isLiteral())
					sb.append("(");
				sb.append(subexp);
				if (!subexp.isLiteral())
					sb.append(")");

				if (it.hasNext())
					sb.append(" " + connective.name() + " ");
			}

			return sb.toString();
		}

	}

	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connective == null) ? 0 : connective.hashCode());
		result = prime * result + ((literal == null) ? 0 : literal.hashCode());

		if (connective != null && (connective.equals(Connective.NOT) || connective.equals(Connective.IMPL)))
			result = prime * result + subexpressions.hashCode();

		else if (connective != null && (connective.equals(Connective.AND) || connective.equals(Connective.OR)))
			result = prime * result + new HashSet<Expression<T>>(subexpressions).hashCode();

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

		Expression<?> other = (Expression<?>) obj;
		if (this.isLiteral())
			return other.isLiteral() && this.getLiteral().equals(other.getLiteral());

		if (!this.getConnective().equals(other.getConnective()))
			return false;

		if (this.getConnective().equals(Connective.IMPL) || this.getConnective().equals(Connective.NOT))
			return this.getSubexpressions().equals(other.getSubexpressions());

		return this.getSubexpressions().size() == other.getSubexpressions().size()
				&& this.getSubexpressions().containsAll(other.getSubexpressions());

	}

}
