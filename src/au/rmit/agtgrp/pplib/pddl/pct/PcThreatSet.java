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
package au.rmit.agtgrp.pplib.pddl.pct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PcThreatSet {

	private final Map<Threat, Set<PcLink>> threatLinksMap;
	private final Map<PcLink, Set<Threat>> linkThreatsMap;

	public PcThreatSet() {
		linkThreatsMap = new HashMap<PcLink, Set<Threat>>();
		threatLinksMap = new HashMap<Threat, Set<PcLink>>();
	}

	public Set<Threat> getThreatsToLink(PcLink link) {
		Set<Threat> threats = linkThreatsMap.get(link);
		if (threats == null) {
			threats = new HashSet<Threat>();
			linkThreatsMap.put(link, threats);
		}

		return threats;

	}

	public Set<PcLink> getLinksThreatenedByProducer(Threat prod) {
		Set<PcLink> threatened = threatLinksMap.get(prod);
		if (threatened == null) {
			threatened = new HashSet<PcLink>();
			threatLinksMap.put(prod, threatened);
		}

		return threatened;
	}

	public void addThreat(PcLink link, Threat threat) {
		Set<Threat> threats = linkThreatsMap.get(link);
		if (threats == null) {
			threats = new HashSet<Threat>();
			linkThreatsMap.put(link, threats);
		}

		threats.add(threat);

		Set<PcLink> threatened = threatLinksMap.get(threat);
		if (threatened == null) {
			threatened = new HashSet<PcLink>();
			threatLinksMap.put(threat, threatened);
		}

		threatened.add(link);

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Threat threat : threatLinksMap.keySet()) {
			sb.append(threat + " != { ");
			Iterator<PcLink> it = threatLinksMap.get(threat).iterator();
			while (it.hasNext()) {
				PcLink l = it.next();
				sb.append("<" + l.getProducer() + "," + l.getConsumer() + ">");
				if (it.hasNext())
					sb.append(", ");
			}

			sb.append(" }\n");

		}

		return sb.toString();
	}

}
