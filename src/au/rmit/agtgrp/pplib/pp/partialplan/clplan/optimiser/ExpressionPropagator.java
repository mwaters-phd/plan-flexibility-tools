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
package au.rmit.agtgrp.pplib.pp.partialplan.clplan.optimiser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.expression.Connective;
import au.rmit.agtgrp.pplib.fol.expression.Expression;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;

public class ExpressionPropagator extends Propagator {

	private final Expression<Variable> original;

	private Expression<Variable> constraint;
	private Set<Variable> changed;

	private Set<Literal<Variable>> literals;
	private Set<Variable> domain;

	private final Set<Variable> extDomainChanged;

	public ExpressionPropagator(Expression<Variable> constraint) {
		if (constraint.isLiteral())
			throw new IllegalArgumentException(constraint.toString());

		this.constraint = constraint;
		this.original = constraint;
		domain = constraint.getDomain();
		literals = constraint.getLiterals();
		extDomainChanged = new HashSet<Variable>();
	}

	public List<Expression<Variable>> splitConjunction() {

		if (!constraint.isLiteral() && constraint.getConnective().equals(Connective.AND))
			return constraint.getSubexpressions();

		List<Expression<Variable>> conjs = new ArrayList<Expression<Variable>>();
		conjs.add(constraint);
		return conjs;
	}

	public void addExtDomainsChanged(Collection<Variable> changed) {
		extDomainChanged.addAll(changed);
		extDomainChanged.retainAll(domain);
	}
	
	public Expression<Variable> getOriginal() {
		return original;
	}

	public Set<Variable> getDomain() {
		return domain;
	}

	public Expression<Variable> getConstraint() {
		return constraint;
	}

	public Set<Variable> getChanged() {
		return changed;
	}

	public Set<Literal<Variable>> getLiterals() {
		return literals;
	}

	public void propagate() {
		if (constraint.isLiteral())
			throw new IllegalStateException(constraint.toString());

		changed = propagateExpressionToDomain(constraint);

		addExtDomainsChanged(changed);

		constraint = simplifySubexpressions(constraint, extDomainChanged);

		domain = constraint.getDomain();
		literals = constraint.getLiterals();

		extDomainChanged.clear();
	}

	private static Set<Variable> propagateExpressionToDomain(Expression<Variable> constraint) {
		if (!constraint.isLiteral() && constraint.getConnective().equals(Connective.OR))
			return propagateDisjToDomain(constraint);
		else
			return new HashSet<Variable>();
	}

	private static Set<Variable> propagateDisjToDomain(Expression<Variable> disj) {
		if (disj.isLiteral() || !disj.getConnective().equals(Connective.OR))
			throw new IllegalArgumentException(disj.toString());

		// is it a disjunction of positive equality literals
		Set<Variable> commonVars = new HashSet<Variable>(disj.getDomain());
		for (Expression<Variable> sub : disj.getSubexpressions()) {
			if (!sub.isLiteral() || !sub.getLiteral().getValue()
					|| !sub.getLiteral().getAtom().getSymbol().equals(Predicate.EQUALS))

				return new HashSet<Variable>();

			// contains (x = x) -- not sure how to handle this so don't!
			if (sub.getLiteral().getAtom().getParameters().get(0)
					.equals(sub.getLiteral().getAtom().getParameters().get(1))) {
				return new HashSet<Variable>();
			}

			commonVars.retainAll(sub.getDomain());
		}

		// x = 1 or x = 2 ...
		if (commonVars.size() == 1) {
			Variable common = commonVars.iterator().next();
			Set<Variable> others = new HashSet<Variable>(disj.getDomain());
			others.remove(common);
			Set<Constant> otherDomains = new HashSet<Constant>();
			for (Variable other : others)
				otherDomains.addAll(CspOptimiser.DOMAINS.get(other));

			Collection<Constant> commonDomain = CspOptimiser.DOMAINS.get(common);
			if (commonDomain.retainAll(otherDomains)) {
				Set<Variable> changed = new HashSet<Variable>();
				changed.add(common);
				return changed;
			}

		}

		return new HashSet<Variable>();
	}

