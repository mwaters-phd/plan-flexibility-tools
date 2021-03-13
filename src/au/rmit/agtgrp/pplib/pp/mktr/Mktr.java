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

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import au.rmit.agtgrp.pplib.csp.ExpressionCsp;
import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.PddlProblem.PlanResult;
import au.rmit.agtgrp.pplib.pddl.pct.CausalStructure;
import au.rmit.agtgrp.pplib.pddl.pct.CausalStructureFactory;
import au.rmit.agtgrp.pplib.pddl.pct.PcLink;
import au.rmit.agtgrp.pplib.pp.mktr.policy.RelaxationPolicy;
import au.rmit.agtgrp.pplib.pp.partialplan.InstantiatablePartialPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder.csp.PcToCspEncoder;
import au.rmit.agtgrp.pplib.pp.partialplan.planset.PlanSet;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class Mktr {

	private Plan plan;

	private PcToCspEncoder constraintEncoder;	
	private InstantiatablePartialPlan<ExpressionCsp> constraints;

	private RelaxationPolicy policy;
	private String policyName;

	private int maxTreewidth;
	private boolean validatePlans;
	private boolean verbose;

	private int toMinutes;

	// results
	private boolean timedOut;
	private int initialSize;
	private int maxSize;
	private int nPcLinksTested;
	private int nPcLinksAdded;
	private PcPlan pcPlan;

	// used to print state
	private int prevNumPlans;
	private int prevPcPlanSize;

	private PrintStream out = System.out;


	/**
	 * Initialises the MKTR algorithm.
	 * 
	 * @param plan				The valid plan.
	 * @param constraintEncoder		The name of the encoder type used to translate the PC plan's causal structure into a CSP.
	 * @param policyName		The name of the policy used to select which PC link to add the PC plan at each step.
	 * @param maxTreewidth		The maximum allowable treewidth of the PC plan.
	 * @param toMinutes			The maximum time.
	 * @param validatePlans		Validate the new plans found by MKTR, i.e., the instantiations of the PC plan.
	 * @param verbose			Print additional information, i.e., the current treewidth and number of instantiations.
	 */
	public Mktr(Plan plan, 
			PcToCspEncoder constraintEncoder,
			String policyName, int maxTreewidth, int toMinutes,
			boolean validatePlans, boolean verbose) {

		this.plan = plan;

		this.constraintEncoder = constraintEncoder;
		this.policyName = policyName;

		this.maxTreewidth = maxTreewidth;
		this.validatePlans = validatePlans;
		this.verbose = verbose;

		this.toMinutes = toMinutes;

	}

	public void setOutput(PrintStream out) {
		this.out = out;
	}

	public void relax() {

		CountDownLatch latch = new CountDownLatch(1);
		ExecutorService ex = Executors.newSingleThreadExecutor();
		Future<Void> f = ex.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					runMktr();
					latch.countDown();
					return null;
				}
				catch (Exception e) {
					latch.countDown();
					throw e;
				}			
			}
		});

		try {		
			if (toMinutes >= 0)
				f.get(toMinutes, TimeUnit.MINUTES); //time out has been set
			else
				f.get(); //wait indefinitely			


		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		} catch (TimeoutException e) {
			out.println("Time limit reached, waiting for MKTR to stop gracefully ...");
			timedOut = true;

			// interrupt the MKTR thread
			f.cancel(true); 
			if (constraints != null)
				constraints.cancel();

			// wait for MKTR to gracefully stop
			try {
				latch.await();
			} catch (InterruptedException e1) {
				throw new RuntimeException(e1);
			}

		}

		ex.shutdown();

	}


	/**
	 * Execute the MKTR algorithm.
	 */
	private void runMktr() {

		printSetup();

		// build PC plan
		out.println("Converting plan into causal structure");
		pcPlan = CausalStructureFactory.getEquivalentPcoPlan(plan);
		pcPlan.getConstraints().setGround(false);
		
		initialSize = pcPlan.getConstraints().getSize();

		// build minimal causal structure
		out.println("Building minimal causal structure");
		CausalStructure minimalConstraints = CausalStructureFactory.getMinimalPcoConstraints(plan, false, false);
		maxSize = minimalConstraints.getSize();

		// build heuristic
		out.println("Initialising relaxation policy");
		policy = RelaxationPolicy.getInstance(policyName, pcPlan, minimalConstraints);

		// init data
		nPcLinksTested = 0;
		nPcLinksAdded = 0;
		prevNumPlans = 1;
		prevPcPlanSize = pcPlan.getConstraints().getAllPcLinks().size();


		out.println("Initialising CSP");
		constraints = constraintEncoder.encodeAsPartialPlan(pcPlan);
		// stop if interrupted
		if (Thread.interrupted()) {
			Thread.currentThread().interrupt();
			return;
		}

		try {	
			if (constraints.isTreewidthGreaterThan(maxTreewidth))
				out.println("Initial CSP treewidth > " + maxTreewidth);
		} catch (InterruptedException e) {
			// tw calc interrupted
			return;
		}	

		printHeaders();
		try {
			printState(null, pcPlan, policy.getCurrentOptions(), null);
		} catch (InterruptedException e) {
			// csp calculation was cancelled
			return;
		}

		// start relaxation
		PcLink edge;
		while ((edge = policy.getNext()) != null) { // select edge

			if (Thread.interrupted()) {
				Thread.currentThread().interrupt();
				break;
			}

			// add edge to graph
			pcPlan.getConstraints().addProducerConsumerOption(edge);

			// convert to constraint
			InstantiatablePartialPlan<ExpressionCsp> attempt = constraintEncoder.encodeAsPartialPlan(pcPlan);

			// test treewidth of csp
			nPcLinksTested++;
			try {
				if (attempt.isTreewidthGreaterThan(maxTreewidth)) {
					pcPlan.getConstraints().removeProducerConsumerOption(edge);
					policy.failed(edge);
				} else {
					constraints = attempt;
					nPcLinksAdded++;
					policy.added(edge);
				}
			} catch (InterruptedException e) {
				// tw calculation was cancelled
				pcPlan.getConstraints().removeProducerConsumerOption(edge);
				break;
			}

			// update console
			try {
				printState(constraints, pcPlan, policy.getCurrentOptions(), edge);
			} catch (InterruptedException e) {
				// csp calc was cancelled
				break;
			}
		}

	}

	private void printSetup() {
		out.println("Initialising MKTR");
		out.println("Domain:    " + plan.getDomain().getName());
		out.println("Problem:   " + plan.getProblem().getName());
		out.println("Treewidth: " + maxTreewidth);
		out.println("Encoder:   " + constraintEncoder.getName());
		out.println("Policy:    " + policyName);
		out.println("Time:      " + (toMinutes > 0 ? (toMinutes + " minute(s)"): "none"));
	}

	private void printHeaders() {
		out.println("Starting MKTR");
		if (verbose)
			out.println("#C\t#C_A \ttw\t#plans\tcnt_t\topt_t\t  producer -> consumer");
		else
			out.println("#C\t#C_A\t  producer -> consumer");
	}

	private void printState(InstantiatablePartialPlan<?> csp, PcPlan pcPlan, List<PcLink> opts, PcLink edge) throws InterruptedException {

		int pcPlanSize = pcPlan.getConstraints().getAllPcLinks().size();
		if (verbose && csp != null) {
			String nsolsStr = "?";
			long solTime = 0;
			int nPlans = prevNumPlans;
			PlanSet pSet = null;
			int twEst = csp.getTreewidthLowerBound();
			//an edge was added (i.e., tw <= maxtreewidth), or this is the first iteration
			if (prevPcPlanSize != pcPlanSize || nPcLinksTested == 0) {
				pSet = getPlans();
				nPlans = pSet.getPlanCount();
				nsolsStr = Integer.toString(nPlans);
			}

			String twStr = twEst == maxTreewidth ? Integer.toString(maxTreewidth) : ">="  + twEst;
			String added = prevPcPlanSize != pcPlanSize ? "+ " : "  ";
			out.println(pcPlan.getConstraints().getAllPcLinks().size() + "\t" + opts.size() + "\t" + twStr
					+ "\t" + nsolsStr + "\t" + FormattingUtils.formatTime(solTime) + "\t"
					+ FormattingUtils.formatTime(((double) constraintEncoder.getEncodingTime())) + "\t"
					+ added
					+ (edge == null ? " " : edge));

			//if new plans were found, or it is the first iteration, validate if required
			if ((nPlans != prevNumPlans || nPcLinksTested == 0) && validatePlans) {
				out.println("Validating new plans");
				PlanResult validationResult = plan.getProblem().validateAll(pSet);
				if (!validationResult.isValid)
					throw new RuntimeException("\nInvalid plan found!\n" + validationResult.message);
			}

			prevNumPlans = nPlans;

		} else {
			String added = prevPcPlanSize != pcPlanSize ? "+ " : "  ";
			String padding = verbose ? "\t\t\t\t\t" : "\t";
			out.println(pcPlan.getConstraints().getAllPcLinks().size() + "\t" + opts.size() + padding
					+ added
					+ (edge == null ? " " : edge));

		}

		prevPcPlanSize = pcPlanSize;

	}
	
	public PlanSet getPlans() throws InterruptedException {
		return constraints.getPlans(-1, -1).plans;
	}

	public boolean timedOut() {
		return timedOut;
	}

	public int getInitialSize() {
		return initialSize;
	}

	public int getNumPcLinksTested() {
		return nPcLinksTested;
	}

	public int getnPcLinksAdded() {
		return nPcLinksAdded;
	}

	public int getFinalSize() {
		return initialSize + nPcLinksAdded;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public ExpressionCsp getFinalCsp() {
		if (constraints == null) {
			return null;
		}	
		return constraints.getConstraints();
	}

	public PcPlan getFinalPcPlan() {
		return pcPlan;
	}


}
