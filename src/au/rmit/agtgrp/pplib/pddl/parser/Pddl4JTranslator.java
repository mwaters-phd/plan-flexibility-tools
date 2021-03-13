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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Atom;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.SymbolMap;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.OperatorFactory;
import au.rmit.agtgrp.pplib.pddl.ParallelPlan;
import au.rmit.agtgrp.pplib.pddl.PddlDomain;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.PlanFactory;
import au.rmit.agtgrp.pplib.pddl.State;
import fr.uga.pddl4j.parser.Connective;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Exp;
import fr.uga.pddl4j.parser.NamedTypedList;
import fr.uga.pddl4j.parser.Op;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.parser.RequireKey;
import fr.uga.pddl4j.parser.Symbol;
import fr.uga.pddl4j.parser.TypedSymbol;

public class Pddl4JTranslator {



	private static EnumSet<Connective> ALLOWED_CONNECTIVES = EnumSet.of(Connective.AND, Connective.NOT, Connective.INCREASE);
	private static EnumSet<RequireKey> SUPPORTED_PDDL = EnumSet.of(RequireKey.STRIPS, RequireKey.TYPING, 
			RequireKey.EQUALITY, RequireKey.NEGATIVE_PRECONDITIONS);

	private boolean verbose;
	private boolean pctFormat;
	private Map<String, Map<Variable, Constant>> constPreconBindings;

	private Domain pddl4jDomain;
	private PddlDomain domain;
	private PddlProblem problem;

	
	public Pddl4JTranslator() {
		this(false, false);
	}

