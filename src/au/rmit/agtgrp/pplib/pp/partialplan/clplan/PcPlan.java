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
package au.rmit.agtgrp.pplib.pp.partialplan.clplan;

import java.util.List;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pddl.pct.CausalStructure;
import au.rmit.agtgrp.pplib.pp.partialplan.PartialPlan;

public class PcPlan extends PartialPlan<CausalStructure> {
	
	private static final long serialVersionUID = 1L;
	
	protected final List<Operator<Variable>> planSteps;
	protected final Substitution<Constant> originalSub;

	public PcPlan(PddlProblem problem, List<Operator<Variable>> planSteps, Substitution<Constant> originalSub, CausalStructure constraints) {
		super(problem, planSteps, originalSub, originalSub, constraints);
		this.originalSub = originalSub;
		this.planSteps = planSteps;
	}

	public List<Operator<Variable>> getPlanSteps() {
		return planSteps;
	}
	
	public Substitution<Constant> getOriginalSub() {
		return originalSub;
	}

}
