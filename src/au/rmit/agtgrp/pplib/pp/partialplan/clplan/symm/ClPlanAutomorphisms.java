package au.rmit.agtgrp.pplib.pp.partialplan.clplan.symm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import au.rmit.agtgrp.pplib.auto.Group;
import au.rmit.agtgrp.pplib.auto.NautyInterface;
import au.rmit.agtgrp.pplib.auto.NautyResult;
import au.rmit.agtgrp.pplib.auto.Permutation;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.pct.Consumer;
import au.rmit.agtgrp.pplib.pddl.pct.Producer;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.utils.collections.Bijection;
import au.rmit.agtgrp.pplib.utils.collections.HashBijection;
import au.rmit.agtgrp.pplib.utils.collections.graph.UndirectedColouredGraph;

public class ClPlanAutomorphisms {


	public static ClPlanAutomorphisms getPdgAutomorphisms(PcPlan plan, boolean verbose) {

		UndirectedColouredGraph<Integer, Integer> graph = new UndirectedColouredGraph<Integer, Integer>();

		// colours
		int c = 0;

		Map<Type, Integer> constTypeColours = new HashMap<Type, Integer>();
		Map<Type, Integer> varTypeColours = new HashMap<Type, Integer>();
		for (Type t : plan.getDomain().getTypes()) {
			constTypeColours.put(t, c++);
			varTypeColours.put(t, c++);
		}
		Map<Boolean, Map<Predicate, Integer>> predColours = new HashMap<Boolean, Map<Predicate, Integer>>();
		predColours.put(true, new HashMap<Predicate, Integer>());
		predColours.put(false, new HashMap<Predicate, Integer>());

		for (Predicate p : plan.getDomain().getPredicates()) {
			predColours.get(true).put(p, c++);
			predColours.get(false).put(p, c++);	
		}

		Map<String, Integer> opTypeColours = new HashMap<String, Integer>();
		for (Operator<Variable> op : plan.getOperators()) {
			String type = op.getName().substring(op.getName().indexOf("_") + 1);
			opTypeColours.put(type, c++);
		}

		// vertices
		int v = 0;
		Map<Constant, Integer> constVerts = new HashMap<Constant, Integer>();
		for (Constant con : plan.getDomain().getConstants()) {
			graph.addVertex(v, constTypeColours.get(con.getType()));
			constVerts.put(con, v++);
		}
		for (Constant con :plan.getProblem().getObjects()) {
			graph.addVertex(v, constTypeColours.get(con.getType()));
			constVerts.put(con, v++);
		}

		Map<Variable, Integer> varVerts = new HashMap<Variable, Integer>();
		for (Operator<Variable> op : plan.getOperators()) {
			if (op.equals(plan.getInitAction()) || op.equals(plan.getGoalAction()))
				continue;

			for (Variable var : op.getVariables()) {
				graph.addVertex(v, varTypeColours.get(var.getType()));
				varVerts.put(var, v++);
			}
		}

		Map<Operator<Variable>, Integer> opVerts = new HashMap<Operator<Variable>, Integer>();
		for (Operator<Variable> op : plan.getOperators()) {
			String type = op.getName().substring(op.getName().indexOf("_") + 1);
			graph.addVertex(v, opTypeColours.get(type));
			opVerts.put(op, v++);
		}

		Map<Producer, List<Integer>> prodVerts = new HashMap<Producer, List<Integer>>();
		for (Literal<Variable> lit : plan.getInitAction().getPostconditions()) {
			List<Integer> verts = new ArrayList<Integer>();
			int col = predColours.get(lit.getValue()).get(lit.getAtom().getSymbol());
			for (int i = 0; i < lit.getAtom().getSymbol().getArity()+1; i++) {
				graph.addVertex(v, col);
				verts.add(v++);
			}
			prodVerts.put(new Producer(plan.getInitAction(), lit), verts);
		}

		Map<Consumer, List<Integer>> consVerts = new HashMap<Consumer, List<Integer>>();
		for (Literal<Variable> lit : plan.getGoalAction().getPreconditions()) {
			List<Integer> verts = new ArrayList<Integer>();
			int col = predColours.get(lit.getValue()).get(lit.getAtom().getSymbol());
			for (int i = 0; i < lit.getAtom().getSymbol().getArity()+1; i++) { // one for lit and one for each parameter
				graph.addVertex(v, col);
				verts.add(v++);
			}
			consVerts.put(new Consumer(plan.getGoalAction(), lit), verts);
		}

		// edges
		for (Operator<Variable> op : plan.getOperators()) {
			if (op.equals(plan.getInitAction()) || op.equals(plan.getGoalAction())) {
				List<Literal<Variable>> lits = op.equals(plan.getInitAction()) ? op.getPostconditions() : op.getPreconditions();
				for (Literal<Variable> lit : lits) {

					List<Integer> paramVerts = op.equals(plan.getInitAction()) ? prodVerts.get(new Producer(plan.getInitAction(), lit)) : consVerts.get(new Consumer(plan.getGoalAction(), lit));
					int opVert = opVerts.get(op);
					int preVert = paramVerts.get(0);
					graph.addEdge(opVert, preVert); // from op to literal

					for (int i = 0; i < lit.getAtom().getSymbol().getArity(); i++) { // from lit to first param and on
						int paramVert = paramVerts.get(i + 1);
						graph.addEdge(paramVert, preVert);
						preVert = paramVert;

						int constVert = constVerts.get(plan.getOriginalSub().apply(lit.getAtom().getVariables().get(i)));
						graph.addEdge(paramVert, constVert);
					}
				}
			} else {
				int preVert = opVerts.get(op);
				for (Variable var : op.getVariables()) {
					int varVert = varVerts.get(var);
					graph.addEdge(preVert, varVert);
					preVert = varVert;
				}
			}
		}

		NautyResult result = NautyInterface.getAutomorphisms(graph, verbose);

		Map<Integer, Producer> prodVert = new HashMap<Integer, Producer>();
		for (Producer prod : prodVerts.keySet())
			prodVert.put(prodVerts.get(prod).get(0), prod);

		Map<Integer, Consumer> consVert = new HashMap<Integer, Consumer>();
		for (Consumer cons : consVerts.keySet())
			consVert.put(consVerts.get(cons).get(0), cons);

		return new ClPlanAutomorphisms(result, prodVert, consVert);
	}


