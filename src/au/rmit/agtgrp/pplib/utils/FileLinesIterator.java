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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class FileLinesIterator implements Iterator<String> {

	private BufferedReader reader;
	private String next;

	public FileLinesIterator(File file) {
		try {
			reader = new BufferedReader(new FileReader(file));
			next = null;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean hasNext() {
		try {
			if (next == null)
				next = reader.readLine();

			return next != null;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String next() {
		try {
			if (next == null)
				next = reader.readLine();

			if (next == null)
				throw new NoSuchElementException();

			String ret = next;
			next = null;

			return ret;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