	private static Expression<Variable> simplifySubexpressions(Expression<Variable> exp,
			Set<Variable> changedDomains) {
		if (exp.isLiteral())
			return simplifyLiteral(exp);

		// get cached version
		Expression<Variable> cached = CspOptimiser.getCached(exp);

		// check whether any work can be done on the cached version
		Set<Variable> tmpCh = new HashSet<Variable>(changedDomains);

		if (tmpCh.removeAll(cached.getDomain()) || changedDomains.isEmpty()) { // overlap between cached domain and changed domains

			Expression<Variable> result = null;

			if (cached.isLiteral())
				result = simplifyLiteral(cached);
			else if (cached.getConnective().equals(Connective.NOT))
				result = simplifySubexpressions(Expression.negate(cached.getSubexpressions().get(0)), changedDomains);
			else if (cached.getConnective().equals(Connective.OR))
				result = simplifyDisj(cached, changedDomains);
			else if (cached.getConnective().equals(Connective.AND))
				result = simplifyConj(cached, changedDomains);
			else if (cached.getConnective().equals(Connective.IMPL))
				result = simplifyImpl(cached, changedDomains);

			CspOptimiser.addToCache(cached, result);
			CspOptimiser.addToCache(exp, result);

			return result;
		}

		return cached;
	}

	private static Expression<Variable> simplifyLiteral(Expression<Variable> exp) {
		if (exp.getLiteral().getAtom().getSymbol().equals(Predicate.PREC))
			exp = checkPrecSubexps(exp);
		else if (exp.getLiteral().getAtom().getSymbol().equals(Predicate.EQUALS))
			exp = checkEqualsSubexps(exp);
		return exp;
	}

	private static Expression<Variable> simplifyConj(Expression<Variable> exp,
			Set<Variable> changedDomains) {
		if (exp.isLiteral() || !exp.getConnective().equals(Connective.AND))
			throw new IllegalArgumentException(exp.toString());

		List<Expression<Variable>> newConj = new ArrayList<Expression<Variable>>();
		for (Expression<Variable> conj : exp.getSubexpressions()) {
			conj = simplifySubexpressions(conj, changedDomains);
			if (conj.equals(Expression.FALSE))
				return Expression.FALSE;
			else if (!conj.equals(Expression.TRUE) && !newConj.contains(conj)) // don't add tautologies, or the same cnj twice
				newConj.add(conj);
		}
		if (newConj.isEmpty())
			return Expression.TRUE;
		if (newConj.size() == 1)
			return newConj.get(0);

		for (int i = 0; i < newConj.size(); i++) { // check for contradictions
			Expression<Variable> conj = newConj.get(i);
			for (int j = i + 1; j < newConj.size(); j++) {
				Expression<Variable> conj2 = newConj.get(j);
				if (conj.isLiteral() && conj2.isLiteral()
						&& conj.getLiteral().equals(conj2.getLiteral().getNegated())) {

					return Expression.FALSE;
				}

			}
		}

		return Expression.buildExpression(Connective.AND, newConj);
	}

	private static Expression<Variable> simplifyDisj(Expression<Variable> disj,
			Set<Variable> changedDomains) {
		if (disj.isLiteral() || !disj.getConnective().equals(Connective.OR))
			throw new IllegalArgumentException(disj.toString());

		List<Expression<Variable>> disjs = new ArrayList<Expression<Variable>>();
		for (Expression<Variable> dis : disj.getSubexpressions()) {
			dis = simplifySubexpressions(dis, changedDomains);
			if (dis.equals(Expression.TRUE)) // if any are tautologies, the whole expression is!
				return Expression.TRUE;
			else if (!dis.equals(Expression.FALSE) && !disjs.contains(dis)) // don't add false, or the add same expression twice
				disjs.add(dis);
		}

		if (disjs.isEmpty())// disjuncts are false, return false
			return Expression.FALSE;
		if (disjs.size() == 1)
			return disjs.get(0);

		for (int i = 0; i < disjs.size(); i++) { // check for tautologies (x = y or x != y)
			Expression<Variable> conj = disjs.get(i);
			for (int j = i + 1; j < disjs.size(); j++) {
				Expression<Variable> conj2 = disjs.get(j);
				if (conj.isLiteral() && conj2.isLiteral() && conj.getLiteral().equals(conj2.getLiteral().getNegated()))

					return Expression.TRUE;

			}
		}

		disj = Expression.buildExpression(Connective.OR, disjs);

		disj = simplifyDisjOfConj(disj);

		disj = propagateDisjOfEqualityLiterals(disj);

		disj = propagateDisjOfPrecLiterals(disj);

		return disj;

	}