	public static List<CsSymmetry> getCausalStructureSymmetries(PcPlan plan, boolean verbose) {

		// get autos for initial state and goal
		ClPlanAutomorphisms pdgAutos = getPdgAutomorphisms(plan, verbose);

		List<CsSymmetry> symms = pdgAutos.getCausalStructureSymmetries();

		// get operator type cs symmetries
		Map<String, List<Operator<Variable>>> opsByType = new HashMap<String, List<Operator<Variable>>>();
		for (Operator<Variable> op : plan.getPlanSteps()) {
			String type = op.getName();
			type = type.substring(type.indexOf("_")+1);
			List<Operator<Variable>> ops = opsByType.get(type);
			if (ops == null) {
				ops = new ArrayList<Operator<Variable>>();
				opsByType.put(type, ops);
			}
			ops.add(op);
		}

		for (String opType : opsByType.keySet()) {
			List<Operator<Variable>> ops = opsByType.get(opType);
			for (int i = 0; i < ops.size()-1; i++) {
				Operator<Variable> op1 = ops.get(i);
				Operator<Variable> op2 = ops.get(i+1);

				Map<Producer, Producer> producerPerm = new HashMap<Producer, Producer>();
				for (int j = 0; j < op1.getPostconditions().size(); j++) {
					Producer prod1 = new Producer(op1, op1.getPostconditions().get(j)).intern();
					Producer prod2 = new Producer(op2, op2.getPostconditions().get(j)).intern();
					producerPerm.put(prod1, prod2);
					producerPerm.put(prod2, prod1);
				}

				Map<Consumer, Consumer> consumerPerm = new HashMap<Consumer, Consumer>();
				for (int j = 0; j < op1.getPreconditions().size(); j++) {
					Consumer cons1 = new Consumer(op1, op1.getPreconditions().get(j)).intern();
					Consumer cons2 = new Consumer(op2, op2.getPreconditions().get(j)).intern();
					consumerPerm.put(cons1, cons2);
					consumerPerm.put(cons2, cons1);
				}

				if (CsSymmetry.isCsSymmetry(producerPerm, consumerPerm)) {
					CsSymmetry symm = new CsSymmetry(producerPerm, consumerPerm);
					symms.add(symm);
				}
			}
		}

		return symms;
	}


	private final NautyResult result;
	private final Map<Integer, Producer> vertexProdMap;
	private final Map<Integer, Consumer> vertexConsMap;


	private ClPlanAutomorphisms(NautyResult result, Map<Integer, Producer> vertexProdMap, Map<Integer,Consumer> vertexConsMap) {
		this.result = result;
		this.vertexProdMap = vertexProdMap;
		this.vertexConsMap = vertexConsMap;
	}

	public NautyResult getNautyResult() {
		return result;
	}

	public Group getGroup() {
		return result.generator;
	}


	public Map<Integer, Producer> getVertexProdMap() {
		return vertexProdMap;
	}

	public Map<Integer, Consumer> getVertexConsMap() {
		return vertexConsMap;
	}


	public List<CsSymmetry> getCausalStructureSymmetries() {
		List<CsSymmetry> symms = new ArrayList<CsSymmetry>();
		for (Permutation perm : result.generator.getPermutations()) {
			Map<Producer, Producer> producerPerm = new HashMap<Producer, Producer>();
			Bijection<Consumer, Consumer> consumerPerm = new HashBijection<Consumer, Consumer>();
			for (int i = 0; i < perm.getDomainSize(); i++){
				if (i != perm.apply(i)) {
					if (vertexProdMap.containsKey(i)) {
						producerPerm.put(vertexProdMap.get(i), vertexProdMap.get(perm.apply(i)));
						producerPerm.put(vertexProdMap.get(perm.apply(i)), vertexProdMap.get(i));
					} else if (vertexConsMap.containsKey(i)) {
						consumerPerm.put(vertexConsMap.get(i), vertexConsMap.get(perm.apply(i)));
						consumerPerm.put(vertexConsMap.get(perm.apply(i)), vertexConsMap.get(i));
					}
				}
			}
			if (!producerPerm.isEmpty() || !consumerPerm.isEmpty()) {
				symms.add(new CsSymmetry(producerPerm, consumerPerm));
			}
		}
		return symms;
	}

}
