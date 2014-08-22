package BSPAT;

import DataType.*;
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
	private String referenceSeq;
	private String outputFolder;
	private String region;
	private ReportSummary reportSummary;
	private Constant constant;

    public Report(String region, String outputPath, String referenceSeq, Constant constant, ReportSummary reportSummary) {
		this.constant = constant;
		this.reportSummary = reportSummary;
		this.region = region;
		this.outputFolder = outputPath;
		this.referenceSeq = referenceSeq;
		File outputFolder = new File(outputPath);
		if (!outputFolder.exists()) {
			outputFolder.mkdirs();
		}
	}

    public void writeReport(Pair<List<Sequence>, List<Sequence>> filteredSequencePair,
                            List<Pattern> methylationPatternList, List<Pattern> mutationPatternList, List<Pattern> meMuPatternList) throws IOException {
        writeAnalysedSequences("_bismark.analysis.txt", filteredSequencePair.getLeft());
        writeAnalysedSequences("_bismark.analysis.filtered.txt", filteredSequencePair.getRight());
        writeStatistics(filteredSequencePair.getLeft());
        writePatterns(methylationPatternList, PatternLink.METHYLATION, filteredSequencePair.getLeft());
        writePatterns(mutationPatternList, PatternLink.MUTATION, filteredSequencePair.getLeft());
        writePatterns(meMuPatternList, PatternLink.METHYLATIONWITHMUTATION, filteredSequencePair.getLeft());
        writePatterns(meMuPatternList, PatternLink.MUTATIONWITHMETHYLATION, filteredSequencePair.getLeft());
    }

    private void writeAnalysedSequences(String fileName, List<Sequence> sequencesList) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFolder + region + fileName))) {
			bufferedWriter.write(
					"methylationString\tID\toriginalSequence\tBisulfiteConversionRate\tmethylationRate\tsequenceIdentity\n");
			bufferedWriter.write(String.format("%s\tref\n", referenceSeq));
			for (Sequence seq : sequencesList) {
				bufferedWriter.write(seq.getMeMuString() + "\t" + seq.getId() + "\t" + seq.getOriginalSeq() + "\t" +
											 seq.getBisulConversionRate() + "\t" + seq.getMethylationRate() + "\t" +
											 seq.getSequenceIdentity() + "\n");
			}
		}
	}

    private void writeStatistics(List<Sequence> sequencesList) throws IOException {
        String reportFileName = outputFolder + region + "_bismark.analysis_report.txt";
		reportSummary.setStatTextLink(reportFileName);
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reportFileName))) {
			Hashtable<Integer, CpGStatistics> cpgStatHashtable = new Hashtable<>();

			// collect information for calculating methylation rate for each CpG site.
			for (Sequence seq : sequencesList) {
				for (CpGSite cpg : seq.getCpGSites()) {
					if (!cpgStatHashtable.containsKey(cpg.getPosition())) {
						CpGStatistics cpgStat = new CpGStatistics(cpg.getPosition());
						cpgStat.allSitePlus();
                        if (cpg.isMethylated()) {
                            cpgStat.methylSitePlus();
						}
						cpgStatHashtable.put(cpg.getPosition(), cpgStat);
					} else {
						CpGStatistics cpgStat = cpgStatHashtable.get(cpg.getPosition());
						cpgStat.allSitePlus();
                        if (cpg.isMethylated()) {
                            cpgStat.methylSitePlus();
						}
					}
				}
			}

			List<CpGStatistics> statList = new ArrayList<>(cpgStatHashtable.values());
			Collections.sort(statList, new CpGStatComparator());
			bufferedWriter.write("reference seq length:\t" + referenceSeq.length() + "\n");
			bufferedWriter.write("Bisulfite conversion rate threshold:\t" + constant.conversionRateThreshold + "\n");
			bufferedWriter.write("Sequence identity threshold:\t" + constant.sequenceIdentityThreshold + "\n");
			bufferedWriter.write("Sequences before filter:\t" + reportSummary.getSeqBeforeFilter() + "\n");
			bufferedWriter.write("Sequences after filter:\t" + reportSummary.getSeqAfterFilter() + "\n");
			bufferedWriter.write("methylation rate for each CpG site:\n");
			bufferedWriter.write("pos\trate" + "\n");

			for (CpGStatistics cpgStat : statList) {
                cpgStat.calcMethylLevel();
                // bismark result is 1-based. Need change to 0-based
                bufferedWriter.write(cpgStat.getPosition() - 1 + "\t" + cpgStat.getMethylLevel() + "\n");
            }

            bufferedWriter.write("mutation stat:\n");
            int[] mutationStat;
            mutationStat = new int[referenceSeq.length()];
            // give mutationStat array zero value
            for (int i : mutationStat) {
                mutationStat[i] = 0;
            }
            for (Sequence seq : sequencesList) {
                char[] mutationArray = seq.getMutationString().toCharArray();
                for (int i = 0; i < mutationArray.length; i++) {
                    if (mutationArray[i] == 'A' || mutationArray[i] == 'C' || mutationArray[i] == 'G' || mutationArray[i] == 'T') {
                        mutationStat[i]++;
                    }
                }
            }
            for (int i = 0; i < mutationStat.length; i++) {
                // 1 based position
                bufferedWriter.write((i + 1) + "\t" + mutationStat[i] + "\n");
            }
		}
	}

    private void writePatterns(List<Pattern> patternList, String patternType,
                               List<Sequence> sequencesList) throws IOException {
        String patternFileName = String.format("%s%s_bismark.analysis_%s.txt", outputFolder, region, patternType);
		reportSummary.getPatternLink(patternType).setTextResultLink(patternFileName);
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(patternFileName))) {
			switch (patternType) {
				case PatternLink.METHYLATION:
				case PatternLink.MUTATION:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tPatternID\n", patternType));
					bufferedWriter.write(String.format("%s\tref\n", referenceSeq));
					for (Pattern pattern : patternList) {
						bufferedWriter.write(
								String.format("%s\t%d\t%f\t%d\n", pattern.getPatternString(), pattern.getCount(),
											  pattern.getCount() / (double) sequencesList.size(),
											  pattern.getPatternID()));
					}
					break;
				case PatternLink.METHYLATIONWITHMUTATION:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tMethylParent\tMutationParent\n",
													   PatternLink.METHYLATIONWITHMUTATION));
					bufferedWriter.write(String.format("%s\tref\n", referenceSeq));
					Collections.sort(patternList, new MeMuPatternComparator());
					for (Pattern pattern : patternList) {
						bufferedWriter.write(
								String.format("%s\t%d\t%f\t%d\t%d\n", pattern.getPatternString(), pattern.getCount(),
											  pattern.getCount() / (double) sequencesList.size(),
											  pattern.getMethylationParentID(), pattern.getMutationParentID()));
					}
					break;
				case PatternLink.MUTATIONWITHMETHYLATION:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tMethylParent\tMutationParent\n",
													   PatternLink.METHYLATIONWITHMUTATION));
					bufferedWriter.write(String.format("%s\tref\n", referenceSeq));
					Collections.sort(patternList, new MuMePatternComparator());
					for (Pattern pattern : patternList) {
						bufferedWriter.write(
								String.format("%s\t%d\t%f\t%d\t%d\n", pattern.getPatternString(), pattern.getCount(),
											  pattern.getCount() / (double) sequencesList.size(),
											  pattern.getMethylationParentID(), pattern.getMutationParentID()));
					}
					break;
				default:
					throw new RuntimeException("unknown pattern type!");
			}
		}
	}
}
