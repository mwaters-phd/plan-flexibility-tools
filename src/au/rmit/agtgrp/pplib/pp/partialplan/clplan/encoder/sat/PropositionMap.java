package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Symbol;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.pct.Consumer;
import au.rmit.agtgrp.pplib.pddl.pct.PcLink;
import au.rmit.agtgrp.pplib.pddl.pct.Producer;
import au.rmit.agtgrp.pplib.pddl.pct.Threat;
import au.rmit.agtgrp.pplib.pp.partialplan.PartialPlan;
import au.rmit.agtgrp.pplib.utils.collections.Bijection;
import au.rmit.agtgrp.pplib.utils.collections.HashBijection;
import au.rmit.agtgrp.pplib.utils.collections.Pair;
import au.rmit.agtgrp.pplib.utils.collections.Triple;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;
import au.rmit.agtgrp.pplib.utils.collections.graph.GraphUtils;

public class PropositionMap implements Serializable {

	private static final long serialVersionUID = 1L;

	//private final List<Object> objsList;
	private final transient Bijection<Object, Integer> objPropMap;
	
	private final transient Bijection<PcLink, Integer> pclToPropMap;
	private final transient Bijection<Pair<PcLink, Threat>, Integer> threatToPropMap;

	private final transient Bijection<EqualityObj, Integer> eqToPropMap;	
	private final transient Bijection<VariableAssignmentObj, Integer> assToPropMap;
	private final transient Bijection<PrecedenceObj, Integer> precToPropMap;

	private final transient Bijection<Pair<Operator<Variable>, Integer>, Integer> opEncodingPropMap;
	private final transient Bijection<Triple<Operator<Variable>, Operator<Variable>, Integer>, Integer> precEncodingToPropMap;

	private final transient Bijection<Pair<Variable, Variable>, Integer> varPrecToPropMap;

	private final int opEncodingBits;

	private int p;

	public PropositionMap(PropositionMap copyMe) {
		this(copyMe.opEncodingBits);
		//this.objsList.addAll(copyMe.objsList);
		this.objPropMap.putAll(copyMe.objPropMap);
		this.pclToPropMap.putAll(copyMe.pclToPropMap);
		this.threatToPropMap.putAll(copyMe.threatToPropMap);
		this.eqToPropMap.putAll(copyMe.eqToPropMap);
		this.assToPropMap.putAll(copyMe.assToPropMap);
		this.precToPropMap.putAll(copyMe.precToPropMap);
		this.opEncodingPropMap.putAll(copyMe.opEncodingPropMap);
		this.precEncodingToPropMap.putAll(copyMe.precEncodingToPropMap);
		this.varPrecToPropMap.putAll(copyMe.varPrecToPropMap);
		this.p = copyMe.p;	
	}
	
	public PropositionMap(int opEncodingBits) {
		this.opEncodingBits = opEncodingBits;

		//objsList = new ArrayList<Object>();
		objPropMap = new HashBijection<Object,Integer>();
		
		pclToPropMap = new HashBijection<PcLink, Integer>();
		threatToPropMap = new HashBijection<Pair<PcLink, Threat>, Integer>();

		eqToPropMap = new HashBijection<EqualityObj, Integer>();
		assToPropMap = new HashBijection<VariableAssignmentObj, Integer>();
		precToPropMap = new HashBijection<PrecedenceObj, Integer>();

		varPrecToPropMap = new HashBijection<Pair<Variable, Variable>, Integer>();

		precEncodingToPropMap = new HashBijection<Triple<Operator<Variable>, Operator<Variable>, Integer>, Integer>();
		opEncodingPropMap = new HashBijection<Pair<Operator<Variable>, Integer>, Integer>();

		p = 1;
	}
	
	public Bijection<PrecedenceObj, Integer> getPrecedencePropositionMap() {
		return precToPropMap;
	}
	
	public Bijection<VariableAssignmentObj, Integer> getVarAssignmentPropositionMap() {
		return assToPropMap;
	}
	
	public Bijection<EqualityObj, Integer> getVarEqualityPropositionMap() {
		return eqToPropMap;
	}
	
	public Bijection<PcLink, Integer> getPcLinkPropositionMap() {
		return pclToPropMap;
	}
	
	public Bijection<Pair<PcLink, Threat>, Integer> getThreatToPropositionMap() {
		return threatToPropMap;
	}
	
	public int addOperatorIdxEncodingProp(Operator<Variable> op1, int k) {
		if (k < 1 || k > opEncodingBits)
			throw new IllegalArgumentException("Arg: " + k);

		Pair<Operator<Variable>, Integer> pair = Pair.instance(op1, k).intern();
		opEncodingPropMap.put(pair, p);
		//objsList.add(pair);
		return p++;
	}

