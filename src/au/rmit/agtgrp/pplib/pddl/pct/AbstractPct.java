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
package au.rmit.agtgrp.pplib.pddl.pct;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;

public abstract class AbstractPct implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final WeakHashMap<AbstractPct, WeakReference<? extends AbstractPct>> CACHE = new WeakHashMap<AbstractPct, WeakReference<? extends AbstractPct>>();

	public static <T extends AbstractPct> T getCached(T prodCon) {
		@SuppressWarnings("unchecked")
		WeakReference<T> cached = (WeakReference<T>) CACHE.get(prodCon);
		if (cached != null) {
			T cachedPc = (T) cached.get();
			if (cachedPc != null) {
				return cachedPc;
			}
		}

		CACHE.put(prodCon, new WeakReference<T>(prodCon));

		return prodCon;
	}

	public final Operator<Variable> operator;
	public final Literal<Variable> literal;

	private final int hashCode;

	public AbstractPct(Operator<Variable> operator, Literal<Variable> literal) {
		this.operator = operator;
		this.literal = literal;

		if (operator == null || literal == null)
			throw new NullPointerException();

		verify();

		hashCode = computeHashCode();
	}

	protected abstract void verify();

	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((literal == null) ? 0 : literal.hashCode());
		result = prime * result + ((operator == null) ? 0 : operator.hashCode());
		return result;
	}

	public String toString() {
		return "(" + operator.getName() + ", " + (literal.getValue() ? "" : "-") + literal.getAtom().formatParameters() + ")";
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
		AbstractPct other = (AbstractPct) obj;
		if (literal == null) {
			if (other.literal != null)
				return false;
		} else if (!literal.equals(other.literal))
			return false;
		if (operator == null) {
			if (other.operator != null)
				return false;
		} else if (!operator.equals(other.operator))
			return false;
		return true;
	}

}
