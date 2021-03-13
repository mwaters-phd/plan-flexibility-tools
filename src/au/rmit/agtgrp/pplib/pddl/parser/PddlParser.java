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
package au.rmit.agtgrp.pplib.pddl.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import au.rmit.agtgrp.pplib.pddl.PddlDomain;
import au.rmit.agtgrp.pplib.pddl.PddlProblem;
import au.rmit.agtgrp.pplib.pddl.Plan;
import fr.uga.pddl4j.parser.Parser;

public class PddlParser {
	
	private Pddl4JTranslator translator;
	private PddlDomain domain;
	private PddlProblem problem;
	private Plan plan;
	
	private boolean verbose;
	private boolean pctFormat;
	
	private Parser parser;
	
	public PddlParser() {
		this(false, false);
	}

	public PddlParser(boolean pctFormat, boolean verbose) {
		this.pctFormat = pctFormat;
		this.verbose = verbose;
	}
	
	
	public void parse(File domainFile) throws FileNotFoundException {

		parser = new Parser();
		
		try {			
			parser.parseDomain(domainFile.getAbsolutePath());
			if (!parser.getErrorManager().isEmpty()) {
				parser.getErrorManager().printAll();
				throw new PddlParserException("Error parsing PDDL in " + domainFile);
			}
			
		}
		catch (FileNotFoundException e) {
			throw new PddlParserException(e.getMessage());
		}

		translator = new Pddl4JTranslator(pctFormat, verbose);
		domain = translator.convertDomain(parser.getDomain());
		problem = null;
		plan = null;
	}
	
	public void parse(File domainFile, File problemFile) throws FileNotFoundException {

		parser = new Parser();
		
		try {			
//			parser.parseDomain(domainFile.getAbsolutePath());
//			if (!parser.getErrorManager().isEmpty()) {
//				parser.getErrorManager().printAll();
//				throw new PddlParserException("Error parsing PDDL in " + domainFile);
//			}

			parser.parse(domainFile, problemFile);
			if (!parser.getErrorManager().isEmpty()) {
				parser.getErrorManager().printAll();
				throw new PddlParserException("Error parsing PDDL in " + problemFile);
			}
			
			
		}
		catch (FileNotFoundException e) {
			throw new PddlParserException(e.getMessage());
		}

		translator = new Pddl4JTranslator(pctFormat, verbose);
		domain = translator.convertDomain(parser.getDomain());
		problem = translator.convertProblem(parser.getProblem());
		plan = null;
	}
	

	
	public void setDomainAndProblem(PddlProblem problem) {
		this.domain = problem.getDomain();
		this.problem = problem;
		plan = null;
	}

	
	public void parseFFPlan(File planFile) throws IOException, PddlParserException {
		List<String> planStrs = Files.readAllLines(Paths.get(planFile.toURI()));
		planStrs.remove(0);

		List<String[]> stepStrings = new ArrayList<String[]>();
		for (String line : planStrs) {
			if (!line.trim().isEmpty()) { // watch out for blank lines
				line = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
				stepStrings.add(line.split(" "));
			}
		}

		plan = translator.convertPlan(stepStrings);
	}

	public void parseFDPlan(File planFile) throws IOException, PddlParserException {
		parseFDPlan(Files.readAllLines(Paths.get(planFile.toURI())));
	}

	public void parseFDPlan(List<String> planStrs) throws IOException, PddlParserException {
		List<String[]> stepStrings = new ArrayList<String[]>();
		for (String line : planStrs) {
			if (!line.trim().isEmpty() && !line.contains(";")) { // watch out for blank lines, e.g., ;cost =
				line = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
				stepStrings.add(line.split(" "));
			}
		}
		plan = translator.convertPlan(stepStrings);
	}
	
	
	public void parseMadagascarPlan(File planFile) throws IOException, PddlParserException {
		List<String> planStrs = Files.readAllLines(Paths.get(planFile.toURI()));
		List<List<String[]>> stepStrings = new ArrayList<List<String[]>>();
		for (String line : planStrs) {
			//System.out.println("LINE: " + line);
			// eg STEP 0: drive(truck0,distributor1,depot0) drive(truck1,depot0,distributor0)
			if (!line.trim().isEmpty() && !line.contains(";")) { // watch out for blank lines, e.g., ;cost =
				List<String[]> stepOps = new ArrayList<String[]>();
				line = line.split(":")[1].trim(); // drive(truck0,distributor1,depot0) drive(truck1,depot0,distributor0)
				for (String action : line.split(" ")) {
					String actionName = action.substring(0, action.indexOf("("));
					String[] params = action.substring(action.indexOf('(') + 1, action.indexOf(')')).split(",");
					
					List<String> op = new ArrayList<String>();
					op.add(actionName);
					for (int i = 0; i < params.length; i++)
						if (!params[i].isEmpty()) // no params creates an empty param
							op.add(params[i]);
					stepOps.add(op.toArray(new String[op.size()]));
					//System.out.println(FormattingUtils.toString(op, "--"));
				}	
				stepStrings.add(stepOps);
			}
		}
		
		plan = translator.convertParallelPlan(stepStrings);
	}

	public PddlDomain getDomain() {
		return domain;
	}

	public PddlProblem getProblem() {
		return problem;
	}

	public Plan getPlan() {
		return plan;
	}

}
