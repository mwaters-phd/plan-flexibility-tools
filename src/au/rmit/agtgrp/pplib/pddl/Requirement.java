package au.rmit.agtgrp.pplib.pddl;

public enum Requirement {

	STRIPS ("strips"),
	ADL ("adl"),
	EQUALITY ("equality"),
	TYPING ("typing"),
	NEGATIVE_PRECONDITIONS ("negative-preconditions");
	
	private final String pddlString;
	
	private Requirement(String pddlString) {
		this.pddlString = pddlString;
	}
	
	public String getPddlString() {
		return pddlString;
	}
}
