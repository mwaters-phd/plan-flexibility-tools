package au.rmit.agtgrp.pplib.fol.predicate;

import java.util.ArrayList;

import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;

public class Proposition extends Literal<Variable> {

	private static final long serialVersionUID = 1L;

	private static Atom<Variable> buildProposition(String symbol) {
		return new Atom<Variable>(new Predicate(symbol, new ArrayList<Type>()), new ArrayList<Variable>(), new ArrayList<Variable>()).intern();
	}
	
	public Proposition(String symbol, boolean value) {
		super(buildProposition(symbol), value);
	}

}
