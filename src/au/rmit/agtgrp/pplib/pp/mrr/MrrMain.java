package au.rmit.agtgrp.pplib.pp.mrr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import au.rmit.agtgrp.pplib.utils.PddlCmdLineOptions;
import org.kohsuke.args4j.Option;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.ParallelPlan;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.pct.CausalStructureFactory;
import au.rmit.agtgrp.pplib.pp.eog.Eog;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp.CspEncoderOptions.ThreatRestriction;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.CnfEncoderOptions;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.EqualityObj;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.PrecedenceObj;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.CnfEncoderOptions.AcyclicityOpt;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.CnfEncoderOptions.AsymmetryOpt;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.CnfEncoderOptions.CausalStructureOpt;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.CnfEncoderOptions.EqualityOpt;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat.CnfEncoderOptions.OutputOpt;
import au.rmit.agtgrp.pplib.sat.SatFormula;
import au.rmit.agtgrp.pplib.sat.WeightedSatFormula;
import au.rmit.agtgrp.pplib.sat.solver.SatSolver.SatSolverResult;
import au.rmit.agtgrp.pplib.utils.FileUtils;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;
import au.rmit.agtgrp.pplib.utils.collections.Bijection;
import au.rmit.agtgrp.pplib.utils.collections.graph.DirectedGraph;
import au.rmit.agtgrp.pplib.utils.collections.graph.GraphUtils;

public class MrrMain {

	public static void main(String[] args) throws IOException, InterruptedException {
		MrrOptions options = new MrrOptions();
		options.parse(args);
		if (options.decode)
			decode(options);
		else
			encode(options);
	}

	private static void encode(MrrOptions options) throws IOException {

		long start = System.currentTimeMillis();
		long time = TimeUnit.SECONDS.toMillis((long) (options.time * 60));

		MrrResult result = new MrrResult();
		result.domainName = new File(options.domainFile.getParent()).getName();
		result.problemName = options.problemFile.getName().split(".pddl")[0];
		result.planFileName = options.planFile.getName();
		result.algorithm = options.algorithm;
		result.acycl = options.algorithm.acyc;
		result.asymm = options.algorithm.asymm;
		result.maxTime = time;

		System.out.println("Loading PDDL");
		Plan plan = options.getPlan();		

		System.out.println("Lifting input plan");
		PcPlan pcoPlan = CausalStructureFactory.getMinimalPcoPlan(plan, !options.algorithm.csOpt.equals(CausalStructureOpt.REORDER), options.algorithm.ground);

		System.out.println("Encoding WCNF");
		DirectedGraph<Operator<Variable>> customPrecGraph = null;
		if (options.algorithm.equals(OptAlgorithm.REOG)) {
			customPrecGraph = new Eog(plan).getExplanationBasedOrderGeneralisation();
		} else if (plan instanceof ParallelPlan && !options.algorithm.csOpt.equals(CausalStructureOpt.REORDER)) {
			System.out.println("Computing parallel plan ordering");
			options.algorithm.csOpt = CausalStructureOpt.CUSTOM;
			customPrecGraph = getParallelPlanOrdering((ParallelPlan) plan);	
		}

		CnfEncoderOptions opts = new CnfEncoderOptions(options.algorithm.asymm, options.algorithm.eq,
				options.algorithm.acyc, options.algorithm.csOpt, ThreatRestriction.NONE, OutputOpt.PARTIAL_ORDER, 0,
				options.algorithm.optTransClosure, options.verbose, customPrecGraph);	
		MrrWcnfEncoder enc = new MrrWcnfEncoder(opts);	
		WeightedSatFormula wcnf = enc.encodeConstraints(pcoPlan);

		// save stats etc
		result.nProps = wcnf.getNumProps();
		result.nClauses = wcnf.getNumClauses();
		result.nSymmProps = enc.getNumSymmetryProps();
		result.nSymmClauses = enc.getNumSymmetryClauses();
		result.encTime = System.currentTimeMillis() - start;
		System.out.println("Encoding time: " + FormattingUtils.DF_3.format(((double) result.encTime)/1000));

		if (result.encTime > time) {
			System.out.println("Timed out after encoding");
			result.maxSatResult = SatSolverResult.TIMEOUT;
			FileUtils.writeFile(options.outFile, result.toString());
			return;
		}

		Map<PrecedenceObj, Integer> precPropMap = enc.getPropositionMap().getPrecedencePropositionMap();
		Map<Integer, PrecedenceObj> propPrecMap = new HashMap<Integer, PrecedenceObj>();		
		for (PrecedenceObj prec : precPropMap.keySet())
			propPrecMap.put(precPropMap.get(prec), prec);

		Bijection<EqualityObj, Integer> eqPropMap = enc.getPropositionMap().getVarEqualityPropositionMap();
		Map<Integer, EqualityObj> propBindMap = new HashMap<Integer, EqualityObj>();
		for (Entry<EqualityObj, Integer> entry : eqPropMap.entrySet()) {
			propBindMap.put(entry.getValue(), entry.getKey());
		}

		// save wcnf and serialized data
		System.out.println("Writing weighted CNF to " + options.wcnfFile);
		wcnf.writeToFile(options.wcnfFile);	
		FileUtils.serialize(propPrecMap, new File(options.wcnfFile.getAbsolutePath() + ".prec.dat"));
		FileUtils.serialize(propBindMap, new File(options.wcnfFile.getAbsolutePath() + ".bind.dat"));
		
		// save stats and options
		FileUtils.writeFile(options.outFile, result.toString());
	}