	public Integer getOperatorIdxEncodingProp(Operator<Variable> op1, int k) {
		if (k < 1 || k > opEncodingBits)
			throw new IllegalArgumentException("Arg: " + k);

		return this.opEncodingPropMap.get(Pair.instance(op1, k));
	}

	public int addVariablePrecProp(Variable v1, Variable v2) {
		varPrecToPropMap.put(formatPair(v1, v2), p);
		//objsList.add(pair);
		return p++;
	}

	public int getVariablePrecProp(Variable v1, Variable v2) {
		return varPrecToPropMap.get(formatPair(v1, v2));
	}

	public int addPrecedenceProposition(Operator<Variable> op1, Operator<Variable> op2) {
		return addPrecedenceProposition(op1, op2, opEncodingBits);
	}

	public int addPrecedenceProposition(Operator<Variable> op1, Operator<Variable> op2, int k) {
		if (k < 1 || k > opEncodingBits)
			throw new IllegalArgumentException("Arg: " + k);

		if (k == opEncodingBits) {
			PrecedenceObj p1 = new PrecedenceObj(op1, op2).intern();
			precToPropMap.put(p1, p);
			//objsList.add(p1);
			return p++;
		}
		else {
			Triple<Operator<Variable>, Operator<Variable>, Integer> p1 = Triple.instance(op1, op2, k).intern();
			precEncodingToPropMap.put(p1, p);
			//objsList.add(p1);
			return p++;
		}
	}

	public Integer getPrecedenceProposition(Operator<Variable> op1, Operator<Variable> op2) {
		return getPrecedenceProposition(op1, op2, opEncodingBits);
	}

	public Integer getPrecedenceProposition(Operator<Variable> op1, Operator<Variable> op2, int k) {
		if (k < 1 || k > opEncodingBits)
			throw new IllegalArgumentException("Arg: " + k);

		if (k == opEncodingBits)
			return precToPropMap.get(new PrecedenceObj(op1, op2).intern());
		else
			return precEncodingToPropMap.get(Triple.instance(op1, op2, k).intern());
	}

	public int addEncodedObject(Object o) {
		//objsList.add(o);
		objPropMap.put(o, p);
		return p++;
	}
	
	public Integer getObjectEncoding(Object o) {
		return objPropMap.get(o);
	}

	public Object getEncodedObject(int i) {
		Object o = objPropMap.getKey(i);
		if (o != null)
			return o;
		o = this.assToPropMap.getKey(i);
		if (o != null)
			return o;
		
		o = this.eqToPropMap.getKey(i);
		if (o != null)
			return o;
		
		o = this.opEncodingPropMap.getKey(i);
		if (o != null)
			return o;
		
		o = this.pclToPropMap.getKey(i);
		if (o != null)
			return o;
		
		o = this.precToPropMap.getKey(i);
		if (o != null)
			return o;
		
		o = this.threatToPropMap.getKey(i);
		if (o != null)
			return o;
		
		o = this.varPrecToPropMap.getKey(i);
		if (o != null)
			return o;
		
		throw new IllegalArgumentException("Cannot find object for proposition " + i);
	}
	
	public PrecedenceObj getPrecedenceObj(int prop) {
		return this.precToPropMap.getKey(prop);
	}

	public int addVariableAssignmentProposition(Variable v, Constant c) {
		VariableAssignmentObj pair = new VariableAssignmentObj(v, c).intern();
		assToPropMap.put(pair, p);
		//objsList.add(pair);
		return p++;
	}

	public Integer getVariableAssignmentProposition(Variable v, Constant c) {
		return assToPropMap.get(new VariableAssignmentObj(v, c));
	}

	public VariableAssignmentObj getVariableAssignmentObj(int prop) {
		return assToPropMap.getKey(prop);
	}
	
	public int addEqualityProposition(Variable v1, Variable v2) {
		EqualityObj pair = formatEqualityObj(new EqualityObj(v1, v2).intern());
		eqToPropMap.put(pair, p);
		//objsList.add(pair);
		return p++;
	}

	public Integer getEqualityProposition(Variable v1, Variable v2) {
		return eqToPropMap.get(formatEqualityObj(new EqualityObj(v1, v2).intern()));
	}

	public EqualityObj getEqualityObj(int prop) {
		return eqToPropMap.getKey(prop);
	}

	private <T extends Symbol> Pair<T, T> formatPair(T v1, T v2) {
		int c = v1.getName().compareTo(v2.getName());
		if (c < 0)
			return Pair.instance(v1, v2).intern();
		else if (c > 0)
			return Pair.instance(v2, v1).intern();
		else
			throw new IllegalArgumentException(v1 + "," + v2);
	}

