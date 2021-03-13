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
package au.rmit.agtgrp.pplib.fol.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import au.rmit.agtgrp.pplib.fol.symbol.Symbol;

public class SymbolMap<T extends Symbol> extends HashMap<String, T> {

	private static final long serialVersionUID = 1L;

	public SymbolMap() {
	}

	public SymbolMap(Iterable<T> symbols) {
		for (T symbol : symbols) {
			put(symbol);
		}
	}

	public T put(T value) {
		super.put(value.getName(), value);
		super.put(value.getName().toLowerCase(), value);
		
		return value;
	}
	
	@Override
	public T put(String key, T value) {
		if (!key.toLowerCase().equals(value.getName().toLowerCase()))
			throw new IllegalArgumentException("Key: " + key + ", value: " + value.getName());
		
		return put(value);
	}
	
	@Override
	public T get(Object key) {
		if (key instanceof String) {
			T value = super.get(key);
			if (value == null)
				value = super.get(((String) key).toLowerCase());

			return value;
		}
		
		throw new IllegalArgumentException("Incorrect type: " + key.getClass() + ", expected " + String.class);
	}

	@Override
	public Set<T> values() {
		return new HashSet<T>(super.values());
	}

}
