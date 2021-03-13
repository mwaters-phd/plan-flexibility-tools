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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import au.rmit.agtgrp.pplib.pddl.pct.*;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;

public class MinimiseThreats extends RelaxationPolicy {

	
	public MinimiseThreats(PcPlan pcoPlan, CausalStructure options) {
		super(pcoPlan, options);
		ThreatMap threats = ThreatMap.getThreatMap(pcoPlan.getPlanSteps());
		Map<PcLink, Integer> threatCounts = new HashMap<PcLink, Integer>();

		for (PcLink pcl : super.options) {
			threatCounts.put(pcl, threats.getNonGroundThreats(pcl).size());
		}
		
		Comparator<PcLink> comp = new Comparator<PcLink>() {
			@Override
			public int compare(PcLink o1, PcLink o2) {
				int c = Integer.compare(threatCounts.get(o1), threatCounts.get(o2));
				if (c == 0)
					c = MinimiseThreats.super.planOrder.compare(o1, o2);

				return c;
			}		
		};
		
		Collections.sort(super.options, comp);
	}

	@Override
	protected PcLink getNextImpl() {
		return super.options.get(0);
	}
}
