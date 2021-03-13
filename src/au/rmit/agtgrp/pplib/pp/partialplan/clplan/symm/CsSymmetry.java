package au.rmit.agtgrp.pplib.pp.partialplan.clplan.symm;

import au.rmit.agtgrp.pplib.pddl.pct.CausalStructure;
import au.rmit.agtgrp.pplib.pddl.pct.Consumer;
import au.rmit.agtgrp.pplib.pddl.pct.PcLink;
import au.rmit.agtgrp.pplib.pddl.pct.Producer;

import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class CsSymmetry {

	public static boolean isCsSymmetry(Map<Producer, Producer> producerPerm, Map<Consumer, Consumer> consumerPerm) {
		for (Producer prod : producerPerm.keySet()) {
			Producer img = producerPerm.get(prod);
			if (!prod.equals(producerPerm.get(img))) {
				return false;
			}
		}

		for (Consumer cons : consumerPerm.keySet()) {
			Consumer img = consumerPerm.get(cons);
			if (!cons.equals(consumerPerm.get(img))) {
				return false;
			}
		}

		return true;
	}

	private final Map<Producer, Producer> producerPerm;
	private final Map<Consumer, Consumer> consumerPerm;

	public CsSymmetry(Map<Producer, Producer> producerPerm, Map<Consumer, Consumer> consumerPerm) {
		this.producerPerm = producerPerm;
		this.consumerPerm = consumerPerm;
		// NB this assumes that isCsSymmetry() has been called
	}

	public Consumer permute(Consumer cons) {
		Consumer perm = consumerPerm.get(cons);
		if (perm == null)
			return cons;
		return perm;
	}

	public Producer permute(Producer prod) {
		Producer perm = producerPerm.get(prod);
		if (perm == null)
			return prod;
		return perm;
	}

	public PcLink permute(PcLink link) {
		return new PcLink(permute(link.getProducer()), permute(link.getConsumer())).intern();
	}

	public CausalStructure permute(CausalStructure cs) {
		CausalStructure permuted = new CausalStructure(cs.isGround());

		for (PcLink link : cs.getAllPcLinks())
			permuted.addProducerConsumerOption(permute(link));

		return permuted;
	}

	public Set<Producer> getProducerDomain() {
		return producerPerm.keySet();
	}

	public Set<Consumer> getConsumerDomain() {
		return consumerPerm.keySet();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CsSymmetry that = (CsSymmetry) o;
		return Objects.equals(producerPerm, that.producerPerm) &&
				Objects.equals(consumerPerm, that.consumerPerm);
	}

	@Override
	public int hashCode() {
		return Objects.hash(producerPerm, consumerPerm);
	}
}