	public Pddl4JTranslator(boolean pctFormat, boolean verbose) {
		this.pctFormat = pctFormat;
		this.verbose = verbose;
	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public PddlDomain convertDomain(Domain pddl4j) {

		// reset
		pddl4jDomain = pddl4j;
		domain = null;
		problem = null;
		constPreconBindings = new HashMap<String, Map<Variable, Constant>>();

		// check requirements
		for (RequireKey req : pddl4j.getRequirements()) {
			if (!SUPPORTED_PDDL.contains(req) && verbose)
				System.err.println("WARNING: requirement " + req + " is not supported (ignoring)");
		}

		// convert predicates
		Map<String, Predicate> predsByName = new HashMap<String, Predicate>();
		Set<Predicate> preds = new HashSet<Predicate>();

		for (NamedTypedList pred : pddl4j.getPredicates()) {
			String name = pred.getName().getImage();
			List<Type> params = new ArrayList<Type>();
			for (TypedSymbol arg : pred.getArguments()) {
				params.add(new Type(arg.getTypes().get(0).getImage()));
			}

			Predicate predSymbol = new Predicate(name, params);
			predsByName.put(name, predSymbol);
			preds.add(predSymbol);
		}

		// get function names
		List<String> functions = new ArrayList<String>();
		for (NamedTypedList func : pddl4j.getFunctions())
			functions.add(func.getName().getImage());

		// convert types
		Set<Type> types = new HashSet<Type>();

		for (TypedSymbol type : pddl4j.getTypes()) {
			Type t = new Type(type.getImage());
			types.add(t);

			if (type.getTypes().size() > 0) {
				Type superType = new Type(type.getTypes().get(0).getImage());
				t.setSupertype(superType);
			}
		}

		// convert constants
		Map<TypedSymbol, Constant> constants = new HashMap<TypedSymbol, Constant>();
		for (TypedSymbol cnst : pddl4j.getConstants()) {
			constants.put(cnst, new Constant(new Type(cnst.getTypes().get(0).getImage()), cnst.getImage()));
		}

		// convert operators
		Set<Operator<Variable>> operators = new HashSet<Operator<Variable>>();


		for (Op pddl4jOp : pddl4j.getOperators()) {					
			// find all constants appearing in precons and effects, add to parameters as variables
			Set<Constant> opCnsts = getConstantsInExpression(pddl4jOp.getPreconditions(), constants);
			opCnsts.addAll(getConstantsInExpression(pddl4jOp.getEffects(), constants));

			Map<Variable, Constant> constBindings = new HashMap<Variable, Constant>();

			for (Constant c : opCnsts) {
				if (verbose)
					System.out.println("Constant '" + c.getName() + "' appears in operator precondition/effect of operator " + pddl4jOp.getName().getImage());				
				Variable v = new Variable(c.getType(), "?_" + c.getName()).intern();				
				constBindings.put(v, c);
				//				System.out.println("Converted into parameter " + v.getName() + ", precondition " + precon + " and init atom " + precon.rebind(Arrays.asList(c)));			
			}

			constPreconBindings.put(pddl4jOp.getName().getImage(), constBindings);
			List<Variable> vars = new ArrayList<Variable>();
			vars.addAll(constBindings.keySet());
			for (TypedSymbol param : pddl4jOp.getParameters()) {
				vars.add(new Variable(new Type(param.getTypes().get(0).getImage()), param.getImage()).intern());
			}

			OperatorFactory<Variable> of = new OperatorFactory<Variable>(pddl4jOp.getName().getImage());
			of.addVariables(vars);
			of.addParameters(vars);

			for (Variable v : constBindings.keySet()) {
				Constant c = constBindings.get(v);
				Literal<Variable> precon = new Literal<Variable>(getConstPreconPredicate(c), Arrays.asList(v), Arrays.asList(v), true);
				preds.add(precon.getAtom().getSymbol());
				of.addPrecondition(precon);
			}

			for (Literal<Variable> pre : convertExpression(pddl4jOp.getPreconditions(), vars, predsByName, verbose))
				of.addPrecondition(pre);

			for (Literal<Variable> post : convertExpression(pddl4jOp.getEffects(), vars, predsByName, verbose))
				of.addPostcondition(post);

			operators.add(of.getOperator());
		}

		domain = new PddlDomain(pddl4j.getName().getImage(), preds, types, new HashSet<Constant>(constants.values()), operators);
		return domain;
	}

	private Predicate getConstPreconPredicate(Constant c) {
		return new Predicate("_is_" + c.getName(), c.getType());
	}

	public PddlProblem convertProblem(Problem prob) {

		problem = null;
		if (domain == null)
			throw new IllegalStateException("Must translate domain first!");

		SymbolMap<Predicate> sm = new SymbolMap<Predicate>(domain.getPredicates());

		// name
		String name = prob.getName().getImage();

		// objects
		Set<Constant> objs = new HashSet<Constant>();
		for (TypedSymbol obj : prob.getObjects())
			objs.add(new Constant(new Type(obj.getTypes().get(0).getImage()), obj.getImage()));

		// initial state
		Set<Literal<Constant>> initialState = new HashSet<Literal<Constant>>();

		Iterator<Exp> it = prob.getInit().iterator();
		while (it.hasNext()) {
			Exp init = it.next();
			it.remove();
			if (init.isLiteral()) {	
				Atom<Constant> i = convertGroundAtom(init, sm, prob, pddl4jDomain);
				initialState.add(new Literal<Constant>(i, true).intern());
			}
			else if (verbose)
				System.out.println("Ignoring non-literal in initial state: " + init);
		}

		Set<Constant> cnsts = new HashSet<Constant>();
		for (Map<Variable, Constant> vcMap : constPreconBindings.values())
			cnsts.addAll(vcMap.values());

		int i = 0;
		for (Constant c : cnsts) {
			Literal<Constant> pcc = new Literal<Constant>(getConstPreconPredicate(c), Arrays.asList(new Variable(c.getType(), "_var_" + (i++))), Arrays.asList(c), true);
			initialState.add(pcc);
		}


		// goal state
		Set<Literal<Constant>> goalState = new HashSet<Literal<Constant>>();
		goalState.addAll(convertGroundExpression(prob.getGoal(), sm, prob, pddl4jDomain));

		problem = new PddlProblem(domain, name, objs, new State<Constant>(initialState), new State<Constant>(goalState));

		return problem;
	}

	public Plan convertPlan(List<String[]> stepStrings) {
		if (domain == null || problem == null)
			throw new PddlParserException("Must parse domain and problem before plan.");

		List<Operator<Constant>> planSteps = new ArrayList<Operator<Constant>>();

		SymbolMap<Operator<Variable>> opMap = new SymbolMap<Operator<Variable>>(domain.getOperators());
		SymbolMap<Constant> objMap = new SymbolMap<Constant>(problem.getObjects());
		SymbolMap<Constant> constsMap = new SymbolMap<Constant>(domain.getConstants());

		for (String[] stepStr : stepStrings) {
			Operator<Variable> op = opMap.get(stepStr[0]);

			if (op == null) {
				throw new PddlParserException("Unknown operator: " + stepStr[0]);
			}

			// find any parameters converted from constants in precons etc
			List<Constant> params = new ArrayList<Constant>();
			for (Variable var : op.getVariables()) {
				Constant c = constPreconBindings.get(op.getName()).get(var);
				if (c != null)
					params.add(c);
			}

			for (int i = 1; i < stepStr.length; i++) {
				Constant ob = objMap.get(stepStr[i]);   //objects in problem
				if (ob == null) 						//constants in domain
					ob = constsMap.get(stepStr[i]);
				if (ob == null)
					throw new PddlParserException("Unknown object: " + stepStr[i]);

				params.add(ob);
			}

			planSteps.add(op.applySubstitution(Substitution.build(op.getVariables(), params)));
		}

		return PlanFactory.formatAsPlan(problem, planSteps, pctFormat, pctFormat);
	}

	public ParallelPlan convertParallelPlan(List<List<String[]>> stepStrings) {
		if (domain == null || problem == null)
			throw new PddlParserException("Must parse domain and problem before plan.");

		List<List<Operator<Constant>>> planSteps = new ArrayList<List<Operator<Constant>>>();

		SymbolMap<Operator<Variable>> opMap = new SymbolMap<Operator<Variable>>(domain.getOperators());
		SymbolMap<Constant> objMap = new SymbolMap<Constant>(problem.getObjects());
		SymbolMap<Constant> constsMap = new SymbolMap<Constant>(domain.getConstants());

		for (List<String[]> step : stepStrings) {
			List<Operator<Constant>> stepOps = new ArrayList<Operator<Constant>>();
			for (String[] stepStr : step) {
				Operator<Variable> op = opMap.get(stepStr[0]);

				if (op == null) {
					throw new PddlParserException("Unknown operator: " + stepStr[0]);
				}

				// find any parameters converted from constants in precons etc
				List<Constant> params = new ArrayList<Constant>();
				for (Variable var : op.getVariables()) {
					Constant c = constPreconBindings.get(op.getName()).get(var);
					if (c != null)
						params.add(c);
				}

				for (int i = 1; i < stepStr.length; i++) {
					Constant ob = objMap.get(stepStr[i]);   //objects in problem
					if (ob == null) 						//constants in domain
						ob = constsMap.get(stepStr[i]);
					if (ob == null)
						throw new PddlParserException("Unknown object: " + stepStr[i]);

					params.add(ob);
				}

				stepOps.add(op.applySubstitution(Substitution.build(op.getVariables(), params)));
			}
			planSteps.add(stepOps);
		}
		
		return PlanFactory.formatAsParallelPlan(problem, planSteps, pctFormat, pctFormat);
	}



	public static Set<Constant> getConstantsInExpression(Exp exp, Map<TypedSymbol, Constant> tsConstMap) {
		Set<Constant> cnsts = new HashSet<Constant>();

		if (exp.isLiteral()) {
			if (exp.getConnective().equals(Connective.NOT)) {
				if (exp.getChildren().size() > 1)
					throw new UnsupportedOperationException("Cannot handle nested expressions" + exp);
				exp = exp.getChildren().get(0);
			}

			cnsts.addAll(getConstantsInAtom(exp, tsConstMap));
			return cnsts;
		}

		if (!ALLOWED_CONNECTIVES.contains(exp.getConnective()))
			throw new UnsupportedOperationException(exp.getConnective() + " is not supported in expression:\n" + exp);

		for (Exp ch : exp.getChildren()) {

			if (!ch.isLiteral() && !ALLOWED_CONNECTIVES.contains(ch.getConnective()))
				throw new UnsupportedOperationException(ch.getConnective() + " is not supported in expression:\n" + ch);

			if (!ch.isLiteral() && ch.getConnective().equals(Connective.INCREASE))
				continue;

			if (!ch.isLiteral() && !ch.getConnective().equals(Connective.NOT))
				throw new UnsupportedOperationException("Nested expressions are not supported:\n" + exp);

			if (ch.getConnective().equals(Connective.NOT)) {
				if (ch.getChildren().size() > 1)
					throw new UnsupportedOperationException("Nested expressions are not supported:\n" + ch);

				ch = ch.getChildren().get(0);
			}

			cnsts.addAll(getConstantsInAtom(ch, tsConstMap));
		}

		return cnsts;
	}

	public static Set<Constant> getConstantsInAtom(Exp exp, Map<TypedSymbol, Constant> tsConstMap) {
		Set<Constant> cnsts = new HashSet<Constant>();	
		for (Symbol s :  exp.getAtom()) {
			if (tsConstMap.containsKey(s))
				cnsts.add(tsConstMap.get(s));
		}
		return cnsts;
	}

	public static List<Literal<Variable>> convertExpression(Exp exp, List<Variable> opParams, Map<String, Predicate> predsByName, boolean verbose) {

		List<Literal<Variable>> lits = new ArrayList<Literal<Variable>>();

		if (exp.isLiteral()) {
			boolean val = true;

			if (exp.getConnective().equals(Connective.NOT)) {
				val = false;
				if (exp.getChildren().size() > 1)
					throw new UnsupportedOperationException("Cannot handle nested expressions" + exp);

				exp = exp.getChildren().get(0);
			}

			lits.add(new Literal<Variable>(convertAtom(exp, opParams, predsByName), val).intern());
			return lits;
		}

		if (!ALLOWED_CONNECTIVES.contains(exp.getConnective()))
			throw new UnsupportedOperationException(exp.getConnective() + " is not supported in expression:\n" + exp);

		for (Exp ch : exp.getChildren()) {

			if (!ch.isLiteral() && !ALLOWED_CONNECTIVES.contains(ch.getConnective()))
				throw new UnsupportedOperationException(ch.getConnective() + " is not supported in expression:\n" + ch);

			if (!ch.isLiteral() && ch.getConnective().equals(Connective.INCREASE)) {
				if (verbose)
					System.out.println("Ignoring action cost expression: " + ch);
				continue;
			}

			boolean val = true;

			if (!ch.isLiteral() && !ch.getConnective().equals(Connective.NOT))
				throw new UnsupportedOperationException("Nested expressions are not supported:\n" + exp);

			if (ch.getConnective().equals(Connective.NOT)) {
				val = false;
				if (ch.getChildren().size() > 1)
					throw new UnsupportedOperationException("Nested expressions are not supported:\n" + ch);

				ch = ch.getChildren().get(0);
			}

			lits.add(new Literal<Variable>(convertAtom(ch, opParams, predsByName), val).intern());
		}

		return lits;

	}

	public static Atom<Variable> convertAtom(Exp exp, List<Variable> opParams, Map<String, Predicate> predsByName) {

		Predicate pred = null;
		if (exp.getConnective().equals(Connective.EQUAL_ATOM)) {
			pred = Predicate.EQUALS;
		}
		else {
			String predname = exp.getAtom().get(0).getImage();
			pred = predsByName.get(predname);
		}

		if (pred == null)
			throw new PddlParserException("Cannot parse atom: " + exp);

		int first = exp.getConnective().equals(Connective.EQUAL_ATOM) ? 0 : 1;
		List<Variable> params = new ArrayList<Variable>();
		for (int i = first; i < exp.getAtom().size(); i++) {
			String paramName = exp.getAtom().get(i).getImage();
			boolean found = false;
			for (Variable opParam : opParams) {
				// try adding '?' -- might be a constant
				if (opParam.getName().equalsIgnoreCase(paramName) || opParam.getName().equalsIgnoreCase("?_" + paramName)) {
					params.add(opParam);
					found = true;
				}
			}
			if (!found) {
				throw new RuntimeException("Precondition/effect parameter not found in operator parameter:\n" + paramName + " in\n" + exp);
			}
		}

		return new Atom<Variable>(pred, params, params).intern();
	}


	public static List<Literal<Constant>> convertGroundExpression(Exp exp,
			Map<String, Predicate> predsByName, Problem prob, Domain pddl4jDomain) {
		List<Literal<Constant>> lits = new ArrayList<Literal<Constant>>();

		if (exp.isLiteral()) {
			boolean val = !exp.getConnective().equals(Connective.NOT);
			lits.add(new Literal<Constant>(convertGroundAtom(exp, predsByName, prob, pddl4jDomain), val).intern());
			return lits;
		}

		if (!exp.getConnective().equals(Connective.AND))
			throw new UnsupportedOperationException(
					exp.getConnective() + " is not supported in goal/init state:\n" + exp);

		for (Exp ch : exp.getChildren()) {
			if (!ch.isLiteral())
				throw new UnsupportedOperationException("Nested expressions are not supported:\n" + exp);
			boolean val = true;
			if (ch.getConnective().equals(Connective.NOT)) {
				val = false;
				if (ch.getChildren().size() > 1)
					throw new UnsupportedOperationException("Nested expressions are not supported:\n" + ch);

				ch = ch.getChildren().get(0);
			}

			lits.add(new Literal<Constant>(convertGroundAtom(ch, predsByName, prob, pddl4jDomain), val).intern());

		}

		return lits;

	}

	public static Atom<Constant> convertGroundAtom(Exp exp, Map<String, Predicate> predsByName, Problem prob, Domain domain) {
		String predname = exp.getAtom().get(0).getImage();
		Predicate pred = predsByName.get(predname);
		List<Constant> params = new ArrayList<Constant>();
		for (int i = 1; i < exp.getAtom().size(); i++) {
			TypedSymbol st = prob.getObject(exp.getAtom().get(i)); // look up declared object in problem
			if (st == null)
				st = domain.getConstant(exp.getAtom().get(i)); // not in prob, find in domain

			Type type = new Type(st.getTypes().get(0).getImage());
			String paramName = st.getImage();

			params.add(new Constant(type, paramName));
		}

		return new Atom<Constant>(pred, Variable.buildVariables(Term.getTypes(params)), params);

	}

}
