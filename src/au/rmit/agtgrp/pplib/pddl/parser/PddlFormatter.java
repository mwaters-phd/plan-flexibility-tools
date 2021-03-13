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
package au.rmit.agtgrp.pplib.pddl.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Atom;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.PddlDomain;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.Requirement;

public class PddlFormatter {

	public static final String PLAN_FILE_SEPARATOR = ";;;";

	public static String getDomainString(PddlDomain domain) {
		StringBuilder sb = new StringBuilder();
		sb.append("(define (domain " + domain.getName() + ")");
		sb.append("\n");
		sb.append(getRequirements(domain) + "\n");
		sb.append("\n;********** TYPES ***********\n");
		sb.append(getTypes(domain) + "\n");
		sb.append("\n;******** CONSTANTS *********\n");
		sb.append(getConstants(domain) + "\n");
		sb.append("\n;******** PREDICATES ********\n");
		sb.append(getPredicates(domain) + "\n");
		sb.append("\n;********* ACTIONS *********\n");
		sb.append(getActions(domain) + "\n");
		sb.append(")");
		return sb.toString();

	}

	public static String getRequirements(PddlDomain domain) {
		StringBuilder sb = new StringBuilder();

		sb.append("(:requirements");
		for (Requirement req : domain.getRequirements()) {
			sb.append(" :" + req.getPddlString());
		}
		sb.append(")");
	
		return sb.toString();
	}

	public static String getTypes(PddlDomain domain) {
			
		StringBuilder sb = new StringBuilder();
		sb.append("(:types ");

		Set<Type> types = new HashSet<Type>(domain.getTypes());

		Queue<Type> queue = new LinkedList<Type>();
		queue.add(Type.ANYTHING_TYPE);

		Type parent;
		List<Type> children;

		while (!queue.isEmpty()) {
			parent = queue.poll();
			types.remove(parent);

			children = new ArrayList<Type>();

			for (Type cls : types) {
				if (cls.getImmediateSupertype().equals(parent)) {
					children.add(cls);
				}
			}

			queue.addAll(children);

			if (!children.isEmpty()) {
				sb.append("\n\t");
				for (Type cls : children)
					sb.append(cls + " ");

				sb.append("- " + parent);
			}
		}

		sb.append("\n)");
		return sb.toString();
	}

	public static String getPredicates(PddlDomain domain) {
		StringBuilder sb = new StringBuilder();
		sb.append("(:predicates \n");

		List<Predicate> relations = new ArrayList<Predicate>(domain.getPredicates());
		Collections.sort(relations, Comparators.SYMBOL_COMPARATOR);

		for (Predicate relation : relations) {
			sb.append("\t(" + relation.getName());

			for (int i = 0; i < relation.getArity(); i++) {
				sb.append(" ?p" + i + " - " + relation.getTypes().get(i));
			}
			sb.append(")\n");
		}

		sb.append(")");
		return sb.toString();
	}

	public static String getActions(PddlDomain domain) {

		List<Operator<Variable>> so = new ArrayList<Operator<Variable>>(domain.getOperators());
		Collections.sort(so, Comparators.SYMBOL_COMPARATOR);

		StringBuilder sb = new StringBuilder();
		for (Operator<Variable> operator : so) {
			sb.append("(:action " + operator.getName() + "\n");

			// parameters
			sb.append("\t:parameters (");
			sb.append(formatTypedList(operator.getVariables()));
			sb.append(")\n");

			// preconditions
			sb.append("\t:precondition (and\n");
			for (Literal<? extends Term> term : operator.getPreconditions()) {
				sb.append("\t\t" + formatCondition(term) + "\n");
			}
			sb.append("\t)\n");

			// effects
			sb.append("\t:effect (and\n");
			for (Literal<? extends Term> term : operator.getPostconditions()) {
				sb.append("\t\t" + formatCondition(term) + "\n");
			}

			sb.append("\t)\n");

			// final bracket
			sb.append(")\n\n");
		}

		return sb.toString();
	}

	public static String getConstants(PddlDomain domain) {
		
		if (domain.getConstants().isEmpty())
			return "";
		
		StringBuilder sb = new StringBuilder();

		sb.append("(:constants\n");
		sb.append(formatObjectList(domain.getConstants()));
		sb.append(")");

		return sb.toString();
	}

	public static String getProblemString(PddlDomain domain, PddlProblem problem) {
		StringBuilder sb = new StringBuilder();

		sb.append("(define (problem " + problem.getName() + ")\n");
		sb.append("(:domain " + domain.getName() + ")\n");

		sb.append("\n;********* OBJECTS **********\n");
		sb.append(getObjects(problem) + "\n");
		sb.append("\n;****** INITIAL STATE *******\n");
		sb.append(getInitialState(problem) + "\n");
		sb.append("\n;******* GOAL STATE *********\n");
		sb.append(getGoalState(problem) + "\n");

		sb.append(")");
		return sb.toString();
	}

