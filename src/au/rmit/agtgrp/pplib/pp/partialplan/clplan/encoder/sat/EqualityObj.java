package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat;

import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.Pair;

public class EqualityObj extends Pair<Variable, Variable> {

	private static final long serialVersionUID = 1L;
	
	public EqualityObj(Variable first, Variable second) {
		super(first, second);
	}
	
	public EqualityObj intern() {
		return Pair.getCached(this);
	}
	
	@Override
	public String toString() {
		return super.getFirst() + " = " + super.getSecond();
	}

}