	private static void decode(MrrOptions options) {
		System.out.println("Loading PDDL");
		Plan plan = options.getPlan();

		System.out.println("Decoding model");		

		Map<Integer, PrecedenceObj> propPrecMap = FileUtils.deserialize(new File(options.wcnfFile.getAbsolutePath() + ".prec.dat"));		
		Map<Integer, EqualityObj> propBindMap = FileUtils.deserialize(new File(options.wcnfFile.getAbsolutePath() + ".bind.dat"));
		
		// get all ordering constraints
		DirectedGraph<Operator<Variable>> precGraph = new DirectedGraph<Operator<Variable>>();

		Set<Variable> initVars = new HashSet<Variable>(plan.getInitialAction().getParameters());
		Map<Variable, Constant> bindings = new HashMap<Variable, Constant>(plan.getSubstitution().getMap()); // init to original

		int[] soln = SatFormula.loadModel(options.model);
		for (int prop : soln) {
			if (prop > 0) {
				if (propPrecMap.containsKey(prop)) {
					PrecedenceObj prec = propPrecMap.get(prop);	
					if (!prec.getFirst().equals(plan.getInitialAction()) && !prec.getSecond().equals(plan.getGoalAction())) {
						GraphUtils.addAndCloseTransitive(precGraph, prec.getFirst(), prec.getSecond());
					}
				} else if (propBindMap.containsKey(prop)){
					EqualityObj bind = propBindMap.get(prop);
					if (initVars.contains(bind.getFirst())) {
						Constant c = plan.getSubstitution().apply(bind.getFirst());
						bindings.put(bind.getSecond(), c);						
					}
					else if (initVars.contains(bind.getSecond())) {
						Constant c = plan.getSubstitution().apply(bind.getSecond());
						bindings.put(bind.getFirst(), c);
					}
				}
			}
		}
		
		MrrResult result = MrrResult.parse(options.outFile);
		
		int relSize = precGraph.getAllEdges().size();
		System.out.println("Order relation size: " + relSize);

		// calculate flex value	
		double flex = getFlex(relSize, plan.getPlanSteps().size()-2);
		System.out.println("Flex: " + flex);

		result.relSize = relSize;
		result.flex = flex;
		if (options.verbose)
			System.out.println(result.toString());

		FileUtils.writeFile(options.outFile, result.toString());

		// print out pop
		GraphUtils.transitiveReduction(precGraph, precGraph);
		File popFile = new File(options.outFile.toString().replaceAll(".csv", ".pop"));
		FileUtils.writeFile(popFile, new PopModel(plan.getPlanSteps(), precGraph, bindings).toString());
		
	}
	
