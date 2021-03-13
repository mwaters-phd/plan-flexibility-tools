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
package au.rmit.agtgrp.pplib.csp.solver.output;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class InMemoryCspOutputSet implements CspOutputSet {

	private Collection<List<Integer>> output;

	public InMemoryCspOutputSet(Collection<List<Integer>> output) {
		this.output = output;
	}

	@Override
	public Iterator<List<Integer>> iterator() {
		return output.iterator();
	}

	@Override
	public int getSolutionCount() {
		return output.size();
	}

	@Override
	public boolean isCached() {
		return false;
	}

	@Override
	public File getSolutionsFile() {
		return null;
	}

}
