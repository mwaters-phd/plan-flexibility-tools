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
package au.rmit.agtgrp.pplib.csp.alldiff;

import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.predicate.Atom;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;

public class AllDifferent {

	public static final String ALL_DIFF_PREDICATE_NAME = "allDiff";

	public static boolean isAllDifferentLiteral(Literal<?> lit) {
		return lit != null && lit.getAtom().getSymbol().getName().equals(ALL_DIFF_PREDICATE_NAME);
	}

	public static boolean isAllDifferentSymbol(Predicate predicate) {
		return predicate.getName().equals(ALL_DIFF_PREDICATE_NAME);
	}

	public static Predicate buildAllDifferentPredicate(int nArgs, Type type) {
		List<Type> types = new ArrayList<Type>();
		for (int i = 0; i < nArgs; i++)
			types.add(type);

		return new Predicate(ALL_DIFF_PREDICATE_NAME, types);

	}

	public static Literal<Variable> buildAllDifferent(List<Variable> variables) {
		return new Literal<Variable>(
				new Atom<Variable>(buildAllDifferentPredicate(variables.size(), variables.get(0).getType()), 
				variables, Substitution.identity(variables)).intern(), true).intern();
	}


}
