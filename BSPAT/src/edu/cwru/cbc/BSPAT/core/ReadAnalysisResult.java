package edu.cwru.cbc.BSPAT.core;

import edu.cwru.cbc.BSPAT.DataType.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadAnalysisResult {
	private List<CpGStatistics> statList = new ArrayList<>();// start from target region
	private int regionLength;
	private String inputFolder;
	private Coordinate coordinate;
	private String cellLine;

	public ReadAnalysisResult(String inputFolder, String cellLine, String region, Coordinate coordinate) throws
			IOException {
		this.inputFolder = inputFolder;
		this.coordinate = coordinate;
		this.cellLine = cellLine;
		readStatFile(region);
	}

	private void readStatFile(String ID) throws IOException {
		try (BufferedReader statBuffReader = new BufferedReader(
				new FileReader(inputFolder + ID + "_bismark.analysis_report.txt"))) {
			String line;
			String[] items;
			// skip target start
			int targetStart = Integer.parseInt(statBuffReader.readLine().split("\t")[1]);
			// read target ref length
			line = statBuffReader.readLine();
			if (line == null) {
				throw new RuntimeException("analysis report is empty!");
			}
			// skip 6 lines
			for (int i = 0; i < 9; i++) {
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
				CpGStatistics cpgStat = new CpGStatistics(Integer.valueOf(items[0]) - targetStart);
				cpgStat.setMethylLevel(Double.valueOf(items[1]));
				statList.add(cpgStat);
				line = statBuffReader.readLine();
			}
			statList.sort(CpG::compareTo);
		}
	}

	public List<PatternResult> readPatternFile(String region, String patternType) throws IOException {
		List<PatternResult> patternResultLists = new ArrayList<>();
		try (BufferedReader patternBuffReader = new BufferedReader(
				new FileReader(inputFolder + region + "_bismark.analysis_" + patternType + ".txt"))) {
			// skip column names
			patternBuffReader.readLine();
			// reference line
			String line = patternBuffReader.readLine();
			int regionLength = line.split("\t")[0].length();
			this.regionLength = regionLength;

			// start to read content
			line = patternBuffReader.readLine();
			String[] items;
			PatternResult patternResult;

			while (line != null) {
				items = line.split("\t");
				patternResult = new PatternResult();
				for (int i = 0; i < regionLength; i++) {
					CpGSitePattern cpg;
					if (items[0].charAt(i) == '*') {
						cpg = new CpGSitePattern(i, false);
						if (i + 1 < regionLength && items[0].charAt(i + 1) == '*') {
							i++;
						} else if (i == 0) {
							cpg = new CpGSitePattern(i - 1, false);
						}
						patternResult.addCpG(cpg);
					} else if (items[0].charAt(i) == '@') {
						cpg = new CpGSitePattern(i, true);
						if (i + 1 < regionLength && items[0].charAt(i + 1) == '@') {
							i++;
						} else if (i == 0) {
							cpg = new CpGSitePattern(i - 1, true);
						}
						patternResult.addCpG(cpg);
					} else if (items[0].charAt(i) == 'A' || items[0].charAt(i) == 'C' || items[0].charAt(i) == 'G' ||
							items[0].charAt(i) == 'T') {
						patternResult.addAllele(i);
					}
				}
				patternResultLists.add(patternResult);
				patternResult.setCount(Integer.valueOf(items[1]));
				patternResult.setPercent(Double.valueOf(items[2]));
				line = patternBuffReader.readLine();
			}
		}
		return patternResultLists;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public int getRegionLength() {
		return regionLength;
	}

	public List<CpGStatistics> getStatList() {
		return statList;
	}

	public String getCellLine() {
		return cellLine;
	}

}