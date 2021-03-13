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

import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.PddlProblem.PlanResult;

public class Plan {

	public static final String TYPE_PREFIX 	= "type_";
	public static final String VAR_PREFIX 	= "v_";

	public static final String GOAL_OP_NAME = "goal";
	public static final String INIT_OP_NAME = "init";
	
	private final PddlProblem problem;

	private final List<Operator<Variable>> planSteps;	
	private final Substitution<Constant> substitution;
	private final List<Operator<Constant>> groundPlanSteps;	

	private final boolean uniqueVarNames;
	private final boolean uniqueOperatorNames;
	private final boolean statesAsOperators;

	
	public Plan(PddlProblem problem, List<Operator<Constant>> planSteps) {

		if (problem == null || planSteps == null)
			throw new NullPointerException("Arguments cannot be null");

		uniqueVarNames = PlanFactory.hasUniqueVariableNames(planSteps);
		uniqueOperatorNames = PlanFactory.hasUniqueOperatorNames(planSteps);
		statesAsOperators = PlanFactory.hasStatesAsOperators(planSteps, problem);	
		
		this.groundPlanSteps = planSteps;
		this.planSteps = new ArrayList<Operator<Variable>>();
		this.problem = problem;

		List<Variable> vars = new ArrayList<Variable>();
		List<Constant> vals = new ArrayList<Constant>();
		for (Operator<Constant> step : planSteps) {
			this.planSteps.add(step.rebind(step.getVariables()));
			vars.addAll(step.getVariables());
			vals.addAll(step.getParameters());
		}

		substitution = Substitution.build(vars, vals);

	}

	public List<Operator<Variable>> getPlanSteps() {
		return planSteps;
	}

	public int length() {
		return planSteps.size();
	}
	
	public int getMakespan() {
		return planSteps.size();
	}

	public PddlProblem getProblem() {
		return problem;
	}

	public PddlDomain getDomain() {
		return problem.getDomain();
	}

	public State<Constant> getInitialState() {
		return problem.getInitialState();
	}

	public State<Constant> getGoalState() {
		return problem.getGoalState();
	}

	public Operator<Variable> getInitialAction() {
		if (planSteps.get(0).getName().equals(INIT_OP_NAME))
			return planSteps.get(0);
		
		return null;
	}

	public Operator<Variable> getGoalAction() {
		if (planSteps.get(planSteps.size() - 1).getName().equals(GOAL_OP_NAME))
			return planSteps.get(planSteps.size() - 1);
		
		return null;
	}

	public Substitution<Constant> getSubstitution() {
		return substitution;
	}
	
	public List<Operator<Constant>> getGroundSteps() {
		return groundPlanSteps;
	}
	
	public PlanResult validate() {
		return problem.validatePlan(this);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < planSteps.size(); i++) {
			Operator<Constant> op = planSteps.get(i).applySubstitution(substitution);

			sb.append("(" + op.getName());
			
			for (Constant c : op.getParameters())
				sb.append(" " + c.getName());
	
			sb.append(")");
			if (i < planSteps.size() - 1)
				sb.append("\n");
		}
		return sb.toString();
	}
	
	public boolean hasUniqueVarNames() {
		return uniqueVarNames;
	}

	public boolean hasUniqueOperatorNames() {
		return uniqueOperatorNames;
	}

	public boolean hasStatesAsOperators() {
		return statesAsOperators;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((planSteps == null) ? 0 : planSteps.hashCode());
		result = prime * result + ((problem == null) ? 0 : problem.hashCode());
		result = prime * result + ((substitution == null) ? 0 : substitution.hashCode());
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
		Plan other = (Plan) obj;
		if (planSteps == null) {
			if (other.planSteps != null)
				return false;
		} else if (!planSteps.equals(other.planSteps))
			return false;
		if (problem == null) {
			if (other.problem != null)
				return false;
		} else if (!problem.equals(other.problem))
			return false;
		if (substitution == null) {
			if (other.substitution != null)
				return false;
		} else if (!substitution.equals(other.substitution))
			return false;
		return true;
	}
}
