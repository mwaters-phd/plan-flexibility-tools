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
package au.rmit.agtgrp.pplib.pp.partialplan.clplan.optimiser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import au.rmit.agtgrp.pplib.csp.ExpressionCsp;
import au.rmit.agtgrp.pplib.csp.PartitionedExpressionCsp;
import au.rmit.agtgrp.pplib.csp.alldiff.AllDifferent;
import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.expression.Connective;
import au.rmit.agtgrp.pplib.fol.expression.Expression;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Atom;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.Pair;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;
import au.rmit.agtgrp.pplib.utils.collections.graph.GraphUtils;
import au.rmit.agtgrp.pplib.utils.collections.graph.treewidth.TreewidthCalculator;

public class CspOptimiser {

	private static final int PARTITION_STEP = 500;

	public static List<Variable> VARS;
	public static Map<Type, Set<Variable>> VARS_BY_TYPE;

	public static Map<Variable, Collection<Constant>> DOMAINS;
	public static int HIGHEST_OP_NO;

	public static Map<Variable, List<Constant>> OP_DOMAINS;

	public static DirectedGraph<Variable> EQUALITY_GRAPH;
	public static DirectedGraph<Variable> ACTIVE_EQUALITY_GRAPH;

	public static DirectedGraph<Variable> PREC_GRAPH;
	public static DirectedGraph<Variable> ACTIVE_PREC_GRAPH;

	public static Set<Literal<Variable>> NEG_LITERALS;
	public static Set<Literal<Variable>> ACTIVE_NEG_LITERALS;

	public static Map<Set<Variable>, Set<ExpressionPropagator>> EXP_CONS;

	public static Set<Literal<Variable>> ALLDIFF_CONS;

	public static Set<Propagator> WORKLIST;
	public static PriorityQueue<Propagator> WORKQUEUE;

	public static Map<Expression<Variable>, Expression<Variable>> PROP_CACHE;

	public static PartitionedExpressionCsp CSP;

	/**
	 * Optimises the input CSP using a specialised AC-3 algorithm.
	 * 
	 * @param csp 	The CSP to optimise.
	 * @return		The optimised CSP.
	 */
	public static ExpressionCsp optimise(ExpressionCsp csp) {
		return optimise(csp, -1, null);
	}


	/**
	 * Optimises the input CSP using a specialised AC-3 algorithm.
	 * 
	 * @param csp 			The CSP to optimise.
	 * @param treewidth  	The target treewidth. If > 0, optimisation will stop when the CSP's treewidth is less than this value.
	 * @param calc 			The treewidth calculator used to compute the treewidth of the CSP.
	 * @return				The optimised CSP.
	 */
	public static ExpressionCsp optimise(ExpressionCsp csp, int treewidth, TreewidthCalculator calc) {

		initialise(csp);

		boolean varsPartitioned = true;
		boolean workDone = false;

		while (!workDone || varsPartitioned) {

			// stop if interrupted
			if (Thread.interrupted()) {
				Thread.currentThread().interrupt();
				return CSP;
			}

			int eqSize = EQUALITY_GRAPH.getSize();

			propagate(PARTITION_STEP);

			workDone = WORKLIST.isEmpty();

			if (EQUALITY_GRAPH.getSize() > (eqSize + 100) || WORKLIST.isEmpty()) {
				int nVars = VARS.size();

				buildCSP(CSP, false);			
				partitionState();

				varsPartitioned = VARS.size() != nVars;

				if (calc != null && calc.getUpperBound(CSP.getPrimalGraph()) <= treewidth) {
					break;
				}
			}

		}

		buildCSP(CSP, true);
		return CSP;
	}

