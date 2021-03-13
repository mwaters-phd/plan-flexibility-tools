package au.rmit.agtgrp.pplib.pp.mktr.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import au.rmit.agtgrp.pplib.pddl.pct.CausalStructure;
import au.rmit.agtgrp.pplib.pddl.pct.Consumer;
import au.rmit.agtgrp.pplib.pddl.pct.PcLink;
import au.rmit.agtgrp.pplib.pddl.pct.Producer;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.symm.CsSymmetry;

public class MultiLex {	

	private final List<PcLink> allLinks;
	private final List<CsSymmetry> symmetries;	
	private final Map<CsSymmetry, List<PcLink>> symmLinksOrig;
	private final Map<CsSymmetry, List<PcLink>> symmLinksPerm;
	

	public MultiLex(List<CsSymmetry> symmetries, PcPlan plan, CausalStructure allLinks) {
		this.symmetries = symmetries;
		this.allLinks = new ArrayList<PcLink>(allLinks.getAllPcLinks());
		
		List<Producer> producers = new ArrayList<Producer>(allLinks.getAllProducers());
		Comparator<Producer> producerComp = new Comparator<Producer>() {
			@Override
			public int compare(Producer o1, Producer o2) {
				int c = Integer.compare(plan.getPlanSteps().indexOf(o1.operator), plan.getPlanSteps().indexOf(o2.operator));
				if (c == 0) {
					c = Integer.compare(o1.operator.getPostconditions().indexOf(o1.literal),
							o2.operator.getPostconditions().indexOf(o2.literal));
				}
				return c;
			}
		};
		Collections.sort(producers, producerComp);


		List<Consumer> consumers = new ArrayList<Consumer>(allLinks.getAllConsumers());
		Comparator<Consumer> consumerComp = new Comparator<Consumer>() {
			@Override
			public int compare(Consumer o1, Consumer o2) {
				int c = Integer.compare(plan.getPlanSteps().indexOf(o1.operator), plan.getPlanSteps().indexOf(o2.operator));
				if (c == 0) {
					c = Integer.compare(o1.operator.getPreconditions().indexOf(o1.literal),
							o2.operator.getPreconditions().indexOf(o2.literal));
				}
				return c;
			}
		};
		Collections.sort(consumers, consumerComp);


		Map<Producer, Integer> producerOrder = new TreeMap<Producer, Integer>(producerComp);
		for (int i = 0; i < producers.size(); i++)
			producerOrder.put(producers.get(i), i);

		Map<Consumer, Integer> consumerOrder = new TreeMap<Consumer, Integer>(consumerComp);
		for (int i = 0; i < consumers.size(); i++)
			consumerOrder.put(consumers.get(i), i);

		Comparator<Consumer> comp = new Comparator<Consumer>() {
			@Override
			public int compare(Consumer o1, Consumer o2) {
				return Integer.compare(consumerOrder.get(o1), consumerOrder.get(o2));
			}
		};

		Map<CsSymmetry, List<Consumer>> consumerLowerDomains = new HashMap<CsSymmetry, List<Consumer>>();
		for (CsSymmetry symm : symmetries) {
			List<Consumer> lower = new ArrayList<Consumer>();
			for (Consumer cons : symm.getConsumerDomain()) {
				if (consumerOrder.get(cons) < consumerOrder.get(symm.permute(cons))) {
					lower.add(cons);
				}
			}

			Collections.sort(lower, comp);
			consumerLowerDomains.put(symm, lower);
		}

		symmLinksOrig = new HashMap<CsSymmetry, List<PcLink>>();
		symmLinksPerm = new HashMap<CsSymmetry, List<PcLink>>();

		for (CsSymmetry symm : symmetries) {
			List<PcLink> orig = new ArrayList<PcLink>();
			List<PcLink> perm = new ArrayList<PcLink>();
			for (Producer prod : producerOrder.keySet()) {
				Iterable<Consumer> consmrs = producerOrder.get(prod) < producerOrder.get(symm.permute(prod)) ?
						consumerOrder.keySet() : consumerLowerDomains.get(symm);

						for (Consumer cons : consmrs) {
							orig.add(new PcLink(prod, cons));
							perm.add(new PcLink(symm.permute(prod), symm.permute(cons)));
						}
			}
			symmLinksOrig.put(symm, orig);
			symmLinksPerm.put(symm, perm);
		}
	}

	
	public CausalStructure getApproximateCanonicalForm(CausalStructure cs) {	
		boolean changed = true;
		while(changed) {
			changed = false;
			for (CsSymmetry symm : symmetries) {
				if (multiLex(cs, symm) > 0) {
					cs = symm.permute(cs); //returns a new instance
					changed = true;
				}
			}
		}		
		return cs;
	}


	public boolean satisfiesMultiLex(CausalStructure cs) {
		for (CsSymmetry symm : symmetries) {
			if (multiLex(cs, symm) > 0)
				return false;
		}
		return true;
	}

	// returns -1, 0, 1 iff cs < p(cs), cs == p(cs) or cs > p(cs)
	private int multiLex(CausalStructure cs, CsSymmetry symm) {
		List<PcLink> orig = symmLinksOrig.get(symm);
		List<PcLink> perm = symmLinksPerm.get(symm);

		for (int i = 0; i < orig.size(); i++) {
			boolean inOrig = cs.getAllPcLinks().contains(orig.get(i));
			boolean inPermuted = cs.getAllPcLinks().contains(perm.get(i));
			if (inOrig == inPermuted)
				continue;
			if (inOrig) // orig is 1, perm is 0
			return 1;
			return -1; // orig is 0, perm is 1
		}

		return 0;
	}

	public String getStringForm(CausalStructure cs) {
		StringBuilder sb = new StringBuilder();
		for (PcLink link : allLinks) {
			if (cs.getAllPcLinks().contains(link))
				sb.append(1);
			else
				sb.append(0);
		}
		return sb.toString();
	}
}
