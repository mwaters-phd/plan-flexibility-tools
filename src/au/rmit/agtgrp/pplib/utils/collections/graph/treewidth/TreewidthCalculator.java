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
package au.rmit.agtgrp.pplib.utils.collections.graph.treewidth;

import java.util.HashMap;
import java.util.Map;

import au.rmit.agtgrp.pplib.utils.collections.Pair;
import au.rmit.agtgrp.pplib.utils.collections.graph.UndirectedGraph;
import nl.uu.cs.treewidth.algorithm.GreedyFillIn;
import nl.uu.cs.treewidth.algorithm.MaximumMinimumDegreePlusLeastC;
import nl.uu.cs.treewidth.input.GraphInput.InputData;
import nl.uu.cs.treewidth.ngraph.ListGraph;
import nl.uu.cs.treewidth.ngraph.ListVertex;
import nl.uu.cs.treewidth.ngraph.NGraph;

public class TreewidthCalculator {

	private static final String VERTEX_LABEL_PREFIX = "v";

	private final TreewidthExactInterface twExact;

	private boolean cancelled;
	
	public TreewidthCalculator() {
		twExact = new TreewidthExactInterface();
	}

	public <V> int calculateExact(UndirectedGraph<V> graph) throws InterruptedException {
		cancelled = false;
		if (graph.getSize() == 0)
			return 1;

		// convert graph type
		NGraph<InputData> ngraph = convertGraphType(graph);

		int lowerbound = getLowerBound(ngraph);
		int upperbound = getUpperBound(ngraph);

		if (upperbound == lowerbound)
			return upperbound;
		else {
			GrFormatter<V> gr = new GrFormatter<V>(graph);
			
			if (cancelled)
				throw new InterruptedException();
			
			twExact.calculateExact(gr.format());
			return twExact.getExact();
		}

	}

	public <V, E> boolean isGreaterThan(UndirectedGraph<V> graph, int maxwidth) throws InterruptedException {
		cancelled = false;
		
		if (graph.getSize() == 0)
			return 1 > maxwidth;

		// convert graph type
		NGraph<InputData> ngraph = convertGraphType(graph);

		int lb = getLowerBound(ngraph);
		if (lb > maxwidth) {
			return true;
		}

		int upperbound = getUpperBound(ngraph);
		if (upperbound <= maxwidth) {
			return false;
		}

		// find a lower bound < max
		GrFormatter<V> gr = new GrFormatter<V>(graph);
		
		if (cancelled)
			throw new InterruptedException();
		
		twExact.calculateLowerbound(gr.format(), maxwidth);

		int lowerbound = twExact.getLowerbound();
		return lowerbound >= maxwidth;

	}

	public <V, E> boolean isGreaterThanExact(UndirectedGraph<V> graph, int maxwidth) throws InterruptedException {
		cancelled = false;
		
		if (graph.getSize() == 0)
			return 1 > maxwidth;

		// find a lower bound < max
		GrFormatter<V> gr = new GrFormatter<V>(graph);
		
		if (cancelled)
			throw new InterruptedException();
		
		twExact.calculateLowerbound(gr.format(), maxwidth);

		int lowerbound = twExact.getLowerbound();
		return lowerbound >= maxwidth;

	}

	public <V, E> int getLowerBound(UndirectedGraph<V> graph) {
		
		if (graph.getSize() == 0)
			return 1;
		return getLowerBound(convertGraphType(graph));
	}

	public int getLowerBound(NGraph<InputData> graph) {

		MaximumMinimumDegreePlusLeastC<InputData> lbAlgo = new MaximumMinimumDegreePlusLeastC<InputData>();
		lbAlgo.setInput(graph);
		lbAlgo.run();
		return lbAlgo.getLowerBound();
	}

	public <V, E> int getUpperBound(UndirectedGraph<V> graph) {
		if (graph.getSize() == 0)
			return 1;
		return getUpperBound(convertGraphType(graph));
	}

	public int getUpperBound(NGraph<InputData> graph) {
		GreedyFillIn<InputData> ubAlgo = new GreedyFillIn<InputData>();
		ubAlgo.setInput(graph);
		ubAlgo.run();
		int upperbound = ubAlgo.getUpperBound();
		if (upperbound <= 0) // why?
			upperbound = 1;
		return upperbound;
	}

	public void cancel() {
		cancelled = true;
		twExact.cancel();
	}
	
	private <V, E> NGraph<InputData> convertGraphType(UndirectedGraph<V> graph) {

		NGraph<InputData> g = new ListGraph<InputData>();
		Map<V, ListVertex<InputData>> varVertMap = new HashMap<V, ListVertex<InputData>>();
		int vnum = 0;
		for (V var : graph.getVertices()) {
			InputData data = new InputData(vnum, VERTEX_LABEL_PREFIX + vnum);
			ListVertex<InputData> vertex = new ListVertex<InputData>(data);
			varVertMap.put(var, vertex);
			g.addVertex(vertex);
			vnum++;
		}

		for (Pair<V, V> edge : graph.getAllLinks()) {
			g.addEdge(varVertMap.get(edge.getFirst()), varVertMap.get(edge.getSecond()));
		}

		return g;
	}
}
