package graph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import DataType.Coordinate;
import DataType.CpGSite;
import DataType.CpGStatComparator;
import DataType.CpGStatistics;
import DataType.PatternResult;
import DataType.PatternLink;

public class ReadAnalysisResult {
	private ArrayList<PatternResult> patternResultLists = new ArrayList<PatternResult>();
	private ArrayList<Integer> refCpGs = new ArrayList<Integer>();
	private ArrayList<CpGStatistics> statList = new ArrayList<CpGStatistics>();
	private int refLength = 0;
	private String inputFolder;
	private Coordinate coordinate;
	private String beginCoor;
	private String endCoor;
	private String FRState;
	private String patternType;
	private String cellLine;

	public ReadAnalysisResult(String inputFolder, String patternType,String cellLine, String ID, String FRState, Coordinate coordinate)
			throws IOException {
		this.inputFolder = inputFolder;
		this.coordinate = coordinate;
		this.FRState = FRState;
		this.patternType = patternType;
		this.cellLine = cellLine;
		System.out.println("readStatFile");
		readStatFile(ID);
		System.out.println("readPatternFile");
		readPatternFile(ID);
		System.out.println("setCoordinate");
		setCoordinate();
	}

	private void readStatFile(String ID) throws IOException {
		FileReader statReader = new FileReader(inputFolder + ID + FRState + "_bismark.analysis_statistics.txt");
		BufferedReader statBuffReader = new BufferedReader(statReader);

		String line;
		String[] items;
		// skip first line.
		statBuffReader.readLine();
		// get start position
		line = statBuffReader.readLine();
		items = line.split("\t");
		refLength = Integer.valueOf(items[2]);
		line = statBuffReader.readLine();
		while (line != null) {
			items = line.split("\t");
			if (Character.isDigit(items[0].charAt(0)) == false) {
				break;
			}
			CpGStatistics cpgStat = null;
			int pos = Integer.valueOf(items[0]);
			refCpGs.add(pos);
			cpgStat = new CpGStatistics(pos);
			cpgStat.setMethylationRate(Double.valueOf(items[1]));
			statList.add(cpgStat);

			line = statBuffReader.readLine();
		}
		Collections.sort(statList, new CpGStatComparator());
		statBuffReader.close();
	}

	private void readPatternFile(String ID) throws IOException {
		FileReader patternReader = new FileReader(inputFolder + ID + FRState + "_bismark.analysis_" + this.patternType + ".txt");
		BufferedReader patternBuffReader = new BufferedReader(patternReader);
		// skip software version line.
		patternBuffReader.readLine();
		String line = patternBuffReader.readLine();
		String[] items;
		PatternResult patternResult;

		while (line != null) {
			items = line.split("\t");
			CpGSite cpg;
			patternResult = new PatternResult();
			for (int i = 0; i < refLength; i++) {
				if (items[0].charAt(i) == '*' && items[0].charAt(i + 1) == '*') {
					// because each CpG consists of two nucleotide, i++
					cpg = new CpGSite(i++, false);
					patternResult.addCpG(cpg);
				} else if (items[0].charAt(i) == '@' && items[0].charAt(i + 1) == '@') {
					// because each CpG consists of two nucleotide, i++
					cpg = new CpGSite(i++, true);
					patternResult.addCpG(cpg);
				} else if (items[0].charAt(i) == 'A' || items[0].charAt(i) == 'C' || items[0].charAt(i) == 'G' || items[0].charAt(i) == 'T') {
					patternResult.addAllele(i);
				}
			}
			patternResultLists.add(patternResult);
			patternResult.setCount(Integer.valueOf(items[1]));
			patternResult.setPercent(Double.valueOf(items[2]));
			line = patternBuffReader.readLine();
		}
		patternBuffReader.close();
	}

	private void setCoordinate() {
		beginCoor = coordinate.getChr() + ":" + coordinate.getStart();
		endCoor = String.valueOf(coordinate.getStart() + refLength);
		if ((coordinate.getStart() + refLength) != coordinate.getEnd()) {
			System.out.println(coordinate.getChr() + "\tEnd:" + coordinate.getEnd() + "\tNewEnd:" + (coordinate.getStart() + refLength));
		}
	}

	public String getBeginCoor() {
		return beginCoor;
	}

	public String getEndCoor() {
		return endCoor;
	}

	public ArrayList<PatternResult> getPatternResultLists() {
		return patternResultLists;
	}

	public ArrayList<Integer> getRefCpGs() {
		return refCpGs;
	}

	public int getRefLength() {
		return refLength;
	}

	public ArrayList<CpGStatistics> getStatList() {
		return statList;
	}

	public String getCellLine() {
		return cellLine;
	}
	
}