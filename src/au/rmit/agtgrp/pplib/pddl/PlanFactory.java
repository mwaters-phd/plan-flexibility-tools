package au.rmit.agtgrp.pplib.pddl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;

public class PlanFactory {

	public static Plan formatAsPlan(PddlProblem stripsProblem, List<Operator<Constant>> planSteps, 
			boolean statesAsOperators, boolean uniqueOperatorNames) {

		planSteps = new ArrayList<Operator<Constant>>(planSteps);
		if (statesAsOperators && !hasStatesAsOperators(planSteps, stripsProblem)) {
			planSteps.add(0, buildInitialOperator(stripsProblem, planSteps));
			planSteps.add(buildGoalOperator(stripsProblem));
		}

		planSteps = uniqueVariableNames(planSteps);

		if (uniqueOperatorNames)
			planSteps = uniqueOperatorNames(planSteps);

		return new Plan(stripsProblem, planSteps);
	}
	
	public static ParallelPlan formatAsParallelPlan(PddlProblem stripsProblem, List<List<Operator<Constant>>> planSteps, 
			boolean statesAsOperators, boolean uniqueOperatorNames) {
		
		List<Operator<Constant>> concat = new ArrayList<Operator<Constant>>();
		for (List<Operator<Constant>> list : planSteps)
			concat.addAll(list);
		
		Plan plan = formatAsPlan(stripsProblem, concat, statesAsOperators, uniqueOperatorNames);

		List<List<Operator<Constant>>> formatted = new ArrayList<List<Operator<Constant>>>();
		
		if (statesAsOperators) {
			List<Operator<Constant>> init = new ArrayList<Operator<Constant>>();
			init.add(plan.getGroundSteps().get(0));
			formatted.add(init);
		}
		
		int n = formatted.size();
		for (List<Operator<Constant>> step : planSteps) {
			formatted.add(plan.getGroundSteps().subList(n, n+step.size()));
			n+=step.size();
		}		
		
		if (statesAsOperators) {
			List<Operator<Constant>> goal = new ArrayList<Operator<Constant>>();
			goal.add(plan.getGroundSteps().get(plan.length()-1));
			formatted.add(goal);
		}
		
		return new ParallelPlan(stripsProblem, formatted);	
	}


	public static Operator<Constant> buildInitialOperator(PddlProblem problem, List<Operator<Constant>> planSteps) {

		// all domain objects/constants must appear as params to initial state
		List<Constant> objs = new ArrayList<Constant>(problem.getObjects()); //problem
		objs.addAll(problem.getDomain().getConstants()); //domain

		// sort by name
		Collections.sort(objs, Comparators.SYMBOL_COMPARATOR);

		// one variable per constant
		int varn = 0;
		Map<Constant, Variable> objVarMap = new HashMap<Constant, Variable>();
		Map<Variable, Constant> varObjMap = new HashMap<Variable, Constant>();
		List<Variable> variables = new ArrayList<Variable>();			

		for (Constant obj : objs) { 
			Variable fv = new Variable(obj.getType(), Plan.VAR_PREFIX + varn++).intern();
			objVarMap.put(obj, fv);
			varObjMap.put(fv, obj);
			variables.add(fv);
		}

		OperatorFactory<Constant> opf = new OperatorFactory<Constant>(Plan.INIT_OP_NAME);
		opf.addVariables(variables);
		opf.addParameters(objs);

		// effect for each literal in initial state
		for (Literal<Constant> fact : problem.getInitialState()) {
			List<Variable> factParams = new ArrayList<Variable>();
			for (Constant gv : fact.getAtom().getParameters())
				factParams.add(objVarMap.get(gv));

			opf.addPostcondition(new Literal<Constant>(fact.getAtom().getSymbol(), 
					factParams, fact.getAtom().getParameters(), true).intern());
		}

		return opf.getOperator();
	}


