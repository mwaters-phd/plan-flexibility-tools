package au.rmit.agtgrp.pplib.utils.collections;


public class Triple<S, T, W> extends Pair<S, T> {

	private static final long serialVersionUID = 1L;

	public static <S, T, W> Triple<S, T, W> instance(S first, T second, W third) {
		return new Triple<S, T, W>(first, second, third);
	}
	
	private final W third;
	
	public Triple(S first, T second, W third) {
		super(first, second);
		this.third = third;
	}
		
	public W getThird() {
		return third;
	}

	public Triple<S, T, W> intern() {
		return Pair.getCached(this);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((third == null) ? 0 : third.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
		if (third == null) {
			if (other.third != null)
				return false;
		} else if (!third.equals(other.third))
			return false;
		return true;
	}

}
