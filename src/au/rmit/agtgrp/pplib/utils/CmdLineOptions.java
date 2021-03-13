package au.rmit.agtgrp.pplib.utils;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

public class CmdLineOptions {
		
	@Option(name = "--help", usage = "print this help message", help = true, metaVar = "OPT")
	public boolean help;

	@Option(name = "--verbose", usage = "verbose", metaVar = "OPT")
	public boolean verbose;

	
	public void parse(String[] args) {
		ParserProperties properties = ParserProperties.defaults();
		properties.withOptionSorter(null);

		CmdLineParser optionParser = new CmdLineParser(this, properties);

		StringWriter usage = new StringWriter();
		optionParser.printUsage(usage, null);

		List<String> optionStrs = new ArrayList<String>();
		for (String arg : args)
			optionStrs.add(arg);

		try {
			optionParser.parseArgument(optionStrs);

		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println(usage);
			System.exit(1);
		}

		// print help message if requested
		if (this.help) {
			System.out.println(usage);
			System.exit(0);
		}
	}
	
}
