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
package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.csp.ExpressionCsp;
import au.rmit.agtgrp.pplib.csp.PartitionedExpressionCsp;
import au.rmit.agtgrp.pplib.csp.alldiff.AllDifferent;
import au.rmit.agtgrp.pplib.fol.expression.Connective;
import au.rmit.agtgrp.pplib.fol.expression.Expression;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.pct.*;
import au.rmit.agtgrp.pplib.pp.partialplan.InstantiatablePartialPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.ConstraintEncoder;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.optimiser.CspOptimiser;

public class PcToCspEncoder extends ConstraintEncoder<ExpressionCsp, CspEncoderOptions> {

	public static Map<Operator<Variable>, Variable> getOperatorVariableMap(Collection<Operator<Variable>> steps) {
		Map<Operator<Variable>, Variable> opVarMap = new HashMap<Operator<Variable>, Variable>();
		for (Operator<Variable> op : steps) {
			Variable opVar = new Variable(Type.OPERATOR_TYPE, op.getName()).intern();
			opVarMap.put(op, opVar);
		}

		return opVarMap;
	}

	public static void addAllDifferent(ExpressionCsp csp) {

		// find all operator variables
		List<Variable> opVars = new ArrayList<Variable>();
		for (Variable var : csp.getVariables()) {
			if (var.getType().equals(Type.OPERATOR_TYPE))
				opVars.add(var);
		}

		// build all diff literal
		Literal<Variable> allDiffLit = AllDifferent.buildAllDifferent(opVars);

		// add constraint
		csp.addConstraint(Expression.buildLiteral(allDiffLit));
	}

	public static void removeAllDifferent(ExpressionCsp csp) {
		for (List<Variable> dom : csp.getConstraints().keySet()) {
			for (Expression<Variable> cons : csp.getConstraints(dom)) {
				if (cons.isLiteral() && AllDifferent.isAllDifferentLiteral(cons.getLiteral())) {
					csp.getConstraints(dom).remove(cons);
					return;
				}
			}
		}
	}

	public static void setOperatorTypeOrdering(ExpressionCsp csp, List<Operator<Variable>> steps, Map<Operator<Variable>, Variable> opVarMap) {

		Map<String, List<Variable>> opsByType = new HashMap<String, List<Variable>>();

		for (Operator<Variable> op : steps) {
			String type = op.getName();
			List<Variable> ops = opsByType.get(type);
			if (ops == null) {
				ops = new ArrayList<Variable>();
				opsByType.put(type, ops);
			}
			ops.add(opVarMap.get(op));
		}

		for (String type : opsByType.keySet()) {
			List<Variable> ops = opsByType.get(type);
			for (int i = 0; i < ops.size() - 1; i++) {
				csp.addConstraint(Expression.buildLiteral(Literal.prec(ops.get(i), ops.get(i + 1))));
			}
		}

	}

	protected static final Variable FALSE_VAR = new Variable(Type.INT_TYPE, "F");
	protected static final Variable TRUE_VAR = new Variable(Type.INT_TYPE, "T");

	protected static final Constant TRUE = new Constant(Type.INT_TYPE, "1");
	protected static final Constant FALSE = new Constant(Type.INT_TYPE, "0");

	protected long time = 0;

	protected Set<Constant> objects;
	protected Set<Type> types;
	protected PcPlan plan;

	protected ExpressionCsp csp;
	protected Map<Operator<Variable>, Variable> opVarMap;
	protected Map<PcLink, Variable> pcLinkVarMap;

	public PcToCspEncoder(CspEncoderOptions options) {
		super(options);
	}

	@Override
	public InstantiatablePartialPlan<ExpressionCsp> encodeAsPartialPlan(PcPlan plan) {
		encodeConstraints(plan);
		return new CspPartialPlan(csp, plan.getProblem(), plan.getPlanSteps(), plan.getOriginalSub(), plan.getOriginalSub());
	}

