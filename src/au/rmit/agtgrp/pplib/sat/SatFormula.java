package au.rmit.agtgrp.pplib.sat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import au.rmit.agtgrp.pplib.utils.FileUtils;
import au.rmit.agtgrp.pplib.utils.collections.graph.UndirectedGraph;

public class SatFormula implements Iterable<int[]>, Serializable {

	private static final long serialVersionUID = 1L;

	public static File getPropositionMapFile(File satFile) {
		return new File(satFile.toString() + ".propmap.dat");
	}
	
	public static int[] parse(String line) {
		String[] split = line.trim().split(" ");
		
		int first = 0;
		try {
			Integer.valueOf(split[0]); //okay to have a letter first
		}
		catch (NumberFormatException e) {
			first = 1;
		}
		
		int len = split.length - first;
		if (split[split.length-1].equals("0")) // ignore zero at the end, if present
			len--;
		
		int[] clause = new int[len];
		
		for (int i = 0; i < clause.length; i++)	 {
			try {	
				clause[i] = Integer.valueOf(split[i + first]);
			}
			catch (Exception e) {
				System.out.println("Exception:");
				e.printStackTrace(System.out);
				System.out.println("Index: " + i);
				System.out.println("First: " + i);
				System.out.println("Value at " + (i + first) + ": " + split[i+first]);
				
				System.out.println("Trying to parse:");
				System.out.println(line);
				
				throw e;
			}
		}
		return clause;
	}
	
	public static int[] loadModel(File file) {
		List<String> lines = new ArrayList<String>();
		for (String line : FileUtils.readFile(file)) {
			line = line.trim();
			if (!line.isEmpty())
				lines.add(line);
		}
		String solStr = lines.get(lines.size()-1);
		return parse(solStr);		
	}
	
	public static SatFormula loadFromFile(File file) throws IOException {
		return parse(file);
	}
	
	public static SatFormula parse(File file) throws IOException {
		FileInputStream inputStream = null;
		Scanner sc = null;
		try {
			inputStream = new FileInputStream(file.getAbsolutePath());
			sc = new Scanner(inputStream, "UTF-8");

			SatFormula sat = new SatFormula();
			while (sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if (line.startsWith("p") || line.startsWith("c"))
					continue;

				String[] split = line.split(" ");
				int[] clause = new int[split.length-1];
				for (int i = 0; i < clause.length; i++)
					clause[i] = Integer.valueOf(split[i]);
				
				sat.addClause(clause);

			}
			// note that Scanner suppresses exceptions
			if (sc.ioException() != null) {
				throw sc.ioException();
			}

			return sat;
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (sc != null) {
				sc.close();
			}
		}
	}
	 

	protected List<int[]> clauses;
	protected int nProps;
	protected String comment;

	public SatFormula(List<int[]> clauses, int nProps) {
		this.clauses = clauses;
		this.nProps = nProps;
	}

	public SatFormula() {
		clauses = new ArrayList<int[]>();
		nProps = 0;
	}

	public void addClause(List<Integer> clause) {
		int[] arr = new int[clause.size()];
		int i = 0;
		for (Integer s : clause)
			arr[i++] = s;
		
		setHighestPropNumber(arr);
		clauses.add(arr);
	}

	public void addClause(int ... clause) {
		setHighestPropNumber(clause);
		clauses.add(Arrays.copyOf(clause, clause.length));
	}

	protected void setHighestPropNumber(int[] clause) {
		for (int p : clause) {
			p = Math.abs(p);
			if (p > nProps)
				nProps = p;
			if (p == 0)
				throw new IllegalArgumentException("Invalid prop: " + Integer.toString(p));
		}
	}

	public int getNumProps() {
		return nProps;
	}

