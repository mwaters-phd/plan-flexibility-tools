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
package au.rmit.agtgrp.pplib.csp;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.function.Constant;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.utils.collections.graph.UndirectedGraph;

public interface Csp<C> extends Serializable {

	public void addVariable(Variable v);

	public void addVariables(Collection<Variable> vs);

	public Collection<Variable> getVariables();

	public void addConstraint(C con);

	public void addConstraint(List<Variable> vars, C con);

	public void addConstraints(List<Variable> vars, Collection<C> cons);

	public Map<List<Variable>, ? extends Collection<C>> getConstraints();

	public Collection<C> getConstraints(List<Variable> vars);

	public void addDomainValue(Constant dv);

	public void addDomainValues(Collection<Constant> dvs);

	public void addDomainValue(Variable var, Constant dv);

	public void addDomainValues(Variable var, Collection<Constant> dvs);

	public Collection<Constant> getDomain();

	public Collection<Constant> getDomain(Variable v);

	public Map<Variable, Set<Constant>> getDomains();

	public UndirectedGraph<Variable> getPrimalGraph();

}
