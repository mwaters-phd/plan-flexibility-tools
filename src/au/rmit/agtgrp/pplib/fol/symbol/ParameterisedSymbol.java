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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class ParameterisedSymbol implements Symbol {

	private static final long serialVersionUID = 1L;
	
	protected final String name;
	protected final List<Type> types;

	private final int hashCode;

	public ParameterisedSymbol(String name, Type... types) {
		this.name = name.intern();
		this.types = Arrays.asList(types);

		hashCode = computeHashCode();
	}

	public ParameterisedSymbol(String name, List<Type> types) {
		this.name = name.intern();
		this.types = types;

		hashCode = computeHashCode();
	}

	public ParameterisedSymbol rename(String newName) {
		return new ParameterisedSymbol(newName, types);
	}

	@Override
	public String getName() {
		return name;
	}

	public List<Type> getTypes() {
		return Collections.unmodifiableList(types);
	}

	public Type getTypeAt(int i) {
		return types.get(i);
	}

	public int getArity() {
		return types.size();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(name + "(");
		sb.append(FormattingUtils.toString(types, " x "));
		sb.append(")");

		return sb.toString();
	}

	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
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
		ParameterisedSymbol other = (ParameterisedSymbol) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

}
