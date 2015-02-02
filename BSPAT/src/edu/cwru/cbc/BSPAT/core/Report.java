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

import static edu.cwru.cbc.BSPAT.core.Utilities.getBoundedSeq;

public class Report {
    private String referenceSeq;
    private String outputFolder;
    private String region;
    private ReportSummary reportSummary;
    private Constant constant;
    private List<CpGStatistics> cpgStatList;

    public Report(String region, String outputPath, String referenceSeq, Constant constant,
                  ReportSummary reportSummary) {
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

    public void writeReport(Pair<List<Sequence>, List<Sequence>> filteredTargetSequencePair,
                            Pair<List<Sequence>, List<Sequence>> filteredCpGSequencePair,
                            List<Pattern> methylationPatternList, List<Pattern> mutationPatternList,
                            List<Pattern> meMuPatternList) throws IOException {
        List<Sequence> combinedSequenceList = new ArrayList<>();
        combinedSequenceList.addAll(filteredTargetSequencePair.getLeft());
        combinedSequenceList.addAll(filteredCpGSequencePair.getLeft());
        writeAnalysedSequences("_bismark.analysis.txt", filteredTargetSequencePair.getLeft());
        writeAnalysedSequences("_bismark.analysis.filtered.txt", filteredTargetSequencePair.getRight());
        writeAnalysedSequences("_bismark.analysis_CpGBounded.txt", filteredCpGSequencePair.getLeft());
        writeAnalysedSequences("_bismark.analysis_CpGBounded.filtered.txt", filteredCpGSequencePair.getRight());
        writeStatistics(filteredTargetSequencePair.getLeft(), combinedSequenceList);
        writePatterns(methylationPatternList, PatternLink.METHYLATION, combinedSequenceList);
        writePatterns(mutationPatternList, PatternLink.MUTATION, filteredTargetSequencePair.getLeft());
        writePatterns(meMuPatternList, PatternLink.METHYLATIONWITHMUTATION, filteredTargetSequencePair.getLeft());
        writePatterns(meMuPatternList, PatternLink.MUTATIONWITHMETHYLATION, filteredTargetSequencePair.getLeft());
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

    private void writeStatistics(List<Sequence> targetSequencesList, List<Sequence> combinedSequencesList) throws IOException {
        String reportFileName = outputFolder + region + "_bismark.analysis_report.txt";
        reportSummary.setStatTextLink(reportFileName);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reportFileName))) {
            Hashtable<Integer, CpGStatistics> cpgStatHashtable = new Hashtable<>();

            // collect information for calculating methylation rate for each CpG site.
            for (Sequence seq : combinedSequencesList) {
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

            cpgStatList = new ArrayList<>(cpgStatHashtable.values());
            Collections.sort(cpgStatList, new CpGStatComparator());
            bufferedWriter.write("target region length:\t" + referenceSeq.length() + "\n");
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

            bufferedWriter.write("mutation stat:\n");
            int[] mutationStat;
            mutationStat = new int[referenceSeq.length()];
            // give mutationStat array zero value
            for (int i : mutationStat) {
                mutationStat[i] = 0;
            }
            for (Sequence seq : targetSequencesList) {
                char[] mutationArray = seq.getMutationString().toCharArray();
                for (int i = 0; i < mutationArray.length; i++) {
                    if (mutationArray[i] == 'A' || mutationArray[i] == 'C' || mutationArray[i] == 'G' ||
                            mutationArray[i] == 'T') {
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
                    bufferedWriter.write(String.format("%s\tcount\tpercentage\tPatternID\n", patternType));
                    bufferedWriter.write(String.format("%s\tref\n", getBoundedSeq("CG", referenceSeq)));
                    for (Pattern pattern : patternList) {
                        bufferedWriter.write(
                                String.format("%s\t%d\t%f\t%d\n", pattern.getPatternString(), pattern.getCount(),
                                              pattern.getCount() / (double) sequencesList.size(),
                                              pattern.getPatternID()));
                    }
                    break;
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

    public List<CpGStatistics> getCpgStatList() {
        return cpgStatList;
    }
}
