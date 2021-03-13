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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import au.rmit.agtgrp.pplib.utils.collections.Pair;
import au.rmit.agtgrp.pplib.utils.collections.graph.UndirectedGraph;

public class GrFormatter<V> {

	private UndirectedGraph<V> graph;
	private String comment;

	private Map<V, Integer> vertexIndexMap;

	public GrFormatter(UndirectedGraph<V> graph) {
		this(graph, "");
	}
	
	public GrFormatter(UndirectedGraph<V> graph, String comment) {
		this.graph = graph;
		if (comment == null)
			this.comment = "";
		else
			this.comment = comment.trim();

		vertexIndexMap = new HashMap<V, Integer>();

		Set<V> vertices = new HashSet<V>();
		for (V vertex : graph.getVertices()) {
			if (!graph.getLinksFrom(vertex).isEmpty())
				vertices.add(vertex);
		}

		int vInd = 1;
		for (V vertex : vertices)
			vertexIndexMap.put(vertex, vInd++);

	}

	public String format() {
		StringBuilder sb = new StringBuilder();

		Set<Pair<V, V>> edges = graph.getAllLinks();

		sb.append("c " + comment + "\n");
		sb.append("p tw " + vertexIndexMap.size() + " " + edges.size() + "\n");

		for (Pair<V, V> edge : edges)
			sb.append(vertexIndexMap.get(edge.getFirst()) + " " + vertexIndexMap.get(edge.getSecond()) + "\n");

		return sb.toString();
	}
	
	public void writeToFile(File file) throws IOException {
		Set<Pair<V, V>> edges = graph.getAllLinks();
		
		BufferedWriter writer = Files.newBufferedWriter(file.toPath());
		writer.write("c " + comment + "\n");
		writer.write("p tw " + vertexIndexMap.size() + " " + edges.size() + "\n");
		for (Pair<V, V> edge : edges)
			writer.write(vertexIndexMap.get(edge.getFirst()) + " " + vertexIndexMap.get(edge.getSecond()) + "\n");
		
		writer.close();
	}

}
