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
package au.rmit.agtgrp.pplib.pp.mktr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import au.rmit.agtgrp.pplib.utils.FileUtils;
import au.rmit.agtgrp.pplib.utils.PddlCmdLineOptions;
import au.rmit.agtgrp.pplib.utils.collections.graph.treewidth.TreewidthCalculator;
import org.kohsuke.args4j.Option;

import au.rmit.agtgrp.pplib.csp.ExpressionCsp;
import au.rmit.agtgrp.pplib.csp.solver.CspSolver;
import au.rmit.agtgrp.pplib.csp.solver.ZincFormatter;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.PddlProblem.PlanResult;
import au.rmit.agtgrp.pplib.pp.mktr.policy.RelaxationPolicyException;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp.CspEncoderOptions;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp.PcToCspEncoder;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp.PcToCspEncoderException;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp.CspEncoderOptions.ThreatRestriction;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.optimiser.CspOptimiserException;
import au.rmit.agtgrp.pplib.pp.partialplan.planset.PlanSet;

public class MktrMain {

	private static final String TIMEOUT = "TIMEOUT";
	private static final String MINIMAL = "MINIMAL";
	private static final String ERROR = "ERROR";

	private static MktrOptions options;

	public static void main(String[] args) {

		// parse command line
		options = new MktrOptions();
		options.parse(args);

		// make required directories
		if (options.count || options.verbose)
			options.tempDir.mkdirs();
		options.outDir.mkdirs();
		CspSolver.TEMP_DIR = options.tempDir;
		
		try {	
			// get plan
			Plan plan = options.getPlan();
			
			// prepare result file
			MktrResult result = new MktrResult();
			result.policyName = options.policyName;
			result.treewidth = options.treewidth;
			result.maxTime = TimeUnit.MINUTES.toMillis(options.mktrTime);
			result.result = ERROR; // for now
			result.domainName = plan.getDomain().getName();
			result.problemFileName = plan.getProblem().getName();
			result.planFileName = options.planFile.getName();
			FileUtils.writeFile(options.getOutCsvFile(), result.toString());
			
			// load constraint encoder
			CspEncoderOptions cspOpts = new CspEncoderOptions(
					options.cspThreatRestriction, options.switchVars, 
					options.optimise, options.verbose,
					options.treewidth, new TreewidthCalculator());

			PcToCspEncoder encoder = new PcToCspEncoder(cspOpts);

			long t = System.currentTimeMillis();
			// run mktr
			Mktr mktr = new Mktr(plan, encoder,
					options.policyName, options.treewidth,
					options.mktrTime, options.validate, options.verbose);

			mktr.relax();

			t = System.currentTimeMillis() - t;

			// get results
			result.initialSize = mktr.getInitialSize();
			result.finalSize = mktr.getFinalSize();
			result.numTested = mktr.getNumPcLinksTested();
			result.maxSize = mktr.getMaxSize();
			result.mktrTime = t;
			result.result = mktr.timedOut() ? TIMEOUT : MINIMAL;

			// write results file
			System.out.println("Writing results to " + options.getOutCsvFile());
			FileUtils.writeFile(options.getOutCsvFile(), result.toString());

			if (result.finalSize == result.initialSize) {
				System.out.println("MKTR added no causal links.");
				return;
			} 	
			
			ExpressionCsp csp = mktr.getFinalCsp();
			// write final CSP
			System.out.println("Writing final CSP to " + options.getOutCspFile());
			writeCsp(csp, options.getOutCspFile());
			
			// count/validate instantiations
			if (options.count) {
				System.out.println("Counting instantiations of final CSP");
				PlanSet plans = mktr.getPlans();
				System.out.println(plans.getPlanCount() + " plans");
				
				if (options.validate) {
					System.out.println("Validating instantiations");
					PlanResult validationResult = plan.getProblem().validateAll(plans);
					if (!validationResult.isValid)
						throw new RuntimeException("\nInvalid plan found!\n" + validationResult.message);
				}
			}
			
		}			
		catch (RelaxationPolicyException | PcToCspEncoderException  e) {
			System.err.println(e.getMessage());
			if (options.verbose)
				e.printStackTrace();
			System.exit(1);
		}
		catch (CspOptimiserException e) {
			System.err.println("Unexpected error while optimising constraints");
			e.printStackTrace();
			System.exit(1);
		}
		catch (Exception e) {
			System.err.println("Unexpected error");
			e.printStackTrace();
			System.exit(1);
		}

	}
	
	
	private static void writeCsp(ExpressionCsp csp, File output) {		
		try (BufferedWriter writer = Files.newBufferedWriter(output.toPath())) {
			output = output.getAbsoluteFile();
			output.getParentFile().mkdirs();
			ZincFormatter zf = new ZincFormatter(csp);
			writer.write(zf.getZincString());		
		} catch (IOException e) {
			System.err.println("Error writing CSP to " + output + ": " + e.getMessage());
			if (options.verbose)
				e.printStackTrace();
			System.exit(1);
		}
	}



	public static class MktrResult {

		public static String header = "domain_name, problem_name, plan_file, policy_name, treewidth, time_limit, mktr_time, "
				+ "init_size, final_size, n_tested, max_size, result";

		public String domainName;
		public String problemFileName;
		public String planFileName;
		public String policyName;
		public int treewidth;
		public long maxTime = -1;
		public long mktrTime;
		public long initialSize = -1;
		public long finalSize = -1;
		public long numTested = -1;
		public long maxSize = -1;
		public String result = "";

		@Override
		public String toString() {
			String csv = header + "\n";
			csv+=domainName +",";
			csv+=problemFileName + ",";
			csv+=planFileName + ",";
			csv+=policyName+ ",";
			csv+=treewidth+ ",";
			csv+=maxTime+ ",";
			csv+=mktrTime+ ",";
			csv+=initialSize+ ",";
			csv+=finalSize+",";
			csv+=numTested+ ",";
			csv+=maxSize+ ",";
			csv+=result+",";

			csv+="\n";
			return csv;
		}	
	}


	private static class MktrOptions extends PddlCmdLineOptions {

		@Option(name = "--temp", usage = "temp directory")
		private File tempDir = new File("./temp");

		@Option(name = "--out", usage = "output directory")
		private File outDir = new File("./");

		@Option(name = "--tw", usage = "maximum treewidth", required = true)
		private int treewidth;

		@Option(name = "--policy", usage = "relaxation policy", required = true)
		private String policyName;

		@Option(name = "--mktr-time", usage = "time limit (in minutes) for running MKTR, or < 0 for no limit")
		private int mktrTime = -1;

		@Option(name = "--optimise", usage = "Optimise CSP")
		private boolean optimise = true;

		@Option(name = "--validate", usage = "validate plans", metaVar = "OPT")
		private boolean validate;

		@Option(name = "--count", usage = "count plans", metaVar = "OPT")
		private boolean count;

		@Option(name = "--switch-vars", usage = "CSP switching vars option", metaVar = "OPT")
		private boolean switchVars = false;

		@Option(name = "--threat-restriction", usage = "CSP threat restriction option")
		private ThreatRestriction cspThreatRestriction = ThreatRestriction.BINDING;

		
		public File getOutCspFile() {
			return new File(this.outDir, "mktr-constraints.mzn");
		}

		public File getOutCsvFile() {
			return new File(this.outDir, "mktr-results.csv");
		}

	}

	private MktrMain() { }

}
