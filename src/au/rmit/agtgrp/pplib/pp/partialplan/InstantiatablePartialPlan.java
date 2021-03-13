package au.rmit.agtgrp.pplib.pp.partialplan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pp.partialplan.planset.PlanSet;

public abstract class InstantiatablePartialPlan<T> extends PartialPlan<T> {

	private static final long serialVersionUID = 1L;
	
	public InstantiatablePartialPlan(PddlProblem problem, Collection<Operator<Variable>> operators, Substitution<Constant> initSub, Substitution<Constant> goalSub, T constraints) {
		super(problem, operators, initSub, goalSub, constraints);
	}
	
	public abstract boolean isTreewidthGreaterThan(int max) throws InterruptedException;

	public abstract int getTreewidthLowerBound() throws InterruptedException;

	public abstract PlanCountResult countSolutions(long timeout) throws InterruptedException;

	public abstract PlanGenerationResult getPlans(int max, long timeout) throws InterruptedException;	

	public abstract void cancel();

	public abstract void serialize(File file) throws FileNotFoundException, IOException;
	
	public abstract void writeToFile(File file) throws FileNotFoundException, IOException;

	public static class PlanCountResult {
		public final int nSolutions;
		public final boolean timedOut;
		public final long runtime;

		public PlanCountResult(int nSolutions, boolean timedOut, long runtime) {
			this.nSolutions = nSolutions;
			this.timedOut = timedOut;
			this.runtime = runtime;
		}
	}

	public static class PlanGenerationResult {
		public final PlanSet plans;
		public final boolean timedOut;
		public final long runtime;

		public PlanGenerationResult(PlanSet plans, boolean timedOut, long runtime) {
			this.plans = plans;
			this.timedOut = timedOut;
			this.runtime = runtime;
		}
	}

}
