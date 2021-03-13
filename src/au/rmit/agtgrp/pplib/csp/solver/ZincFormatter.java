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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import au.rmit.agtgrp.pplib.csp.ExpressionCsp;
import au.rmit.agtgrp.pplib.csp.PartitionedExpressionCsp;
import au.rmit.agtgrp.pplib.csp.alldiff.AllDifferent;
import au.rmit.agtgrp.pplib.fol.Substitution;
import au.rmit.agtgrp.pplib.fol.expression.Connective;
import au.rmit.agtgrp.pplib.fol.expression.Expression;
import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.predicate.Predicate;
import au.rmit.agtgrp.pplib.fol.symbol.Type;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.fol.utils.Comparators;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class ZincFormatter {

	public static final String AND = "/\\";
	public static final String OR = "\\/";
	public static final String IMPL = "->";
	public static final String NOT = "not";

	public static final String EQ = "=";
	public static final String NEQ = "!=";

	public static final String PREC = "<";
	public static final String PREC_EQ = "<=";

	public static final String ALL_DIFF = "alldifferent";

	public static final String COMMENT = "%";
	public static final String OBJ_MAP_FLAG = "OBJ_MAP";
	public static final String PARTITION_FLAG = "PARTITION";

	public static Map<Integer, String> getObjectMapStrs(File mznFile) {

		try {
			BufferedReader br = new BufferedReader(new FileReader(mznFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains(OBJ_MAP_FLAG))
					break;
			}

			br.close();

			Map<Integer, String> objMap = new HashMap<Integer, String>();
			line = line.substring(COMMENT.length() + OBJ_MAP_FLAG.length());
			for (String pair : line.split(",")) {
				String[] split = pair.split(Substitution.MAPS_SYMBOL);
				objMap.put(Integer.valueOf(split[0].trim()), split[1].trim());
			}

			return objMap;

		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static Map<String, String> getPartitionStrs(File mznFile) {

		try {
			BufferedReader br = new BufferedReader(new FileReader(mznFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains(PARTITION_FLAG))
					break;
			}

			br.close();

			Map<String, String> objMap = new HashMap<String, String>();
			line = line.substring(COMMENT.length() + PARTITION_FLAG.length());
			for (String pair : line.split(",")) {
				pair = pair.trim();
				if (!pair.isEmpty()) {
					String[] split = pair.split(Substitution.MAPS_SYMBOL);
					objMap.put(split[0].trim(), split[1].trim());
				}
			}

			return objMap;

		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static Substitution<Variable> getPartition(File mznFile, Collection<Variable> variables) {
		Map<String, String> strPartition = getPartitionStrs(mznFile);

		Map<String, Type> types = new HashMap<String, Type>();
		Map<String, Variable> varsByName = new HashMap<String, Variable>();

		for (Variable var : variables) {
			types.put(var.getType().getName(), var.getType());
			varsByName.put(var.toString(), var);
		}

		Map<Variable, Variable> subMap = new HashMap<Variable, Variable>();
		for (String varStr : strPartition.keySet()) {
			Variable var = varsByName.get(varStr);
			if (var == null) {
				throw new IllegalArgumentException("Unknown variable: " + varStr);
			}
			String[] pvarStrSplit = strPartition.get(var.toString()).split(" ");
			Variable pvar = new Variable(types.get(pvarStrSplit[0]), pvarStrSplit[1]).intern();
			subMap.put(var, pvar);
		}

		return new Substitution<Variable>(subMap);

	}

	public static Map<Integer, Constant> getObjectMap(File mznFile, Collection<Constant> constants) {
		Map<Integer, String> strPartition = getObjectMapStrs(mznFile);

		Map<String, Constant> csByName = new HashMap<String, Constant>();

		for (Constant cons : constants)
			csByName.put(cons.getName(), cons);

		Map<Integer, Constant> objectMap = new HashMap<Integer, Constant>();
		for (int i : strPartition.keySet()) {
			Constant c = csByName.get(strPartition.get(i));
			if (c == null)
				throw new IllegalArgumentException("Unknown constant: " + strPartition.get(i));
			objectMap.put(i, c);
		}

		return objectMap;

	}

	private ExpressionCsp csp;

	private List<Variable> variables;
	private Map<Integer, Constant> indToObj;
	private Map<Constant, Integer> objToInd;
	private int highestObj;

	public ZincFormatter(ExpressionCsp csp) {
		this.csp = csp;

		indToObj = new HashMap<Integer, Constant>();
		objToInd = new HashMap<Constant, Integer>();

		variables = new ArrayList<Variable>(csp.getVariables());
		Collections.sort(variables, Comparators.SYMBOL_COMPARATOR);

		init();
	}

	private void init() {

		List<Constant> cons = new ArrayList<Constant>(csp.getDomain());
		Collections.sort(cons, new Comparator<Constant>() {

			@Override
			public int compare(Constant o1, Constant o2) {
				if (o1.getType().equals(Type.OPERATOR_TYPE) && o2.getType().equals(Type.OPERATOR_TYPE))
					return Integer.compare(Integer.valueOf(o1.getName()), Integer.valueOf(o2.getName()));
				if (o1.getType().equals(Type.OPERATOR_TYPE) && !o2.getType().equals(Type.OPERATOR_TYPE))
					return -1;
				if (o2.getType().equals(Type.OPERATOR_TYPE) && !o1.getType().equals(Type.OPERATOR_TYPE))
					return 1;
				return o1.getName().compareTo(o2.getName());

			}
		});

		highestObj = 0;
		for (Constant obj : cons) {
			highestObj++;
			indToObj.put(highestObj, obj);
			objToInd.put(obj, highestObj);
		}

	}

	public List<Variable> getVariablesInOrder() {
		return variables;
	}

	public Map<Integer, Constant> getObjectsByIndex() {
		return this.indToObj;
	}

	public Constant getObjectByIndex(int i) {
		return indToObj.get(i);
	}

	public int getIndexofObject(Constant obj) {
		return objToInd.get(obj);
	}

	public String getZincString() {
		StringBuilder sb = new StringBuilder();

		sb.append(formatObjectMap());
		sb.append("\n");

		sb.append(formatPartition());
		sb.append("\n");

		sb.append("include \"globals.mzn\";\n\n");

		sb.append(formatVarDeclarations());
		sb.append("\n");

		//sb.append(formatDomains());
		//sb.append("\n");

		sb.append(formatConstraints());
		sb.append("\n");

		sb.append(formatSearch());
		sb.append("\n");

		sb.append(formatOutput());

		return sb.toString();
	}

	public String formatObjectMap() {
		StringBuilder sb = new StringBuilder();
		sb.append(COMMENT + OBJ_MAP_FLAG + " ");
		Iterator<Integer> intIt = indToObj.keySet().iterator();
		while (intIt.hasNext()) {
			int i = intIt.next();
			sb.append(i + Substitution.MAPS_SYMBOL + indToObj.get(i));
			if (intIt.hasNext())
				sb.append(",");
		}
		sb.append("\n");
		return sb.toString();
	}

	public String formatPartition() {
		if (csp instanceof PartitionedExpressionCsp)
			return COMMENT + PARTITION_FLAG + " " + ((PartitionedExpressionCsp) csp).getMapping().toString() + "\n";
		else
			return "";
	}

	public String formatVarDeclarations() {
		StringBuilder sb = new StringBuilder();
		for (Variable var : variables) {
			List<Integer> rangeInds = new ArrayList<Integer>();
			for (Constant d : csp.getDomain(var))
				rangeInds.add(objToInd.get(d));

			Collections.sort(rangeInds);
			
			sb.append("var {" + FormattingUtils.toString(rangeInds, ",") + "} : " + varname(var) + ";\n");
		}
		return sb.toString();
	}

	public String formatDomains() {
		StringBuilder sb = new StringBuilder();
		for (Variable var : variables) {
			List<Integer> rangeInds = new ArrayList<Integer>();
			for (Constant d : csp.getDomain(var))
				rangeInds.add(objToInd.get(d));

			Collections.sort(rangeInds);
			sb.append("constraint " + varname(var) + " in {" + FormattingUtils.toString(rangeInds, ",") + "};\n");
		}

		return sb.toString();
	}

	public String formatConstraints() {
		StringBuilder sb = new StringBuilder();

		for (List<Expression<Variable>> exps : csp.getConstraints().values()) {
			for (Expression<Variable> exp : exps) {
				sb.append("constraint " + formatExpression(exp) + ";\n");
			}
		}

		return sb.toString();
	}

	private String formatExpression(Expression<Variable> exp) {
		if (exp.isLiteral()) {
			Literal<Variable> lit = exp.getLiteral();
			Predicate predicate = lit.getAtom().getSymbol();
			if (predicate.equals(Predicate.EQUALS)) { // x = y or //x != y
				
				return formatVariable(lit.getAtom().getParameters().get(0)) + 
						" " + (lit.getValue() ? EQ : NEQ) + " "
						+ formatVariable(lit.getAtom().getParameters().get(1));
				
			} 
			else if (predicate.equals(Predicate.PREC)) {
				
				if (lit.getValue()) { // x < y
					return formatVariable(lit.getAtom().getParameters().get(0)) + " " + PREC + " "
					+ formatVariable(lit.getAtom().getParameters().get(1));
				} else { // x !< y, i.e., y <= x
					return formatVariable(lit.getAtom().getParameters().get(1)) + " " + PREC_EQ + " "
					+ formatVariable(lit.getAtom().getParameters().get(0));
				}
			} 
			else if (predicate.getName().equals(AllDifferent.ALL_DIFF_PREDICATE_NAME)) {
				StringBuilder sb = new StringBuilder();
				sb.append(ALL_DIFF + "([");
				for (int i = 0; i < lit.getAtom().getSymbol().getArity(); i++) {
					sb.append(formatVariable(lit.getAtom().getParameters().get(i)));
					if (i < lit.getAtom().getSymbol().getArity() - 1)
						sb.append(",");
				}

				sb.append("])");
				
				if (lit.getValue())
					return sb.toString();
				else
					return NOT + "(" + sb.toString() + ")";
			} else {
				throw new IllegalArgumentException("Cannot handle literal: " + lit);
			}
		} else {
			StringBuilder sb = new StringBuilder();
			Connective conn = exp.getConnective();
			String cs = getOpString(conn);
			switch (conn) {
			case AND:
			case OR:
			case IMPL:
				for (int i = 0; i < exp.getSubexpressions().size(); i++) {
					sb.append("(" + formatExpression(exp.getSubexpressions().get(i)) + ")");
					if (i < exp.getSubexpressions().size() - 1)
						sb.append(" " + cs + " ");
				}
				return sb.toString();

			case NOT:
				return NOT + " (" + formatExpression(exp.getSubexpressions().get(0)) + ")";
			}

		}
		return null;
	}
	
	
	private String varname(Variable var) {	
		return "var_" + var.getName().replaceAll("-", "_");
	}
	
	private String formatVariable(Variable var) {
		if (csp.getDomain(var).size() == 1) // sub wit constant
			return Integer.toString(objToInd.get(csp.getDomain(var).iterator().next()));
		else
			return varname(var);
	}

	private String getOpString(Connective conn) {
		switch (conn) {
		case AND:
			return AND;
		case OR:
			return OR;
		case IMPL:
			return IMPL;
		case NOT:
			return NOT;
		}
		return null;

	}
	
	private String formatSearch() {
		StringBuilder sb = new StringBuilder();
		sb.append("solve :: int_search([");
		Iterator<Variable> it = variables.iterator();
		
		while (it.hasNext()) {
			sb.append(varname(it.next()));
			if (it.hasNext())
				sb.append(", ");
		}
			
		sb.append("], most_constrained, indomain, complete) satisfy;\n");
		return sb.toString();
	}

	public String formatOutput() {
		StringBuilder sb = new StringBuilder();

		sb.append("output [\n");

		for (Variable var : variables) {
			sb.append("show(" + varname(var) + "), \",\",");
		}

		sb.append("];");

		return sb.toString();
	}

}