	private static Expression<Variable> simplifyDisjOfConj(Expression<Variable> exp) {
		if (exp.isLiteral() || !exp.getConnective().equals(Connective.OR))
			throw new IllegalArgumentException(exp.toString());

		List<List<Expression<Variable>>> subexps = new ArrayList<List<Expression<Variable>>>();

		// is it a disj of conj?
		for (Expression<Variable> subexp : exp.getSubexpressions()) {
			if (subexp.isLiteral())
				subexps.add(Arrays.asList(subexp));
			else if (subexp.getConnective().equals(Connective.AND))
				subexps.add(subexp.getSubexpressions());
			else
				return exp;
		}

		// filter
		List<List<Expression<Variable>>> redundant = new ArrayList<List<Expression<Variable>>>();
		for (int i = 0; i < subexps.size(); i++) {
			for (int j = i + 1; j < subexps.size(); j++) {
				if (subexps.get(i).containsAll(subexps.get(j))) {
					redundant.add(subexps.get(i));
				} else if (subexps.get(j).containsAll(subexps.get(i))) {
					redundant.add(subexps.get(j));
				}
			}
		}

		subexps.removeAll(redundant);

		if (subexps.size() == 1) {
			List<Expression<Variable>> remainder = subexps.get(0);
			if (remainder.size() == 1)
				return remainder.get(0);
			else
				return Expression.buildExpression(Connective.AND, remainder);
		}

		// find common vars
		Set<Expression<Variable>> common = null;
		for (List<Expression<Variable>> subexp : subexps) {
			if (common == null)
				common = new HashSet<Expression<Variable>>(subexp);
			else
				common.retainAll(subexp);

		}

		// (p & q & r) | (p & s & t) == (p) & ((q & r) | (s & t))
		if (common.size() > 0) {
			Expression<Variable> cexp = Expression.buildExpression(Connective.AND, common);

			List<Expression<Variable>> rest = new ArrayList<Expression<Variable>>();
			for (List<Expression<Variable>> disj : subexps) {
				List<Expression<Variable>> conjs = new ArrayList<Expression<Variable>>(disj);
				conjs.removeAll(common);
				rest.add(Expression.buildExpression(Connective.AND, conjs));
			}

			Expression<Variable> dis = Expression.buildExpression(Connective.OR, rest);

			return Expression.buildExpression(Connective.AND, Arrays.asList(cexp, dis));
		}

		List<Expression<Variable>> disjs = new ArrayList<Expression<Variable>>();
		for (List<Expression<Variable>> subexp : subexps) {
			disjs.add(Expression.buildExpression(Connective.AND, subexp));
		}
		return Expression.buildExpression(Connective.OR, disjs);

	}

