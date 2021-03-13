package au.rmit.agtgrp.pplib.pp.mktr.policy;

import au.rmit.agtgrp.pplib.pddl.pct.CausalStructure;
import au.rmit.agtgrp.pplib.pddl.pct.Consumer;
import au.rmit.agtgrp.pplib.pddl.pct.PcLink;
import au.rmit.agtgrp.pplib.pddl.pct.Producer;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.symm.CsSymmetry;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.symm.ClPlanAutomorphisms;

import java.util.*;

public class MinimiseThreatsMultiLex extends MinimiseThreats {

	private final Set<String> canonicalCache;
	private final MultiLex multiLex;
	private int idx = 0; // the idx of the last PcLink returned by getNextImpl()

	public MinimiseThreatsMultiLex(PcPlan pcoPlan, CausalStructure options) {
		super(pcoPlan, options);
		System.out.println("Getting PDG automorphisms");
		List<CsSymmetry> symms = ClPlanAutomorphisms.getCausalStructureSymmetries(pcoPlan, true);
		System.out.println("Building symmetry group");
		int i = 1;
		for (CsSymmetry symm : symms) {
			System.out.println("Symmetry " + i++);
			for (Producer prod : symm.getProducerDomain())
				System.out.println("\t" + prod + " -> " + symm.permute(prod));
			for (Consumer cons : symm.getConsumerDomain())
				System.out.println("\t" + cons + " -> " + symm.permute(cons));
		}

		canonicalCache = new HashSet<String>();
		multiLex = new MultiLex(symms, pcoPlan, options);
	}

	@Override
	protected void addedImpl(PcLink link) {
		super.addedImpl(link);
		idx = 0;
	}
	
	@Override
	protected PcLink getNextImpl() {	
		while (idx < super.options.size()) {
			PcLink next = super.options.get(idx);
			
			// get approximate canonical form
			super.current.addProducerConsumerOption(next);
			CausalStructure approxCanonical = multiLex.getApproximateCanonicalForm(super.current);
						
			// check if it has been tested already
			String s = multiLex.getStringForm(approxCanonical);
			boolean unexplored = canonicalCache.add(s);
			
			// do this after the check as maybe current == approxCanonical
			super.current.removeProducerConsumerOption(next);
						
			if (unexplored) {
				return next;
			} else {	
				System.out.println("\t\tMultiLex skipping " + next);
			}
			
			idx++; 
		}
		
		System.out.println("No remaining PcLinks have unexplored canonical forms");
		return null;
	}
	
}
