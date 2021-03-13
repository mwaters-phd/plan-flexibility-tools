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
package au.rmit.agtgrp.pplib.csp.solver.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CachedCspOutputSet implements CspOutputSet {

	private File solnsFile;
	private int solnCount;

	public CachedCspOutputSet(File solnsFile, int count) {
		this.solnsFile = solnsFile;
		this.solnCount = count;
	}

	@Override
	public File getSolutionsFile() {
		return solnsFile;
	}

	@Override
	public int getSolutionCount() {
		return solnCount;
	}

	@Override
	public boolean isCached() {
		return true;
	}

	@Override
	public Iterator<List<Integer>> iterator() {
		return new CspOutputIterator();
	}

	private class CspOutputIterator implements Iterator<List<Integer>> {

		private ObjectInputStream in;

		private int rotationNum;

		List<Integer> soln;

		public CspOutputIterator() {
			try {
				in = new ObjectInputStream(new FileInputStream(solnsFile));
				rotationNum = 0;
				readLine();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

		@Override
		public boolean hasNext() {
			return soln != null;
		}

		@Override
		public List<Integer> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			List<Integer> sol = soln;
			readLine();
			return sol;
		}

		private void readLine() {

			try {
				soln = new ArrayList<Integer>();

				while (true) {
					short val = in.readShort();

					if (val == CspOutputWriter.EOL) {
						break;
					} else if (val == CspOutputWriter.EOF) {
						if (!soln.isEmpty()) {
							throw new RuntimeException("EOF without EOL!");
						}

						rotationNum++;
						File nextFile = new File(solnsFile.getAbsolutePath() + "." + rotationNum);
						if (nextFile.exists()) { // there is a next file
							in = new ObjectInputStream(new FileInputStream(nextFile));
						} else { // current file is empty, there is no next file
									// -- done
							soln = null;
							break;
						}
					} else
						soln.add((int) val);

				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

	}

}