	private EqualityObj formatEqualityObj(EqualityObj obj) {
		int c = Comparators.SYMBOL_COMPARATOR.compare(obj.getFirst(), obj.getSecond());
		if (c < 0)
			return obj;
		else if (c > 0)
			return new EqualityObj(obj.getSecond(), obj.getFirst()).intern();
		else
			throw new IllegalArgumentException(obj.getFirst() + "," + obj.getSecond());
	}

	public int addProducerConsumerProposition(Producer prod, Consumer cons) {
		return addProducerConsumerProposition(new PcLink(prod, cons).intern());
	}

	public int addProducerConsumerProposition(PcLink pcLink) {
		pclToPropMap.put(pcLink, p);
		//objsList.add(pcLink);
		return p++;
	}

	public Integer getProducerConsumerProposition(Producer prod, Consumer cons) {
		return getProducerConsumerProposition(new PcLink(prod, cons).intern());
	}

	public Integer getProducerConsumerProposition(PcLink pcLink) {
		return pclToPropMap.get(pcLink);
	}

	public PcLink getProducerConsumerLink(int prop) {
		return pclToPropMap.getKey(prop);
	}
	
	public Set<PcLink> getAllPcLinks() {
		return pclToPropMap.keySet();
	}

	public int addThreatProposition(PcLink link, Threat threat) {
		Pair<PcLink, Threat> thr = Pair.instance(link, threat).intern();
		threatToPropMap.put(thr, p);
		//objsList.add(thr);
		return p++;
	}

	public Integer getThreatProposition(PcLink link, Threat threat) {
		Pair<PcLink, Threat> thr = Pair.instance(link, threat).intern();
		return threatToPropMap.get(thr);
	}

	public int getNPropositions() {
		return p-1;
	}
	
	public Plan decodeModel(int[] soln, PartialPlan<?> plan) {
		if (soln.length != getNPropositions())
			throw new IllegalArgumentException("Illegal solution length: " + soln.length + 
					", expected " + getNPropositions() + "\n" + Arrays.toString(soln));
		
		// ordering
		DirectedGraph<Operator<Variable>> precGraph = new DirectedGraph<Operator<Variable>>();

		// var bindings
		Map<Variable, Constant> subMap = new HashMap<Variable, Constant>();
		for (Variable var : plan.getInitAction().getVariables())
			subMap.put(var, plan.getInitSub().apply(var));
		for (Variable var : plan.getGoalAction().getVariables())
			subMap.put(var, plan.getGoalSub().apply(var));

		for (int prop : soln) {
			if (prop == 0)
				throw new IllegalArgumentException("Solution contains 0");

			PrecedenceObj prec = getPrecedenceObj(prop);		
			if (prec != null && prop > 0) {
				GraphUtils.addAndCloseTransitive(precGraph, prec.getFirst(), prec.getSecond());
			}

			VariableAssignmentObj vAssigment = getVariableAssignmentObj(prop);
			if (vAssigment != null && prop > 0) {	
				subMap.put(vAssigment.getFirst(), vAssigment.getSecond());
			}
			
		}
		
		// order and bind steps
		List<Operator<Variable>> ungroundSteps = GraphUtils.getLinearExtension(precGraph);
		Substitution<Constant> sub = new Substitution<Constant>(subMap);

		List<Operator<Constant>> planSteps = new ArrayList<Operator<Constant>>();
		for (Operator<Variable> step : ungroundSteps)	
			planSteps.add(step.applySubstitution(sub));

		return new Plan(plan.getProblem(), planSteps);
	}
	
	public Plan decodeGroundModel(int[] soln, PartialPlan<?> plan, Substitution<Constant> sub) {
		if (soln.length != getNPropositions())
			throw new IllegalArgumentException("Illegal solution length: " + soln.length + 
					", expected " + getNPropositions() + "\n" + Arrays.toString(soln));
		
		// ordering
		DirectedGraph<Operator<Variable>> precGraph = new DirectedGraph<Operator<Variable>>();

		for (int prop : soln) {
			if (prop == 0)
				throw new IllegalArgumentException("Solution contains 0");

			PrecedenceObj prec = getPrecedenceObj(prop);		
			if (prec != null && prop > 0)
				GraphUtils.addAndCloseTransitive(precGraph, prec.getFirst(), prec.getSecond());		
		}
		
		
		// order and bind steps
		List<Operator<Variable>> ungroundSteps = GraphUtils.getLinearExtension(precGraph);

		List<Operator<Constant>> planSteps = new ArrayList<Operator<Constant>>();
		for (Operator<Variable> step : ungroundSteps)	
			planSteps.add(step.applySubstitution(sub));

		return new Plan(plan.getProblem(), planSteps);
	}

}
