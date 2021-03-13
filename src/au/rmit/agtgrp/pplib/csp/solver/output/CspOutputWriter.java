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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class CspOutputWriter {

	public static final short EOF = Short.MIN_VALUE;
	public static final short EOL = Short.MIN_VALUE + 1;

	public static final int MAX_FILE_SIZE = 1000000000;
	public static int MAX_FILES = 20;

	private ObjectOutputStream out;
	private File file;

	private int bytesWritten;

	private int rotationNum;

	public CspOutputWriter(File outputFile) {
		try {

			bytesWritten = 0;
			rotationNum = 0;

			clear(outputFile);

			file = new File(outputFile.getAbsolutePath());
			out = new ObjectOutputStream(new FileOutputStream(outputFile));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void clear(File outputFile) {
		if (outputFile.exists())
			outputFile.delete();

		int r = 1;
		File newFile;
		do {
			newFile = new File(outputFile.getAbsolutePath() + "." + r);
			r++;
		} while (newFile.delete());
	}

	public void writeValue(short value) {
		try {
			out.writeShort(value);
			bytesWritten += 2;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void endSolution() {
		try {
			out.writeShort(EOL);
			out.flush();
			bytesWritten += 2;
			// rotate output file?
			if (bytesWritten >= MAX_FILE_SIZE) {
				out.writeShort(EOF);
				out.flush();
				out.close();

				rotationNum++;
				if (rotationNum >= MAX_FILES)
					throw new RuntimeException("Output size limit reached!");

				File newFile = new File(file.getAbsolutePath() + "." + rotationNum);
				out = new ObjectOutputStream(new FileOutputStream(newFile));

				bytesWritten = 0;

			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void end() {
		try {
			out.writeShort(EOF);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
