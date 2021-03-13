package au.rmit.agtgrp.pplib.auto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import au.rmit.agtgrp.pplib.utils.FormattingUtils;
import au.rmit.agtgrp.pplib.utils.collections.graph.UndirectedColouredGraph;

public class NautyInterface {

	public static NautyResult getAutomorphisms(UndirectedColouredGraph<Integer, Integer> graph, boolean verbose) {

		try {
			List<Integer> vertices = new ArrayList<Integer>(graph.getVertices());
			Collections.sort(vertices);
			if (vertices.get(0) != 0 || vertices.get(vertices.size()-1) != vertices.size()-1)
				throw new IllegalArgumentException("Vertices must be from 0 to " + (vertices.size()-1) + ": " + FormattingUtils.toString(vertices));


			Map<Integer, List<Integer>> vertsByColour = new HashMap<Integer, List<Integer>>();
			for (int vertex : vertices) {
				int colour = graph.getColour(vertex);
				List<Integer> verts = vertsByColour.get(colour);
				if (verts == null) {
					verts = new ArrayList<Integer>();
					vertsByColour.put(colour, verts);
				}
				verts.add(vertex);
			}

			ProcessBuilder pb = new ProcessBuilder("dreadnaut");
			pb.redirectErrorStream(true);
			Process process = pb.start();
			ProcessListener  pl = new ProcessListener(process, verbose);
			Thread t = new Thread(pl);
			t.start();

			BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));		
			wr.write("n=" + vertices.size() + " g\n");

			for (int i = 0; i < graph.getVertices().size(); i++) {
				StringBuilder sb = new StringBuilder();
				for (int dest : graph.getLinksFrom(i))
					sb.append(dest + " ");
				sb.append(";\n");
				wr.write(sb.toString());
			}

			StringBuilder sb = new StringBuilder();
			sb.append("f=[");
			for (int col : graph.getColours()) {			
				for (int vert : vertsByColour.get(col))
					sb.append(vert + " ");
				sb.append("|");
			}
			sb.append("]\n");
			wr.write(sb.toString());
			wr.write("x\n o\n q\n"); // execute, orbits, quit
			wr.flush(); // close?

			process.waitFor(); // needed?
			pl.waitFor();

			List<Permutation> permutations = new ArrayList<Permutation>();
			for (int[][] cycles : pl.generators)
				permutations.add(Permutation.buildFromCycleFormat(graph.getVertices().size(), cycles));

			return new NautyResult(new Group(permutations), pl.orbits, pl.grpSize, graph.getVertices().size());

		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}


	private static class ProcessListener implements Runnable {
		private final boolean verbose;
		private final Process process;
		private List<int[][]> generators;
		private List<int[]> orbits;
		private long grpSize;

		private final Semaphore sem;

		public ProcessListener(Process process, boolean verbose) {
			this.process = process;
			this.verbose = verbose;
			sem = new Semaphore(0);
		}

		public void waitFor() throws InterruptedException {
			sem.acquire();
		}

		@Override
		public void run() {	
			try {

				BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

				generators = new ArrayList<int[][]>();					
				String perm = "";

				String line = null;
				while((line = br.readLine()) != null && !line.contains("grpsize=")) {
					// generating set					
					if (verbose)
						System.out.println(line);

					if (line.startsWith("(")) { // start of new permutation string
						if (!perm.isEmpty()) { // is there a previous one					
							generators.add(toCycles(perm));	
							perm = "";
						}						
						perm+=line;							
					}
					else if (line.startsWith(" ")) { // continuation of a permutation string
						line=line.trim();
						if (line.startsWith("("))
							perm+=line;
						else
							perm+=" " + line;
					} 
				}

				//final one
				if (!perm.isEmpty()) {
					generators.add(toCycles(perm));	
				}

				if (verbose)
					System.out.println(line);					
				grpSize = grpSize(line);

				StringBuilder sb = new StringBuilder();
				while((line = br.readLine()) != null) {
					if (verbose)
						System.out.println(line);

					if (line.contains("cpu"))
						continue;

					sb.append(line.trim() + " ");
				}

				orbits = toOrbits(sb.toString());

			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			sem.release();
		}

		private int[][] toCycles(String perm) {
			perm = perm.substring(1, perm.length()-1);
			String[] split = perm.split("\\)\\(");
			int[][] cycles = new int[split.length][];
			for (int i = 0; i < split.length; i++) {
				String[] intStrs = split[i].split(" ");
				cycles[i] = new int[intStrs.length];
				for (int j = 0; j < intStrs.length; j++)
					cycles[i][j] = Integer.valueOf(intStrs[j]);
			}

			return cycles;
		}

		private long grpSize(String line) {
			// eg, "1 orbit; grpsize=24; 3 gens; 10 nodes; maxlev=4"
			String str = line.split("grpsize=")[1].split(";")[0];
			return Double.valueOf(str).longValue();
		}

		private List<int[]> toOrbits(String line) {			
			List<int[]> orbits = new ArrayList<int[]>();
			for (String orbStr : line.trim().split(";")) {
				// remove (n) at the end
				if (orbStr.contains("(")) {
					orbStr = orbStr.substring(0, orbStr.indexOf("("));
				}

				List<Integer> nums = new ArrayList<Integer>();
				for (String numStr : orbStr.trim().split(" ")) {
					if (numStr.contains(":")) {
						String[] hiLo = numStr.trim().split(":");
						int low = Integer.valueOf(hiLo[0].trim());
						int high = Integer.valueOf(hiLo[1].trim());

						for (int i = low; i <= high; i++)
							nums.add(i);

					} else {
						nums.add(Integer.valueOf(numStr));
					}
				}

				int[] orbit = new int[nums.size()];
				for (int i = 0; i < orbit.length; i++)
					orbit[i] = nums.get(i);

				orbits.add(orbit);
			}

			return orbits;
		}
	}

}
