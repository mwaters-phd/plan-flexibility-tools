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
package au.rmit.agtgrp.pplib.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FormattingUtils {

	public static final DecimalFormat DF_2 = new DecimalFormat("#.##");

	public static final DecimalFormat DF_3 = new DecimalFormat("#.###");

	public static final DecimalFormat DF_4 = new DecimalFormat("#.####");

	public static String formatTime(double ms) {
		if (ms < 100)
			return FormattingUtils.DF_2.format(ms) + "ms";
		else if (ms < TimeUnit.MINUTES.toMillis(1))
			return FormattingUtils.DF_2.format(ms / 1000) + "s";
		else // if (ms < TimeUnit.HOURS.toMillis(1))
			return FormattingUtils.DF_2.format((ms / 1000) / 60) + "m";
	}

	public static String classArrayToString(Class<?>[] arr) {
		return classArrayToString(arr, ", ");
	}

	public static String classArrayToString(Class<?>[] arr, String separator) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < arr.length; i++) {
			sb.append(arr[i].getSimpleName());

			if (i < arr.length - 1)
				sb.append(separator);

		}

		return sb.toString();

	}

	public static String arrayToString(int[] arr) {
		return arrayToString(arr, ", ");
	}

	public static String arrayToString(int[] arr, String separator) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < arr.length; i++) {
			sb.append(arr[i]);

			if (i < arr.length - 1)
				sb.append(separator);

		}

		return sb.toString();

	}

	public static <T> String arrayToString(T[] arr) {
		return arrayToString(arr, ", ");
	}

	public static <T> String arrayToString(T[] arr, String separator) {

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < arr.length; i++) {
			T obj = arr[i];
			if (obj == null)
				sb.append("null");
			else
				sb.append(obj.toString());

			if (i < arr.length - 1)
				sb.append(separator);

		}

		return sb.toString();
	}

	public static <T> String toString(Collection<? extends T> collection, int lineLen, Comparator<T> comparator) {
		List<T> sorted = new ArrayList<T>(collection);
		Collections.sort(sorted, comparator);

		return toString(sorted, lineLen);
	}

	public static <T> String toString(Collection<? extends T> collection, int lineLen) {
		StringBuilder sb = new StringBuilder();

		StringBuilder lineBuilder = new StringBuilder();
		for (T t : collection) {
			String tString = t.toString();
			if ((tString.length() + lineBuilder.length() + 2 > lineLen) && lineBuilder.length() > 0) {
				sb.append(lineBuilder.toString() + "\n");
				lineBuilder = new StringBuilder();
			}

			if (lineBuilder.length() > 0)
				lineBuilder.append(", ");

			lineBuilder.append(tString);
		}

		return sb.toString();
	}

	public static <T> String toString(Collection<? extends T> collection, Comparator<T> comparator) {
		return toString(collection, ", ", comparator);
	}

	public static <T> String toString(Collection<? extends T> collection, String separator, Comparator<T> comparator) {
		List<T> sorted = new ArrayList<T>(collection);
		Collections.sort(sorted, comparator);

		return toString(sorted, separator);
	}

	public static <T> String toString(Collection<? extends T> coll) {
		return toString(coll, ", ");
	}

	public static <T> String toString(Collection<? extends T> collection, String separator) {
		StringBuilder sb = new StringBuilder();

		Iterator<? extends T> it = collection.iterator();
		while (it.hasNext()) {
			T obj = it.next();
			if (obj == null)
				sb.append("null");
			else
				sb.append(obj.toString());

			if (it.hasNext())
				sb.append(separator);
		}

		return sb.toString();
	}

	public static <T, S> String toString(Map<T, S> map) {
		return toString(map, ", ");
	}

	public static <T, S> String toString(Map<T, S> map, String delim) {
		StringBuilder sb = new StringBuilder();
		Iterator<T> it = map.keySet().iterator();
		while (it.hasNext()) {
			T key = it.next();
			sb.append(key + " : " + map.get(key));
			if (it.hasNext())
				sb.append(delim);
		}

		return sb.toString();
	}
	
	
	private FormattingUtils() { }

}
