package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat;

import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp.CspEncoderOptions.ThreatRestriction;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;

public class CnfEncoderOptions {

	public enum AsymmetryOpt {
		NONE, OP_TYPES, STRUCT, OPVAL, INIT_STATE
	}

	public enum AcyclicityOpt {
		ATOM, BINARY
	}
	
	public enum EqualityOpt {
		NONE, ATOM, IDX
	}
	
	public enum CausalStructureOpt {
		REORDER, DEORDER, CUSTOM
	}
	
	public enum OutputOpt {
		TOTAL_ORDER, PARTIAL_ORDER
	}
	
	public final boolean verbose;	
	public final AsymmetryOpt asymmOpt;
	public final AcyclicityOpt acyclOpt;
	public final EqualityOpt equalityOpt;
	public final CausalStructureOpt csOpt;
	public final ThreatRestriction threatRest;
	public final OutputOpt outOpt;
	public final long optTime;
	public final boolean optTransClosure;
	public final DirectedGraph<Operator<Variable>> customPrecGraph;
	
	public CnfEncoderOptions(AsymmetryOpt asymm, EqualityOpt equality, AcyclicityOpt acycl, CausalStructureOpt csOpt, ThreatRestriction threatRest,
			OutputOpt outOpt, long optTime, boolean optTransClosure, boolean verbose, DirectedGraph<Operator<Variable>> customPrecGraph) {
		this.verbose = verbose;
		this.asymmOpt = asymm;
		this.acyclOpt = acycl;
		this.csOpt = csOpt;
		this.outOpt = outOpt;
		this.threatRest = threatRest;
		this.optTime = optTime;
		this.optTransClosure = optTransClosure;	
		this.equalityOpt = equality;
		this.customPrecGraph = customPrecGraph;
	}
	
	
	public CnfEncoderOptions(AsymmetryOpt asymm, AcyclicityOpt acyc, long optTime, boolean verbose) {
		this(asymm, EqualityOpt.IDX, acyc, CausalStructureOpt.REORDER, ThreatRestriction.NONE, OutputOpt.PARTIAL_ORDER, optTime, true, verbose, null);
	}
	
	
	public CnfEncoderOptions(AsymmetryOpt asymm, AcyclicityOpt acyc, boolean verbose) {
		this(asymm, EqualityOpt.IDX, acyc, CausalStructureOpt.REORDER, ThreatRestriction.NONE, OutputOpt.PARTIAL_ORDER, 0, true, verbose, null);
	}
	
}