	private static Expression<Variable> simplifyImpl(Expression<Variable> exp,
			Set<Variable> changedDomains) {

		Expression<Variable> prec = simplifySubexpressions(exp.getSubexpressions().get(0), changedDomains);
		Expression<Variable> ante = simplifySubexpressions(exp.getSubexpressions().get(1), changedDomains);

		if (ante.equals(Expression.TRUE)) // p -> true, return true
			return Expression.TRUE;

		if (prec.equals(Expression.TRUE)) // true -> p, return p
			return ante;

		if (prec.equals(Expression.FALSE)) // false -> p, return true
			return Expression.TRUE;

		if (ante.equals(Expression.FALSE)) // p -> false return -p
			return Expression.negate(prec);

		if (ante.equals(prec)) // p -> p return true
			return Expression.TRUE;

		List<Expression<Variable>> precExps = new ArrayList<Expression<Variable>>();
		if (prec.isLiteral())
			precExps.add(prec);
		else
			precExps.addAll(prec.getSubexpressions());

		List<Expression<Variable>> anteExps = new ArrayList<Expression<Variable>>();
		if (ante.isLiteral())
			anteExps.add(ante);
		else
			anteExps.addAll(ante.getSubexpressions());

		// and implies and
		if ((prec.isLiteral() || prec.getConnective().equals(Connective.AND))
				&& (ante.isLiteral() || ante.getConnective().equals(Connective.AND))) {

			List<Expression<Variable>> remainder = new ArrayList<Expression<Variable>>(anteExps);
			remainder.removeAll(precExps);

			if (remainder.isEmpty()) // p & q & r -> p & q
				return Expression.TRUE;

			for (Expression<Variable> antLit : remainder) { // p & q -> !p & r
				if (antLit.isLiteral()) {
					Expression<Variable> negated = Expression.buildLiteral(antLit.getLiteral().getNegated());
					if (precExps.contains(negated))
						return Expression.negate(prec);
				}
			}

			if (remainder.size() == 1)
				return Expression.buildImplication(prec, remainder.get(0));
			else
				return Expression.buildImplication(prec, Expression.buildExpression(Connective.AND, remainder));

		}

		if ((prec.isLiteral() || prec.getConnective().equals(Connective.OR))
				&& (ante.isLiteral() || ante.getConnective().equals(Connective.OR))) {

			List<Expression<Variable>> remainder = new ArrayList<Expression<Variable>>(precExps);
			remainder.removeAll(anteExps);

			if (remainder.isEmpty()) // p | q -> p | q | r
				return Expression.TRUE;

			if (remainder.size() == 1)
				return Expression.buildImplication(remainder.get(0), ante);
			else
				return Expression.buildImplication(Expression.buildExpression(Connective.OR, remainder), ante);

		}

		if ((prec.isLiteral() || prec.getConnective().equals(Connective.AND))
				&& (ante.isLiteral() || ante.getConnective().equals(Connective.OR))) {

			List<Expression<Variable>> remainder = new ArrayList<Expression<Variable>>(anteExps);
			remainder.retainAll(precExps);

			if (!remainder.isEmpty()) // p & q -> p | r
				return Expression.TRUE;

			// check for p & q -> -p | r
			List<Expression<Variable>> negs = new ArrayList<Expression<Variable>>();
			for (Expression<Variable> p : precExps)
				negs.add(Expression.negate(p));

			remainder = new ArrayList<Expression<Variable>>(anteExps);
			remainder.removeAll(negs);

			if (remainder.size() == 1)
				return Expression.buildImplication(prec, remainder.get(0));
			else if (remainder.size() == 0) { // prec implies a contradiction
				return Expression.negate(prec);
			} else
				return Expression.buildImplication(prec, Expression.buildExpression(Connective.OR, remainder));

		}
		return Expression.buildImplication(prec, ante);
	}

	private static Expression<Variable> checkPrecSubexps(Expression<Variable> exp) {

		if (!exp.isLiteral() || !exp.getLiteral().getAtom().getSymbol().equals(Predicate.PREC))
			throw new IllegalArgumentException(exp.toString());

		Literal<Variable> lit = exp.getLiteral();

		lit = CspOptimiser.simplifyPrec(lit);
		if (lit.equals(Literal.TRUE) || lit.equals(Literal.FALSE))
			return Expression.buildLiteral(lit);

		// x < x
		if (lit.getAtom().getParameters().get(0).equals(lit.getAtom().getParameters().get(1)))
			return exp.getLiteral().getValue() ? Expression.FALSE : Expression.TRUE;

		// is in prec relation
		if (CspOptimiser.PREC_GRAPH.containsEdge(lit.getAtom().getParameters().get(0),
				lit.getAtom().getParameters().get(1)))
			return lit.getValue() ? Expression.TRUE : Expression.FALSE;

		// exp = x < y, y < x is in prec relation
		if (CspOptimiser.PREC_GRAPH.containsEdge(lit.getAtom().getParameters().get(1),
				lit.getAtom().getParameters().get(0)))
			return lit.getValue() ? Expression.FALSE : Expression.TRUE;

		// exp = x < y but we know x = y
		if (lit.getValue() && CspOptimiser.EQUALITY_GRAPH.containsEdge(lit.getAtom().getParameters().get(0),
				lit.getAtom().getParameters().get(1)))
			return Expression.FALSE;

		if (CspOptimiser.NEG_LITERALS.contains(lit))
			return Expression.TRUE;

		if (lit.getAtom().getParameters().get(0).equals(lit.getAtom().getParameters().get(1))) // are equal
			return lit.getValue() ? Expression.FALSE : Expression.TRUE;

		return exp;

	}

