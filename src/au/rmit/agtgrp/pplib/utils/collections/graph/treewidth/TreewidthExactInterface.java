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
package au.rmit.agtgrp.pplib.utils.collections.graph.treewidth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.List;

public class TreewidthExactInterface {

	public static final int UNKNOWN_VALUE = -1;
	public static final String TW_EXACT_EXEC = "tw-exact";

	private volatile Process process;
	private volatile Thread thread;

	private int exact;
	private int lowerbound;

	public TreewidthExactInterface() {
		exact = -1;
		lowerbound = -1;
	}

	public int getLowerbound() {
		return lowerbound;
	}

	public int getExact() {
		return exact;
	}

	public void calculateExact(File graphFile) throws InterruptedException {
		calculateLowerbound(graphFile, Integer.MAX_VALUE);
	}

	public void calculateLowerbound(File graphFile, int maxwidth) throws InterruptedException {
		List<String> lines;
		try {
			lines = Files.readAllLines(graphFile.toPath());
		} catch (IOException e) {
			throw new RuntimeException("Cannot read file: " + graphFile);
		}
		StringBuilder sb = new StringBuilder();
		for (String line : lines)
			sb.append(line + "\n");

		calculateLowerbound(sb.toString(), maxwidth);
	}

	public void calculateExact(String graphString) throws InterruptedException {
		calculateLowerbound(graphString, Integer.MAX_VALUE);
	}

	public synchronized void cancel() {
		if (process != null) { // if process == null, nothing is running, so ignore
			process.destroy();
			if (thread != null)
				thread.interrupt();		
		}
	}

	public void calculateLowerbound(String graphString, int maxwidth) throws InterruptedException {
		try {

			BufferedReader stdInput = null;
			synchronized(this) {
				
				lowerbound = 0;
				exact = UNKNOWN_VALUE;
				
				thread = Thread.currentThread();

				ProcessBuilder pb = new ProcessBuilder(TW_EXACT_EXEC);
				pb.redirectErrorStream(true);

				process = pb.start();

				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
				writer.write(graphString);
				writer.flush();

				stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			}

			// read the output from the command
			String s = null;
			while ((s = stdInput.readLine()) != null) { // if cancelled, this will be null
			
				synchronized(this) {
					
					if (Thread.interrupted()) {
						Thread.currentThread().interrupt();
						throw new InterruptedException();
					}
					
					if (s.contains("=")) {
						String numStr = s.startsWith("c ") ?
								s.substring(s.indexOf("=")+1, s.indexOf(",")).trim() :
								s.substring("width = ".length()).trim();
						
						lowerbound = Integer.valueOf(numStr);
						if (lowerbound > maxwidth) {
							synchronized(this) {
								if (process != null) {
									process.destroy();
									process = null;
								}
								return;
							}
						}
					}
				}
			}

			synchronized(this) {
				
				exact = lowerbound;
				process = null;
				thread = null;

				if (Thread.interrupted()) {
					Thread.currentThread().interrupt();
					throw new InterruptedException();
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
