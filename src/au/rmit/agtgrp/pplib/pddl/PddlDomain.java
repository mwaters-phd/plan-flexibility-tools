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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.parser.PddlFormatter;

public class PddlDomain {
	
	protected final String name;

	protected final Set<Predicate> relations;
	protected final Set<Type> types;
	protected final Set<Constant> constants;
	protected final Set<Operator<Variable>> operators;
	protected final Set<Literal<Constant>> facts;
	
	protected final EnumSet<Requirement> requirements;
	
	public PddlDomain(String name, Set<Predicate> relations, Set<Type> types, Set<Constant> constants, Set<Operator<Variable>> operators) {
		this(name, relations, types, constants, operators, new HashSet<Literal<Constant>>());
	}
	
	public PddlDomain(String name, Set<Predicate> relations, Set<Type> types, Set<Constant> constants, Set<Operator<Variable>> operators, Set<Literal<Constant>> facts) {
		this.name = name;
		this.relations = relations;
		this.types = types;
		this.constants = constants;
		this.operators = operators;
		this.facts = facts;
		
		requirements = EnumSet.noneOf(Requirement.class);
		findRequirements();
	}
	
	private void findRequirements() {
		requirements.add(Requirement.STRIPS);
		requirements.add(Requirement.TYPING);
		
		for (Operator<Variable> op : operators) {
			for (Literal<Variable> precon : op.getPreconditions()) {
				if (!precon.getValue())
					requirements.add(Requirement.NEGATIVE_PRECONDITIONS);
				
				if (precon.getAtom().getSymbol().equals(Predicate.EQUALS))
					requirements.add(Requirement.EQUALITY);
			}
		}
	}

	public String getName() {
		return name;
	}
	
	public Set<Literal<Constant>> getFacts() {
		return facts;
	}

	public Set<Predicate> getPredicates() {
		return relations;
	}

	public Set<Operator<Variable>> getOperators() {
		return operators;
	}

	public Set<Type> getTypes() {
		return types;
	}

	public Set<Constant> getConstants() {
		return constants;
	}
	
	public EnumSet<Requirement> getRequirements() {
		return requirements;
	}

	public String toString() {
		return PddlFormatter.getDomainString(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constants == null) ? 0 : constants.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((operators == null) ? 0 : operators.hashCode());
		result = prime * result + ((relations == null) ? 0 : relations.hashCode());
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PddlDomain other = (PddlDomain) obj;
		if (constants == null) {
			if (other.constants != null)
				return false;
		} else if (!constants.equals(other.constants))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (operators == null) {
			if (other.operators != null)
				return false;
		} else if (!operators.equals(other.operators))
			return false;
		if (relations == null) {
			if (other.relations != null)
				return false;
		} else if (!relations.equals(other.relations))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}
	
	

}
