package au.rmit.agtgrp.pplib.pp.eog;

import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.pct.CausalStructure;
import au.rmit.agtgrp.pplib.pddl.pct.CausalStructureFactory;
import au.rmit.agtgrp.pplib.pddl.pct.PcLink;
import au.rmit.agtgrp.pplib.pddl.pct.Threat;
import au.rmit.agtgrp.pplib.pddl.pct.ThreatMap;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;
import au.rmit.agtgrp.pplib.utils.collections.graph.GraphUtils;

public class Eog {

	private final Plan plan;
	
	public Eog(Plan plan) {
		this.plan = plan;
	}

	
	public DirectedGraph<Operator<Variable>> getExplanationBasedOrderGeneralisation() {
		
		
		CausalStructure constraints = CausalStructureFactory.getEquivalentPcoConstraints(plan);
		ThreatMap threats = ThreatMap.getThreatMap(plan.getPlanSteps());
		
		// pc links
		DirectedGraph<Operator<Variable>> precGraph = new DirectedGraph<Operator<Variable>>();
		
		// threats
		for (PcLink link : constraints.getAllPcLinks()) {
			if (!link.getProducer().operator.equals(plan.getInitialAction()) && 
				!link.getConsumer().operator.equals(plan.getGoalAction()))
				GraphUtils.addAndCloseTransitive(precGraph, link.getProducer().operator, link.getConsumer().operator);	
			
			for (Threat threat : threats.getGroundThreats(link, plan.getSubstitution())) {					
				Operator<Variable> threatOp = threat.operator;
				if (plan.getPlanSteps().indexOf(threatOp) == plan.getPlanSteps().indexOf(link.getConsumer().operator)) {
					continue;
				}
				
				if (plan.getPlanSteps().indexOf(threatOp) < plan.getPlanSteps().indexOf(link.getProducer().operator)) {
					if (!threatOp.equals(plan.getInitialAction()) && !link.getProducer().operator.equals(plan.getGoalAction()))		
						GraphUtils.addAndCloseTransitive(precGraph, threatOp, link.getProducer().operator);
				}
				else if (plan.getPlanSteps().indexOf(threatOp) > plan.getPlanSteps().indexOf(link.getConsumer().operator)){
					if (!link.getConsumer().operator.equals(plan.getInitialAction()) && !threatOp.equals(plan.getGoalAction()))
						GraphUtils.addAndCloseTransitive(precGraph, link.getConsumer().operator, threatOp);
				}	
			}
		}
	
		return precGraph;		
	}
	
}
