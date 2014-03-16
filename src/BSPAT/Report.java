package BSPAT;

import DataType.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class Report {
    private Hashtable<String, String> referenceSeqs = new Hashtable<String, String>();
    private List<Sequence> sequencesList = new ArrayList<Sequence>();
    private List<Pattern> methylationPatterns = new ArrayList<Pattern>();
    private List<Pattern> mutationPatterns = new ArrayList<Pattern>();
    private int[] mutationStat;
    private String outputFolder;
    private String region;
    private int totalCount;
    private String FRState;
    private ReportSummary reportSummary;
    private Constant constant;

    public Report(String FRState, String region, String outputPath, List<Sequence> sequencesList, List<Pattern> methylationPatterns, List<Pattern> mutationPatterns, Hashtable<String, String> referenceSeqs, int totalCount, ReportSummary reportSummary, Constant constant) {
        this.constant = constant;
        this.reportSummary = reportSummary;
        this.FRState = FRState;
        this.region = region;
        this.outputFolder = outputPath;
        this.sequencesList = sequencesList;
        this.methylationPatterns = methylationPatterns;
        this.mutationPatterns = mutationPatterns;
        this.referenceSeqs = referenceSeqs;
        this.totalCount = totalCount;
        File outputFolder = new File(outputPath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

    }

    public void writeResult() throws IOException {
        FileWriter fileWriter = new FileWriter(outputFolder + region + FRState + "_bismark.analysis.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("methylationString\tID\toriginalSequence\tBisulfiteConversionRate\tmethylationRate\tsequenceIdentity\n");
        bufferedWriter.write(String.format("%s\tref\n", referenceSeqs.get(region)));
        for (Sequence seq : sequencesList) {
            bufferedWriter.write(seq.getMethylationStringWithMutations() + "\t" + seq.getId() + "\t" + seq.getOriginalSeq() + "\t" + seq.getBisulConversionRate() + "\t" + seq.getMethylationRate() + "\t" + seq.getSequenceIdentity() + "\n");
        }
        bufferedWriter.close();
    }

    public void writeStatistics() throws IOException {
        String reportFileName = outputFolder + region + FRState + "_bismark.analysis_report.txt";
        FileWriter fileWriter = new FileWriter(reportFileName);
        reportSummary.setStatTextLink(reportFileName);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        Hashtable<Integer, CpGStatistics> cpgStatHashtable = new Hashtable<Integer, CpGStatistics>();

        // collect information for calculating methylation rate for each CpG
        // site.
        for (Sequence seq : sequencesList) {
            for (CpGSite cpg : seq.getCpGSites()) {
                if (!cpgStatHashtable.containsKey(cpg.getPosition())) {
                    CpGStatistics cpgStat = new CpGStatistics(cpg.getPosition());
                    cpgStat.allSitePlus();
                    if (cpg.getMethylLabel() == true) {
                        cpgStat.methylSitePlus();
                    }
                    cpgStatHashtable.put(cpg.getPosition(), cpgStat);
                } else {
                    CpGStatistics cpgStat = cpgStatHashtable.get(cpg.getPosition());
                    cpgStat.allSitePlus();
                    if (cpg.getMethylLabel() == true) {
                        cpgStat.methylSitePlus();
                    }
                }
            }
        }

        List<CpGStatistics> statList = new ArrayList<CpGStatistics>(cpgStatHashtable.values());
        Collections.sort(statList, new CpGStatComparator());
        bufferedWriter.write("reference seq length:\t" + referenceSeqs.get(region).length() + "\n");
        bufferedWriter.write("Bisulfite conversion rate threshold:\t" + constant.conversionRateThreshold + "\n");
        bufferedWriter.write("Sequence identity threshold:\t" + constant.sequenceIdentityThreshold + "\n");
        bufferedWriter.write("Sequences before filter:\t" + totalCount + "\n");
        bufferedWriter.write("Sequences after filter:\t" + sequencesList.size() + "\n");
        bufferedWriter.write("methylation rate for each CpG site:\n");
        bufferedWriter.write("pos\trate" + "\n");

        for (CpGStatistics cpgStat : statList) {
            cpgStat.calcMethylRate();
            // bismark result is 1-based. Need change to 0-based
            bufferedWriter.write(cpgStat.getPosition() - 1 + "\t" + cpgStat.getMethylationRate() + "\n");
        }

        bufferedWriter.close();
    }

    public void writeMethylationPatterns() throws IOException {
        String methylationFileName = String.format("%s%s%s_bismark.analysis_%s.txt", outputFolder, region, FRState, PatternLink.METHYLATION);
        String methylationWithMutationFileName = String.format("%s%s%s_bismark.analysis_%s.txt", outputFolder, region, FRState, PatternLink.METHYLATIONWITHMUTATION);
        FileWriter fileWriter = new FileWriter(methylationFileName);
        FileWriter fileWriterWithMutations = new FileWriter(methylationWithMutationFileName);
        reportSummary.getPatternLink(PatternLink.METHYLATION).setTextResultLink(methylationFileName);
        reportSummary.getPatternLink(PatternLink.METHYLATIONWITHMUTATION).setTextResultLink(methylationWithMutationFileName);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        BufferedWriter bufferedWriterWithMutations = new BufferedWriter(fileWriterWithMutations);

        bufferedWriter.write(String.format("%s\tcount\tpercentage\tID\n", PatternLink.METHYLATION));
        bufferedWriter.write(String.format("%s\tref\n", referenceSeqs.get(region)));
        bufferedWriterWithMutations.write(String.format("%s\tcount\tpercentage\tParentPatternID\n", PatternLink.METHYLATIONWITHMUTATION));
        bufferedWriterWithMutations.write(String.format("%s\tref\n", referenceSeqs.get(region)));
        // sort methylation pattern.
        Collections.sort(methylationPatterns, new PatternComparator());

        double totalCount = sequencesList.size();
        if (totalCount == 0) {
            System.err.println("no reads result. Maybe due to read length longer than reference");
            bufferedWriter.close();
            bufferedWriterWithMutations.close();
            return;
        }

        // use percentage pattern threshold
        if (constant.minP0Threshold == -1) {
            double percentage;
            for (Pattern methylationPattern : methylationPatterns) {
                percentage = methylationPattern.getCount() / totalCount;
                if (percentage >= constant.minMethylThreshold) {
                    bufferedWriter.write(methylationPattern.getPatternString() + "\t" + methylationPattern.getCount() + "\t" + methylationPattern.getCount() / totalCount + "\t" + methylationPattern.getParrentPatternID() + "\n");

                    // sort child pattern.
                    Collections.sort(methylationPattern.getChildPatternsList(), new PatternComparator());
                    double totalCountMutation = methylationPattern.getCount();
                    for (Pattern childPattern : methylationPattern.getChildPatternsList()) {
                        double mutationPercentage = (double) childPattern.getCount() / totalCountMutation;
                        if (mutationPercentage >= constant.mutationPatternThreshold) {
                            bufferedWriterWithMutations.write(childPattern.getPatternString() + "\t" + childPattern.getCount() + "\t" + childPattern.getCount() / totalCountMutation + "\t" + childPattern.getParrentPatternID() + "\n");
                        }
                    }
                }
            }
        } else {
            // calculate methylation rate for each CpG site.
            double p = 0.5;// probability of CpG site to be methylated.
            double z;
            double ph, p0;

            NormalDistributionImpl nd = new NormalDistributionImpl(0, 1);
            double criticalZ = 0;
            try {
                criticalZ = nd.inverseCumulativeProbability(1 - constant.criticalValue / totalCount);
            } catch (MathException e) {
                e.printStackTrace();
            }
            List<Pattern> outputMethylationPattern = new ArrayList<Pattern>();
            // significant pattern selection
            for (Pattern methylationPattern : methylationPatterns) {
                ph = methylationPattern.getCount() / totalCount;
                p0 = Math.max(constant.minP0Threshold, Math.pow(p, methylationPattern.getCGcount()));
                z = (ph - p0) / Math.sqrt(ph * (1 - ph) / totalCount);
                if (z > criticalZ) {
                    outputMethylationPattern.add(methylationPattern);
                }
            }
            // if no significant pattern exist,  use minimum threshold to select patterns.
            if (outputMethylationPattern.size() == 0) {
                double percentage;
                for (Pattern methylationPattern : methylationPatterns) {
                    percentage = methylationPattern.getCount() / totalCount;
                    if (percentage >= constant.minMethylThreshold) {
                        bufferedWriter.write(methylationPattern.getPatternString() + "\t" + methylationPattern.getCount() + "\t" + methylationPattern.getCount() / totalCount + "\t" + methylationPattern.getParrentPatternID() + "\n");

                        // sort child pattern.
                        Collections.sort(methylationPattern.getChildPatternsList(), new PatternComparator());
                        for (Pattern childPattern : methylationPattern.getChildPatternsList()) {
                            double mutationPercentage = (double) childPattern.getCount() / totalCount;
                            if (mutationPercentage >= constant.mutationPatternThreshold) {
                                bufferedWriterWithMutations.write(childPattern.getPatternString() + "\t" + childPattern.getCount() + "\t" + childPattern.getCount() / totalCount + "\t" + childPattern.getParrentPatternID() + "\n");
                            }
                        }
                    }
                }
            } else {
                for (Pattern methylationPattern : outputMethylationPattern) {
                    bufferedWriter.write(methylationPattern.getPatternString() + "\t" + methylationPattern.getCount() + "\t" + methylationPattern.getCount() / totalCount + "\t" + methylationPattern.getParrentPatternID() + "\n");
                    // sort child pattern.
                    Collections.sort(methylationPattern.getChildPatternsList(), new PatternComparator());
                    for (Pattern childPattern : methylationPattern.getChildPatternsList()) {
                        double mutationPercentage = (double) childPattern.getCount() / totalCount;
                        if (mutationPercentage >= constant.mutationPatternThreshold) {
                            bufferedWriterWithMutations.write(childPattern.getPatternString() + "\t" + childPattern.getCount() + "\t" + childPattern.getCount() / totalCount + "\t" + childPattern.getParrentPatternID() + "\n");
                        }
                    }
                }
            }
        }
        bufferedWriter.close();
        bufferedWriterWithMutations.close();
    }

    public void writeMutationPatterns() throws IOException {
        String mutationFileName = String.format("%s%s%s_bismark.analysis_%s.txt", outputFolder, region, FRState, PatternLink.MUTATION);
        String mutationWithPatternsFileName = String.format("%s%s%s_bismark.analysis_%s.txt", outputFolder, region, FRState, PatternLink.MUTATIONWITHMETHYLATION);
        FileWriter fileWriter = new FileWriter(mutationFileName);
        FileWriter fileWriterWithPatterns = new FileWriter(mutationWithPatternsFileName);
        reportSummary.getPatternLink(PatternLink.MUTATION).setTextResultLink(mutationFileName);
        reportSummary.getPatternLink(PatternLink.MUTATIONWITHMETHYLATION).setTextResultLink(mutationWithPatternsFileName);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        BufferedWriter bufferedWriterWithPatterns = new BufferedWriter(fileWriterWithPatterns);

        bufferedWriter.write(String.format("%s\tcount\tpercentage\tID\n", PatternLink.MUTATION));
        bufferedWriter.write(String.format("%s\tref\n", referenceSeqs.get(region)));
        bufferedWriterWithPatterns.write(String.format("%s\tcount\tpercentage\tParentPatternID\n", PatternLink.MUTATIONWITHMETHYLATION));
        bufferedWriterWithPatterns.write(String.format("%s\tref\n", referenceSeqs.get(region)));
        Collections.sort(mutationPatterns, new PatternComparator());
        // sort mutation pattern.

        double totalCount = sequencesList.size();
        if (totalCount == 0) {
            System.err.println("no reads result. Maybe due to read length longer than reference");
            bufferedWriter.close();
            bufferedWriterWithPatterns.close();
            return;
        }
        for (Pattern mutationPattern : mutationPatterns) {
            double percentage = (double) mutationPattern.getCount() / sequencesList.size();
            if (percentage >= constant.mutationPatternThreshold) {
                bufferedWriter.write(mutationPattern.getPatternString() + "\t" + mutationPattern.getCount() + "\t" + mutationPattern.getCount() / totalCount + "\t" + mutationPattern.getParrentPatternID() + "\n");
                Collections.sort(mutationPattern.getChildPatternsList(), new PatternComparator());

                if (constant.minP0Threshold == -1) {
                    for (Pattern childPattern : mutationPattern.getChildPatternsList()) {
                        if (percentage >= constant.minMethylThreshold) {
                            bufferedWriterWithPatterns.write(childPattern.getPatternString() + "\t" + childPattern.getCount() + "\t" + childPattern.getCount() / totalCount + "\t" + childPattern.getParrentPatternID() + "\n");
                        }
                    }
                } else {

                    // calculate methylation rate for each CpG site.
                    double p = 0.5;// probability of CpG site to be methylated.
                    double z;
                    double ph, p0;

                    NormalDistributionImpl nd = new NormalDistributionImpl(0, 1);
                    double criticalZ = 0;
                    try {
                        criticalZ = nd.inverseCumulativeProbability(1 - constant.criticalValue / totalCount);
                    } catch (MathException e) {
                        e.printStackTrace();
                    }
                    List<Pattern> outputMethylationPattern = new ArrayList<Pattern>();
                    // significant pattern selection
                    for (Pattern childPattern : mutationPattern.getChildPatternsList()) {
                        ph = childPattern.getCount() / totalCount;
                        p0 = Math.max(constant.minP0Threshold, Math.pow(p, childPattern.getCGcount()));
                        z = (ph - p0) / Math.sqrt(ph * (1 - ph) / totalCount);
                        if (z > criticalZ) {
                            outputMethylationPattern.add(childPattern);
                        }
                    }
                    if (outputMethylationPattern.size() == 0) {
                        for (Pattern childPattern : mutationPattern.getChildPatternsList()) {
                            if (percentage >= constant.minMethylThreshold) {
                                bufferedWriterWithPatterns.write(childPattern.getPatternString() + "\t" + childPattern.getCount() + "\t" + childPattern.getCount() / totalCount + "\t" + childPattern.getParrentPatternID() + "\n");
                            }
                        }
                    } else {
                        for (Pattern childPattern : outputMethylationPattern) {
                            bufferedWriterWithPatterns.write(childPattern.getPatternString() + "\t" + childPattern.getCount() + "\t" + childPattern.getCount() / totalCount + "\t" + childPattern.getParrentPatternID() + "\n");
                        }
                    }
                }
            }
        }

        bufferedWriter.close();
        bufferedWriterWithPatterns.close();
    }
}
