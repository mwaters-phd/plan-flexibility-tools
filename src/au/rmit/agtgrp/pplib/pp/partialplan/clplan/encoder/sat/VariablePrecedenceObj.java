package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat;

import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.Pair;

public class VariablePrecedenceObj extends Pair<Variable, Variable>{

	private static final long serialVersionUID = 1L;

	public VariablePrecedenceObj(Variable first, Variable second) {
		super(first, second);
	}
	
	public VariablePrecedenceObj intern() {
		return Pair.getCached(this);
	}

}
