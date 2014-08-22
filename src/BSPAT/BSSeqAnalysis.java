package BSPAT;

import DataType.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.IOException;
import java.util.*;

/**
 * Bisulfite sequences analysis. Include obtaining methylation string, mutation
 * string, methyl&mutation string. And group sequences by pattern.
 *
 * @author Ke
 */
public class BSSeqAnalysis {
    /**
     * Execute analysis.
     *
     * @param experimentName
     * @param constant
     * @return List of ReportSummary
     */
    public List<ReportSummary> execute(String experimentName,
                                       Constant constant) throws IOException, InterruptedException {
        List<ReportSummary> reportSummaries = new ArrayList<>();
        String inputFolder = constant.mappingResultPath + experimentName + "/";
        String outputFolder = constant.patternResultPath + experimentName + "/";
        ImportBismarkResult importBismarkResult = new ImportBismarkResult(constant.originalRefPath, inputFolder);
        Map<String, String> referenceSeqs = importBismarkResult.getReferenceSeqs();
        List<Sequence> sequencesList = importBismarkResult.getSequencesList();
        if (sequencesList.size() == 0) {
            throw new RuntimeException("mapping result is empty, please double check input!");
        }

        // 1. read refCoorMap
        Map<String, Coordinate> refCoorMap;
        if (constant.coorReady) {
            refCoorMap = IO.readCoordinates(constant.coorFilePath, constant.coorFileName);
        } else {
            throw new RuntimeException("refCoorMap file is not ready!");
        }

        Map<String, Coordinate> targetCoorMap = IO.readCoordinates(constant.targetPath, constant.targetFileName);
        // 2. group seqs by region
        Map<String, List<Sequence>> sequenceGroupMap = groupSeqsByKey(sequencesList, new GetKeyFunction() {
            @Override
            public String apply(Sequence seq) {
                return seq.getRegion();
            }
        });

        // 3. cut and filter sequences
        cutAndFilterSequence(sequenceGroupMap, refCoorMap, targetCoorMap, referenceSeqs);
        // TODO handle case which no sequence remain after cut.

        // 4. generate report for each region
        for (String region : sequenceGroupMap.keySet()) {
            ReportSummary reportSummary = new ReportSummary(region);
            List<Sequence> seqGroup = sequenceGroupMap.get(region);

            Sequence.processSequence(referenceSeqs.get(region), seqGroup);
            reportSummary.setSeqBeforeFilter(seqGroup.size());
            seqGroup = filterSequences(seqGroup, constant);
            reportSummary.setSeqAfterFilter(seqGroup.size());
            // if no sequence exist after filtering, return empty reportSummary
            if (seqGroup.size() == 0) {
                continue;
            }
            List<Pattern> methylationPatternList = getMethylPattern(seqGroup);
            List<Pattern> mutationPatternList = getMutationPattern(seqGroup);
            List<Pattern> allelePatternList = getAllelePattern(seqGroup);

            methylationPatternList = filterMethylationPatterns(methylationPatternList, seqGroup.size(),
                                                               StringUtils.countMatches(referenceSeqs.get(region),
                                                                                        "CG"), constant);
            mutationPatternList = filterMutationPatterns(mutationPatternList, seqGroup.size(), constant);
            Pattern allelePattern = filterAllelePatterns(allelePatternList, seqGroup.size(), constant);
            Pattern nonAllelePattern = generateNonAllelePattern(allelePattern, seqGroup);

            Pattern.resetPatternCount();
            sortAndAssignPatternID(methylationPatternList);
            sortAndAssignPatternID(mutationPatternList);

            List<Pattern> meMuPatternList = getMeMuPatern(methylationPatternList, mutationPatternList);
            meMuPatternList = filterMethylationPatterns(meMuPatternList, seqGroup.size(),
                                                        StringUtils.countMatches(referenceSeqs.get(region), "CG"),
                                                        constant);


            Report report = new Report(region, outputFolder, seqGroup, referenceSeqs.get(region), constant,
                                       reportSummary);
            report.writeReport(methylationPatternList, mutationPatternList, meMuPatternList);

            if (constant.coorReady) {
                System.out.println("start drawing -- BSSeqAnalysis -- execute");
                DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
                                                              constant.toolsPath, region, outputFolder, experimentName,
                                                              targetCoorMap);
                drawFigureLocal.drawMethylPattern(reportSummary.getPatternLink(PatternLink.METHYLATION));
                drawFigureLocal.drawMethylPattern(reportSummary.getPatternLink(PatternLink.MUTATION));
                drawFigureLocal.drawMethylPattern(reportSummary.getPatternLink(PatternLink.MUTATIONWITHMETHYLATION));
                drawFigureLocal.drawMethylPattern(reportSummary.getPatternLink(PatternLink.METHYLATIONWITHMUTATION));
                drawFigureLocal.drawASMPattern(reportSummary, allelePattern, nonAllelePattern, seqGroup.size());

            }
            reportSummary.replacePath(Constant.DISKROOTPATH, constant.webRootPath, constant.coorReady, constant.host);
            reportSummaries.add(reportSummary);

        }
        return reportSummaries;
    }

    private Pattern generateNonAllelePattern(Pattern allelePattern, List<Sequence> seqGroup) {
        Pattern nonAllelePattern = new Pattern("", Pattern.PatternType.NONALLELE);
        if (allelePattern != null) {
            nonAllelePattern.sequenceList().addAll(seqGroup);
            nonAllelePattern.sequenceList().removeAll(allelePattern.sequenceList());
        }
        return nonAllelePattern;
    }

    private List<Pattern> getAllelePattern(List<Sequence> seqGroup) {
        // generate allele pattern for each unique(position and character) allele
        Map<String, Pattern> allelePatternMap = new HashMap<>();
        for (Sequence sequence : seqGroup) {
            for (String allele : sequence.getAlleleList()) {
                if (allelePatternMap.containsKey(allele)) {
                    allelePatternMap.get(allele).addSequence(sequence);
                } else {
                    allelePatternMap.put(allele, new Pattern(allele, Pattern.PatternType.ALLELE));
                    allelePatternMap.get(allele).addSequence(sequence);
                }
            }
        }
        return new ArrayList<>(allelePatternMap.values());
    }

    private void sortAndAssignPatternID(List<Pattern> patternList) {
        // sort methylation pattern.
        Collections.sort(patternList, new PatternByCountComparator());
        // assign pattern id after sorting. So id is associated with order. Smaller id has large count.
        for (Pattern pattern : patternList) {
            pattern.assignPatternID();
        }
    }

    // TODO replace list intersection with sequence parent id checking
    private List<Pattern> getMeMuPatern(List<Pattern> methylationPatternList, List<Pattern> mutationPatternList) {
        List<Pattern> meMuPaternList = new ArrayList<>();
        for (Pattern methylationPattern : methylationPatternList) {
            for (Pattern mutationPattern : mutationPatternList) {
                Pattern memuPatern = new Pattern("", Pattern.PatternType.MEMU);
                for (Sequence methylSeq : methylationPattern.sequenceList()) {
                    for (Sequence mutationSeq : mutationPattern.sequenceList()) {
                        // find intersection of two list
                        if (methylSeq == mutationSeq) {
                            memuPatern.addSequence(methylSeq);
                        }
                    }
                }
                if (memuPatern.sequenceList().size() != 0) {
                    memuPatern.setPatternString(memuPatern.sequenceList().get(0).getMeMuString());
                    memuPatern.setMethylationParentID(methylationPattern.getPatternID());
                    memuPatern.setMutationParentID(mutationPattern.getPatternID());
                    meMuPaternList.add(memuPatern);
                }
            }
        }
        return meMuPaternList;
    }

    private Pattern filterAllelePatterns(List<Pattern> allelePatternList, int totalSeqCount, Constant constant) {
        List<Pattern> qualifiedAllelePAtternList = new ArrayList<>();
        for (Pattern pattern : allelePatternList) {
            double percentage = (double) pattern.getCount() / totalSeqCount;
            if (percentage >= constant.mutationPatternThreshold) {
                qualifiedAllelePAtternList.add(pattern);
            }
        }
        Collections.sort(qualifiedAllelePAtternList, new PatternByCountComparator());
        // only keep most significant allele pattern
        if (qualifiedAllelePAtternList.size() > 0) {
            return qualifiedAllelePAtternList.get(0);
        } else {
            return null;
        }
    }

    private List<Pattern> filterMutationPatterns(List<Pattern> mutationPatterns, int totalSeqCount, Constant constant) {
        List<Pattern> qualifiedMutationPatternList = new ArrayList<>();
        for (Pattern mutationPattern : mutationPatterns) {
            double percentage = (double) mutationPattern.getCount() / totalSeqCount;
            if (percentage >= constant.mutationPatternThreshold) {
                qualifiedMutationPatternList.add(mutationPattern);
            }
        }
        return qualifiedMutationPatternList;
    }

    private List<Pattern> filterMethylationPatterns(List<Pattern> methylationPatterns, double totalSeqCount,
                                                    int cpgCount, Constant constant) {
        List<Pattern> qualifiedMethylationPatternList = new ArrayList<>();
        if (constant.minP0Threshold != -1 && cpgCount >= 3) {
            filterMethylPatternsByP0Threshold(methylationPatterns, totalSeqCount, qualifiedMethylationPatternList,
                                              constant);
            if (qualifiedMethylationPatternList.size() == 0) {
                filterMethylPatternsByMethylThreshold(methylationPatterns, totalSeqCount,
                                                      qualifiedMethylationPatternList, constant);
            }
        } else {
            filterMethylPatternsByMethylThreshold(methylationPatterns, totalSeqCount, qualifiedMethylationPatternList,
                                                  constant);
        }
        return qualifiedMethylationPatternList;
    }

    private void filterMethylPatternsByMethylThreshold(List<Pattern> methylationPatterns, double totalSeqCount,
                                                       List<Pattern> qualifiedMethylationPatternList,
                                                       Constant constant) {
        // use percentage pattern threshold
        for (Pattern methylationPattern : methylationPatterns) {
            double percentage = methylationPattern.getCount() / totalSeqCount;
            if (percentage >= constant.minMethylThreshold) {
                qualifiedMethylationPatternList.add(methylationPattern);
            }
        }
    }

    private void filterMethylPatternsByP0Threshold(List<Pattern> methylationPatterns, double totalSeqCount,
                                                   List<Pattern> qualifiedMethylationPatternList, Constant constant) {
        // calculate methylation rate for each CpG site.
        double p = 0.5;// probability of CpG site to be methylated.
        double z;
        double ph, p0;
        NormalDistributionImpl nd = new NormalDistributionImpl(0, 1);
        double criticalZ = 0;
        try {
            criticalZ = nd.inverseCumulativeProbability(1 - constant.criticalValue / totalSeqCount);
        } catch (MathException e) {
            e.printStackTrace();
            throw new RuntimeException("MathException in calculating of critical level", e);
        }
        // significant pattern selection
        for (Pattern methylationPattern : methylationPatterns) {
            ph = methylationPattern.getCount() / totalSeqCount;
            p0 = Math.max(constant.minP0Threshold, Math.pow(p, methylationPattern.getCGcount()));
            z = (ph - p0) / Math.sqrt(ph * (1 - ph) / totalSeqCount);
            if (z > criticalZ) {
                qualifiedMethylationPatternList.add(methylationPattern);
            }
        }
    }

    /**
     * cutting mapped sequences to reference region and filter reads without covering whole reference seq
     *
     * @param sequenceGroupMap
     */
    private void cutAndFilterSequence(Map<String, List<Sequence>> sequenceGroupMap, Map<String, Coordinate> refCoorMap,
                                      Map<String, Coordinate> targetCoorMap,
                                      Map<String, String> referenceSeqs) throws IOException {
        for (String region : sequenceGroupMap.keySet()) {
            Coordinate targetCoor = targetCoorMap.get(region);
            Coordinate refCoor = refCoorMap.get(region);
            List<Sequence> sequenceGroup = sequenceGroupMap.get(region);
            Iterator<Sequence> sequenceIterator = sequenceGroup.iterator();
            // cut reference seq
            referenceSeqs.put(region,
                              referenceSeqs.get(region).substring((int) (targetCoor.getStart() - refCoor.getStart()),
                                                                  (int) (targetCoor.getEnd() - refCoor.getStart())));
            while (sequenceIterator.hasNext()) {
                Sequence sequence = sequenceIterator.next();
                int refStart = Constant.REFEXTENSIONLENGTH - 1 + (int) (targetCoor.getStart() - refCoor.getStart());
                int refEnd = Constant.REFEXTENSIONLENGTH + (int) (targetCoor.getEnd() - refCoor.getStart()) - 2;
                if (sequence.getStartPos() <= refStart && sequence.getEndPos() >= refEnd) {
                    // cut sequence to suit reference
                    sequence.setOriginalSeq(sequence.getOriginalSeq().substring(refStart - sequence.getStartPos(),
                                                                                refEnd - sequence.getStartPos() + 1));
                    // update CpG sites
                    Iterator<CpGSite> cpGSiteIterator = sequence.getCpGSites().iterator();
                    while (cpGSiteIterator.hasNext()) {
                        CpGSite cpGSite = cpGSiteIterator.next();
                        // only keep CpG site wholly sit in ref.
                        if (cpGSite.getPosition() >= refStart && cpGSite.getPosition() + 1 <= refEnd) {
                            cpGSite.setPosition(cpGSite.getPosition() - refStart + 1);
                        } else {
                            cpGSiteIterator.remove();
                        }
                    }
                    sequence.setStartPos(1);
                } else {
                    // filter out
                    sequenceIterator.remove();
                }
            }
        }
    }

    /**
     * group sequences by given key function
     *
     * @param sequencesList
     * @param getKey        function parameter to return String key.
     * @return HashMap contains <key function return value, grouped sequence list>
     */
    private Map<String, List<Sequence>> groupSeqsByKey(List<Sequence> sequencesList, GetKeyFunction getKey) {
        Map<String, List<Sequence>> sequenceGroupMap = new HashMap<>();
        for (Sequence seq : sequencesList) {
            if (sequenceGroupMap.containsKey(getKey.apply(seq))) {
                sequenceGroupMap.get(getKey.apply(seq)).add(seq);
            } else {
                List<Sequence> sequenceGroup = new ArrayList<>();
                sequenceGroup.add(seq);
                sequenceGroupMap.put(getKey.apply(seq), sequenceGroup);
            }
        }
        return sequenceGroupMap;
    }

    /**
     * fill sequence list filtered by threshold.
     *
     * @param seqList
     * @return
     */
    private List<Sequence> filterSequences(List<Sequence> seqList, Constant constant) {
        System.out.println("Filter Sequences: before filter count:\t" + seqList.size());
        List<Sequence> qualifiedSeqList = new ArrayList<>();
        List<Sequence> unQualifiedSeqList = new ArrayList<>();
        for (Sequence seq : seqList) {
            // filter unqualified reads
            if (seq.getBisulConversionRate() >= constant.conversionRateThreshold &&
                    seq.getSequenceIdentity() >= constant.sequenceIdentityThreshold) {
                qualifiedSeqList.add(seq);
            } else {
                unQualifiedSeqList.add(seq);
            }
        }
        System.out.println("Filter Sequences: after filter count:\t" + qualifiedSeqList.size());
        return qualifiedSeqList;
    }

    private List<Pattern> getMethylPattern(List<Sequence> seqList) {
        List<Pattern> methylationPatterns = new ArrayList<>();
        // group sequences by methylationString, distribute each seq into one pattern
        Map<String, List<Sequence>> patternMap = groupSeqsByKey(seqList, new GetKeyFunction() {
            @Override
            public String apply(Sequence seq) {
                return seq.getMethylationString();
            }
        });

        for (String methylString : patternMap.keySet()) {
            List<Sequence> patternSeqList = patternMap.get(methylString);
            Pattern methylationPattern = new Pattern(methylString, Pattern.PatternType.METHYLATION);
            for (Sequence seq : patternSeqList) {
                methylationPattern.addSequence(seq);
            }
            methylationPatterns.add(methylationPattern);
        }
        return methylationPatterns;
    }

    private List<Pattern> getMutationPattern(List<Sequence> seqList) {
        List<Pattern> mutationPatterns = new ArrayList<>();
        // group sequences by mutationString, distribute each seq into one pattern
        Map<String, List<Sequence>> patternMap = groupSeqsByKey(seqList, new GetKeyFunction() {
            @Override
            public String apply(Sequence seq) {
                return seq.getMutationString();
            }
        });

        for (String mutationString : patternMap.keySet()) {
            List<Sequence> patternSeqList = patternMap.get(mutationString);
            Pattern mutationPattern = new Pattern(mutationString, Pattern.PatternType.METHYLATION);
            for (Sequence seq : patternSeqList) {
                mutationPattern.addSequence(seq);
            }
            mutationPatterns.add(mutationPattern);
        }
        return mutationPatterns;
    }
}
