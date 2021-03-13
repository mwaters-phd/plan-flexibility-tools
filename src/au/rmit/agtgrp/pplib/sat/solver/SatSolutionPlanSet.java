package au.rmit.agtgrp.pplib.sat.solver;

import java.util.Iterator;

import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pp.partialplan.InstantiatablePartialPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.PropositionMap;
import au.rmit.agtgrp.pplib.pp.partialplan.planset.PlanSet;
import au.rmit.agtgrp.pplib.sat.SatFormula;

public class SatSolutionPlanSet implements PlanSet {

	private final SatSolutionSet satSolutions;
	private final PropositionMap propMap;
	private final InstantiatablePartialPlan<SatFormula> partialPlan;
	
	public SatSolutionPlanSet(InstantiatablePartialPlan<SatFormula> partialPlan, SatSolutionSet satSolutions, PropositionMap propMap) {
		this.partialPlan = partialPlan;
		this.satSolutions = satSolutions;
		this.propMap = propMap;	
	}

	@Override
	public Iterator<Plan> iterator() {
		return new SatSolutionPlanSetIterator(satSolutions.iterator());
	}

	@Override
	public int getPlanCount() {
		return satSolutions.getSolutionCount();
	}
		
	private class SatSolutionPlanSetIterator implements Iterator<Plan> {

		private final Iterator<int[]> satSolIterator;
			
		public SatSolutionPlanSetIterator(Iterator<int[]> satSolIterator) {
			this.satSolIterator = satSolIterator;
		}

		@Override
		public boolean hasNext() {
			return satSolIterator.hasNext();
		}

		@Override
		public Plan next() {
			return propMap.decodeModel(satSolIterator.next(), partialPlan);
		}
		
	}

}
