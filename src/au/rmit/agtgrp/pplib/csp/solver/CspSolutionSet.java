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
package au.rmit.agtgrp.pplib.csp.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import au.rmit.agtgrp.pplib.csp.solver.output.CspOutputSet;
import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;

public class CspSolutionSet implements Iterable<Substitution<Constant>> {

	protected List<Variable> variables;
	protected Map<Integer, Constant> consMap;
	protected CspOutputSet outputSet;

	public CspSolutionSet(CspOutputSet outputSet, List<Variable> variables, Map<Integer, Constant> consMap) {
		this.outputSet = outputSet;
		this.variables = variables;
		this.consMap = consMap;

		Collections.sort(variables, Comparators.SYMBOL_COMPARATOR);
	}

	@Override
	public Iterator<Substitution<Constant>> iterator() {
		return new CspSolnIterator();
	}

	public int getSolutionCount() {
		return outputSet.getSolutionCount();
	}

	public CspOutputSet getCspOutputSet() {
		return outputSet;
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public Map<Integer, Constant> getConstantMap() {
		return consMap;
	}

	private class CspSolnIterator implements Iterator<Substitution<Constant>> {

		private Iterator<List<Integer>> outputIterator = outputSet.iterator();

		@Override
		public boolean hasNext() {
			return outputIterator.hasNext();
		}

		@Override
		public Substitution<Constant> next() {
			List<Integer> soln = outputIterator.next();
			if (soln.size() != variables.size()) {
				System.out.println(soln);
				System.out.println(variables);

				throw new IllegalArgumentException(
						"Solution size (" + soln.size() + ") != #variables (" + variables.size() + ")");
			}
			List<Constant> vals = new ArrayList<Constant>();

			for (int i = 0; i < soln.size(); i++) {
				Constant val = consMap.get(soln.get(i));
				if (val == null)
					throw new RuntimeException("Value not found: " + soln.get(i));

				vals.add(val);
			}
			return Substitution.buildFromValues(variables, vals);
		}
	}

}
