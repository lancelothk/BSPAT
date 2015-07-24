package edu.cwru.cbc.BSPAT.core;

import edu.cwru.cbc.BSPAT.DataType.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class Report {
	private String targetRefSeq;
	private String cpgRefSeq;
	private int targetStart;
	private String outputFolder;
	private String region;
	private ReportSummary reportSummary;
	private Constant constant;
	private List<CpGStatistics> cpgStatList;

	public Report(String region, String outputPath, String targetRefSeq, int targetStart, String cpgRefSeq,
	              Constant constant,
	              ReportSummary reportSummary) {
		this.constant = constant;
		this.targetStart = targetStart;
		this.reportSummary = reportSummary;
		this.region = region;
		this.outputFolder = outputPath;
		this.targetRefSeq = targetRefSeq;
		this.cpgRefSeq = cpgRefSeq;
		File outputFolder = new File(outputPath);
		if (!outputFolder.exists()) {
			if (!outputFolder.mkdirs()) {
				System.err.printf("failed to create folder: %s\n", outputFolder);
			}
		}
	}

	public void writeReport(Pair<List<Sequence>, List<Sequence>> filteredTargetSequencePair,
	                        Pair<List<Sequence>, List<Sequence>> filteredCpGSequencePair,
	                        int[][] mutationStat) throws IOException {
		List<Sequence> combinedSequenceList = new ArrayList<>();
		combinedSequenceList.addAll(filteredTargetSequencePair.getLeft());
		combinedSequenceList.addAll(filteredCpGSequencePair.getLeft());
		writeAnalysedSequences("_bismark.analysis.txt", filteredTargetSequencePair.getLeft());
		writeAnalysedSequences("_bismark.analysis.filtered.txt", filteredTargetSequencePair.getRight());
		writeAnalysedSequences("_bismark.analysis_CpGBounded.txt", filteredCpGSequencePair.getLeft());
		writeAnalysedSequences("_bismark.analysis_CpGBounded.filtered.txt", filteredCpGSequencePair.getRight());
		writeStatistics(combinedSequenceList, mutationStat);
	}

	private void writeAnalysedSequences(String fileName, List<Sequence> sequencesList) throws IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFolder + region + fileName))) {
			bufferedWriter.write(
					"methylationString\tID\toriginalSequence\tBisulfiteConversionRate\tmethylationRate\tsequenceIdentity\n");
			for (Sequence seq : sequencesList) {
				bufferedWriter.write(
						seq.getMethylationString() + "\t" + seq.getId() + "\t" + seq.getOriginalSeq() + "\t" +
								seq.getBisulConversionRate() + "\t" + seq.getMethylationRate() + "\t" +
								seq.getSequenceIdentity() + "\n");
			}
		}
	}

	private void writeStatistics(List<Sequence> combinedSequencesList, int[][] mutationStat) throws IOException {
		String reportFileName = outputFolder + region + "_bismark.analysis_report.txt";
		reportSummary.setStatTextLink(reportFileName);
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reportFileName))) {
			Hashtable<Integer, CpGStatistics> cpgStatHashTable = new Hashtable<>();
			// collect information for calculating methylation rate for each CpG site.
			for (Sequence seq : combinedSequencesList) {
				for (CpGSite cpg : seq.getCpGSites()) {
					if (!cpgStatHashTable.containsKey(cpg.getPosition())) {
						CpGStatistics cpgStat = new CpGStatistics(cpg.getPosition());
						cpgStat.allSitePlus();
						if (cpg.isMethylated()) {
							cpgStat.methylSitePlus();
						}
						cpgStatHashTable.put(cpg.getPosition(), cpgStat);
					} else {
						CpGStatistics cpgStat = cpgStatHashTable.get(cpg.getPosition());
						cpgStat.allSitePlus();
						if (cpg.isMethylated()) {
							cpgStat.methylSitePlus();
						}
					}
				}
			}

			cpgStatList = new ArrayList<>();
			for (CpGStatistics cpgStat : cpgStatHashTable.values()) {
				if (cpgStat.getPosition() == targetStart - 1) { // display half cpg in the beginning of pattern.
					cpgStatList.add(cpgStat);
				} else if (cpgStat.getPosition() >= targetStart && cpgStat.getPosition() <= targetStart + targetRefSeq.length() - 1) {
					cpgStatList.add(cpgStat);
				}
			}

			cpgStatList.sort(CpG::compareTo);
			bufferedWriter.write("target region start position:\t" + targetStart + "\n");
			bufferedWriter.write("target region length:\t" + targetRefSeq.length() + "\n");
			bufferedWriter.write("Bisulfite conversion rate threshold:\t" + constant.conversionRateThreshold + "\n");
			bufferedWriter.write("Sequence identity threshold:\t" + constant.sequenceIdentityThreshold + "\n");
			bufferedWriter.write(ReportSummary.targetBoundedText + "\t" + reportSummary.getSeqTargetBounded() + "\n");
			bufferedWriter.write(
					ReportSummary.targetAfterFilterText + "\t" + reportSummary.getSeqTargetAfterFilter() + "\n");
			bufferedWriter.write(ReportSummary.cpgBoundedText + "\t" + reportSummary.getSeqCpGBounded() + "\n");
			bufferedWriter.write(ReportSummary.cpgAfterFilterText + "\t" + reportSummary.getSeqCpGAfterFilter() + "\n");
			bufferedWriter.write(ReportSummary.othersText + "\t" + reportSummary.getSeqOthers() + "\n");
			bufferedWriter.write("methylation rate for each CpG site:\n");
			bufferedWriter.write("pos\trate" + "\n");

			for (CpGStatistics cpgStat : cpgStatList) {
				cpgStat.calcMethylLevel();
				bufferedWriter.write(cpgStat.getPosition() + "\t" + cpgStat.getMethylLevel() + "\n");
			}

			bufferedWriter.write("mismatch stat:\n");
			for (int i = 0; i < mutationStat.length; i++) {
				// 1 based position
				int total = mutationStat[i][0] + mutationStat[i][1] + mutationStat[i][2] + mutationStat[i][3] +
						mutationStat[i][4];
				int coverage = mutationStat[i][0] + mutationStat[i][1] + mutationStat[i][2] + mutationStat[i][3] +
						mutationStat[i][4] + mutationStat[i][5];
				bufferedWriter.write(
						String.format("%d\tref:%c\tA:%d\tC:%d\tG:%d\tT:%d\tN:%d\ttotal:%6d\tcoverage:%d\n", i + 1,
								targetRefSeq.charAt(i),
								mutationStat[i][0], mutationStat[i][1], mutationStat[i][2], mutationStat[i][3],
								mutationStat[i][4], total, coverage));
			}
		}
	}

	public void writePatterns(List<Pattern> patternList, String patternType,
	                          List<Sequence> sequencesList) throws IOException {
		String patternFileName = String.format("%s%s_bismark.analysis_%s.txt", outputFolder, region, patternType);
		PatternLink patternLink = reportSummary.getPatternLink(patternType);
		if (patternLink == null) {
			return;
		}
		patternLink.setTextResultLink(patternFileName);
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(patternFileName))) {
			switch (patternType) {
				case PatternLink.METHYLATION:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tPatternID\n", patternType));
					bufferedWriter.write(
							String.format("%s\tref\n", cpgRefSeq));
					for (Pattern pattern : patternList) {
						bufferedWriter.write(
								String.format("%s\t%d\t%f\t%d\n", pattern.getPatternString(), pattern.getCount(),
										pattern.getCount() / (double) sequencesList.size(),
										pattern.getPatternID()));
					}
					break;
				case PatternLink.METHYLATIONWITHSNP:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tMethylParent\n",
							PatternLink.METHYLATIONWITHSNP));
					bufferedWriter.write(String.format("%s\tref\n", targetRefSeq));
					Collections.sort(patternList, new MeMuPatternComparator());
					for (Pattern pattern : patternList) {
						bufferedWriter.write(
								String.format("%s\t%d\t%f\t%d\n", pattern.getPatternString(), pattern.getCount(),
										pattern.getCount() / (double) sequencesList.size(),
										pattern.getMethylationParentID()));
					}
					break;
				default:
					throw new RuntimeException("unknown pattern type!");
			}
		}
	}

	public List<CpGStatistics> getCpgStatList() {
		return cpgStatList;
	}
}