	public int getNumClauses() {
		return clauses.size();
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	protected String formatDescription() {
		return "p cnf " + nProps + " " + clauses.size();
	}

	@Override
	public Iterator<int[]> iterator() {
		return clauses.iterator();
	}

	public List<int[]> getClauses() {
		return clauses;
	}

	public void writeToFileRandomAccess(File file) throws IOException {

		if (file.exists())
			file.delete();
		
		byte[] sp = " ".getBytes();
		byte[] end = "0\n".getBytes();
		
		long spSize = sp.length;
		long endSize = end.length;
		long buf = 0;
		if (comment != null) {
			for (String cline : comment.split("\n"))
				buf += ("c " + cline + "\n").getBytes().length;
		}
		buf+=(formatDescription()+"\n").getBytes().length;
		
		for (int[] clause : clauses) {
			for (int i = 0; i < clause.length; i++){
				buf += Integer.toString(clause[i]).getBytes().length;
				buf += spSize;
			}
			buf+=endSize;
		}		
		
		RandomAccessFile raf = new RandomAccessFile(file.getAbsolutePath(), "rw");
		FileChannel rwChannel = raf.getChannel();
		ByteBuffer wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, buf);
		
		if (comment != null) {
			for (String cline : comment.split("\n"))
				wrBuf.put(("c " + cline + "\n").getBytes());
		}
		wrBuf.put((formatDescription()+"\n").getBytes());
		
		for (int[] clause : clauses) {
			for (int i = 0; i < clause.length; i++){
				wrBuf.put(Integer.toString(clause[i]).getBytes());
				wrBuf.put(sp);
			}
			wrBuf.put(end);
		}
		rwChannel.close();
		raf.close();
	}
	
	public void writeToFile(File file) throws IOException {

		if (file.exists())
			file.delete();
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		 	
		if (comment != null) {
			for (String cline : comment.split("\n"))
				bw.write("c " + cline + "\n");
		}
		bw.write(formatDescription()+"\n");
		
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (int[] clause : clauses) {	
			for (int i = 0; i < clause.length; i++){
				//bw.write(Integer.toString(clause[i]));
				//bw.write(" ");
				sb.append(Integer.toString(clause[i]) + " ");
			}
			sb.append("0\n");
			//bw.write("0\n");
			if (c % 1000 == 0) {			
				bw.write(sb.toString());
				sb = new StringBuilder();
			}
			c++;
		}
		
		bw.write(sb.toString());
		bw.close();
	}

	public void writeToStream(OutputStream out) throws IOException {
		byte[] sp = " ".getBytes();
		byte[] end = "0\n".getBytes();
		
		if (comment != null) {
			for (String cline : comment.split("\n"))
				out.write(("c " + cline + "\n").getBytes());
		}
		out.write((formatDescription()+"\n").getBytes());
		out.flush();
		for (int[] clause : clauses) {
			for (int i = 0; i < clause.length; i++) {
				out.write(Integer.toString(clause[i]).getBytes());
				out.write(sp);
			}
			out.write(end);
			out.flush();
		}
	}
	
	public UndirectedGraph<Integer> getPrimalGraph() {
		UndirectedGraph<Integer> primalGraph = new UndirectedGraph<Integer>();
		for (int[] clause : clauses) {
			for (int i = 0; i < clause.length; i++) {
				for (int j = i+1; j < clause.length; j++) {
					primalGraph.addEdge(clause[i], clause[j]);
				}
			}
		}
		return primalGraph;
	}
	
	public UndirectedGraph<Integer> getIncidenceGraph() {
		UndirectedGraph<Integer> incGraph = new UndirectedGraph<Integer>();
		int cnum = nProps+1;
		for (int[] clause : clauses) {
			for (int i = 0; i < clause.length; i++) {
				incGraph.addEdge(clause[i], cnum);
			}
			cnum++;
		}
		return incGraph;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(formatDescription());
		for (int[] clause : clauses) {
			for (int prop : clause) {
				sb.append(prop);
				sb.append(" ");
			}		
			sb.append("0\n");
		}

		return sb.toString();
	}

}
