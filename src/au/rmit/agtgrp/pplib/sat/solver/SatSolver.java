package au.rmit.agtgrp.pplib.sat.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.sat.SatFormula;
import au.rmit.agtgrp.pplib.sat.WeightedSatFormula;

public class SatSolver {

	public enum SatSolverResult {
		SATISFIABLE, UNSATISFIABLE, OPTIMAL, TIMEOUT, ERROR
	}
	
	public static File TEMP_DIR = new File("temp");
	
	private SatSolutionSet solutionSet;
	private SatSolverResult result;

	public SatSolutionSet getSolutions() {
		return solutionSet;
	}
	
	public SatSolverResult getResult() {
		return result;
	}
	
	public void solve(SatFormula sat, int n, long timeout) {
		solve(sat, n, timeout, false);
	}
	
	public void solve(SatFormula sat, int n, long timeout, boolean verbose) {

		solutionSet = null;
		result = null;
		
		if (sat instanceof WeightedSatFormula)
			solveWeighted((WeightedSatFormula) sat, timeout, verbose);
		else if (n == 1)
			solveUnweighted(sat, timeout, verbose);
		else
			solveUnweighted(sat, n, timeout, verbose);
			
	}
	
	public void solveUnweighted(SatFormula sat, int n, long timeout, boolean verbose) {
		
		if (!TEMP_DIR.exists())
			TEMP_DIR.mkdirs();
		
		try {
			File cnfFile = new File(TEMP_DIR, "maple-temp.cnf");
			sat.writeToFileRandomAccess(cnfFile);

			File solnsFile = new File(TEMP_DIR, "maple-temp-solns.cnf");

			timeout = timeout/1000; // to seconds
			if (timeout > Integer.MAX_VALUE || timeout < 1)
				timeout = Integer.MAX_VALUE;
			
			ProcessBuilder pb = new ProcessBuilder(
					"./sat-scripts/sat-solver.sh",
					cnfFile.getAbsolutePath(), 
					solnsFile.getAbsolutePath(), 
					Integer.toString(n < 1 ? Integer.MAX_VALUE : n),
					Long.toString(timeout));
			Process process = pb.start();

			List<int[]> solns = new ArrayList<int[]>();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String s = null;
			while ((s = reader.readLine()) != null) {	
				if (verbose)
					System.out.println(s);
				result = SatSolverResult.SATISFIABLE;
				solns.add(SatFormula.parse(s));
			}

			solutionSet = new InMemorySatSolutionSet(solns);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void solveUnweighted(SatFormula sat, long timeout, boolean verbose) {
		
		try {	
			timeout = timeout/1000; // to seconds
			if (timeout > Integer.MAX_VALUE || timeout < 1)
				timeout = Integer.MAX_VALUE;
			
			ProcessBuilder pb = new ProcessBuilder(
					"maple-lrb", "-cpu-lim=" + timeout);
			Process process = pb.start();

			sat.writeToStream(process.getOutputStream());
			process.getOutputStream().close();
			
			List<int[]> solns = new ArrayList<int[]>();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String s = null;
			while ((s = reader.readLine()) != null) {
				if (verbose)
					System.out.println(s);
				
				if (s.startsWith("v ")) {
					result = SatSolverResult.SATISFIABLE;
					solns.add(SatFormula.parse(s));
				} 
				else if (s.startsWith("s UNSATISFIABLE"))
					result = SatSolverResult.UNSATISFIABLE;
				else if (s.contains("INTERRUPTED"))
					result = SatSolverResult.TIMEOUT;
				else if (s.contains("ERROR"))
					result = SatSolverResult.ERROR;		
			}
			solutionSet = new InMemorySatSolutionSet(solns);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	

	public void solveWeighted(WeightedSatFormula sat, long timeout, boolean verbose) {
		try {
			int seconds = (int) (timeout/1000);
			
			ProcessBuilder pb = new ProcessBuilder(
					"loandra", 
					"-cpu-lim=" + seconds, 
					"-printM"
				//	"-algorithm=3" // uses too much memory!
					);
			Process process = pb.start();
			pb.redirectErrorStream(true);
			
			sat.writeToStream(process.getOutputStream());
			process.getOutputStream().close();
			
			List<int[]> solns = new ArrayList<int[]>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String s = null;
			while ((s = reader.readLine()) != null) {
				if (verbose)
					System.out.println(s);
				
				sb.append(s + "\n");
				if (s.startsWith("s OPTIMUM")) 
					result = SatSolverResult.OPTIMAL;
				
				if (s.startsWith("v ")) {
					if (!SatSolverResult.OPTIMAL.equals(result))
						result = SatSolverResult.SATISFIABLE;
					int[] soln = SatFormula.parse(s);
					if (soln.length != sat.getNumProps()) {
						System.err.println("Solution length = " + soln.length);
						System.err.println("Expected length = " + sat.getNumProps());
						System.err.println("Writing CNF to temp/loandra_error_dump.cnf");
						sat.writeToFileRandomAccess(new File("temp/loandra_error_dump.cnf"));
						result = SatSolverResult.ERROR;
					}
					solns.add(soln);
				} 
				else if (s.startsWith("s UNSATISFIABLE"))
					result = SatSolverResult.UNSATISFIABLE;
				else if (s.contains("UNKNOWN"))
					result = SatSolverResult.TIMEOUT;
				else if (s.contains("ERROR"))
					result = SatSolverResult.ERROR;	
			}
			
			if (result == SatSolverResult.ERROR)
				throw new RuntimeException(sb.toString());
			
			solutionSet = new InMemorySatSolutionSet(solns);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