	private static void initialise(ExpressionCsp csp) {

		// init csp
		CSP = PartitionedExpressionCsp.partition(csp);

		initVarsAndDomains();

		EQUALITY_GRAPH = new DirectedGraph<Variable>();
		ACTIVE_EQUALITY_GRAPH = new DirectedGraph<Variable>();

		PREC_GRAPH = new DirectedGraph<Variable>();
		ACTIVE_PREC_GRAPH = new DirectedGraph<Variable>();

		NEG_LITERALS = new HashSet<Literal<Variable>>();
		ACTIVE_NEG_LITERALS = new HashSet<Literal<Variable>>();

		EXP_CONS = new HashMap<Set<Variable>, Set<ExpressionPropagator>>();
		ALLDIFF_CONS = new HashSet<Literal<Variable>>();

		WORKQUEUE = new PriorityQueue<Propagator>(PropagatorComparator.INSTANCE);
		WORKLIST = new HashSet<Propagator>();

		Iterator<List<Variable>> it = new ArrayList<List<Variable>>(CSP.getConstraints().keySet()).iterator();
		while (it.hasNext()) {
			List<Variable> key = it.next();
			//it.remove();

			Set<Expression<Variable>> cons = new HashSet<Expression<Variable>>(CSP.getConstraints().remove(key));
			Set<ExpressionPropagator> exp = new HashSet<ExpressionPropagator>();

			for (Expression<Variable> con : cons) {
				if (con.isLiteral()) { // true
					Literal<Variable> lit = con.getLiteral();
					newFact(lit);

				} else {
					ExpressionPropagator ep = new ExpressionPropagator(con);
					exp.add(ep);
				}
			}

			EXP_CONS.put(new HashSet<Variable>(key), exp);
			WORKLIST.addAll(exp);
			WORKQUEUE.addAll(exp);

		}

		domainsChanged(new HashSet<Variable>(VARS));

		PROP_CACHE = new HashMap<Expression<Variable>, Expression<Variable>>();
	}

