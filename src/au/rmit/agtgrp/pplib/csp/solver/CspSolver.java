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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import au.rmit.agtgrp.pplib.csp.Csp;
import au.rmit.agtgrp.pplib.csp.ExpressionCsp;
import au.rmit.agtgrp.pplib.csp.solver.output.CachedCspOutputSet;
import au.rmit.agtgrp.pplib.csp.solver.output.CspOutputSet;

public abstract class CspSolver {

	private static int COUNTER = 0;

	public static File TEMP_DIR = new File("temp");
	
	protected ZincFormatter zf;
	protected final File tempCspFile;

	protected ExpressionCsp csp;
	protected File cspFile;
	protected File solnsFile;

	protected boolean verbose;
	
	public CspSolver() {
		tempCspFile = new File(TEMP_DIR, "temp_csp.mzn");
		verbose = false;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	private File getInternalSolnsFile() {
		return new File(TEMP_DIR, "sols_" + COUNTER++ + ".subs");
	}

	public CspSolutionSet getSolutions() {
		if (csp == null)
			throw new IllegalStateException(
					"Can only get solutions when solver is called on an instance of " + Csp.class.getName());

		CspOutputSet outputIt = getSolverOutput();
		if (outputIt == null)
			return null;

		return new CspSolutionSet((CachedCspOutputSet) outputIt, zf.getVariablesInOrder(), zf.getObjectsByIndex());
	}

	public File getCspFile() {
		return cspFile;
	}

	public ExpressionCsp getCsp() {
		return csp;
	}

	public File getSolutionsFile() {
		return solnsFile;
	}

	public void getAllSolutions(File cspFile) throws InterruptedException {
		getAllSolutions(cspFile, getInternalSolnsFile());
	}

	public void getAllSolutions(File cspFile, File solnsFile) throws InterruptedException {
		solve(cspFile, 0, solnsFile, 0, 0);
	}

	public void getNSolutions(File cspFile, int n) throws InterruptedException {
		getNSolutions(cspFile, n, getInternalSolnsFile(), 0);
	}
	
	public void getNSolutions(File cspFile, int n, long timeout) throws InterruptedException {
		getNSolutions(cspFile, n, getInternalSolnsFile(), timeout);
	}

	public void getNSolutions(File cspFile, int n, File solnsFile, long timeout) throws InterruptedException {
		solve(cspFile, n, solnsFile, timeout, 0);
	}

	public void countSolutions(File cspFile) throws InterruptedException {
		countSolutions(cspFile, 0);
	}

	public void countSolutions(File cspFile, long timeout) throws InterruptedException {
		solve(cspFile, 0, null, timeout, 0);
	}

	public void getSolutions(ExpressionCsp csp, long timeout) throws InterruptedException {
		getSolutions(csp, getInternalSolnsFile(), timeout);
	}

	public void getSolutions(ExpressionCsp csp, File solnsFile, long timeout) throws InterruptedException {
		solve(csp, 0, solnsFile, timeout, 0);
	}

	public void getAllSolutions(ExpressionCsp csp) throws InterruptedException {
		getAllSolutions(csp, getInternalSolnsFile());
	}

	public void getAllSolutions(ExpressionCsp csp, File solnsFile) throws InterruptedException {
		solve(csp, 0, solnsFile, 0, 0);
	}

	public void getNSolutions(ExpressionCsp csp, int n) throws InterruptedException {
		solve(csp, n, getInternalSolnsFile(), 0, 0);
	}
	
	public void getNSolutions(ExpressionCsp csp, int n, long timeout) throws InterruptedException {
		solve(csp, n, getInternalSolnsFile(), timeout, 0);
	}

	public void getNSolutions(ExpressionCsp csp, int n, File solnsFile, long timeout) throws InterruptedException {
		solve(csp, n, solnsFile, timeout, 0);
	}

	public void countSolutions(ExpressionCsp csp) throws InterruptedException {
		solve(csp, 0, null, 0, 0);
	}

	public void countSolutions(ExpressionCsp csp, long timeout) throws InterruptedException {
		solve(csp, 0, null, timeout, 0);
	}

	public void solve(ExpressionCsp csp, int nsols, File solnsFile, long timeout, int nthreads) throws InterruptedException {

		this.csp = csp;
		this.solnsFile = solnsFile;
		this.cspFile = tempCspFile;
		zf = new ZincFormatter(csp);

		if (tempCspFile.exists())
			tempCspFile.delete();

		try (BufferedWriter writer = Files.newBufferedWriter(tempCspFile.toPath())) {
			writer.write(zf.getZincString());
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		// run CSP solver
		execute(tempCspFile, nsols, solnsFile, timeout, nthreads);

	}

	public void solve(File cspFile, int nsols, File solnsFile, long timeout, int nthreads) throws InterruptedException {

		csp = null;
		this.cspFile = cspFile;
		this.solnsFile = solnsFile;
		zf = null;

		// run CSP solver
		execute(cspFile, nsols, solnsFile, timeout, nthreads);

	}

	protected abstract void execute(File cspFile, int nsols, File solnsFile, long timeout, int nthreads) throws InterruptedException;

	public abstract void cancel();
	
	public abstract int getSolutionCount();

	public abstract double getRuntime();

	public abstract boolean timedOut();
	
	public abstract boolean satisfiable();

	public abstract CspOutputSet getSolverOutput();

	public abstract void setPrintStream(PrintStream out);
	
}
