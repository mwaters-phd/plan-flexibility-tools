package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp;

import au.rmit.agtgrp.pplib.utils.collections.graph.treewidth.TreewidthCalculator;

public class CspEncoderOptions {

	public enum ThreatRestriction {
		BINDING, ORDERING, NONE
	}
	
	public final boolean optimise; 
	public final boolean verbose;
	public final ThreatRestriction threatRestriction;
	public final int targetTreewidth;
	public final TreewidthCalculator twCalc;
	public final boolean switchingVars;

	public CspEncoderOptions(ThreatRestriction threatRestriction, boolean switchVars, boolean optimise, boolean verbose) {
		this(threatRestriction, switchVars, optimise, verbose, -1, null);
	}

	public CspEncoderOptions(ThreatRestriction threatRestriction, boolean switchVars, boolean optimise, boolean verbose, int targetTreewidth, TreewidthCalculator twCalc) {
		this.switchingVars = switchVars;
		this.optimise = optimise;
		this.verbose = verbose;
		this.threatRestriction = threatRestriction;
		this.targetTreewidth = targetTreewidth;
		this.twCalc = twCalc;
	}
	
}