	private static void partitionState() {

		Substitution<Variable> prevPartition = CSP.getMapping();		
		CSP = PartitionedExpressionCsp.partition(CSP, EQUALITY_GRAPH);

		// build step partition
		Map<Variable, Variable> stepPartMap = new HashMap<Variable, Variable>();
		for (Variable var : CSP.getMapping().getVariables())
			stepPartMap.put(prevPartition.apply(var), CSP.getMapping().apply(var));

		Substitution<Variable> stepPartition = new Substitution<Variable>(stepPartMap);

		// check for changed domains
		List<Variable> changedDomains = new ArrayList<Variable>();
		for (Variable var : CSP.getMapping().getVariables()) {
			Variable partvar = CSP.getMapping().apply(var);
			Collection<Constant> domain = var.getType().equals(Type.OPERATOR_TYPE) ? 
					OP_DOMAINS.get(prevPartition.apply(var)) : DOMAINS.get(prevPartition.apply(var)); 

					if (domain.size() !=  CSP.getDomain(partvar).size())
						changedDomains.add(partvar);
		}

		// set domains
		initVarsAndDomains();

		EQUALITY_GRAPH = partitionGraph(EQUALITY_GRAPH, stepPartition);
		ACTIVE_EQUALITY_GRAPH = partitionGraph(ACTIVE_EQUALITY_GRAPH, stepPartition);

		PREC_GRAPH = partitionGraph(PREC_GRAPH, stepPartition);
		ACTIVE_PREC_GRAPH = partitionGraph(ACTIVE_PREC_GRAPH, stepPartition);

		NEG_LITERALS = partitionAll(NEG_LITERALS, stepPartition);
		ACTIVE_NEG_LITERALS = partitionAll(ACTIVE_NEG_LITERALS, stepPartition);

		Set<Propagator> partWorkList = new HashSet<Propagator>();
		Substitution<Variable> id = Substitution.identity(stepPartition.getDomain());
		for (Propagator prop : WORKLIST) {
			if (prop instanceof BinaryPropagator) {
				Literal<Variable> cons = ((BinaryPropagator) prop).getConstraint();
				cons = cons.resetVariables(stepPartition.apply(cons.getAtom().getVariables()));
				cons = cons.rebind(cons.getAtom().getVariables());
				partWorkList.add(new BinaryPropagator(cons));
			} else if (prop instanceof ExpressionPropagator) {
				Expression<Variable> cons = ((ExpressionPropagator) prop).getConstraint();
				cons = cons.resetVariables(stepPartition);
				cons = cons.applySubstitution(id);
				partWorkList.add(new ExpressionPropagator(cons));
			}
		}
		WORKLIST = partWorkList;

		WORKLIST.addAll(toBinaryProps(ACTIVE_EQUALITY_GRAPH, Predicate.EQUALS));
		WORKLIST.addAll(toBinaryProps(ACTIVE_PREC_GRAPH, Predicate.PREC));
		for (Literal<Variable> neg : ACTIVE_NEG_LITERALS)
			WORKLIST.add(new BinaryPropagator(neg));

		Map<Set<Variable>, Set<ExpressionPropagator>> partitionedExp = new HashMap<Set<Variable>, Set<ExpressionPropagator>>();
		for (Set<Variable> dom : EXP_CONS.keySet()) {
			for (ExpressionPropagator ep : EXP_CONS.get(dom)) {
				Expression<Variable> cons = ((ExpressionPropagator) ep).getConstraint();
				cons = cons.resetVariables(stepPartition);
				cons = cons.applySubstitution(id);
				ExpressionPropagator pep = new ExpressionPropagator(cons);
				Set<ExpressionPropagator> eps = partitionedExp.get(pep.getDomain());
				if (eps == null) {
					eps = new HashSet<ExpressionPropagator>();
					partitionedExp.put(pep.getDomain(), eps);
				}
				eps.add(pep);

				if (ep.getDomain().size() != pep.getDomain().size()) {
					WORKLIST.add(pep);
				}
			}
		}

		EXP_CONS = partitionedExp;

		Set<Literal<Variable>> partitionedAllDiffs = new HashSet<Literal<Variable>>();
		for (Literal<Variable> allDiff : ALLDIFF_CONS) {
			allDiff = allDiff.resetVariables(stepPartition.apply(allDiff.getAtom().getVariables()));
			allDiff = allDiff.rebind(allDiff.getAtom().getVariables());
			partitionedAllDiffs.add(allDiff);
		}
		ALLDIFF_CONS = partitionedAllDiffs;

		WORKQUEUE.clear();
		WORKQUEUE.addAll(WORKLIST);

		domainsChanged(changedDomains);

		// partition cache
		Map<Expression<Variable>, Expression<Variable>> ppc = new HashMap<Expression<Variable>, Expression<Variable>>();
		for (Expression<Variable> exp : PROP_CACHE.keySet()) {
			Expression<Variable> val = PROP_CACHE.get(exp);
			val = val.resetVariables(stepPartition);
			val = val.applySubstitution(id);
			exp = exp.resetVariables(stepPartition);
			exp = exp.applySubstitution(id);
			ppc.put(exp, val);
		}
		PROP_CACHE = ppc;
	}

	private static void initVarsAndDomains() {
		VARS = new ArrayList<Variable>(CSP.getVariables());
		VARS_BY_TYPE = new HashMap<Type, Set<Variable>>();
		for (Variable var : VARS) {
			Type t = var.getType();
			do {
				Set<Variable> typevars = VARS_BY_TYPE.get(t);
				if (typevars == null) {
					typevars = new HashSet<Variable>();
					VARS_BY_TYPE.put(t, typevars);
				}
				typevars.add(var);
				t = t.getImmediateSupertype();
			} while (!t.equals(Type.ANYTHING_TYPE));
		}

		DOMAINS = new HashMap<Variable, Collection<Constant>>();
		OP_DOMAINS = new HashMap<Variable, List<Constant>>();
		List<Constant> opDomain = new ArrayList<Constant>();

		for (Variable var : VARS) {
			if (var.getType().equals(Type.OPERATOR_TYPE)) {
				List<Constant> opd = new ArrayList<Constant>(CSP.getDomain(var));
				Collections.sort(opd, ConstComp.INSTANCE);
				OP_DOMAINS.put(var, opd);
				opDomain.add(opd.get(opd.size() - 1));
			} else
				DOMAINS.put(var, new HashSet<Constant>(CSP.getDomain(var)));
		}

		Collections.sort(opDomain, ConstComp.INSTANCE);
		HIGHEST_OP_NO = Integer.valueOf(opDomain.get(opDomain.size() - 1).getName());
	}

