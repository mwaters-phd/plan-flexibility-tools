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

import java.util.HashSet;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;
import au.rmit.agtgrp.pplib.pddl.parser.PddlFormatter;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class PddlProblem {

	protected final PddlDomain domain;
	protected final String name;
	protected final Set<Constant> objects;
	protected final State<Constant> initialState;
	protected final State<Constant> goalState;

	public PddlProblem(PddlDomain domain, String name, Set<Constant> objects, State<Constant> initialState, State<Constant> goalState) {
		this.domain = domain;
		this.name = name;
		this.objects = objects;
		Set<Literal<Constant>> initFacts = new HashSet<Literal<Constant>>(domain.getFacts());
		initFacts.addAll(initialState.getFacts());
		this.initialState = new State<Constant>(initFacts);
		this.goalState = goalState;
	}
	
	public PddlDomain getDomain() {
		return domain;
	}

	public String getName() {
		return name;
	}

	public Set<Constant> getObjects() {
		return objects;
	}

	public State<Constant> getInitialState() {
		return initialState;
	}

	public State<Constant> getGoalState() {
		return goalState;
	}

	public PlanResult validateAll(Iterable<? extends Plan> plans) {
		for (Plan plan : plans) {
			PlanResult result = validatePlan(plan);
			if (!result.isValid) {
				return result;
			}
		}
		return PlanResult.valid(null);
	}

	public PlanResult validatePlan(Plan plan) {
		
		State<Constant> s = null;
		Operator<Variable> initOp = plan.getInitialAction();
		if (initOp != null) {
			if (!initOp.getPreconditions().isEmpty())
				return PlanResult.failure(plan, "Initial operator has preconditions");
			
			s = new State<Constant>(initOp.applySubstitution(plan.getSubstitution()).getPostconditions());
			
		}
		else
			s = initialState;
		
		
		// check that plan can be executed
		int firstOp = initOp == null ? 0 : 1; //ignore first op if init is present
		for (int i = firstOp; i < plan.length(); i++) {
			
			Operator<Constant> step = plan.getPlanSteps().get(i).applySubstitution(plan.getSubstitution());
			
			//check that action can be executed
			if (!s.satisfiesPreconditions(step)) {
				return PlanResult.failure(plan, "PctPlan: " + plan.toString() + "\nCannot execute:\nOp: " + step
						+ "\nOp def: " + step.getSymbol() + "\nParams: " + FormattingUtils.toString(step.getParameters())
						+ "\nIn state: \n"
						+ FormattingUtils.toString(s.getFacts(), "\n", Comparators.LITERAL_COMPARATOR));
			}
			
			s = s.applyOperator(step);
		}

		// end of plan reached -- check goal state
		if (s.isSuperState(goalState))
			return PlanResult.valid(plan);
		else
			return PlanResult.failure(plan, "Did not achieve goal: s_g = \n"
					+ FormattingUtils.toString(s.getFacts(), "\n", Comparators.LITERAL_COMPARATOR));

	}

	public String toString() {
		return PddlFormatter.getProblemString(domain, this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((goalState == null) ? 0 : goalState.hashCode());
		result = prime * result + ((initialState == null) ? 0 : initialState.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((objects == null) ? 0 : objects.hashCode());
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
		PddlProblem other = (PddlProblem) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (goalState == null) {
			if (other.goalState != null)
				return false;
		} else if (!goalState.equals(other.goalState))
			return false;
		if (initialState == null) {
			if (other.initialState != null)
				return false;
		} else if (!initialState.equals(other.initialState))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (objects == null) {
			if (other.objects != null)
				return false;
		} else if (!objects.equals(other.objects))
			return false;
		return true;
	}
	
	public static class PlanResult {

		public static PlanResult valid(Plan plan) {
			return new PlanResult(plan, true, "pass");
		};

		public static PlanResult failure(Plan plan, String message) {
			return new PlanResult(plan, false, message);
		}

		public final boolean isValid;
		public final Plan plan;
		public final String message;

		private PlanResult(Plan plan, boolean isValid, String message) {
			this.isValid = isValid;
			this.message = message;
			this.plan = plan;
		}
	}





	
}
