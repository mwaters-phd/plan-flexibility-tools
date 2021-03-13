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

import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;

public class Producer extends AbstractPct {

	private static final long serialVersionUID = 1L;

	public Producer(Operator<Variable> operator, Literal<Variable> literal) {
		super(operator, literal);		
	}
	
	@Override
	protected void verify() {
	
		if (operator.getPostconditions().contains(literal))
			return;
		
		if (!literal.getValue() && !operator.getPostconditions().contains(literal.getNegated()))
			return;
		
		throw new IllegalArgumentException(literal + " is not a postcondition of " + operator.getName());
			
	}

	public Producer intern() {
		return getCached(this);
	}
	
}
