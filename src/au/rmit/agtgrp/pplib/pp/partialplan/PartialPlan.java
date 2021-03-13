package au.rmit.agtgrp.pplib.pp.partialplan;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.PddlDomain;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.PlanFactory;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class PartialPlan<T> implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	protected final PddlProblem problem;
	protected final Set<Operator<Variable>> operators;
	protected final Operator<Variable> init;
	protected final Operator<Variable> goal;
	
	protected final Substitution<Constant> initSub;
	protected final Substitution<Constant> goalSub;

	protected final T constraints;
	
	public PartialPlan(PddlProblem problem, Collection<Operator<Variable>> operators, Substitution<Constant> initSub, Substitution<Constant> goalSub, T constraints) {

		if (problem == null || operators == null || initSub == null || goalSub == null || constraints == null) 		
			throw new NullPointerException("Arguments cannot be null");

		this.problem = problem;
		this.operators = new HashSet<Operator<Variable>>(operators);

		goal = getOpByName(operators, Plan.GOAL_OP_NAME);
		init = getOpByName(operators, Plan.INIT_OP_NAME);

		if (init == null || goal == null)
			throw new IllegalArgumentException("No init or goal operator");

		if (!PlanFactory.hasUniqueVariableNames(operators)) {
			StringBuilder sb = new StringBuilder();
			for (Operator<Variable> step : operators) 
				sb.append(step.getName() + "(" + FormattingUtils.toString(step.getVariables(), ",") + ")\n");

			throw new IllegalArgumentException("Operator/variable name error:\n" + sb.toString());
		}
		
		this.goalSub = Substitution.trim(goalSub, goal.getVariables());
		this.initSub = Substitution.trim(initSub, init.getVariables());
		this.constraints = constraints;
		
	}

	private Operator<Variable> getOpByName(Collection<Operator<Variable>> operators, String name) {
		for (Operator<Variable> op : operators) {
			if (op.getName().equals(name))
				return op;
		}
		
		return null;
	}
	
	public PddlProblem getProblem() {
		return problem;
	}

	public PddlDomain getDomain() {
		return problem.getDomain();
	}
	
	public Set<Operator<Variable>> getOperators() {
		return operators;
	}

	public Operator<Variable> getInitAction() {
		return init;
	}

	public Operator<Variable> getGoalAction() {
		return goal;
	}
	
	public Substitution<Constant> getInitSub() {
		return initSub;
	}

	public Substitution<Constant> getGoalSub() {
		return goalSub;
	}
	
	public T getConstraints() {
		return constraints;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((constraints == null) ? 0 : constraints.hashCode());
		result = prime * result + ((goal == null) ? 0 : goal.hashCode());
		result = prime * result + ((goalSub == null) ? 0 : goalSub.hashCode());
		result = prime * result + ((init == null) ? 0 : init.hashCode());
		result = prime * result + ((initSub == null) ? 0 : initSub.hashCode());
		result = prime * result + ((operators == null) ? 0 : operators.hashCode());
		result = prime * result + ((problem == null) ? 0 : problem.hashCode());
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
		PartialPlan<?> other = (PartialPlan<?>) obj;
		if (constraints == null) {
			if (other.constraints != null)
				return false;
		} else if (!constraints.equals(other.constraints))
			return false;
		if (goal == null) {
			if (other.goal != null)
				return false;
		} else if (!goal.equals(other.goal))
			return false;
		if (goalSub == null) {
			if (other.goalSub != null)
				return false;
		} else if (!goalSub.equals(other.goalSub))
			return false;
		if (init == null) {
			if (other.init != null)
				return false;
		} else if (!init.equals(other.init))
			return false;
		if (initSub == null) {
			if (other.initSub != null)
				return false;
		} else if (!initSub.equals(other.initSub))
			return false;
		if (operators == null) {
			if (other.operators != null)
				return false;
		} else if (!operators.equals(other.operators))
			return false;
		if (problem == null) {
			if (other.problem != null)
				return false;
		} else if (!problem.equals(other.problem))
			return false;
		return true;
	}
	
}
