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

public interface Term extends Symbol {

	public static <T extends Term> List<T> normaliseTypes(List<T> terms, List<T> refTerms) {

		List<T> newParams = new ArrayList<T>();
		
		for (T term : terms) {

			boolean found = false;
			for (T refTerm : refTerms) {
				if (term.getName().equals(refTerm.getName()) && 
					(term.getType().hasSubtype(refTerm.getType()))) {
					found = true;
					newParams.add(refTerm);
					break;
				}
			}

			if (!found)
				throw new IllegalArgumentException("No matching term for " + term + " found in " + refTerms);

		}

		return newParams;
	}
	
	public static List<Type> getTypes(Term ... terms) {
		List<Type> types = new ArrayList<Type>();
		for (Term var : terms)
			types.add(var.getType());

		return types;
	}
	
	public static List<Type> getTypes(List<? extends Term> terms) {
		List<Type> types = new ArrayList<Type>();
		for (Term var : terms)
			types.add(var.getType());

		return types;
	}

	public Type getType();

}
