package au.rmit.agtgrp.pplib.auto;

import java.util.Arrays;
import java.util.List;

public class NautyResult {
	
	private static double log2(double d) {
		// log2(12)=log10(12)/log10(2)
		return Math.log(d)/Math.log(2);
	}
	
	public Group generator;
	public List<int[]> orbits;
	public long groupSize;
	public int nVerts;
	
	public NautyResult(Group generator, List<int[]> orbits, long groupSize, int nVerts) {
		this.generator = generator;
		this.orbits = orbits;
		this.groupSize = groupSize;
		this.nVerts = nVerts;
	}	
	
	public double getGraphEntropy() {
		double ent = 0.0;
		for (int[] orbit : orbits) {		
			double nov = ((double) orbit.length)/nVerts;
			ent+=nov*log2(nov);
		}	
		return -ent;
	}
	
	public double getMowshowitzSymmetryIndex() {
		double symm = 0.0;
		for (int[] orbit : orbits)
			symm+=orbit.length * log2(orbit.length);
		
		symm/=nVerts;
		symm+=log2(groupSize);
		return symm;
	}
	
	public double getVertexSymmetryIndex() {
		double max = 0.0;
		for (int[] orbit : orbits) {
			if (orbit.length > max)
				max = orbit.length;
		}
		
		return max/nVerts;
	}
	
	public double getOrbitHomogeneityIndex() {
		double min = Double.MAX_VALUE;
		double max = 0.0;
		for (int[] orbit : orbits) {
			if (orbit.length > max)
				max = orbit.length;
			if (orbit.length < min)
				min = orbit.length;
		}	
		
		return min/max;
	}
	
	public double getOrbitDeviationIndex() {
		double max = 0.0;
		for (int[] orbit : orbits) {
			if (orbit.length > max)
				max = orbit.length;
		}
		
		double sum = 0.0;
		for (int[] orbit : orbits)
			sum+=orbit.length/max;
		
		return sum/orbits.size();
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Generator: " + generator.toString() + "\n");
		sb.append("Orbits: ");
		for (int[] orbit : orbits)
			sb.append(Arrays.toString(orbit) + " ");
		sb.append("\n");
		sb.append("Autos size: " + groupSize);
		
		return sb.toString();
	}
}