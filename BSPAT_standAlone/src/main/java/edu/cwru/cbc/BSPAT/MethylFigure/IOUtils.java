package edu.cwru.cbc.BSPAT.MethylFigure;

import edu.cwru.cbc.BSPAT.commons.CpG;
import edu.cwru.cbc.BSPAT.commons.CpGStatistics;
import edu.cwru.cbc.BSPAT.commons.PatternResult;
import edu.cwru.cbc.BSPAT.commons.PotentialSNP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class IOUtils {
	static List<PatternResult> readASMPatterns(String ASMPatternFileName) throws IOException {
		List<PatternResult> patternResultLists = new ArrayList<>();
		try (BufferedReader patternBuffReader = new BufferedReader(new FileReader(ASMPatternFileName))) {
			// skip column names
			patternBuffReader.readLine();
			// reference line
			String line = patternBuffReader.readLine();
			int regionLength = line.split("\t")[0].length() - 2;// ignore two space at two ends of reference string
			PatternResult.targetRegionLength = regionLength;
			PatternResult patternWithoutAllele = readASMPattern(patternBuffReader, regionLength);
			PatternResult patternWithAllele = readASMPattern(patternBuffReader, regionLength);

			// skip 2 lines
			patternBuffReader.readLine();
			patternBuffReader.readLine();

			for (CpGStatistics cpg : patternWithAllele.getCpGList()) {
				double methylLevel = Double.parseDouble(patternBuffReader.readLine().split("\t")[1]);
				cpg.setMethylLevel(methylLevel);
			}

			// skip 2 lines
			patternBuffReader.readLine();
			patternBuffReader.readLine();

			for (CpGStatistics cpg : patternWithoutAllele.getCpGList()) {
				double methylLevel = Double.parseDouble(patternBuffReader.readLine().split("\t")[1]);
				cpg.setMethylLevel(methylLevel);
			}

			patternResultLists.add(patternWithoutAllele);
			patternResultLists.add(patternWithAllele);
			patternBuffReader.close();
		}
		return patternResultLists;
	}

	private static PatternResult readASMPattern(BufferedReader patternBuffReader, int regionLength) throws IOException {
		String line;// start to read content
		String[] items;
		line = patternBuffReader.readLine();
		items = line.split("\t");
		String patternString = items[0].trim();
		return parsePatternString(regionLength, items, patternString);
	}

	private static PatternResult parsePatternString(int regionLength, String[] items, String patternString) {
		PatternResult patternResult = new PatternResult();
		for (int i = 0; i < regionLength; i++) {
			CpGStatistics cpg;
			if (patternString.charAt(i) == '*') {
				cpg = new CpGStatistics(i, false);
				if (i + 1 < regionLength && patternString.charAt(i + 1) == '*') {
					i++;
				} else if (i == 0) {
					cpg = new CpGStatistics(i - 1, false);
				} else if (patternString.charAt(i - 1) != '-') {
					cpg = new CpGStatistics(i - 1, false);
				}
				patternResult.addCpG(cpg);
			} else if (patternString.charAt(i) == '@') {
				cpg = new CpGStatistics(i, true);
				if (i + 1 < regionLength && patternString.charAt(i + 1) == '@') {
					i++;
				} else if (i == 0) {
					cpg = new CpGStatistics(i - 1, true);
				} else if (patternString.charAt(i - 1) != '-') {
					cpg = new CpGStatistics(i - 1, false);
				}
				patternResult.addCpG(cpg);
			} else if (patternString.charAt(i) == 'A' || patternString.charAt(i) == 'C' || patternString.charAt(
					i) == 'G' ||
					patternString.charAt(i) == 'T') {
				patternResult.setSnp(new PotentialSNP(i, patternString.charAt(i)));
			}
		}
		patternResult.setCount(Integer.parseInt(items[1]));
		patternResult.setPercent(Double.parseDouble(items[2]));
		return patternResult;
	}

	static List<CpGStatistics> readReportFile(String reportFileName) throws IOException {
		List<CpGStatistics> statList = new ArrayList<>();
		try (BufferedReader statBuffReader = new BufferedReader(new FileReader(reportFileName))) {
			String line;
			String[] items;
			// skip target start
			int targetStart = Integer.parseInt(statBuffReader.readLine().split("\t")[1]);
			// read target ref length
			line = statBuffReader.readLine();
			if (line == null) {
				throw new RuntimeException("analysis report is empty!");
			}
			// skip lines
			for (int i = 0; i < 10; i++) {
				statBuffReader.readLine();
			}
			// get start position
			line = statBuffReader.readLine();
			while (line != null && !line.startsWith("mutation")) {
				items = line.split("\t");
				if (!Character.isDigit(items[0].charAt(0))) {
					break;
				}
				// start from target start. 0-based.
				int pos = Integer.parseInt(items[0]) - targetStart;
				CpGStatistics cpgStat = new CpGStatistics(pos, false);
				cpgStat.setMethylLevel(Double.parseDouble(items[1]));
				statList.add(cpgStat);
				line = statBuffReader.readLine();
			}
			statList.sort(CpG::compareTo);
		}
		return statList;
	}

	static List<PatternResult> readPatternFile(String patternFileName) throws IOException {
		List<PatternResult> patternResultLists = new ArrayList<>();
		try (BufferedReader patternBuffReader = new BufferedReader(new FileReader(patternFileName))) {
			// skip column names
			patternBuffReader.readLine();
			// reference line
			String line = patternBuffReader.readLine();
			int regionLength = line.split("\t")[0].length();
			PatternResult.targetRegionLength = regionLength;

			// start to read content
			line = patternBuffReader.readLine();
			String[] items;
			PatternResult patternResult;

			while (line != null) {
				items = line.split("\t");
				String patternString = items[0];
				patternResult = parsePatternString(regionLength, items, patternString);
				patternResultLists.add(patternResult);
				line = patternBuffReader.readLine();
			}
		}
		return patternResultLists;
	}
}
