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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Type implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Map<Type, List<Type>> SUBCLASS_MAP = new HashMap<Type, List<Type>>();
	private static final Map<Type, Type> SUPERCLASS_MAP = new HashMap<Type, Type>();

	public static final Type ANYTHING_TYPE = new Type("anything");
	public static final Type OPERATOR_TYPE = new Type("operator");
	public static final Type INT_TYPE = new Type("int");

	/**
	 *  Use this method when loading a new domain into the same JVM.
	 */
	public static void clearTypeHierarchy() {
		SUBCLASS_MAP.clear();
		SUPERCLASS_MAP.clear();
	}

	private final String name;
	private final int hashCode;

	public Type(String name) {
		this.name = name.trim().toLowerCase().intern();
		hashCode = computeHashCode();

		if (!SUBCLASS_MAP.containsKey(this))
			SUBCLASS_MAP.put(this, new ArrayList<Type>());

	}

	public String getName() {
		return name;
	}

	public List<Type> getImmediateSubtypes() {
		return SUBCLASS_MAP.get(this);
	}

	public void addSubtype(Type subtype) {

		Type supertype = SUPERCLASS_MAP.get(subtype);
		if (supertype != null && !supertype.equals(this))
			throw new IllegalArgumentException(subtype + " already has supertype " + supertype);

		SUBCLASS_MAP.get(this).add(subtype);
		SUPERCLASS_MAP.put(subtype, this);
	}

	public Type getImmediateSupertype() {
		Type supertype = SUPERCLASS_MAP.get(this);
		if (supertype != null)
			return supertype;

		return ANYTHING_TYPE;
	}

	public void setSupertype(Type supertype) {
		supertype.addSubtype(this);
	}

	public boolean hasSupertype(Type type) {
		if (this.equals(type))
			return true;

		if (this.equals(ANYTHING_TYPE))
			return false;

		return this.getImmediateSupertype().hasSupertype(type);
	}

	public boolean hasSubtype(Type type) {
		if (type.equals(this))
			return true;

		for (Type subtype : this.getImmediateSubtypes()) {
			if (subtype.hasSubtype(type))
				return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return name;
	}

	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Type other = (Type) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
