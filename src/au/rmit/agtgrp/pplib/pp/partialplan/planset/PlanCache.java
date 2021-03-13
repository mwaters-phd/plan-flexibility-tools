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
package au.rmit.agtgrp.pplib.pp.partialplan.planset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.parser.PddlFormatter;
import au.rmit.agtgrp.pplib.pddl.parser.PddlParser;
import au.rmit.agtgrp.pplib.utils.FileLinesIterator;

public class PlanCache implements PlanSet {

	private PddlProblem stripsProblem;

	private File planFile;
	private int count;

	public PlanCache(PddlProblem stripsProblem, File planFile, int count) {
		this.stripsProblem = stripsProblem;
		this.planFile = planFile;
		this.count = count;
	}

	public File getPlanFile() {
		return planFile;
	}

	@Override
	public int getPlanCount() {
		return count;
	}

	@Override
	public Iterator<Plan> iterator() {
		return new PctPlanCacheIterator(planFile);
	}

	private class PctPlanCacheIterator implements Iterator<Plan> {

		private PddlParser parser;
		private FileLinesIterator fileIt;

		public PctPlanCacheIterator(File planFile) {
			fileIt = new FileLinesIterator(planFile);
			parser = new PddlParser();
			parser.setDomainAndProblem(stripsProblem);
		}

		@Override
		public boolean hasNext() {
			return fileIt.hasNext();
		}

		@Override
		public Plan next() {
			try {
				List<String> planStrs = new ArrayList<String>();

				String line;
				do {
					line = fileIt.next();
					if (!line.equals(PddlFormatter.PLAN_FILE_SEPARATOR))
						planStrs.add(line);
				} while (fileIt.hasNext() && !line.equals(PddlFormatter.PLAN_FILE_SEPARATOR));

				parser.parseFDPlan(planStrs);
				return parser.getPlan();

			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

	}

}
