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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.rmit.agtgrp.pplib.csp.alldiff.AllDifferent;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;

public class BinaryPropagator extends Propagator {

	private final Literal<Variable> original;
	private Literal<Variable> constraint;
	private Set<Variable> domain;
	private Set<Variable> changed;

	public BinaryPropagator(Literal<Variable> constraint) {
		if (constraint.equals(Literal.TRUE) || constraint.equals(Literal.FALSE))
			throw new IllegalArgumentException(constraint.toString());

		this.constraint = constraint;
		this.original = constraint;
		domain = new HashSet<Variable>(constraint.getAtom().getParameters());
	}

	public Literal<Variable> getOriginal() {
		return original;
	}

	public Set<Variable> getDomain() {
		return domain;
	}

	public Literal<Variable> getConstraint() {
		return constraint;
	}

	public Set<Variable> getChanged() {
		return changed;
	}

	public void propagate() {
		changed = propagateLiteralToDomain(constraint);
		constraint = propagateLiteral(constraint);
		domain = new HashSet<Variable>(constraint.getAtom().getParameters());
	}

	private static Literal<Variable> propagateLiteral(Literal<Variable> lit) {
		if (lit.getAtom().getSymbol().equals(Predicate.EQUALS))
			return CspOptimiser.simplifyEquals(lit);
		else if (lit.getAtom().getSymbol().equals(Predicate.PREC))
			return CspOptimiser.simplifyPrec(lit);
		else if (AllDifferent.isAllDifferentLiteral(lit))
			return lit;

		throw new IllegalArgumentException("Cannot handle literal: " + lit);

	}

	private static Set<Variable> propagateLiteralToDomain(Literal<Variable> lit) {
		if (lit.getAtom().getSymbol().equals(Predicate.EQUALS))
			return propagateEqualsLiteralToDomain(lit);
		else if (lit.getAtom().getSymbol().equals(Predicate.PREC))
			return propagatePrecLiteralToDomain(lit);
		else if (AllDifferent.isAllDifferentLiteral(lit))
			return new HashSet<Variable>();
		else
			throw new IllegalArgumentException("Cannot handle literal: " + lit);

	}

	private static Set<Variable> propagateEqualsLiteralToDomain(Literal<Variable> lit) {
		if (!lit.getAtom().getSymbol().equals(Predicate.EQUALS))
			throw new IllegalArgumentException(lit.toString());

		Variable v1 = lit.getAtom().getParameters().get(0);
		Variable v2 = lit.getAtom().getParameters().get(1);
		Collection<Constant> d1 = CspOptimiser.DOMAINS.get(v1); // they are sets tho
		Collection<Constant> d2 = CspOptimiser.DOMAINS.get(v2);

		Set<Variable> changed = new HashSet<Variable>();
		if (!lit.getValue()) {
			if (d1.size() == 1) {
				if (d2.removeAll(d1)) {
					changed.add(v2);
				}
			}
			if (d2.size() == 1) {
				if (d1.removeAll(d2))
					changed.add(v1);
			}
		} else {
			if (d1.retainAll(d2))
				changed.add(v1);
			if (d2.retainAll(d1))
				changed.add(v2);
		}

		return changed;
	}

	private static Set<Variable> propagatePrecLiteralToDomain(Literal<Variable> lit) {
		Set<Variable> changed = new HashSet<Variable>();
		
		Variable v1 = lit.getAtom().getParameters().get(0);
		Variable v2 = lit.getAtom().getParameters().get(1);

		List<Constant> d1 = CspOptimiser.OP_DOMAINS.get(v1);
		List<Constant> d2 = CspOptimiser.OP_DOMAINS.get(v2);
		
		// MODIFY DOMAINS BASED ON LOWER/UPPER SET
		int postCount = CspOptimiser.PREC_GRAPH.getEdgesFrom(v1).size();			
		Set<Constant> oor = new HashSet<Constant>();
		for (int i = d1.size() - 1; i >= 0; i--) {
			Constant c = d1.get(i);
			int v = Integer.valueOf(c.getName());
			if (v > (CspOptimiser.HIGHEST_OP_NO - postCount))
				oor.add(c);
			else
				break;

		}

		if (d1.removeAll(oor))
			changed.add(v1);

		if (d1.isEmpty())
			throw new CspOptimiserException("CSP is unsatisfiable: " + lit);

		oor.clear();

		int preCount = CspOptimiser.PREC_GRAPH.getEdgesTo(v2).size();
		for (Constant c : d2) {
			int v = Integer.valueOf(c.getName());
			if (v < preCount)
				oor.add(c);
			else
				break;

		}

		if (d2.removeAll(oor))
			changed.add(v2);

		if (d2.isEmpty())
			throw new CspOptimiserException("CSP is unsatisfiable: " + lit);

		oor.clear();
		
		// MODIFY DOMAINS BASED ON MIN/MAX		
		if (lit.getValue()) { // v1 < v2
		
			// d1 < d2
			int d2highest = Integer.valueOf(d2.get(d2.size() - 1).getName());

			for (int i = d1.size() - 1; i >= 0; i--) {
				Constant c = d1.get(i);
				if (Integer.valueOf(c.getName()) >= d2highest)
					oor.add(c);
				else
					break;
			}

			if (d1.removeAll(oor))
				changed.add(v1);

			if (d1.isEmpty())
				throw new CspOptimiserException("CSP is unsatisfiable: " + lit);

			oor.clear();
			
			int d1lowest = Integer.valueOf(d1.get(0).getName());
			for (Constant c : d2) {
				if (Integer.valueOf(c.getName()) <= d1lowest)
					oor.add(c);
				else
					break;
			}

			if (d2.removeAll(oor))
				changed.add(v2);

			if (d2.isEmpty())
				throw new CspOptimiserException("CSP is unsatisfiable: " + lit);
			
			return changed;

		} else { // v2 <= v1
			
			// d2 <= d1, ie d2 < d1 as ops cannot be parallel
			int d1highest = Integer.valueOf(d1.get(d1.size() - 1).getName());

			for (int i = d2.size() - 1; i >= 0; i--) {
				Constant c = d2.get(i);
				if (Integer.valueOf(c.getName()) >= d1highest)
					oor.add(c);
				else
					break;
			}

			if (d2.removeAll(oor))
				changed.add(v2);

			if (d2.isEmpty())
				throw new CspOptimiserException("CSP is unsatisfiable: " + lit);

			oor.clear();
			
			int d2lowest = Integer.valueOf(d2.get(0).getName());
			for (Constant c : d1) {
				if (Integer.valueOf(c.getName()) <= d2lowest)
					oor.add(c);
				else
					break;
			}

			if (d1.removeAll(oor))
				changed.add(v1);

			if (d1.isEmpty())
				throw new CspOptimiserException("CSP is unsatisfiable: " + lit);

			return changed;

		}
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
		BinaryPropagator other = (BinaryPropagator) obj;
		if (original == null) {
			if (other.original != null)
				return false;
		} else if (!original.equals(other.original))
			return false;
		return true;
	}

}
