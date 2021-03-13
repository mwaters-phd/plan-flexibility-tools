package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import au.rmit.agtgrp.pplib.csp.ExpressionCsp;
import au.rmit.agtgrp.pplib.csp.PartitionedExpressionCsp;
import au.rmit.agtgrp.pplib.csp.solver.CspSolutionSet;
import au.rmit.agtgrp.pplib.csp.solver.CspSolver;
import au.rmit.agtgrp.pplib.csp.solver.GeCodeInterface;
import au.rmit.agtgrp.pplib.csp.solver.ZincFormatter;
import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pp.partialplan.InstantiatablePartialPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.planset.PlanSet;
import au.rmit.agtgrp.pplib.pp.partialplan.planset.PlanSubstitutionSet;
import au.rmit.agtgrp.pplib.utils.SerializationUtils;
import au.rmit.agtgrp.pplib.utils.collections.graph.treewidth.TreewidthCalculator;

public class CspPartialPlan extends InstantiatablePartialPlan<ExpressionCsp> {
	
	private static final long serialVersionUID = 1L;
	
	public static CspPartialPlan deserialize(File file, PddlProblem problem, Collection<Operator<Variable>> ops, Substitution<Constant> initSub,Substitution<Constant> goalSub) throws FileNotFoundException, ClassNotFoundException, IOException {
		ExpressionCsp csp = SerializationUtils.deserialize(file);
		return new CspPartialPlan(csp, problem, ops, initSub, goalSub);
	}
	
	private final CspSolver cspSolver = new GeCodeInterface();
	private final TreewidthCalculator twCalc = new TreewidthCalculator();
	
	public CspPartialPlan(ExpressionCsp csp, PddlProblem problem, Collection<Operator<Variable>> planSteps, Substitution<Constant> initSub, Substitution<Constant> goalSub) {
		super(problem, planSteps, initSub, goalSub, csp);
	}
	
	@Override
	public boolean isTreewidthGreaterThan(int max) throws InterruptedException {
		return twCalc.isGreaterThan(super.constraints.getPrimalGraph(), max);
	}

	@Override
	public int getTreewidthLowerBound() throws InterruptedException {
		return twCalc.getLowerBound(super.constraints.getPrimalGraph());
	}

	@Override
	public PlanCountResult countSolutions(long timeout) throws InterruptedException {
		cspSolver.countSolutions(super.constraints, TimeUnit.MINUTES.toMillis(timeout));
		return new PlanCountResult(cspSolver.getSolutionCount(), cspSolver.timedOut(), (long) cspSolver.getRuntime());
	}

	@Override
	public PlanGenerationResult getPlans(int max, long timeout) throws InterruptedException {
		cspSolver.getNSolutions(super.constraints, max, timeout);
		CspSolutionSet cspSols = cspSolver.getSolutions();
		if (super.constraints instanceof PartitionedExpressionCsp) {
			cspSols = ((PartitionedExpressionCsp) super.constraints).departitionSolutions(cspSols);
		}
		PlanSet partialPlan = new PlanSubstitutionSet(this, cspSols, cspSols.getSolutionCount());
		return new PlanGenerationResult(partialPlan, cspSolver.timedOut(), (long) cspSolver.getRuntime());
	}

	@Override
	public void cancel() {
		twCalc.cancel();
		cspSolver.cancel();
	}

	@Override
	public void serialize(File file) throws FileNotFoundException, IOException {
		SerializationUtils.serialize(super.constraints, file);
	}

	@Override
	public void writeToFile(File file) throws FileNotFoundException, IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
			ZincFormatter zf = new ZincFormatter(super.constraints);
			writer.write(zf.getZincString());
		} catch (IOException e) {
			throw e;
		}
	}

}