	private static Set<BinaryPropagator> toBinaryProps(DirectedGraph<Variable> graph, Predicate pred) {
		Set<BinaryPropagator> props = new HashSet<BinaryPropagator>();

		for (Variable var : graph.getVertices()) {
			for (Variable from : graph.getEdgesFrom(var))
				props.add(new BinaryPropagator(new Literal<Variable>(pred, 
						Arrays.asList(var, from), Arrays.asList(var, from), true).intern()));
		}

		return props;
	}

	private static Set<Literal<Variable>> partitionAll(Collection<Literal<Variable>> set, Substitution<Variable> part) {

		Set<Literal<Variable>> partitioned = new HashSet<Literal<Variable>>();
		Substitution<Variable> id = Substitution.identity(part.getDomain());
		for (Literal<Variable> lit : set) {
			Variable v1Part = part.apply(lit.getAtom().getVariables().get(0));
			Variable v2Part = part.apply(lit.getAtom().getVariables().get(1));
			if (!v1Part.equals(v2Part)) {
				lit = lit.resetVariables(part.apply(lit.getAtom().getVariables()));
				lit = lit.applySubstitution(id);
				partitioned.add(lit);
			}
		}
		return partitioned;
	}

	private static DirectedGraph<Variable> partitionGraph(DirectedGraph<Variable> graph, Substitution<Variable> part) {

		DirectedGraph<Variable> partitioned = new DirectedGraph<Variable>();

		for (Variable var : graph.getVertices()) {
			Variable partVar = part.apply(var);
			for (Variable linkTo : graph.getEdgesTo(var)) {
				linkTo = part.apply(linkTo);
				partitioned.addEdge(linkTo, partVar);
			}

			for (Variable linkFrom : graph.getEdgesFrom(var)) {
				linkFrom = part.apply(linkFrom);
				partitioned.addEdge(partVar, linkFrom);
			}
		}

		return partitioned;

	}

	private static void propagate(int maxProp) {
		int j = 0;

		while (!WORKQUEUE.isEmpty() && j < maxProp) {
			// stop if interrupted
			if (Thread.interrupted()) {
				Thread.currentThread().interrupt();
				break;
			}

			Propagator p = WORKQUEUE.poll();
			WORKLIST.remove(p);

			if (p instanceof BinaryPropagator)
				propagateBinary((BinaryPropagator) p);
			if (p instanceof ExpressionPropagator)
				propagateExpression((ExpressionPropagator) p);

			j++;
		}
	}

	private static void propagateExpression(ExpressionPropagator parent) {

		EXP_CONS.get(parent.getDomain()).remove(parent);

		for (Expression<Variable> conj : splitConjunctions(parent.getConstraint())) {

			if (conj.isLiteral()) {
				newFact(conj.getLiteral());
				continue;
			}

			ExpressionPropagator ep = new ExpressionPropagator(conj);
			ep.propagate();
			domainsChanged(ep.getChanged());

			if (ep.getConstraint().equals(Expression.TRUE)) {
				continue;
			} else if (ep.getConstraint().equals(Expression.FALSE)) {
				throw new CspOptimiserException("CSP is unsatisfiable: " + ep.getOriginal() + " evaluates to FALSE");
			} else if (ep.getConstraint().isLiteral()) {
				newFact(ep.getConstraint().getLiteral());
			} else { // replace modified exp based on new domain
				Set<ExpressionPropagator> eps = EXP_CONS.get(ep.getDomain());
				if (eps == null) {
					eps = new HashSet<ExpressionPropagator>();
					EXP_CONS.put(ep.getDomain(), eps);
				}
				eps.add(ep);
			}
		}
	}

	private static void propagateBinary(BinaryPropagator bp) {
		// AC-3 over binary
		bp.propagate();

		// notify of changed domains
		domainsChanged(bp.getChanged());

		// if tautology, no need to propagate this constraint again
		setActive(bp, !bp.getConstraint().equals(Literal.TRUE));

		if (bp.getConstraint().equals(Literal.FALSE))
			throw new CspOptimiserException("CSP is unsatisfiable: " + bp.getOriginal() + " evaluates to FALSE");
	}

