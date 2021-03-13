package au.rmit.agtgrp.pplib.pp.mrr;

import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.CnfEncoderOptions;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.ClToSatEncoder;
import au.rmit.agtgrp.pplib.sat.WeightedSatFormula;

public class MrrWcnfEncoder extends ClToSatEncoder {

	public MrrWcnfEncoder(CnfEncoderOptions options) {
		super(options);
	}
	
	@Override
	public WeightedSatFormula encodeConstraints(PcPlan plan) {
		super.encodeConstraints(plan);
		return (WeightedSatFormula) super.satFormula;
	}
	
	@Override
	protected void encode() {
		super.encode();
		buildSoftOrderingConstraints();
	}
	
	@Override
	protected WeightedSatFormula initSatFormula() {
		return new WeightedSatFormula(Integer.MAX_VALUE);
	}

	private void buildSoftOrderingConstraints() {
		WeightedSatFormula weightedSat = (WeightedSatFormula) super.satFormula;
		for (int i = 0; i < plan.getPlanSteps().size(); i++) {
			Operator<Variable> op1 = plan.getPlanSteps().get(i);
			for (int j = 0; j < plan.getPlanSteps().size(); j++) {
				Operator<Variable> op2 = plan.getPlanSteps().get(j);

				if (i == j || propMap.getPrecedenceProposition(op1, op2) == null)
					continue;

				int p12 = propMap.getPrecedenceProposition(op1, op2);
				weightedSat.addWeightedClause(1, -p12);

			}
		}
	}
}
