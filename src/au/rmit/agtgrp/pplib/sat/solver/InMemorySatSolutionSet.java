package au.rmit.agtgrp.pplib.sat.solver;

import java.util.Iterator;
import java.util.List;

public class InMemorySatSolutionSet extends SatSolutionSet {

	private final List<int[]> solns;
	
	public InMemorySatSolutionSet(List<int[]> solns) {
		this.solns = solns;
	}

	@Override
	public Iterator<int[]> iterator() {
		return solns.iterator();
	}

	@Override
	public int getSolutionCount() {
		return solns.size();
	}

}