	private static void buildCSP(ExpressionCsp opt, boolean reduce) {

		opt.getVariables().clear();
		opt.getDomain().clear();
		opt.getDomains().clear();
		opt.getConstraints().clear();

		opt.addVariables(VARS);

		for (Variable var : DOMAINS.keySet())
			opt.addDomainValues(var, DOMAINS.get(var));

		for (Variable var : OP_DOMAINS.keySet())
			opt.addDomainValues(var, OP_DOMAINS.get(var));

		// only add the reduced equality relation
		DirectedGraph<Variable> reducedEquality = EQUALITY_GRAPH;
		if (reduce) {
			reducedEquality = new DirectedGraph<Variable>();
			for (Variable vert : EQUALITY_GRAPH.getVertices()) {
				for (Variable dest : EQUALITY_GRAPH.getEdgesFrom(vert))
					reducedEquality.addEdge(vert, dest);
			}
			GraphUtils.transitiveReduction(reducedEquality, EQUALITY_GRAPH);
		}

		// simplify
		for (Variable vert : reducedEquality.getVertices()) {
			for (Variable dest : reducedEquality.getEdgesFrom(vert)) {
				Literal<Variable> lit = Literal.equals(vert, dest, vert, dest, true);
				lit = simplifyEquals(lit);
				if (!lit.equals(Literal.TRUE))
					opt.addConstraint(Expression.buildLiteral(lit));
			}
		}

		// only add the reduced prec relation
		DirectedGraph<Variable> reducedPrec = PREC_GRAPH;
		if (reduce) {
			reducedPrec = new DirectedGraph<Variable>();
			for (Variable vert : PREC_GRAPH.getVertices()) {
				for (Variable other : PREC_GRAPH.getEdgesFrom(vert))
					reducedPrec.addEdge(vert, other);
			}
			GraphUtils.transitiveReduction(reducedPrec, PREC_GRAPH);
		}

		for (Variable vert : reducedPrec.getVertices()) {
			for (Variable dest : reducedPrec.getEdgesFrom(vert)) {
				Literal<Variable> lit = Literal.prec(vert, dest, vert, dest, true);
				lit = simplifyPrec(lit);
				if (!lit.equals(Literal.TRUE))
					opt.addConstraint(Expression.buildLiteral(lit));
			}
		}

		// add all negated facts
		for (Literal<Variable> neg : NEG_LITERALS) {
			neg = neg.getAtom().getSymbol().equals(Predicate.EQUALS) ? simplifyEquals(neg) : simplifyPrec(neg);
			if (!neg.equals(Literal.TRUE))
				opt.addConstraint(Expression.buildLiteral(neg));
		}

		// add all expression constraints
		for (Set<ExpressionPropagator> eps : EXP_CONS.values()) {
			for (ExpressionPropagator ep : eps) {
				opt.addConstraint(ep.getConstraint());
			}
		}

		// add all alldiffs
		for (Literal<Variable> allDiff : ALLDIFF_CONS)
			opt.addConstraint(Expression.buildLiteral(allDiff));
	}

