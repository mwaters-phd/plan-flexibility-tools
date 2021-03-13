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
package au.rmit.agtgrp.pplib.pddl.pct;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.predicate.Literal;
import au.rmit.agtgrp.pplib.fol.symbol.Variable;
import au.rmit.agtgrp.pplib.pddl.Operator;
import au.rmit.agtgrp.pplib.utils.FormattingUtils;

public class CausalStructure implements Iterable<PcLink>{

	private final PcGraph producerConsumerGraph;
	private boolean ground;

	public CausalStructure(boolean ground) {
		this.ground = ground;
		producerConsumerGraph = new PcGraph();
	}

	public Set<Consumer> getAllConsumers() {
		Set<Consumer> consumers = new HashSet<Consumer>();
		for (Consumer cons : producerConsumerGraph.getDestinationVertices())
			if (!producerConsumerGraph.getEdgesTo(cons).isEmpty())
				consumers.add(cons);

		return consumers;
	}

	public Set<Producer> getAllProducers() {
		Set<Producer> producers = new HashSet<Producer>();
		for (Producer prd : producerConsumerGraph.getSourceVertices())
			if (!producerConsumerGraph.getEdgesFrom(prd).isEmpty())
				producers.add(prd);

		return producers;
	}

	public void addProducerConsumerOption(PcLink link) {
		addProducerConsumerOption(link.getProducer(), link.getConsumer());
	}

	public void addProducerConsumerOption(Operator<Variable> prodOp, Literal<Variable> prodLit,
			Operator<Variable> consOp, Literal<Variable> consLit) {

		addProducerConsumerOption(new Producer(prodOp, prodLit).intern(), new Consumer(consOp, consLit).intern());
	}

	public void addProducerConsumerOption(Producer producer, Consumer consumer) {
		producerConsumerGraph.addEdge(producer, consumer);
	}

	public void removeProducerConsumerOption(PcLink link) {
		removeProducerConsumerOption(link.getProducer(), link.getConsumer());
	}

	public void removeProducerConsumerOption(Operator<Variable> prodOp, Literal<Variable> prodLit,
			Operator<Variable> consOp, Literal<Variable> consLit) {

		removeProducerConsumerOption(new Producer(prodOp, prodLit).intern(), new Consumer(consOp, consLit).intern());
	}

	public void removeProducerConsumerOption(Producer producer, Consumer consumer) {
		producerConsumerGraph.removeEdge(producer, consumer);
	}

	public Set<Producer> getProducers(Operator<Variable> consOp, Literal<Variable> consLit) {
		return getProducers(new Consumer(consOp, consLit).intern());
	}

	public Set<Producer> getProducers(Consumer consumer) {
		Set<Producer> producers = producerConsumerGraph.getEdgesTo(consumer);
		if (producers == null)
			return new HashSet<Producer>();
		return producers;
	}

	public Set<Consumer> getConsumers(Operator<Variable> prodOp, Literal<Variable> prodLit) {
		return getConsumers(new Producer(prodOp, prodLit).intern());
	}

	public Set<Consumer> getConsumers(Producer producer) {
		Set<Consumer> cons = producerConsumerGraph.getEdgesFrom(producer);
		if (cons == null)
			return new HashSet<Consumer>();
		return cons;
	}

	public Set<PcLink> getAllPcLinks() {
		return producerConsumerGraph.getAllEdges();
	}

	@Override
	public Iterator<PcLink> iterator() {
		return producerConsumerGraph.getAllEdges().iterator();
	}
	
	public int getSize() {
		return producerConsumerGraph.getSize();
	}

	public boolean isGround() {
		return ground;
	}
	
	public void setGround(boolean ground) {
		this.ground = ground;
	}
	
	public void clear() {
		producerConsumerGraph.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Consumer consumer : producerConsumerGraph.getDestinationVertices())
			sb.append(consumer + " = { " + 
					FormattingUtils.toString(producerConsumerGraph.getEdgesTo(consumer)) + " }\n");

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ground ? 1231 : 1237);
		result = prime * result + ((producerConsumerGraph == null) ? 0 : producerConsumerGraph.hashCode());
		result = prime * result + (1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CausalStructure other = (CausalStructure) obj;
		if (ground != other.ground)
			return false;
		if (producerConsumerGraph == null) {
			if (other.producerConsumerGraph != null)
				return false;
		} else if (!producerConsumerGraph.equals(other.producerConsumerGraph))
			return false;
	
		return true;
	}
	
	

}
