package au.rmit.agtgrp.pplib.utils.collections;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class ObjectCache<T> {

	private final WeakHashMap<T, WeakReference<? extends T>> cache;

	public ObjectCache() {
		this(16);
	}
	
	public ObjectCache(int initCapacity) {
		cache = new WeakHashMap<T, WeakReference<? extends T>>(initCapacity);
	}
		
	public <S extends T> S get(S obj) {
		@SuppressWarnings("unchecked")
		WeakReference<S> reference = (WeakReference<S>) cache.get(obj);
		if (reference != null) {
			S cachedObj = reference.get();
			if (cachedObj != null)
				return cachedObj;
		}

		cache.put(obj, new WeakReference<S>(obj));

		return obj;
	}
	
}
