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

import au.rmit.agtgrp.pplib.pddl.pct.CausalStructure;
import au.rmit.agtgrp.pplib.pddl.pct.PcLink;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;

public class Random extends RelaxationPolicy {

	public Random(PcPlan pcoPlan, CausalStructure options) {
		super(pcoPlan, options);
		Collections.shuffle(super.options);
	}

	@Override
	protected PcLink getNextImpl() {
		return options.get(0);
	}

}