	@Override
	public ExpressionCsp encodeConstraints(PcPlan plan) {
		long startTime = System.currentTimeMillis();

		this.plan = plan;

		objects = new HashSet<Constant>();
		objects.addAll(plan.getDomain().getConstants());
		objects.addAll(plan.getProblem().getObjects());

		types = new HashSet<Type>(plan.getDomain().getTypes());

		csp = new ExpressionCsp();

		// add variables from preconditions etc
		for (Operator<Variable> op : plan.getPlanSteps())
			csp.addVariables(op.getParameters());

		// create a variable for each operator
		opVarMap = getOperatorVariableMap(plan.getPlanSteps());
		csp.addVariables(opVarMap.values());

		// set var domains
		setDomains();

		// create a switching var for each pc link
		if (options.switchingVars) {
			List<Constant> tf = new ArrayList<Constant>();
			tf.add(TRUE);
			tf.add(FALSE);
			pcLinkVarMap = new HashMap<PcLink, Variable>();
			int i = 0;
			for (PcLink link : plan.getConstraints().getAllPcLinks()) {
				Variable switchVar = new Variable(Type.INT_TYPE, "sw_"+i++).intern();
				pcLinkVarMap.put(link, switchVar);
				csp.addVariable(switchVar);
				csp.addDomainValues(switchVar, tf);
			}	

			csp.addVariable(TRUE_VAR);
			csp.addDomainValue(TRUE_VAR, TRUE);
			csp.addVariable(FALSE_VAR);
			csp.addDomainValue(FALSE_VAR, FALSE);
		}

		// set pc constraints
		setProducerConsumerConstraints();

		if (options.optimise)
			csp = CspOptimiser.optimise(csp, options.targetTreewidth, options.twCalc);

		// set all diff over operators
		Literal<Variable> allDiffLit = AllDifferent.buildAllDifferent(new ArrayList<Variable>(opVarMap.values()));
		if (csp instanceof PartitionedExpressionCsp) {
			PartitionedExpressionCsp pcsp = (PartitionedExpressionCsp) csp;
			allDiffLit = allDiffLit.applySubstitution(pcsp.getMapping());
		}	
		csp.addConstraint(Expression.buildLiteral(allDiffLit));

		time = System.currentTimeMillis() - startTime;
		return csp;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}


	public long getEncodingTime() {
		return time;
	}


	private void setDomains() {
		setNongroundVariableDomains();
		setPartialOrderOperatorDomains();
	}

	private void setPartialOrderOperatorDomains() {
		// set global domain from operator positions
		List<Constant> ordinalDomain = new ArrayList<Constant>();
		for (int i = 0; i < plan.getPlanSteps().size(); i++) {
			ordinalDomain.add(new Constant(Type.OPERATOR_TYPE, Integer.toString(i)));
		}
		csp.addDomainValues(ordinalDomain);

		// domains for operator variables
		List<Constant> midCons = ordinalDomain.subList(1, ordinalDomain.size() - 1);
		for (int i = 0; i < plan.getPlanSteps().size(); i++) {
			Variable opVar = opVarMap.get(plan.getPlanSteps().get(i));
			csp.addDomainValue(opVar, ordinalDomain.get(i));
			if (i != 0 && i != plan.getPlanSteps().size() - 1)
				csp.addDomainValues(opVar, midCons);

		}
	}


	protected void setNongroundVariableDomains() {
		// set global domain from planning problem
		csp.addDomainValues(objects);

		// get types
		Map<Type, Set<Constant>> objsByType = new HashMap<Type, Set<Constant>>();
		for (Type t : types)
			objsByType.put(t, new HashSet<Constant>());

		// get objs from problem
		for (Constant obj : objects)
			objsByType.get(obj.getType()).add(obj);

		for (Variable var : csp.getVariables()) {
			if (!var.getType().equals(Type.OPERATOR_TYPE)) {
				if (plan.getInitAction().getVariables().contains(var))
					csp.addDomainValue(var, plan.getOriginalSub().apply(var));
				else if (plan.getGoalAction().getVariables().contains(var))
					csp.addDomainValue(var, plan.getOriginalSub().apply(var));
				else {
					for (Type t : objsByType.keySet()) {
						if (var.getType().hasSubtype(t))
							csp.addDomainValues(var, objsByType.get(t));
					}
				}
			}
		}
	}

