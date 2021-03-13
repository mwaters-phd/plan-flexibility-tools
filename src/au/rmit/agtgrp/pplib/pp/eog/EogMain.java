package au.rmit.agtgrp.pplib.pp.eog;

import java.io.File;
import au.rmit.agtgrp.pplib.utils.PddlCmdLineOptions;
import org.kohsuke.args4j.Option;

import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pp.mrr.MrrMain;
import au.rmit.agtgrp.pplib.pp.mrr.MrrMain.MrrResult;
import au.rmit.agtgrp.pplib.pp.mrr.MrrMain.OptAlgorithm;
import au.rmit.agtgrp.pplib.pp.mrr.PopModel;
import au.rmit.agtgrp.pplib.sat.solver.SatSolver.SatSolverResult;
import au.rmit.agtgrp.pplib.utils.FileUtils;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;

public class EogMain {

	public static void main(String[] args) {

		EogOptions options = new EogOptions();
		options.parse(args);

		if (options.outFile != null) {
			long start = System.currentTimeMillis();
			long timeLimit = options.time * 60 * 1000;

			Plan plan = options.getPlan();		

			MrrResult result = new MrrResult();
			result.encTime = timeLimit;
			result.algorithm = OptAlgorithm.EOG;
			result.acycl = OptAlgorithm.EOG.acyc;
			result.asymm = OptAlgorithm.EOG.asymm;
			result.maxSatResult = SatSolverResult.TIMEOUT;
			result.domainName = new File(options.domainFile.getParent()).getName();
			result.problemName = options.problemFile.getName().split(".pddl")[0];
			result.planFileName = options.planFile.getName();
			result.relSize = -1;
			result.flex = -1;

			FileUtils.writeFile(options.outFile, result.toString());

			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						long elapsed = System.currentTimeMillis() - start;
						Thread.sleep(timeLimit - elapsed);
					} catch (InterruptedException e) {
						//no worries
					}
					System.out.println("EOG timed out!");
					System.exit(1);
				}});
			t.start();

			Eog eog = new Eog(plan);
			DirectedGraph<Operator<Variable>> precGraph = eog.getExplanationBasedOrderGeneralisation();

			int relSize = precGraph.getAllEdges().size();
			System.out.println("Order relation size: " + relSize);

			double flex = MrrMain.getFlex(relSize, plan.length()-2); // ignore edges to and from goal and init	
			System.out.println("Flex: " + flex);

			result.encTime = System.currentTimeMillis() - start;
			result.relSize = relSize;
			result.flex = flex;
			result.maxSatResult = SatSolverResult.OPTIMAL;

			FileUtils.writeFile(options.outFile, result.toString());
			
			PopModel pm = new PopModel(options.getPlan().getPlanSteps(), precGraph, options.getPlan().getSubstitution().getMap());
			System.out.println(pm.toString());
		}
		
		
		System.exit(0); // just in case threads are running?
		
	}


	private static class EogOptions extends PddlCmdLineOptions {

		@Option(name = "--out-file", usage = "out file")
		private File outFile;

		@Option(name = "--time", usage = "time limit in minutes", metaVar = "OPT")
		private long time = 30;
		
		@Option(name = "--cnf", usage = "CNF file", metaVar = "OPT")
		private File cnfFile;
		

	}

}
