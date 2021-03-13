package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.sat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pp.partialplan.InstantiatablePartialPlan;
import au.rmit.agtgrp.pplib.sat.SatFormula;
import au.rmit.agtgrp.pplib.sat.solver.SatSolutionPlanSet;
import au.rmit.agtgrp.pplib.sat.solver.SatSolver;
import au.rmit.agtgrp.pplib.utils.SerializationUtils;
import au.rmit.agtgrp.pplib.utils.collections.graph.treewidth.TreewidthCalculator;

public class SatPartialPlan extends InstantiatablePartialPlan<SatFormula> {

	private static final long serialVersionUID = 1L;
	
	public static SatPartialPlan deserialize(File file, PddlProblem problem, Collection<Operator<Variable>> ops, Substitution<Constant> initSub,Substitution<Constant> goalSub) throws FileNotFoundException, ClassNotFoundException, IOException {
		SatFormula sat = SatFormula.loadFromFile(file);
		PropositionMap propMap = SerializationUtils.deserialize(SatFormula.getPropositionMapFile(file));
		return new SatPartialPlan(sat, propMap, problem, ops, initSub, goalSub);
	}
	
	private final SatSolver solver = new SatSolver();
	private final TreewidthCalculator twCalc = new TreewidthCalculator();	
	private final PropositionMap propMap;
	
	public SatPartialPlan(SatFormula satFormula, PropositionMap propMap, PddlProblem problem, Collection<Operator<Variable>> planSteps, Substitution<Constant> initSub, Substitution<Constant> goalSub) {
		super(problem, planSteps, initSub, goalSub, satFormula);
		this.propMap = propMap;
	}
			
	public PropositionMap getPropositionMap() {
		return propMap;
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
		solver.solve(super.constraints, -1, timeout);
		return new PlanCountResult(solver.getSolutions().getSolutionCount(), false, -1);
	}

	@Override
	public PlanGenerationResult getPlans(int max, long timeout) throws InterruptedException {
		solver.solve(super.constraints, max, timeout);
		return new PlanGenerationResult(new SatSolutionPlanSet(this, solver.getSolutions(), propMap), false, -1);
	}

	@Override
	public void cancel() {
		//solver.cancel();
	}

	@Override
	public void serialize(File file) throws FileNotFoundException, IOException {
		super.constraints.writeToFileRandomAccess(file);
		SerializationUtils.serialize(propMap, SatFormula.getPropositionMapFile(file));
	}

	@Override
	public void writeToFile(File file) throws FileNotFoundException, IOException {
		super.constraints.writeToFileRandomAccess(file);
		SerializationUtils.serialize(propMap, SatFormula.getPropositionMapFile(file));
	}

	
	
}