	/*
	protected void setProducerConsumerConstraints(CspEncoderOptions options) {
		switch (options.threatRestriction) {
		case BINDING:
			setRestrictedBindingPcConstraints();
			break;
		case ORDERING:
			setRestrictedOrderPcConstraints();
			break;
		case NONE:
			setFullPcConstraints();
			break;
		default:
			throw new IllegalArgumentException("Unknown encoder option: " + options.threatRestriction);
		}
	}
	 */


	protected void setProducerConsumerConstraints() {

		CausalStructure constraints = plan.getConstraints();

		ThreatMap threatMap = ThreatMap.getThreatMap(plan.getPlanSteps());

		// for each precon of each operator
		for (Operator<Variable> consOp : plan.getPlanSteps()) {

			// for each consumer
			for (Literal<Variable> consLit : consOp.getPreconditions()) {

				Consumer consumer = new Consumer(consOp, consLit).intern();

				if (constraints.getProducers(consumer).isEmpty())
					throw new RuntimeException("No producer for consumer: " + consumer);

				Variable consOrdinal = opVarMap.get(consumer.operator);

				// add producer-consumer constraints
				Map<PcLink, Expression<Variable>> pcOptions = new HashMap<PcLink, Expression<Variable>>();

				for (Producer producer : constraints.getProducers(consumer)) {

					PcLink pcLink = new PcLink(producer, consumer).intern();

					//System.out.println(pcLink);

					Variable prodOrdinal = opVarMap.get(producer.operator);
					List<Expression<Variable>> conj = new ArrayList<Expression<Variable>>();

					// co-designation constraint
					for (int v = 0; v < consumer.literal.getAtom().getParameters().size(); v++)
						conj.add(Expression.buildLiteral(Literal.equals(
								producer.literal.getAtom().getVariables().get(v),
								consumer.literal.getAtom().getVariables().get(v), true)));

					// ordering constraint
					conj.add(Expression.buildLiteral(Literal.prec(prodOrdinal, consOrdinal)));

					// now each threat
				
					for (Threat threat : threatMap.getNonGroundThreats(pcLink)) {
						Variable threatOrd = opVarMap.get(threat.operator);

						switch (options.threatRestriction) {

						case BINDING:			
							// threat was undone by a later postcondition of the same action, 
							// ensure bindings remain the same
							Literal<Variable> undo = threat.operator.getUndoing(threat.literal.getNegated());
							if (undo != null && 
									plan.getOriginalSub().apply(threat.literal.getAtom().getVariables()).equals(
									plan.getOriginalSub().apply(undo.getAtom().getVariables()))) {

								List<Expression<Variable>> undoConj = new ArrayList<Expression<Variable>>();
								for (int k = 0; k < threat.literal.getAtom().getVariables().size(); k++) {
									undoConj.add(Expression.buildLiteral(Literal.equals(
											threat.literal.getAtom().getVariables().get(k),
											undo.getAtom().getVariables().get(k), true)));
								}
								conj.add(Expression.buildExpression(Connective.AND, undoConj));				
								break;
							}

							// bindings were different, must remain different
							boolean codesig = true;
							for (int v = 0; v < consumer.literal.getAtom().getVariables().size(); v++) {
								Constant cval = plan.getOriginalSub().apply(consumer.literal.getAtom().getVariables().get(v));
								Constant tval = plan.getOriginalSub().apply(threat.literal.getAtom().getVariables().get(v));

								if (!cval.equals(tval)) { // values must remain different
									conj.add(Expression.buildLiteral(
											Literal.equals(consumer.literal.getAtom().getVariables().get(v),
													threat.literal.getAtom().getVariables().get(v), false)));
									codesig = false;
									break;
								}
								
							}

							if (!codesig)
								break;

							// t < p in original plan, keep this
							if (plan.getPlanSteps().indexOf(threat.operator) < plan.getPlanSteps().indexOf(producer.operator)) {
								conj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
								break;
							}
							
							// c < t in original plan, keep this
							if (plan.getPlanSteps().indexOf(threat.operator) > plan.getPlanSteps().indexOf(consumer.operator)) {
								conj.add(Expression.buildLiteral(Literal.prec(consOrdinal, threatOrd)));
								break;
							}
							
							// must be p < t < c in original plan, all codesignated. so p is not the original producer for c.
							// require that t < p or c < t
							List<Expression<Variable>> precDisj = new ArrayList<Expression<Variable>>();
							precDisj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
							precDisj.add(Expression.buildLiteral(Literal.prec(consOrdinal, threatOrd)));	
							conj.add(Expression.buildExpression(Connective.OR, precDisj));
						
							break;	
							
						case ORDERING:
							// threat was undone by a later postcondition of the same action, 
							// ensure bindings remain the same
							Literal<Variable> undoing = threat.operator.getUndoing(threat.literal.getNegated());
							if (undoing != null && 
									plan.getOriginalSub().apply(threat.literal.getAtom().getVariables()).equals(
									plan.getOriginalSub().apply(undoing.getAtom().getVariables()))) {

								List<Expression<Variable>> undoConj = new ArrayList<Expression<Variable>>();
								for (int k = 0; k < threat.literal.getAtom().getVariables().size(); k++) {
									undoConj.add(Expression.buildLiteral(Literal.equals(
											threat.literal.getAtom().getVariables().get(k),
											undoing.getAtom().getVariables().get(k), true)));
								}
								conj.add(Expression.buildExpression(Connective.AND, undoConj));
							
								break;
							}
							
							// t < p in original plan, keep this
							if (plan.getPlanSteps().indexOf(threat.operator) < plan.getPlanSteps().indexOf(producer.operator)) {
								conj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
								break;
							}
							// c < t in original plan, keep this
							if (plan.getPlanSteps().indexOf(threat.operator) > plan.getPlanSteps().indexOf(consumer.operator)) {
								conj.add(Expression.buildLiteral(Literal.prec(consOrdinal, threatOrd)));
								break;
							}

							// bindings were different, must remain different
							boolean codesign = true;
							for (int v = 0; v < consumer.literal.getAtom().getVariables().size(); v++) {
								Constant cval = plan.getOriginalSub().apply(consumer.literal.getAtom().getVariables().get(v));
								Constant tval = plan.getOriginalSub().apply(threat.literal.getAtom().getVariables().get(v));

								if (!cval.equals(tval)) { // values must remain different
									conj.add(Expression.buildLiteral(
											Literal.equals(consumer.literal.getAtom().getVariables().get(v),
													threat.literal.getAtom().getVariables().get(v), false)));
									codesign = false;
									break;
								}
								
							}

							if (!codesign)
								break;

							// must be p < t < c in original plan, all codesignated. so p is not the original producer for c.
							// require that t < p or c < t
							List<Expression<Variable>> precOPts = new ArrayList<Expression<Variable>>();
							precOPts.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
							precOPts.add(Expression.buildLiteral(Literal.prec(consOrdinal, threatOrd)));	
							conj.add(Expression.buildExpression(Connective.OR, precOPts));
						
							break;

					case NONE:							
						// c != t or ...
						List<Expression<Variable>> disj = new ArrayList<Expression<Variable>>();
						for (int v = 0; v < consumer.literal.getAtom().getParameters().size(); v++) {
							// threat var is the same as producer var
							if (producer.literal.getAtom().getVariables().get(v).equals(threat.literal.getAtom().getVariables().get(v)))
								continue;

							disj.add(Expression.buildLiteral(Literal.equals(
									threat.literal.getAtom().getVariables().get(v),
									producer.literal.getAtom().getVariables().get(v), false)));
						}

						// or cons <= t or t < prod	
						if (!threat.operator.equals(producer.operator))				
							disj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
						disj.add(Expression.buildLiteral(Literal.prec(consOrdinal, threatOrd)));

						// threat can undone by a later postcondition of the same action, if they have the same bindings
						Literal<Variable> und = threat.operator.getUndoing(threat.literal.getNegated());
						if (und != null) {				
							List<Expression<Variable>> undoConj = new ArrayList<Expression<Variable>>();
							for (int k = 0; k < threat.literal.getAtom().getVariables().size(); k++) {
								undoConj.add(Expression.buildLiteral(Literal.equals(
										threat.literal.getAtom().getVariables().get(k),
										und.getAtom().getParameters().get(k), true)));
							}
							disj.add(Expression.buildExpression(Connective.AND, undoConj));
						}

						// finalise
						conj.add(Expression.buildExpression(Connective.OR, disj));

						break;
					default:
						throw new IllegalArgumentException("Unknown encoder option: " + options.threatRestriction);
					}

				}

				if (!conj.isEmpty()) // this can happen when predicate has no parameters, i.e. is a proposition
					pcOptions.put(pcLink, Expression.buildExpression(Connective.AND, conj));
				else if (!consumer.literal.getAtom().getParameters().isEmpty()) // not a proposition
					throw new RuntimeException("No constraints for consumer " + consumer);

			}

			if (!pcOptions.isEmpty()) {
				if (options.switchingVars) {
					List<Expression<Variable>> switchExps = new ArrayList<Expression<Variable>>();
					for (PcLink link : pcOptions.keySet()) {
						Variable switchVar = pcLinkVarMap.get(link);
						Expression<Variable> switchExp = Expression.buildLiteral(Literal.equals(switchVar, TRUE_VAR, true));
						switchExps.add(switchExp);

						csp.addConstraint(Expression.buildImplication(switchExp, pcOptions.get(link)));
						csp.addConstraint(Expression.buildImplication(pcOptions.get(link), switchExp));

					}
					csp.addConstraint(Expression.buildExpression(Connective.OR, switchExps));

				} else {
					List<Expression<Variable>> poss = new ArrayList<Expression<Variable>>();
					for (PcLink link : pcOptions.keySet()) {
						poss.add(pcOptions.get(link));
					}
					csp.addConstraint(Expression.buildExpression(Connective.OR, poss));
				}						
			} else if (!consumer.literal.getAtom().getParameters().isEmpty())
				throw new RuntimeException("No constraints for consumer " + consumer);

		}
	}
}


/*

	protected void setRestrictedBindingPcConstraints() {

		CausalStructure constraints = plan.getConstraints();

		ThreatMap threats = ThreatMap.getThreatMap(plan.getPlanSteps());

		// for each precon of each operator
		for (Operator<Variable> consOp : plan.getPlanSteps()) {

			// for each consumer
			for (Literal<Variable> consLit : consOp.getPreconditions()) {

				Consumer consumer = new Consumer(consOp, consLit).intern();

				if (constraints.getProducers(consumer).isEmpty())
					throw new RuntimeException("No producer for consumer: " + consumer);

				Variable consOrdinal = opVarMap.get(consumer.operator);

				// add producer-consumer constraints
				List<Expression<Variable>> pcOptions = new ArrayList<Expression<Variable>>();

				for (Producer producer : constraints.getProducers(consumer)) {

					Variable prodOrdinal = opVarMap.get(producer.operator);
					List<Expression<Variable>> conj = new ArrayList<Expression<Variable>>();

					// co-designation constraint
					for (int v = 0; v < consumer.literal.getAtom().getParameters().size(); v++)
						conj.add(Expression
								.buildLiteral(Literal.equals(producer.literal.getAtom().getVariables().get(v),
										consumer.literal.getAtom().getVariables().get(v), true)));

					// ordering constraint
					conj.add(Expression.buildLiteral(Literal.prec(prodOrdinal, consOrdinal)));

					// now each threat
					for (Threat threat : threats.getNonGroundThreats(new PcLink(producer, consumer))) { 

						Variable threatOrd = opVarMap.get(threat.operator);
						boolean codesig = true;
						for (int v = 0; v < consumer.literal.getAtom().getParameters().size(); v++) {
							Constant cval = plan.getOriginalSub()
									.apply(consumer.literal.getAtom().getVariables().get(v));
							Constant tval = plan.getOriginalSub()
									.apply(threat.literal.getAtom().getVariables().get(v));

							if (!cval.equals(tval)) { // values must remain
								// different
								conj.add(Expression
										.buildLiteral(Literal.equals(consumer.literal.getAtom().getVariables().get(v),
												threat.literal.getAtom().getVariables().get(v), false)));
								codesig = false;
								break;
							}

						}
						if (codesig) {
							// t < p
							if (plan.getPlanSteps().indexOf(threat.operator) < plan.getPlanSteps()
									.indexOf(producer.operator))
								conj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
							// c < t
							else if (plan.getPlanSteps().indexOf(threat.operator) > plan.getPlanSteps()
									.indexOf(consumer.operator))
								conj.add(Expression.buildLiteral(Literal.prec(consOrdinal, threatOrd)));
							else
								conj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
						}

					}

					if (!conj.isEmpty()) // this can happen when predicate has
						// no parameters, i.e. is a
						// proposition
						pcOptions.add(Expression.buildExpression(Connective.AND, conj));
					else if (!consumer.literal.getAtom().getParameters().isEmpty())
						throw new RuntimeException("No constraints for consumer " + consumer);

				}

				if (!pcOptions.isEmpty()) {
					Expression<Variable> possBindings = Expression.buildExpression(Connective.OR, pcOptions);
					csp.addConstraint(possBindings);
				} else if (!consumer.literal.getAtom().getParameters().isEmpty())
					throw new RuntimeException("No constraints for consumer " + consumer);

			}
		}
	}



	protected void setRestrictedOrderPcConstraints() {

		CausalStructure constraints = plan.getConstraints();

		ThreatMap threats = ThreatMap.getThreatMap(plan.getPlanSteps());

		// for each precon of each operator
		for (Operator<Variable> consOp : plan.getPlanSteps()) {

			// for each consumer
			for (Literal<Variable> consLit : consOp.getPreconditions()) {

				Consumer consumer = new Consumer(consOp, consLit).intern();

				if (constraints.getProducers(consumer).isEmpty())
					throw new RuntimeException("No producer for consumer: " + consumer);

				Variable consOrdinal = opVarMap.get(consumer.operator);

				// add producer-consumer constraints
				List<Expression<Variable>> pcOptions = new ArrayList<Expression<Variable>>();

				for (Producer producer : constraints.getProducers(consumer)) {

					Variable prodOrdinal = opVarMap.get(producer.operator);
					List<Expression<Variable>> conj = new ArrayList<Expression<Variable>>();

					// co-designation constraint
					for (int v = 0; v < consumer.literal.getAtom().getParameters().size(); v++)
						conj.add(Expression
								.buildLiteral(Literal.equals(producer.literal.getAtom().getVariables().get(v),
										consumer.literal.getAtom().getVariables().get(v), true)));

					// ordering constraint
					conj.add(Expression.buildLiteral(Literal.prec(prodOrdinal, consOrdinal)));

					// now each threat
					for (Threat threat : threats.getNonGroundThreats(new PcLink(producer, consumer))) { 

						// t < p
						Variable threatOrd = opVarMap.get(threat.operator);
						if (plan.getPlanSteps().indexOf(threat.operator) < plan.getPlanSteps()
								.indexOf(producer.operator))
							conj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
						// c < t
						else if (plan.getPlanSteps().indexOf(threat.operator) > plan.getPlanSteps()
								.indexOf(consumer.operator))
							conj.add(Expression.buildLiteral(Literal.prec(consOrdinal, threatOrd)));
						else {
							boolean codesig = true;
							for (int v = 0; v < consumer.literal.getAtom().getVariables().size(); v++) {
								Constant cval = plan.getOriginalSub().apply(consumer.literal.getAtom().getVariables().get(v));
								Constant tval = plan.getOriginalSub().apply(threat.literal.getAtom().getVariables().get(v));

								if (!cval.equals(tval)) { // values must remain different
									conj.add(Expression.buildLiteral(
											Literal.equals(consumer.literal.getAtom().getVariables().get(v),
													threat.literal.getAtom().getVariables().get(v), false)));
									codesig = false;
									break;
								}

							}
							if (codesig) {
								conj.add(Expression.buildLiteral(Literal.prec(threatOrd, prodOrdinal)));
							}

						}

					}

					if (!conj.isEmpty()) // this can happen when predicate has
						// no parameters, i.e. is a
						// proposition
						pcOptions.add(Expression.buildExpression(Connective.AND, conj));
					else if (!consumer.literal.getAtom().getParameters().isEmpty())
						throw new RuntimeException("No constraints for consumer " + consumer);

				}

				if (!pcOptions.isEmpty()) {
					Expression<Variable> possBindings = Expression.buildExpression(Connective.OR, pcOptions);
					csp.addConstraint(possBindings);
				} else if (!consumer.literal.getAtom().getParameters().isEmpty())
					throw new RuntimeException("No constraints for consumer " + consumer);

			}
		}
	}

 */


}