	private static void newFact(Literal<Variable> lit) {

		if (AllDifferent.isAllDifferentLiteral(lit))
			ALLDIFF_CONS.add(lit);

		// add new literal to fact collections
		if (lit.getValue()) {

			if (lit.getAtom().getSymbol().equals(Predicate.EQUALS)) {

				// is it already there?
				if (EQUALITY_GRAPH.containsEdge(lit.getAtom().getParameters().get(0), lit.getAtom().getParameters().get(1)) || 
						simplifyEquals(lit).equals(Literal.TRUE))
					return;

				domainsChanged(lit.getAtom().getParameters());
				BinaryPropagator bp = new BinaryPropagator(lit);
				WORKLIST.add(bp);
				WORKQUEUE.add(bp);

				// notify of any new equality
				for (Pair<Variable, Variable> newEq : GraphUtils.addAndCloseTransitive(EQUALITY_GRAPH, lit.getAtom().getParameters().get(0), lit.getAtom().getParameters().get(1))) {

					Literal<Variable> eqLit = new Literal<Variable>(Atom.equals(newEq.getFirst(), newEq.getSecond(), newEq.getFirst(), newEq.getSecond()).normalise(), true).intern();
					eqLit = simplifyEquals(eqLit);
					if (eqLit.equals(Literal.FALSE)) {
						throw new CspOptimiserException("Unsatisfiable constraint!\n"
								+ new Literal<Variable>(Atom.equals(newEq.getFirst(), newEq.getSecond(), newEq.getFirst(), newEq.getSecond()).normalise(), true).intern()
								+ "\n" + newEq.getFirst() + ": " + DOMAINS.get(newEq.getFirst()) + "\n"
								+ newEq.getSecond() + ": " + DOMAINS.get(newEq.getSecond()));
					}
					if (!eqLit.equals(Literal.TRUE)) {
						domainsChanged(eqLit.getAtom().getParameters());
						WORKLIST.add(new BinaryPropagator(eqLit));
						WORKQUEUE.add(new BinaryPropagator(eqLit));
					}
				}

			} else if (lit.getAtom().getSymbol().equals(Predicate.PREC)) {

				// is it already there?
				if (PREC_GRAPH.containsEdge(lit.getAtom().getParameters().get(0), lit.getAtom().getParameters().get(1)) || simplifyPrec(lit).equals(Literal.TRUE))
					return;

				domainsChanged(lit.getAtom().getParameters());
				WORKLIST.add(new BinaryPropagator(lit));
				WORKQUEUE.add(new BinaryPropagator(lit));

				// notify of any new prec
				for (Pair<Variable, Variable> newPrec : GraphUtils.addAndCloseTransitive(PREC_GRAPH, lit.getAtom().getParameters().get(0), lit.getAtom().getParameters().get(1))) {

					Literal<Variable> precLit = new Literal<Variable>(Atom.prec(newPrec.getFirst(), newPrec.getSecond(), newPrec.getFirst(), newPrec.getSecond()).normalise(), true).intern();
					domainsChanged(precLit.getAtom().getParameters());
					WORKLIST.add(new BinaryPropagator(precLit));
					WORKQUEUE.add(new BinaryPropagator(precLit));

				}
			}
		} else {
			// is it already there?
			if (NEG_LITERALS.contains(lit))
				return;

			domainsChanged(lit.getAtom().getParameters());
			WORKLIST.add(new BinaryPropagator(lit));
			WORKQUEUE.add(new BinaryPropagator(lit));

			NEG_LITERALS.add(lit);
		}
	}

