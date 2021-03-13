package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.Pair;

public class VariableAssignmentObj extends Pair<Variable, Constant>{

	private static final long serialVersionUID = 1L;

	public VariableAssignmentObj(Variable first, Constant second) {
		super(first, second);
	}

	public VariableAssignmentObj intern() {
		return Pair.getCached(this);
	}
	
	@Override
	public String toString() {
		return super.getFirst() + " = " + super.getSecond();
	}
	
}
