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

import java.util.List;

import au.rmit.agtgrp.pplib.fol.symbol.ParameterisedSymbol;
import au.rmit.agtgrp.pplib.fol.symbol.Type;

public class Predicate extends ParameterisedSymbol {

	private static final long serialVersionUID = 1L;
	
	public static final Predicate TRUE = new Predicate("+");
	public static final Predicate EQUALS = new Predicate("=", true, true, true, Type.ANYTHING_TYPE, Type.ANYTHING_TYPE);
	public static final Predicate PREC = new Predicate("<", false, true, false, Type.ANYTHING_TYPE, Type.ANYTHING_TYPE);

	private final boolean reflexive;
	private final boolean transitive;
	private final boolean symmetric;

	private final int hashCode;

	public Predicate(String name, boolean reflexive, boolean transitive, boolean symmetric, List<Type> types) {
		super(name, types);
		this.reflexive = reflexive;
		this.transitive = transitive;
		this.symmetric = symmetric;

		hashCode = computeHashCode();
	}

	public Predicate(String name, boolean reflexive, boolean transitive, boolean symmetric, Type... types) {
		super(name, types);
		this.reflexive = reflexive;
		this.transitive = transitive;
		this.symmetric = symmetric;

		hashCode = computeHashCode();
	}

	public Predicate(String name, List<Type> types) {
		super(name, types);
		this.reflexive = false;
		this.transitive = false;
		this.symmetric = false;

		hashCode = computeHashCode();
	}

	public Predicate(String name, Type... types) {
		super(name, types);
		this.reflexive = false;
		this.transitive = false;
		this.symmetric = false;

		hashCode = computeHashCode();
	}

	@Override
	public Predicate rename(String newName) {
		return new Predicate(newName, this.reflexive, this.transitive, this.symmetric, super.types);
	}

	public boolean isReflexive() {
		return reflexive;
	}

	public boolean isTransitive() {
		return transitive;
	}

	public boolean isSymmetric() {
		return symmetric;
	}

	private int computeHashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (reflexive ? 1231 : 1237);
		result = prime * result + (symmetric ? 1231 : 1237);
		result = prime * result + (transitive ? 1231 : 1237);
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
		Predicate other = (Predicate) obj;
		if (reflexive != other.reflexive)
			return false;
		if (symmetric != other.symmetric)
			return false;
		if (transitive != other.transitive)
			return false;
		return true;
	}

}
