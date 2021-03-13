package au.rmit.agtgrp.pplib.pp.mrr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.utils.collections.Pair;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;

public class PopModel {

	public final List<Operator<Variable>> steps;
	public final DirectedGraph<Operator<Variable>> precGraph;
	public final Map<Variable, Constant> bindings;
	
	
	public PopModel(List<Operator<Variable>> steps, DirectedGraph<Operator<Variable>> precGraph, Map<Variable, Constant> bindings) {
		this.steps = steps;
		this.precGraph = precGraph;
		this.bindings = bindings;
	}
	
	@Override
	public String toString() {
		StringBuilder popSb = new StringBuilder();
		
		popSb.append("** Operators\n");
		for (Operator<Variable> op : this.steps)
			popSb.append(op.formatParameters() + "\n");

		popSb.append("** Ordering\n");
		List<Pair<Operator<Variable>, Operator<Variable>>> edges = new ArrayList<Pair<Operator<Variable>, Operator<Variable>>>(precGraph.getAllEdges());
		edges.sort(new Comparator<Pair<Operator<Variable>, Operator<Variable>>>() {
			@Override
			public int compare(Pair<Operator<Variable>, Operator<Variable>> o1,
					Pair<Operator<Variable>, Operator<Variable>> o2) {

				int c = o1.getFirst().getName().compareTo(o2.getFirst().getName());
				if (c == 0)
					c = o1.getSecond().getName().compareTo(o2.getSecond().getName());
				return c;
			}
		});

		for (Pair<Operator<Variable>, Operator<Variable>> edge : edges)
			popSb.append(edge.getFirst().getName() + " < " + edge.getSecond().getName() + "\n");

		popSb.append("** Binding\n");
		for (Operator<Variable> op : this.steps) {
			for (Variable var : op.getVariables())
				popSb.append(var.getName() + "=" + bindings.get(var).getName() + "\n");
		}
		
		return popSb.toString();
	}
	
	
}
