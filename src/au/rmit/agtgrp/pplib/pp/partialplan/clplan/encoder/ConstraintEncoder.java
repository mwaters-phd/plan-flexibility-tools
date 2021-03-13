package au.rmit.agtgrp.pplib.pp.partialplan.clplan.encoder;

import au.rmit.agtgrp.pplib.pp.partialplan.InstantiatablePartialPlan;
import au.rmit.agtgrp.pplib.pp.partialplan.clplan.PcPlan;

public abstract class ConstraintEncoder<T, O> {

	@SuppressWarnings("unchecked")
	public static ConstraintEncoder<?, ?> getInstance(String encoderName) {
		String clsName = ConstraintEncoder.class.getPackage().getName()  + "." + encoderName;
		try {		
			return getInstance((Class<? extends ConstraintEncoder<?, ?>>) Class.forName(clsName));
		} catch (ClassNotFoundException e) {
			throw new ConstraintEncoderException("Error loading CSP encoder: " + clsName + " not found");
		}
	}

	public static ConstraintEncoder<?, ?> getInstance(Class<? extends ConstraintEncoder<?, ?>> encClass) {
		try {
			return encClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ConstraintEncoderException("Error loading csp encoder: " + encClass + ": " + e.getMessage());
		}
	}

	protected O options;
	
	public ConstraintEncoder(O options) {
		this.options = options;
	}
	
	public abstract InstantiatablePartialPlan<T> encodeAsPartialPlan(PcPlan plan);

	public abstract T encodeConstraints(PcPlan plan);
	
	public abstract long getEncodingTime();
	
	public abstract String getName();
	
	
}
