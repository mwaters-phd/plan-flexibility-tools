package au.rmit.agtgrp.pplib.pddl.pct;

import java.util.HashSet;
import java.util.Set;

import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedBipartiteGraph;

public class PcGraph extends DirectedBipartiteGraph<Producer, Consumer> {
	
	protected HashSet<PcLink> all = null;
	
	@Override
	public void addEdge(Producer source, Consumer dest) {
		super.addEdge(source, dest);
		all = null;
	}

	@Override
	public void removeEdge(Producer source, Consumer dest) {
		super.removeEdge(source, dest);
		all = null;
	}
	
	public Set<PcLink> getAllEdges() {
		if (all == null) {
			all = new HashSet<PcLink>();
			for (Producer source : linksFrom.keySet()) {
				for (Consumer dest : linksFrom.get(source))
					all.add(new PcLink(source, dest).intern());
			}
		}
		return all;
	}

	@Override
	public void clear() {
		super.clear();
		all.clear();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((all == null) ? 0 : all.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PcGraph other = (PcGraph) obj;
		if (all == null) {
			if (other.all != null)
				return false;
		} else if (!all.equals(other.all))
			return false;
		return true;
	}

	

}
