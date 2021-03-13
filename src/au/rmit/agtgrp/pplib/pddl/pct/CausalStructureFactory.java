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
package au.rmit.agtgrp.pplib.pddl.pct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;

public class CausalStructureFactory {

	public static PcPlan getEquivalentPcoPlan(Plan plan) {
		CausalStructure constraints = getEquivalentPcoConstraints(plan);
		return new PcPlan(plan.getProblem(), plan.getPlanSteps(), plan.getSubstitution(), constraints);
	}

	public static PcPlan getMinimalPcoPlan(Plan plan) {
		return getMinimalPcoPlan(plan, false, false);
	}

	public static PcPlan getMinimalPcoPlan(Plan plan, boolean deorder, boolean ground) {
		CausalStructure constraints = getMinimalPcoConstraints(plan, deorder, ground);
		return new PcPlan(plan.getProblem(), plan.getPlanSteps(), plan.getSubstitution(), constraints);
	}

	public static CausalStructure getEquivalentPcoConstraints(Plan plan) {

		CausalStructure constraints = new CausalStructure(true);

		for (int i = 1; i < plan.length(); i++) { // each step in plan, except initial step

			Operator<Variable> cons = plan.getPlanSteps().get(i);
			for (int pre_i = 0; pre_i < cons.getPreconditions().size(); pre_i++) { // each precon in step

				Literal<Variable> pre = cons.getPreconditions().get(pre_i);

				Producer actualProducer = null;
				boolean threatFound = false;
				// find actual producer
				for (int j = i - 1; j >= 0; j--) { // each previous step in plan, in reverse order

					Operator<Variable> prod = plan.getPlanSteps().get(j);
					//equality, neg preconditions can link to initial state
					if (j == 0) {			
						if (pre.getAtom().getSymbol().equals(Predicate.EQUALS)) { // equality precon
							Variable v1 = pre.getAtom().getVariables().get(0);
							Variable v2 = pre.getAtom().getVariables().get(1);

							// (H1 = H1) or (H1 != H2)
							if (plan.getSubstitution().apply(v1).equals(plan.getSubstitution().apply(v2)) == pre.getValue()) {

								List<Variable> newVars = new ArrayList<Variable>();
								for (Variable var : pre.getAtom().getVariables())
									newVars.add(getInitialStateVariable(var, plan));

								actualProducer = new Producer(prod, pre.resetVariables(newVars).rebind(newVars)).intern();

							}
						}
						else if (!pre.getValue()) { // neg precon, not equality 
							// pre = -p(x), p(x) is not in initial state

							List<Variable> newVars = new ArrayList<Variable>();
							for (Variable var : pre.getAtom().getVariables())
								newVars.add(getInitialStateVariable(var, plan));

							Literal<Variable> resetPre = pre.resetVariables(newVars).rebind(newVars);
							if (!prod.getPostconditions().contains(resetPre.getNegated()))
								actualProducer = new Producer(prod, resetPre).intern();

						}						
					}


					// each postcon in previous steps
					for (int post_j = 0; post_j < prod.getPostconditions().size(); post_j++) { 

						Literal<Variable> postcon = prod.getPostconditions().get(post_j);

						if (pre.getAtom().getSymbol().equals(postcon.getAtom().getSymbol())) { // same symbol
							// check bindings
							boolean sameBindings = true;
							for (int b = 0; b < pre.getAtom().getVariables().size(); b++) {
								Variable prev = pre.getAtom().getVariables().get(b);
								Variable postv = postcon.getAtom().getVariables().get(b);

								// if bindings not the same
								if (!plan.getSubstitution().apply(prev).equals(plan.getSubstitution().apply(postv))) {
									sameBindings = false;
									break;
								}
							}
							
							if (!sameBindings) {
								continue;
							}
							
							// check if undone by later effect
							if (prod.applySubstitution(plan.getSubstitution()).isUndone(postcon.applySubstitution(plan.getSubstitution()))) {
								continue;
							}
							
							// same value
							if (pre.getValue() == postcon.getValue()) { 
								actualProducer = new Producer(prod, postcon).intern();
							} else { // opposite value, not undone -- deletes precon
								threatFound = true;
								break;
							}
						}
					}
					
					if (threatFound)
						break;
				}


				if (actualProducer == null) {
					throw new RuntimeException("No producer found for precondition:\n" 
							+ pre.applySubstitution(plan.getSubstitution()) + "\nIn operator: \n" + cons);
				}

				// actual pc link
				constraints.addProducerConsumerOption(actualProducer, new Consumer(cons, pre).intern());

			}
		}

		return constraints;
	}

