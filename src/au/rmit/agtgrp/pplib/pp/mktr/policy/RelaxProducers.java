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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Term;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.pct.*;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;

public class RelaxProducers extends RelaxationPolicy {

	public RelaxProducers(PcPlan pcoPlan, CausalStructure options) {
		super(pcoPlan, options);

		// op -> n operators with precons linked to op's postcons
		Map<String, Integer> consumerCount = new HashMap<String, Integer>();

		for (Operator<Variable> op : planSteps) {
			Set<Operator<? extends Term>> consOps = new HashSet<Operator<? extends Term>>();

			for (Literal<Variable> prod : op.getPostconditions()) {
				for (Consumer cons : current.getConsumers(op, prod))
					consOps.add(cons.operator);

			}

			consumerCount.put(op.getName(), consOps.size());

		}

		
		// op -> highest cc of any op that threatens a causal link to op's precons
		Map<String, Integer> maxThreatConsCount = new HashMap<String, Integer>();
		
		ThreatMap threats = ThreatMap.getThreatMap(pcoPlan.getPlanSteps());
		for (Operator<Variable> op : planSteps) {
			int max = 0;
			for (Literal<Variable> pre : op.getPreconditions()) {
				Consumer cons = new Consumer(op, pre).intern();
				for (Producer prod : current.getProducers(cons)) {
					for (Threat threat : threats.getGroundThreats(new PcLink(prod, cons).intern(), pcoPlan.getOriginalSub()))
						max = Math.max(consumerCount.get(threat.operator.getName()), max);
				}
			}

			maxThreatConsCount.put(op.getName(), max);
		}

		
		
		Comparator<PcLink> comp = new Comparator<PcLink>() {
			@Override
			public int compare(PcLink o1, PcLink o2) {

				int cc1 = consumerCount.get(o1.getConsumer().operator.getName());
				int cc2 = consumerCount.get(o2.getConsumer().operator.getName());

				int tb1 = maxThreatConsCount.get(o1.getConsumer().operator.getName());
				int tb2 = maxThreatConsCount.get(o2.getConsumer().operator.getName());

				int c = -Integer.compare(Math.max(tb1, cc1), Math.max(tb2, cc2));

				if (c == 0)
					c = RelaxProducers.super.planOrder.compare(o1, o2);

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