	private static Expression<Variable> checkEqualsSubexps(Expression<Variable> exp) {

		if (!exp.isLiteral() || !exp.getLiteral().getAtom().getSymbol().equals(Predicate.EQUALS))
			throw new IllegalArgumentException(exp.toString());

		Literal<Variable> lit = CspOptimiser.simplifyEquals(exp.getLiteral());
		if (lit.equals(Literal.TRUE) || lit.equals(Literal.FALSE))
			return Expression.buildLiteral(lit);

		if (lit.getAtom().getParameters().get(0).equals(lit.getAtom().getParameters().get(1))) // are equal
			return lit.getValue() ? Expression.TRUE : Expression.FALSE;

		if (!lit.getValue()) {
			List<Variable> params = exp.getLiteral().getAtom().getParameters();
			if (CspOptimiser.PREC_GRAPH.containsEdge(params.get(0), params.get(1))
					|| CspOptimiser.PREC_GRAPH.containsEdge(params.get(1), params.get(0)))
				return Expression.TRUE;

			if (CspOptimiser.NEG_LITERALS.contains(exp.getLiteral()))
				return Expression.TRUE;
		}

		if (CspOptimiser.EQUALITY_GRAPH.containsEdge(exp.getLiteral().getAtom().getParameters().get(0),
				exp.getLiteral().getAtom().getParameters().get(1)))
			return exp.getLiteral().getValue() ? Expression.TRUE : Expression.FALSE;

		return exp;

	}

	private static Expression<Variable> propagateDisjOfEqualityLiterals(Expression<Variable> disj) {
		// is it a disj of positive equality literals?
		Set<Variable> commonVars = new HashSet<Variable>(disj.getDomain());
		for (Expression<Variable> sub : disj.getSubexpressions()) {
			if (!sub.isLiteral() || !sub.getLiteral().getValue() || 
					!sub.getLiteral().getAtom().getSymbol().equals(Predicate.EQUALS))
				return disj;

			commonVars.retainAll(sub.getDomain());
		}

		if (commonVars.size() == 1) {
			Variable common = commonVars.iterator().next();
			Set<Variable> others = new HashSet<Variable>(disj.getDomain());
			others.remove(common);

			Set<Constant> otherDomains = new HashSet<Constant>();
			for (Variable other : others) {
				if (CspOptimiser.DOMAINS.get(other).size() != 1)
					return disj;

				otherDomains.addAll(CspOptimiser.DOMAINS.get(other));
			}

			if (otherDomains.containsAll(CspOptimiser.DOMAINS.get(common)))
				return Expression.TRUE;

		}

		return disj;
	}

	private static Expression<Variable> propagateDisjOfPrecLiterals(Expression<Variable> disj) {
		// is it a disj of positive prec literals?, i.e., x < y or x < z or ...
		Variable commonVar = null;
		for (Expression<Variable> sub : disj.getSubexpressions()) {
			if (!sub.isLiteral() || !sub.getLiteral().getValue()
					|| !sub.getLiteral().getAtom().getSymbol().equals(Predicate.PREC)) {

				return disj;
			} else {
				if (commonVar == null)
					commonVar = sub.getLiteral().getAtom().getParameters().get(0);
				else if (!commonVar.equals(sub.getLiteral().getAtom().getParameters().get(0)))
					return disj;

			}
		}

		if (commonVar != null) {
			List<Variable> others = new ArrayList<Variable>(disj.getDomain());
			others.remove(commonVar);

			Set<Variable> remainder = new HashSet<Variable>(others);
			for (int i = 0; i < others.size() - 1; i++) {

				// i < i+1
				if (CspOptimiser.PREC_GRAPH.containsEdge(others.get(i), others.get(i + 1)))
					remainder.remove(others.get(i));
				// i+1 < i
				if (CspOptimiser.PREC_GRAPH.containsEdge(others.get(i + 1), others.get(i)))
					remainder.remove(others.get(i + 1));

			}

			List<Expression<Variable>> disjs = new ArrayList<Expression<Variable>>();
			for (Variable var : remainder)
				disjs.add(Expression.buildLiteral(Literal.prec(commonVar, var, commonVar, var)));

			return Expression.buildExpression(Connective.OR, disjs);

		}

		return disj;
	}

	@Override
	public String toString() {
		return constraint.toString();
	}

	@Override
	public int hashCode() {
		return original.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionPropagator other = (ExpressionPropagator) obj;
		if (original == null) {
			if (other.original != null)
				return false;
		} else if (!original.equals(other.original))
			return false;
		return true;
	}

}