	public static CausalStructure getMinimalPcoConstraints(Plan plan, boolean deorder, boolean ground) {
		CausalStructure constraints = new CausalStructure(ground);

		Map<Predicate, ArrayList<Literal<Variable>>> initLitsMap = new HashMap<Predicate, ArrayList<Literal<Variable>>>();
		for (Literal<Variable> postCon : plan.getInitialAction().getPostconditions()) {
			ArrayList<Literal<Variable>> predLits = initLitsMap.get(postCon.getAtom().getSymbol());
			if (predLits == null) {
				predLits = new ArrayList<Literal<Variable>>();
				initLitsMap.put(postCon.getAtom().getSymbol(), predLits);
			}

			predLits.add(postCon);
		}

		Set<Producer> inequalities = null;

		for (int i = 1; i < plan.length(); i++) { // each step in plan, except initial step
			Operator<Variable> cons = plan.getPlanSteps().get(i);
			for (int pre_i = 0; pre_i < cons.getPreconditions().size(); pre_i++) { // each precon in step

				Literal<Variable> pre = cons.getPreconditions().get(pre_i);
				Consumer consPc = new Consumer(cons, pre).intern();

				// find all potential producers
				for (int j = 0; j < (deorder ? i : plan.length()); j++) { // de/reorder -- previous steps/every step in plan 

					// equality, negated precons
					if (j == 0) {
						if (pre.getAtom().getSymbol().equals(Predicate.EQUALS)) {
							if (pre.getValue())
								throw new IllegalArgumentException("POSITIVE EQUALITY REQUIREMENT: " + pre);
							
							if (inequalities == null)
								inequalities = getEqualityProducers(plan);
							
							for (Producer eqProd : inequalities)
								if (assignable(eqProd.literal.getAtom(), pre.getAtom())) {
									if (!ground || codesignated(eqProd.literal.getAtom(), consPc.literal.getAtom(), plan.getSubstitution())) {
										constraints.addProducerConsumerOption(eqProd, consPc);
									}
								}					
						}
						else if (!pre.getValue()) {
							for (Producer negProd : getInitialStateNegationProducers(pre, plan)) {
								if (assignable(negProd.literal.getAtom(), pre.getAtom())) {
									if (!ground || codesignated(negProd.literal.getAtom(), consPc.literal.getAtom(), plan.getSubstitution())) {
										constraints.addProducerConsumerOption(negProd, consPc);
									}
								}
							}
						}
					}

					if (j != i) {
						Operator<Variable> prod = plan.getPlanSteps().get(j);
						List<Literal<Variable>> lits = null;
						if (j == 0)
							lits = initLitsMap.get(consPc.literal.getAtom().getSymbol());
						else
							lits = prod.getPostconditions();

						if (lits != null) {
							for (int post_j = 0; post_j < lits.size(); post_j++) { // each postcon in other step

								Literal<Variable> post = lits.get(post_j);
								Producer prodPc = new Producer(prod, post).intern();

								if (pre.getAtom().getSymbol().equals(post.getAtom().getSymbol()) && // same symbol
										pre.getValue() == post.getValue() && // same value
										assignable(post.getAtom(), pre.getAtom()) && // bindable, as per variable types
										(j == 0 || !prod.isUndone(post))) { // initial state "effects" are never undone.

									// check if causal structure must be ground
									if (!ground || codesignated(prodPc.literal.getAtom(), consPc.literal.getAtom(), plan.getSubstitution())) {
										constraints.addProducerConsumerOption(prodPc, consPc);
									}
								}
							}
						}
					}
				}

				if (constraints.getProducers(consPc).isEmpty())
					throw new RuntimeException("No producer for consumer: " + consPc);
			}
		}

		return constraints;

	}

	private static Set<Producer> getEqualityProducers(Plan plan) {
		Set<Producer> negProds = new HashSet<Producer>();
		
		for (Variable v1 : plan.getInitialAction().getVariables()) {
			Constant c1 = plan.getSubstitution().apply(v1);
			for (Variable v2 : plan.getInitialAction().getVariables()) {
				Constant c2 = plan.getSubstitution().apply(v2);
				if (!c1.equals(c2)) {
					negProds.add(new Producer(plan.getInitialAction(), Literal.equals(v1, v2, false)).intern());
				}
			}
		}
		return negProds;
	}


	private static Set<Producer> getInitialStateNegationProducers(Literal<Variable> pre, Plan plan) {

		if (pre.getValue())
			throw new IllegalArgumentException(pre.toString());

		Set<Producer> prods = new HashSet<Producer>();

		for (List<Variable> vars : getAllCombs(pre.getAtom().getSymbol().getTypes(), plan.getInitialAction().getVariables())) {		
			Literal<Variable> negEff = pre.resetVariables(vars).rebind(vars);
			if (!plan.getInitialAction().getPostconditions().contains(negEff.getNegated()))
				prods.add(new Producer(plan.getInitialAction(), negEff).intern());

		}

		return prods;

	}


	private static List<List<Variable>> getAllCombs(List<Type> types, List<Variable> initVars) {

		List<List<Variable>> varLists = new ArrayList<List<Variable>>();
		if (types.isEmpty()) {
			varLists.add(new ArrayList<Variable>()); // a single empty list
			return varLists;
		}

		List<List<Variable>> recLists = getAllCombs(types.subList(1, types.size()), initVars);

		Type t = types.get(0);

		for (Variable var : initVars) {
			if (t.hasSubtype(var.getType())) {
				for (List<Variable> recList : recLists) {			
					List<Variable> l = new ArrayList<Variable>();
					l.add(var);
					l.addAll(recList);
					varLists.add(l);
				}
			}
		}

		return varLists;

	}


	private static Variable getInitialStateVariable(Variable var, Plan plan) {
		for (Variable initVar : plan.getInitialAction().getVariables()) {
			if (plan.getSubstitution().apply(initVar).equals(plan.getSubstitution().apply(var))) {
				return initVar;
			}			
		}

		return null;
	}


	private static boolean codesignated(Atom<Variable> prod, Atom<Variable> cons, Substitution<Constant> sub) {
		for (int i = 0; i < prod.getParameters().size(); i++) {
			if (!sub.apply(cons.getVariables().get(i)).equals(sub.apply(prod.getVariables().get(i)))) {
				return false;
			}
		}

		return true;
	}

	private static boolean assignable(Atom<? extends Term> prod, Atom<? extends Term> cons) {
		for (int i = 0; i < prod.getParameters().size(); i++) {
			if (!cons.getParameters().get(i).getType().hasSubtype(prod.getParameters().get(i).getType())
					&& !prod.getParameters().get(i).getType().hasSubtype(cons.getParameters().get(i).getType())) {
				return false;
			}
		}

		return true;
	}


	
	private CausalStructureFactory() { }

}