	private static void domainsChanged(Collection<Variable> changed) {

		// update expression
		for (Set<Variable> domain : EXP_CONS.keySet()) {
			Set<Variable> dCopy = new HashSet<Variable>(domain);
			if (dCopy.removeAll(changed)) {
				for (ExpressionPropagator ep : EXP_CONS.get(domain)) {
					ep.addExtDomainsChanged(changed);
					if (!WORKLIST.contains(ep)) {
						WORKLIST.add(ep);
						WORKQUEUE.add(ep);
					}
				}
			}
		}

		// check for new (in)equalities -- add to facts, do not propagate,
		// remove from active sets
		for (Variable var : changed) {
			if (!var.getType().equals(Type.OPERATOR_TYPE)) {

				for (Variable other : VARS_BY_TYPE.get(var.getType())) {

					Literal<Variable> res = simplifyEquals(Literal.equals(var, other, var, other, true));
					if (res.equals(Literal.TRUE)) {
						Atom<Variable> newEq = Atom.equals(var, other, var, other);
						GraphUtils.addAndCloseTransitive(EQUALITY_GRAPH, newEq.getParameters().get(0), newEq.getParameters().get(1));
						ACTIVE_EQUALITY_GRAPH.removeEdge(newEq.getParameters().get(0), newEq.getParameters().get(1));
					} else if (res.equals(Literal.FALSE)) {
						Literal<Variable> newNeg = Literal.equals(var, other, var, other, false);
						NEG_LITERALS.add(newNeg);
						ACTIVE_NEG_LITERALS.remove(newNeg);
					}
				}
			}
		}

		// add new binary props from active lists
		for (Variable var : changed) {

			for (Variable other : ACTIVE_EQUALITY_GRAPH.getEdgesFrom(var)) {
				WORKLIST.add(new BinaryPropagator(Literal.equals(var, other, var, other, true)));
				WORKQUEUE.add(new BinaryPropagator(Literal.equals(var, other, var, other, true)));
			}

			for (Variable other : ACTIVE_EQUALITY_GRAPH.getEdgesTo(var)) {
				WORKLIST.add(new BinaryPropagator(Literal.equals(var, other, var, other, true)));
				WORKQUEUE.add(new BinaryPropagator(Literal.equals(var, other, var, other, true)));

			}

			for (Variable other : ACTIVE_PREC_GRAPH.getEdgesFrom(var)) {
				WORKLIST.add(new BinaryPropagator(Literal.prec(var, other, var, other, true)));
				WORKQUEUE.add(new BinaryPropagator(Literal.prec(var, other, var, other, true)));
			}

			for (Variable other : ACTIVE_PREC_GRAPH.getEdgesTo(var)) {
				WORKLIST.add(new BinaryPropagator(Literal.prec(other, var, other, var, true)));
				WORKQUEUE.add(new BinaryPropagator(Literal.prec(other, var, other, var, true)));
			}

			for (Literal<Variable> lit : ACTIVE_NEG_LITERALS) {
				Set<Variable> dCopy = new HashSet<Variable>(lit.getAtom().getParameters());
				if (dCopy.removeAll(changed)) {
					WORKLIST.add(new BinaryPropagator(lit));
					WORKQUEUE.add(new BinaryPropagator(lit));

				}
			}
		}

	}

	public static Expression<Variable> getCached(Expression<Variable> exp) {

		Expression<Variable> cached = PROP_CACHE.get(exp);
		if (cached == null)
			return exp;

		return cached;
	}

	public static boolean addToCache(Expression<Variable> orig, Expression<Variable> propped) {
		if (!orig.equals(propped)) {
			PROP_CACHE.put(orig, propped);
			return true;
		}

		return false;
	}

	public static Literal<Variable> simplifyEquals(Literal<Variable> lit) {
		if (lit.getAtom().getParameters().get(0).equals(lit.getAtom().getParameters().get(1))) // are equal
			return lit.getValue() ? Literal.TRUE : Literal.FALSE;

		Set<Constant> d1 = new HashSet<Constant>(DOMAINS.get(lit.getAtom().getParameters().get(0)));
		Set<Constant> d2 = new HashSet<Constant>(DOMAINS.get(lit.getAtom().getParameters().get(1)));

		if (d1.equals(d2) && d1.size() == 1) // domains are the same, must be equal
			return lit.getValue() ? Literal.TRUE : Literal.FALSE;

		d1.retainAll(d2); // get intersection
		if (d1.isEmpty()) // domains are disjoint, cannot be equal
			return lit.getValue() ? Literal.FALSE : Literal.TRUE;

		// compare inequality with with prec relation
		if (!lit.getValue()) {
			List<Variable> params = lit.getAtom().getParameters();
			if (PREC_GRAPH.containsEdge(params.get(0), params.get(1)) || PREC_GRAPH.containsEdge(params.get(1), params.get(0)))
				return Literal.TRUE;
		}

		return lit;
	}

	public static Literal<Variable> simplifyPrec(Literal<Variable> lit) {
		// x1 = x2 -> !(x1 < x2)
		if (lit.getAtom().getParameters().get(0).equals(lit.getAtom().getParameters().get(1))) // are equal
			return lit.getValue() ? Literal.FALSE : Literal.TRUE;

		// x1 < x2
		List<Constant> d1 = CspOptimiser.OP_DOMAINS.get(lit.getAtom().getParameters().get(0));
		List<Constant> d2 = CspOptimiser.OP_DOMAINS.get(lit.getAtom().getParameters().get(1));

		int d1highest = Integer.valueOf(d1.get(d1.size() - 1).getName());
		int d2lowest = Integer.valueOf(d2.get(0).getName());

		// d1 < d2
		if (d1highest < d2lowest)
			return lit.getValue() ? Literal.TRUE : Literal.FALSE;

		int d1lowest = Integer.valueOf(d1.get(0).getName());
		int d2highest = Integer.valueOf(d2.get(d2.size() - 1).getName());

		// d2 < d1
		if (d2highest < d1lowest)
			return lit.getValue() ? Literal.FALSE : Literal.TRUE;

		return lit;
	}