	public static Operator<Constant> buildGoalOperator(PddlProblem problem) {

		// get all objs appearing in goal state
		Set<Constant> objSet = new HashSet<Constant>();

		for (Literal<Constant> fact : problem.getGoalState())
			objSet.addAll(fact.getAtom().getParameters());

		// sort by name
		List<Constant> objs = new ArrayList<Constant>(objSet);
		Collections.sort(objs, Comparators.SYMBOL_COMPARATOR);

		// one variable per constant
		int varn = 0;
		List<Variable> params = new ArrayList<Variable>();		
		Map<Constant, Variable> objVarMap = new HashMap<Constant, Variable>();

		for (Constant obj : objs) { 
			Variable fv = new Variable(obj.getType(), Plan.VAR_PREFIX + varn++).intern();
			objVarMap.put(obj, fv);
			params.add(fv);
		}

		OperatorFactory<Constant> opf = new OperatorFactory<Constant>(Plan.GOAL_OP_NAME);

		opf.addVariables(params);
		opf.addParameters(objs);

		for (Literal<Constant> fact : problem.getGoalState()) {
			List<Variable> factParams = new ArrayList<Variable>();
			for (Constant gv : fact.getAtom().getParameters())
				factParams.add(objVarMap.get(gv));

			opf.addPrecondition(new Literal<Constant>(fact.getAtom().getSymbol(), factParams, fact.getAtom().getParameters(), true).intern());
		}

		return opf.getOperator();
	}

	public static boolean hasStatesAsOperators(List<Operator<Constant>> actions, PddlProblem problem) {

		Operator<Constant> initAction = actions.get(0);
		if (!initAction.getName().equals(Plan.INIT_OP_NAME) || !initAction.getPreconditions().isEmpty())
			return false;

		State<Constant> s = new State<Constant>();
		s.applyOperator(initAction);
		if (!problem.getInitialState().equals(s))
			return false;

		Operator<Constant> goalAction = actions.get(actions.size()-1);
		if (!goalAction.getName().equals(Plan.GOAL_OP_NAME))
			return false;
		
		s = new State<Constant>(new HashSet<Literal<Constant>>(goalAction.getPreconditions()));
		if (!s.isSuperState(problem.getGoalState()))
			return false;

		return true;
	}


	public static List<Operator<Constant>> uniqueVariableNames(List<Operator<Constant>> planSteps) {

		List<Operator<Constant>> renamed = new ArrayList<Operator<Constant>>();

		int varn = 0;
		for (Operator<Constant> step : planSteps) {
			List<Variable> newVars = new ArrayList<Variable>();
			for (Variable var : step.getVariables())
				newVars.add(new Variable(var.getType(), Plan.VAR_PREFIX + varn++).intern());

			Operator<Constant> renamedOp = step.resetVariables(newVars);
			renamed.add(renamedOp);
		}

		return renamed;
	}

	public static List<Operator<Constant>> uniqueOperatorNames(List<Operator<Constant>> planSteps) {

		List<Operator<Constant>> renamed = new ArrayList<Operator<Constant>>();

		int opn = 1;
		String formatStr = "%0" + (int) Math.ceil(Math.log10(planSteps.size())) + "d";
		
		for (Operator<Constant> step : planSteps) {
			// only rename standard steps, not init or goal.
			if (!step.getSymbol().getName().equals(Plan.GOAL_OP_NAME) && 
				!step.getSymbol().getName().equals(Plan.INIT_OP_NAME)) {
				
				String newName = String.format(formatStr, opn++) + "_" + step.getSymbol().getName();
				Operator<Constant> renamedOp = step.rename(newName);
				renamed.add(renamedOp);
			}
			else
				renamed.add(step);
		}

		return renamed;
	}

	public static boolean hasUniqueVariableNames(Collection<? extends Operator<?>> actions) {
		Set<String> uniqueVars = new HashSet<String>();
		int nVars = 0;
		for (Operator<?> planStep : actions) {
			for (Variable var : planStep.getVariables()) {
				uniqueVars.add(var.getName());
				nVars++;
			}
		}
		
		return uniqueVars.size() == nVars;
	}

	public static boolean hasUniqueOperatorNames(Collection<? extends Operator<?>> actions) {
		Set<String> uniqueOps = new HashSet<String>();
		for (Operator<?> planStep : actions)
			uniqueOps.add(planStep.getName());
		
		return uniqueOps.size() == actions.size();
	}

}
