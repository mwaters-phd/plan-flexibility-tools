package au.rmit.agtgrp.pplib.utils;

import java.io.File;

import org.kohsuke.args4j.Option;

import au.rmit.agtgrp.pplib.pddl.Plan;
import au.rmit.agtgrp.pplib.pddl.PddlProblem.PlanResult;
import au.rmit.agtgrp.pplib.pddl.parser.PddlParser;
import au.rmit.agtgrp.pplib.pddl.parser.PddlParserException;

public class PddlCmdLineOptions extends CmdLineOptions {

	@Option(name = "--domain", usage = "domain file")
	public File domainFile;

	@Option(name = "--problem", usage = "problem file")
	public File problemFile;

	@Option(name = "--plan", usage = "plan file")
	public File planFile;

	private Plan plan;
	
	@Override
	public void parse(String[] args) {
		super.parse(args);
		loadPlan();
	}
		
	private void loadPlan() {

		PddlParser pddlParser = new PddlParser(true, false);

		try {
			pddlParser.parse(domainFile, problemFile);
			String planFileName = planFile.getName();
			if (planFileName.endsWith(".lama") || planFileName.endsWith(".ss") || planFileName.endsWith(".bfws")) {
				pddlParser.parseFDPlan(planFile);
			}
			else if (planFileName.endsWith(".m")) {
				pddlParser.parseMadagascarPlan(planFile);
			}
			else {
				throw new IllegalArgumentException("Unknown plan type: " + planFile);
			}
			
			
		}  catch (PddlParserException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Unexpected error while parsing PDDL");
			e.printStackTrace();
			System.exit(1);
		}

		// check that the plan is valid
		PlanResult pr = pddlParser.getProblem().validatePlan(pddlParser.getPlan());
		if (!pr.isValid) {
			System.err.println("Input plan is not valid");
			System.err.println(pr.message);
			System.exit(1);
		}
		
		plan = pddlParser.getPlan();		
	}
	
	public Plan getPlan() {
		if (plan == null)
			loadPlan();
		return plan;
	}
	
}
