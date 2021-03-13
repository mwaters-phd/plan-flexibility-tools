/*******************************************************************************
 * MKTR - Minimal k-Treewidth Relaxation
 *
 * Copyright (C) 2018 
 * Max Waters (max.waters@rmit.edu.au)
 * RMIT University, Melbourne VIC 3000
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package au.rmit.agtgrp.pplib.pp.mktr.policy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.rmit.agtgrp.pplib.pddl.pct.*;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.symm.CsSymmetry;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.symm.ClPlanAutomorphisms;

public class RelaxProducersMultiLex extends RelaxProducers {


	private final Set<String> canonicalCache;
	private final MultiLex multiLex;
	private int idx = 0; // the idx of the last PcLink returned by getNextImpl()

	public RelaxProducersMultiLex(PcPlan pcoPlan, CausalStructure options) {
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
			super.current.removeProducerConsumerOption(next);	
			
			// check if it has been tested already
			boolean unexplored = canonicalCache.add(multiLex.getStringForm(approxCanonical));
			if (unexplored) {
				return next;
			}
			
			idx++; 
		}
		
		System.out.println("No remaining PcLinks satisfy MultiLex");
		return null;
	}


}
