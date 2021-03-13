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
package au.rmit.agtgrp.pplib.csp.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.rmit.agtgrp.pplib.csp.solver.output.CachedCspOutputSet;
import au.rmit.agtgrp.pplib.csp.solver.output.CspOutputWriter;
import au.rmit.agtgrp.pplib.utils.FileLinesIterator;
import au.rmit.agtgrp.pplib.utils.NullPrintStream;

public class GeCodeInterface extends CspSolver {

	public final String fznGecodeExec = "fzn-gecode";
	public final String mzn2fzn = "mzn2fzn";

	private int nSolutions;
	private double runtime;
	private boolean timedOut;
	private boolean satisfiable;

	private volatile Process process;
	private volatile Thread thread;

	private PrintStream out;

	public GeCodeInterface() {
		out = NullPrintStream.INSTANCE;
	}


	@Override
	public void setPrintStream(PrintStream out) {
		this.out = out;
	}

	@Override
	public CachedCspOutputSet getSolverOutput() {
		if (solnsFile == null)
			return null;

		return new CachedCspOutputSet(solnsFile, nSolutions);
	}

	@Override
	public int getSolutionCount() {
		return nSolutions;
	}

	@Override
	public double getRuntime() {
		return runtime;
	}

	@Override
	public boolean timedOut() {
		return timedOut;
	}

	@Override
	public boolean satisfiable() {
		return satisfiable;
	}

	@Override
	public synchronized void cancel() {
		if (process != null) { // if process == null, nothing is running, so ignore
			process.destroy();
			if (thread != null)
				thread.interrupt();
		}
	}

	public void execute(File mzn, int nsols, File output, long timeout, int nthreads) throws InterruptedException {

		// this is a short-running process, always execute in its entirety even if cancelled
		synchronized(this) {
			mzn2fzn(mzn);
		}

		if (Thread.interrupted()) {
			Thread.currentThread().interrupt();
			throw new InterruptedException();
		}

		File fzn = new File(mzn.getParent(), mzn.getName().substring(0, mzn.getName().lastIndexOf(".")) + ".fzn");
		fzngecode(fzn, nsols, output, timeout, nthreads);

	}


	private void mzn2fzn(File csp) throws InterruptedException {
		try {		
			thread = Thread.currentThread();
			
			ProcessBuilder pb = new ProcessBuilder(mzn2fzn, csp.getAbsolutePath());
			process = pb.start();
			process.waitFor();

		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		process = null;
		thread = null;

	}

	private void fzngecode(File fzn, int nsols, File output, long timeout, int nthreads) throws InterruptedException {
		if (timeout < 0)
			timeout = 0;

		try {

			BufferedReader stdInput = null;
			CspOutputWriter cspOutputWriter = null;

			// prevent cancellation while process is being initialised
			synchronized(this) {
				
				// clean up from previous
				nSolutions = 0;
				runtime = -1;
				timedOut = false;
				satisfiable = true;


				if (solnsFile != null) {
					cspOutputWriter = new CspOutputWriter(solnsFile);
					solnsFile.mkdirs();
				}
				if (nsols < 0)
					nsols = 0;

				ProcessBuilder pb = new ProcessBuilder(fznGecodeExec,
						"-p", Integer.toString(nthreads), 
						"-n", Integer.toString(nsols), 
						"-time", Long.toString(timeout),
						"-s", fzn.getAbsolutePath());

				pb.redirectErrorStream(true);

				process = pb.start();
				stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			}

			boolean printingSolutions = true;

			String s = null;
			Map<String, Short> soln = new HashMap<String, Short>();

			File oznFile = new File(fzn.getParent(), fzn.getName().substring(0, fzn.getName().lastIndexOf(".")) + ".ozn");
			Ozn ozn = new Ozn(oznFile);

			while ((s = stdInput.readLine()) != null) { // if cancelled, this will be null

				synchronized(this) {
					if (Thread.interrupted()) {
						Thread.currentThread().interrupt();
						throw new InterruptedException();
					}

					out.println(s);

					if (s.contains("UNSATISFIABLE"))
						satisfiable = false;

					// gecode prints ==== when all solutions are found.
					// if limit is reached, it goes straight to stats.
					if (s.startsWith("%%") || s.startsWith("===="))
						printingSolutions = false;

					// solutions are being printed and solutions are being saved
					if (printingSolutions && solnsFile != null && !s.isEmpty()) {

						// format is for each line is
						// p_1 = 1;
						// p_10 = 3;

						if (s.contains("----")) {
							nSolutions++;
							for (String var : ozn.vars) {
								var = ozn.getEquivalentVar(var);
								if (soln.containsKey(var))
									cspOutputWriter.writeValue(soln.get(var));
								else if (ozn.variableVals.containsKey(var))
									cspOutputWriter.writeValue(ozn.variableVals.get(var));
								else
									throw new RuntimeException("Cannot find value for variable: " + var);
							}

							cspOutputWriter.endSolution();
							soln.clear();

						} else {
							String[] split = s.split("=");

							short val = Short.valueOf(split[1].substring(0, split[1].length() - 1).trim());
							String var = split[0].trim();
							soln.put(var, val);
						}
					}

					if (s.contains("solutions="))
						nSolutions = Integer.valueOf(s.substring(s.indexOf("solutions=") + "solutions=".length()).trim());
					else if (s.contains("runtime"))
						runtime = Double.valueOf(s.substring(s.indexOf("(") + 1, s.indexOf(" ms")));
				}
			}

			synchronized(this) {
				
				process = null;
				thread = null;
				
				if (solnsFile != null)
					cspOutputWriter.end();

				timedOut = timeout > 0 && runtime > timeout;

				if (Thread.interrupted()) {
					Thread.currentThread().interrupt();
					throw new InterruptedException();
				}		
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private class Ozn {

		List<String> vars;
		Map<String, String> equivalentVars;
		Map<String, Short> variableVals;

		public Ozn(File oznFile) {
			vars = new ArrayList<String>();
			equivalentVars = new HashMap<String, String>();
			variableVals = new HashMap<String, Short>();

			FileLinesIterator lines = new FileLinesIterator(oznFile);

			while (lines.hasNext()) {
				String line = lines.next();

				if (line.startsWith("int:")) {
					if (line.contains("=")) {
						String var = line.substring(4, line.indexOf("=")).trim();
						String val = line.substring(line.indexOf("=") + 1, line.length() - 1).trim();

						try { // v_1 = 3
							variableVals.put(var, Short.parseShort(val));
						} catch (NumberFormatException e) { // v_1 = v_2
							equivalentVars.put(var, val);
						}

						vars.add(var);
					} else {
						String var = line.substring(4, line.length() - 1).trim();
						vars.add(var);
					}
				}

			}
		}

		public String getEquivalentVar(String var) {

			while (equivalentVars.containsKey(var))
				var = equivalentVars.get(var);

			return var;

		}
	}

}