	private static void setActive(BinaryPropagator bp, boolean active) {

		Literal<Variable> lit = bp.getOriginal();

		if (active) {
			if (lit.getValue()) {
				if (lit.getAtom().getSymbol().equals(Predicate.EQUALS)) {
					ACTIVE_EQUALITY_GRAPH.addEdge(lit.getAtom().getParameters().get(0),
							lit.getAtom().getParameters().get(1));

				} else if (lit.getAtom().getSymbol().equals(Predicate.PREC)) {
					ACTIVE_PREC_GRAPH.addEdge(lit.getAtom().getParameters().get(0),
							lit.getAtom().getParameters().get(1));

				}
			} else
				ACTIVE_NEG_LITERALS.add(lit);
		} else { // inactive

			WORKLIST.remove(bp);
			WORKQUEUE.remove(bp);

			if (lit.getValue()) {

				if (lit.getAtom().getSymbol().equals(Predicate.EQUALS)) {
					ACTIVE_EQUALITY_GRAPH.removeEdge(lit.getAtom().getParameters().get(0),
							lit.getAtom().getParameters().get(1));

				} else if (lit.getAtom().getSymbol().equals(Predicate.PREC)) {
					ACTIVE_PREC_GRAPH.removeEdge(lit.getAtom().getParameters().get(0),
							lit.getAtom().getParameters().get(1));

				}
			} else
				ACTIVE_NEG_LITERALS.remove(lit);
		}
	}

	private static List<Expression<Variable>> splitConjunctions(Expression<Variable> exp) {

		if (exp.isLiteral())
			return Arrays.asList(exp);

		if (exp.getConnective().equals(Connective.AND))
			return exp.getSubexpressions();

		if (exp.getConnective().equals(Connective.IMPL)) {
			Expression<Variable> prec = exp.getSubexpressions().get(0);
			Expression<Variable> ante = exp.getSubexpressions().get(1);

			// p | q -> r
			if (!prec.isLiteral() && prec.getConnective().equals(Connective.OR)) {
				List<Expression<Variable>> split = new ArrayList<Expression<Variable>>();
				for (Expression<Variable> disj : prec.getSubexpressions())
					split.add(Expression.buildImplication(disj, ante));

				return split;

			}

			// p -> q & r
			if (!ante.isLiteral() && ante.getConnective().equals(Connective.AND)) {
				List<Expression<Variable>> split = new ArrayList<Expression<Variable>>();
				for (Expression<Variable> conj : ante.getSubexpressions())
					split.add(Expression.buildImplication(prec, conj));

				return split;
			}
		}

		// OR
		return Arrays.asList(exp);

	}

	private static class ConstComp implements Comparator<Constant> {

		public static ConstComp INSTANCE = new ConstComp();

		@Override
		public int compare(Constant o1, Constant o2) {
			return Integer.compare(Integer.valueOf(o1.getName()), Integer.valueOf(o2.getName()));
		}
	}

	private static class PropagatorComparator implements Comparator<Propagator> {

		public static PropagatorComparator INSTANCE = new PropagatorComparator();

		// first exp by decreasing size, then binary
		@Override
		public int compare(Propagator o1, Propagator o2) {

			if (o1.getClass().equals(o2.getClass())) {
				if (o1 instanceof BinaryPropagator) {
					return 0;
				}
				if (o1 instanceof ExpressionPropagator) {
					// largest expression first
					return Integer.compare(((ExpressionPropagator) o1).getDomain().size(), 
							((ExpressionPropagator) o2).getDomain().size());
				}

				return 0;
			}

			if (o1 instanceof BinaryPropagator)
				return 1;

			return -1;
		}

	}


	private CspOptimiser() {  }

}
