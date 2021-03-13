package au.rmit.agtgrp.pplib.auto;

import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class Permutation {

	public static Permutation getId(int n) {
		int[] mapping = new int[n];
		for (int i = 0; i < n; i++)
			mapping[i] = i;

		return new Permutation(mapping);
	}

	public static Permutation buildFromCycleFormat(int n, int[] ... cycles) {
		int[] mapping = new int[n];
		for (int i = 0; i < n; i++)
			mapping[i] = i;

		boolean[] found = new boolean[n];
		for (int[] cycle : cycles) {
			for (int i = 0; i  < cycle.length; i++) {
				if (cycle[i] >= n)
					throw new IllegalArgumentException("Must be in the range 0 to " + (mapping.length-1) + ": " + formatCycles(cycles));
				if (found[cycle[i]])
					throw new IllegalArgumentException("Duplicate var " + cycle[i] + ": " + formatCycles(cycles));

				mapping[cycle[i]] = cycle[(i+1)%cycle.length];
				found[cycle[i]] = true;
			}
		}

		return new Permutation(mapping);
	}

	private static String formatCycles(int[] ... cycles) {
		StringBuilder sb = new StringBuilder();
		for (int[] cycle : cycles) {
			sb.append("(" + FormattingUtils.arrayToString(cycle, " ") + ")");
		}
		return sb.toString();
	}

	private final int[] mapping;

	public Permutation(int[] mapping) {
		boolean[] found = new boolean[mapping.length];
		for (int i : mapping) {
			if (i < 0 || i >= mapping.length)
				throw new IllegalArgumentException("Must be in the range 0 to " + (mapping.length-1) + ": " + FormattingUtils.arrayToString(mapping));
			found[i] = true;
		}

		for (boolean b : found) {
			if (!b)
				throw new IllegalArgumentException("Not a bijection: " + FormattingUtils.arrayToString(mapping));
		}

		this.mapping = mapping;
	}

	public int getDomainSize() {
		return mapping.length;
	}

	public int apply(int i) {
		return mapping[i];
	}	

	public Permutation apply(Permutation p) {
		if (p.mapping.length != this.mapping.length) 
			throw new IllegalArgumentException("Size mismatch, this: " + this.mapping.length + ", other: " + p.mapping.length);

		int[] permuted = new int[mapping.length];
		for (int i = 0; i < mapping.length; i++)
			permuted[i] = mapping[p.mapping[i]];

		return new Permutation(permuted);
	}

	public int[] getMapping() {
		return mapping;
	}

	public int[][] getCycles() {
		List<int[]> cycles = new ArrayList<int[]>();
		boolean[] found = new boolean[mapping.length];
		for (int i = 0; i < mapping.length; i++) {
			if (found[i])
				continue;

			found[i] = true;
			List<Integer> cycle = new ArrayList<Integer>();
			cycle.add(i);
			int next = mapping[i];
			while (next != i) {
				found[next] = true;
				cycle.add(next);
				next = mapping[next];
			}

			if (cycle.size() > 1) {
				int[] cyclArr = new int[cycle.size()];
				for (int j = 0; j < cycle.size(); j++)
					cyclArr[j] = cycle.get(j);
				cycles.add(cyclArr);
			}
		}

		int[][] cyclesArr = new int[cycles.size()][];
		for (int j = 0; j < cycles.size(); j++)
			cyclesArr[j] = cycles.get(j);

		return cyclesArr;

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int[] cycle : getCycles()) {
			sb.append("(");
			sb.append(FormattingUtils.arrayToString(cycle, " "));
			sb.append(")");
		}
		return sb.toString();
	}

}