	public static String getObjects(PddlProblem problem) {
		StringBuilder sb = new StringBuilder();

		sb.append("(:objects\n");
		sb.append(formatObjectList(problem.getObjects()));
		sb.append(")");

		return sb.toString();
	}

	public static String getInitialState(PddlProblem problem) {
		StringBuilder sb = new StringBuilder();
		sb.append("(:init\n");

		List<Literal<Constant>> initialState = new ArrayList<Literal<Constant>>(
				problem.getInitialState().getFacts());
		Collections.sort(initialState, Comparators.LITERAL_COMPARATOR);

		for (Literal<Constant> row : initialState) {
			sb.append("\t(" + row.getAtom().getSymbol().getName());
			for (Constant p : row.getAtom().getParameters())
				sb.append(" " + p.getName());

			sb.append(")\n");
		}

		sb.append(")");

		return sb.toString();
	}

	public static String formatObjectList(Set<Constant> objects) {
		StringBuilder sb = new StringBuilder();

		Map<Type, Set<Constant>> objectsByType = new HashMap<Type, Set<Constant>>();
		for (Constant obj : objects) {
			Set<Constant> objs = objectsByType.get(obj.getType());
			if (objs == null) {
				objs = new HashSet<Constant>();
				objectsByType.put(obj.getType(), objs);
			}
			objs.add(obj);
		}

		for (Type t : objectsByType.keySet()) {
			Set<Constant> objs = objectsByType.get(t);
			if (objs.size() > 0) {
				sb.append("\t");
				for (Constant obj : objs) {
					sb.append(obj.getName() + " ");
				}
				sb.append("- " + t + "\n");
			}
		}

		return sb.toString();
	}

	public static String getGoalState(PddlProblem problem) {
		StringBuilder sb = new StringBuilder();

		sb.append("(:goal\n (and\n");

		for (Literal<Constant> condition : problem.getGoalState()) {
			sb.append(" " + formatCondition(condition) + "\n");
		}

		sb.append("))");

		return sb.toString();

	}

	public static String getPlanString(List<Operator<Constant>> steps) {
		StringBuilder sb = new StringBuilder();
		Iterator<Operator<Constant>> it = steps.iterator();
		while (it.hasNext()) {
			sb.append(formatPlanStep(it.next()));
			if (it.hasNext())
				sb.append("\n");
		}
		return sb.toString();
	}
	
	public static String getPlanString(Plan plan) {
		StringBuilder sb = new StringBuilder();
		Iterator<Operator<Variable>> it = plan.getPlanSteps().iterator();
		while (it.hasNext()) {
			sb.append(formatPlanStep(it.next().applySubstitution(plan.getSubstitution())));
			if (it.hasNext())
				sb.append("\n");
		}
		return sb.toString();
	}

	private static String formatPlanStep(Operator<Constant> action) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(action.getName());
		for (Constant param : action.getParameters())
			sb.append(" " + param.getName());

		sb.append(")");
		return sb.toString();
	}

	public static String formatCondition(Literal<? extends Term> condition) {
		StringBuilder sb = new StringBuilder();
		if (!condition.getValue())
			sb.append("(not ");

		sb.append("(" + formatAtom(condition.getAtom()) + ")");

		if (!condition.getValue())
			sb.append(") ");

		return sb.toString();
	}

	private static String formatAtom(Atom<? extends Term> atom) {
		StringBuilder sb = new StringBuilder();
		sb.append(atom.getSymbol().getName() + " " + formatUntypedList(atom.getParameters()));
		return sb.toString();
	}

	public static String formatTypedList(List<? extends Term> terms) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < terms.size(); i++) {
			sb.append(formatTerm(terms.get(i)) + " - " + terms.get(i).getType());
			if (i < terms.size() - 1)
				sb.append(" ");
		}

		return sb.toString();
	}

	public static String formatUntypedList(List<? extends Term> terms) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < terms.size(); i++) {
			sb.append(formatTerm(terms.get(i)));
			if (i < terms.size() - 1)
				sb.append(" ");
		}

		return sb.toString();
	}
	
	private static String formatTerm(Term term) {
		if (term instanceof Constant)
			return ((Constant) term).getName();
		else {
			String val = ((Variable) term).getName().replaceAll("\\$", "pl_");
			if (val.charAt(0) == '?')
				return val;
			return "?" + val;
		}
	}

	
	private PddlFormatter() { }
	
}
