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

import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.utils.collections.ObjectCache;

public class Variable implements Term {

	private static final long serialVersionUID = 1L;

	private static final ObjectCache<Variable> CACHE = new ObjectCache<Variable>();
	
	public static List<Variable> buildVariables(List<Type> types) {
		List<Variable> variables = new ArrayList<Variable>();
		int i = 0;
		for (Type t : types)
			variables.add(new Variable(t, "x_" + i++).intern());
		
		return variables;
	}
	
	public static List<Variable> buildVariables(List<Type> types, List<String> labels) {
		List<Variable> variables = new ArrayList<Variable>();
		int i = 0;
		for (Type t : types)
			variables.add(new Variable(t, labels.get(i++)).intern());
		
		return variables;
	}
	
	private final String name;
	private final Type type;
	
	private final int hashCode;
	
	public Variable(Type type, String name) {
		this.type = type;
		this.name = name.intern();	
		
		this.hashCode = computeHashCode();
	}

	public Variable intern() {
		return CACHE.get(this);
	}
	
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return type + " " + name;
	}
	
	private int computeHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Variable other = (Variable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	


}