	public static DirectedGraph<Operator<Variable>> getParallelPlanOrdering(ParallelPlan pplan) {
		DirectedGraph<Operator<Variable>> customPrecGraph = new DirectedGraph<Operator<Variable>>();
		List<Operator<Variable>> prevStep = null;
		for (List<Operator<Variable>> step : pplan.getParallelSteps()) {
			if (prevStep != null) {
				for (Operator<Variable> prevOp : prevStep) {
					for (Operator<Variable> op : step)
						GraphUtils.addAndCloseTransitive(customPrecGraph, prevOp, op);
				}
			}
			prevStep = step;
		}
		return customPrecGraph;
	}

	public static double getFlex(int relSize, int nOps) {
		int den = 0;
		for (int i = 1; i <= nOps-1; i++) 
			den+=i;

		if (den == 0) // happens if plan has one step
			den = 1;

		double flex = 1 - (((double) relSize) / den);
		return flex;
	}


	public static class MrrResult {

		public static String header = "domain_name, problem_name, plan_file, alg, acyc, asymm, time_limit, enc_time, prepro_time, maxsat_time, n_props, n_clauses, n_symm_props, n_symm_clauses, maxsat_result, pop_size, pop_flex";

		public static MrrResult parse(File file) {
			List<String> lines = new ArrayList<String>();
			for (String line: FileUtils.readFile(file)) {
				if (!line.trim().isEmpty())
					lines.add(line);
			}
			MrrResult result = new MrrResult();
			if (lines.size() > 1) {
				String[] split = lines.get(1).split(",");
				result.domainName = split[0].trim();
				result.problemName = split[1].trim();
				result.planFileName = split[2].trim();
				result.algorithm = split[3].equals("null") || split[3].equals("None") || split[3].isEmpty() ? null : OptAlgorithm.valueOf(split[3].trim());
				result.acycl = split[4].equals("null") || split[4].equals("None")|| split[4].isEmpty() ? null : AcyclicityOpt.valueOf(split[4].trim());
				result.asymm = split[5].equals("null") || split[4].equals("None")|| split[5].isEmpty() ? null :AsymmetryOpt.valueOf(split[5].trim());
				result.maxTime = Long.valueOf(split[6].trim());
				result.encTime = Long.valueOf(split[7].trim());
				result.ppTime = Long.valueOf(split[8].trim());
				result.maxSatTime = Long.valueOf(split[9].trim());
				result.nProps = Integer.valueOf(split[10].trim());
				result.nClauses = Integer.valueOf(split[11].trim());	
				result.nSymmProps = Integer.valueOf(split[12].trim());
				result.nSymmClauses = Integer.valueOf(split[13].trim());	
				result.maxSatResult = split[14].equals("null") || split[14].equals("None")|| split[14].isEmpty() ? null : SatSolverResult.valueOf(split[14].trim());
				result.relSize = Integer.valueOf(split[15].trim());
				result.flex = Double.valueOf(split[16].trim());
			}

			return result;
		}

		public String domainName;
		public String problemName;
		public String planFileName;
		public OptAlgorithm algorithm;
		public AcyclicityOpt acycl;
		public AsymmetryOpt asymm;
		public long maxTime = -1;
		public long encTime = 0;
		public long ppTime = 0;
		public long maxSatTime = 0;
		public int nProps = -1;
		public int nClauses = -1;
		public int nSymmProps = -1;
		public int nSymmClauses = -1;
		public int relSize = -1;
		public SatSolverResult maxSatResult;	
		public double flex = -1;

		@Override
		public String toString() {
			String csv = header + "\n";
			csv+=domainName +",";
			csv+=problemName + ",";
			csv+=planFileName + ",";
			csv+=algorithm + ",";
			csv+=acycl + ",";
			csv+=asymm + ",";
			csv+=maxTime + ",";
			csv+=encTime + ",";
			csv+=ppTime + ",";
			csv+=maxSatTime + ",";
			csv+=nProps + ",";
			csv+=nClauses + ",";
			csv+=nSymmProps + ",";
			csv+=nSymmClauses + ",";
			csv+=maxSatResult + ",";
			csv+=relSize + ",";
			csv+=FormattingUtils.DF_3.format(flex) + "\n";

			return csv;
		}	
	}

	public static enum OptAlgorithm {

