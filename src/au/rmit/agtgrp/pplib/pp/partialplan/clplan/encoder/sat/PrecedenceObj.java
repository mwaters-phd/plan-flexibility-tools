package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat;

import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.utils.collections.Pair;

public class PrecedenceObj extends Pair<Operator<Variable>, Operator<Variable>>{

	private static final long serialVersionUID = 1L;

	public PrecedenceObj(Operator<Variable> first, Operator<Variable> second) {
		super(first, second);
	}
	
	public PrecedenceObj intern() {
		return Pair.getCached(this);
	}
	
	@Override
	public String toString() {
		return super.getFirst().formatParameters() + " < " + super.getSecond().formatParameters();
	}

}