		MD_ORIG 		(true, CausalStructureOpt.DEORDER,	AsymmetryOpt.NONE, EqualityOpt.NONE, AcyclicityOpt.ATOM, false), 	// Muise's minimum deorder
		MR_ORIG 		(true, CausalStructureOpt.REORDER, 	AsymmetryOpt.NONE, EqualityOpt.NONE, AcyclicityOpt.ATOM, false), 	// Muise's minimum reorder 		

		MD 		(true, CausalStructureOpt.DEORDER,	AsymmetryOpt.NONE, EqualityOpt.NONE, AcyclicityOpt.ATOM, true), 	// minimum deorder
		MR 		(true, CausalStructureOpt.REORDER, 	AsymmetryOpt.NONE, EqualityOpt.NONE, AcyclicityOpt.ATOM, true), 	// minimum reorder 		
		MR_OPSB	(true, CausalStructureOpt.REORDER, 	AsymmetryOpt.OP_TYPES, EqualityOpt.NONE, AcyclicityOpt.ATOM, true), 	// minimum reorder 		

		MRD 	 (false, CausalStructureOpt.DEORDER, AsymmetryOpt.NONE, EqualityOpt.ATOM, AcyclicityOpt.ATOM, true),	// minimum reinstantiated deorder
		//MRD_CSSB (false, CausalStructureOpt.DEORDER, AsymmetryOpt.INIT_STATE, EqualityOpt.ATOM, AcyclicityOpt.ATOM, true),	// minimum reinstantiated deorder w/SB
		
		MRR 	(false, CausalStructureOpt.REORDER, AsymmetryOpt.NONE, EqualityOpt.ATOM, AcyclicityOpt.ATOM, true),	// minimum reinstantiated reorder
		MRR_OPSB (false, CausalStructureOpt.REORDER, AsymmetryOpt.OP_TYPES, EqualityOpt.ATOM, AcyclicityOpt.ATOM, true),
		MRR_CSSB (false, CausalStructureOpt.REORDER, AsymmetryOpt.STRUCT,   EqualityOpt.ATOM, AcyclicityOpt.ATOM, true),
		
		EOG  (true,  CausalStructureOpt.CUSTOM, AsymmetryOpt.NONE, EqualityOpt.NONE, AcyclicityOpt.ATOM, false), // explanation-based order generalisation
		REOG (false, CausalStructureOpt.CUSTOM, AsymmetryOpt.NONE, EqualityOpt.ATOM, AcyclicityOpt.ATOM, true); // reinstantiated explanation-based order generalisation

		public final boolean ground;
		public AsymmetryOpt asymm;
		public EqualityOpt eq;
		public AcyclicityOpt acyc;
		public CausalStructureOpt csOpt;
		public boolean optTransClosure;

		private OptAlgorithm(boolean ground, CausalStructureOpt csOpt, AsymmetryOpt asymm, EqualityOpt equal, AcyclicityOpt acyc, boolean filterLinks) {
			this.ground = ground;
			this.csOpt = csOpt;
			this.asymm = asymm;
			this.eq = equal;
			this.acyc = acyc;
			this.optTransClosure = filterLinks;
		}

	}

	public static class MrrOptions extends PddlCmdLineOptions {

		@Override
		public void parse(String[] args) {
			super.parse(args);
			if (OptAlgorithm.EOG.equals(this.algorithm)) {
				System.err.println("EOG algorithm not supported");
				System.exit(1);
			}
		}

		@Option(name = "--time", usage = "maximum cpu time (minutes)")
		public double time = 30;

		@Option(name = "--out-file", usage = "out file")
		public File outFile = new File(System.getProperty("user.dir"));

		@Option(name = "--wcnf-file", usage = "output wcnf file")
		public File wcnfFile = null;

		@Option(name = "--alg", usage = "optimisation algorithm")
		public OptAlgorithm algorithm = null;

		@Option(name = "--encode", usage = "encode to WCNF", metaVar = "OPT")
		public boolean encode = false;

		@Option(name = "--decode", usage = "decode model from model-file", metaVar = "OPT")
		public boolean decode = false;
		
		@Option(name = "--model-file", usage = "model file")
		public File model = null;

		
		
	}

}
