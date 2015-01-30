package edu.cwru.cbc.BSPAT.core;

import edu.cwru.cbc.BSPAT.DataType.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Bisulfite sequences analysis. Include obtaining methylation string, mutation
 * string, methyl&mutation string. And group sequences by pattern.
 *
 * @author Ke
 */
public class BSSeqAnalysis {
    private final static Logger LOGGER = Logger.getLogger(BSSeqAnalysis.class.getName());
    private final int DEFAULTTARGETLENGTH = 70;

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

        Map<String, Coordinate> targetCoorMap;
        if (constant.targetFileName == null) {
            // get position of first CpG and generate DEFAULTTARGETLENGTH bp region.
            targetCoorMap = new HashMap<>();
            for (String key : refCoorMap.keySet()) {
                String refString = referenceSeqs.get(key);
                long firstPos = refString.indexOf("CG");
                long endPos = refCoorMap.get(key).getStart() + firstPos + DEFAULTTARGETLENGTH;
                targetCoorMap.put(key, new Coordinate(refCoorMap.get(key).getId(), refCoorMap.get(key).getChr(),
                                                      refCoorMap.get(key).getStrand(),
                                                      refCoorMap.get(key).getStart() + firstPos,
                                                      endPos < refCoorMap.get(key).getEnd() ? endPos : refCoorMap.get(
                                                              key).getEnd()));
            }
        } else {
            targetCoorMap = IO.readCoordinates(constant.targetPath, constant.targetFileName);
        }
        // 2. group seqs by region
        Map<String, List<Sequence>> sequenceGroupMap = groupSeqsByKey(sequencesList, new GetKeyFunction() {
            @Override
            public String apply(Sequence seq) {
                return seq.getRegion();
            }
        });

        // 3. generate report for each region
        for (String region : sequenceGroupMap.keySet()) {
            ReportSummary reportSummary = new ReportSummary(region);
            List<Sequence> CpGBoundSequenceList = cutAndFilterSequence(region, sequenceGroupMap, refCoorMap,
                                                                       targetCoorMap, referenceSeqs);
            List<Sequence> seqGroup = sequenceGroupMap.get(region);
            Sequence.processSequence(referenceSeqs.get(region), seqGroup);
            Sequence.processSequence(referenceSeqs.get(region), CpGBoundSequenceList);

            reportSummary.setSeqBeforeFilter(seqGroup.size());
            Pair<List<Sequence>, List<Sequence>> filteredSequencePair = filterSequences(seqGroup, constant);
            seqGroup = filteredSequencePair.getLeft();
            reportSummary.setSeqAfterFilter(seqGroup.size());
            // if no sequence exist after filtering, return empty reportSummary
            if (seqGroup.size() == 0 && CpGBoundSequenceList.size() == 0) {
                continue;
            }
            List<Pattern> methylationPatternList = getMethylPattern(seqGroup, CpGBoundSequenceList,
                                                                    referenceSeqs.get(region));
            List<Pattern> mutationPatternList = getMutationPattern(seqGroup);
            List<Pattern> allelePatternList = getAllelePattern(seqGroup);

            methylationPatternList = filterMethylationPatterns(methylationPatternList,
                                                               seqGroup.size() + CpGBoundSequenceList.size(),
                                                               StringUtils.countMatches(referenceSeqs.get(region),
                                                                                        "CG"), constant);
            mutationPatternList = filterMutationPatterns(mutationPatternList, seqGroup.size(), constant);


            Pattern.resetPatternCount();
            sortAndAssignPatternID(methylationPatternList);
            sortAndAssignPatternID(mutationPatternList);

            // generate memu pattern after filtering me & mu pattern since filtered non-significant patterns won't contribute to memu result.
            List<Pattern> meMuPatternList = getMeMuPatern(seqGroup, methylationPatternList, mutationPatternList);
            meMuPatternList = filterMethylationPatterns(meMuPatternList, seqGroup.size(),
                                                        StringUtils.countMatches(referenceSeqs.get(region), "CG"),
                                                        constant);

            Pattern allelePattern = filterAllelePatterns(allelePatternList, seqGroup.size(), constant);
            Pattern nonAllelePattern = generateNonAllelePattern(allelePattern, seqGroup);

            Report report = new Report(region, outputFolder, referenceSeqs.get(region), constant, reportSummary);
            report.writeReport(filteredSequencePair, methylationPatternList, mutationPatternList, meMuPatternList);

            if (constant.coorReady) {
                DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
                                                              constant.toolsPath, region, outputFolder, experimentName,
                                                              targetCoorMap, referenceSeqs.get(region));
                drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.METHYLATION));
                drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.MUTATION));
                drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.MUTATIONWITHMETHYLATION));
                drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.METHYLATIONWITHMUTATION));

                if (allelePattern != null && allelePattern.getSequenceMap().size() != 0 &&
                        nonAllelePattern.getSequenceMap().size() != 0) {

                    PatternResult patternWithAllele = patternToPatternResult(allelePattern, report.getCpgStatList(),
                                                                             seqGroup.size());
                    PatternResult patternWithoutAllele = patternToPatternResult(nonAllelePattern,
                                                                                report.getCpgStatList(),
                                                                                seqGroup.size());

                    if (!hasASM(patternWithAllele, patternWithoutAllele)) {
                        reportSummary.setHasASM(false);
                    } else {
                        reportSummary.setHasASM(true);
                        drawFigureLocal.drawASMPattern(reportSummary, patternWithAllele, patternWithoutAllele,
                                                       constant.logPath);
                    }
                }

            }
            reportSummary.replacePath(Constant.DISKROOTPATH, constant.webRootPath, constant.coorReady, constant.host);
            reportSummaries.add(reportSummary);
        }
        return reportSummaries;
    }

    private boolean hasASM(PatternResult patternWithAllele, PatternResult patternWithoutAllele) {
        // use 0.2 as threshold to filter out unequal patterns. ASM pattern should be roughly equal.
        if (patternWithAllele.getPercent() < 0.2 || patternWithoutAllele.getPercent() < 0.2) {
            return false;
        }
        List<CpGSitePattern> cglistWithAllele = patternWithAllele.getCpGList();
        List<CpGSitePattern> cglistWithoutAllele = patternWithoutAllele.getCpGList();
        // if there is at least one cpg site with different methyl type and the different bigger than 0.2, it is ASM
        for (int i = 0; i < cglistWithAllele.size(); i++) {
            if (cglistWithAllele.get(i).getMethylType() != cglistWithoutAllele.get(i).getMethylType() &&
                    Math.abs(cglistWithAllele.get(i).getMethylLevel() - cglistWithoutAllele.get(i).getMethylLevel()) >=
                            0.2) {
                return true;
            }
        }
        return false;
    }

    private PatternResult patternToPatternResult(Pattern pattern, List<CpGStatistics> cpGStatisticsList,
                                                 int totalCount) {
        PatternResult patternResult = new PatternResult();
        Map<Integer, CpGSitePattern> cpGSiteMap = new HashMap<>();
        for (CpGStatistics cpg : cpGStatisticsList) {
            if (cpGSiteMap.containsKey(cpg.getPosition())) {
                throw new RuntimeException("refCpG has duplicated CpGsites!");
            }
            cpGSiteMap.put(cpg.getPosition(), new CpGSitePattern(cpg.getPosition(), false));
        }
        for (Sequence sequence : pattern.getSequenceMap().values()) {
            for (CpGSite cpGSite : sequence.getCpGSites()) {
                int pos = cpGSite.getPosition();
                if (cpGSiteMap.containsKey(pos)) {
                    if (cpGSite.isMethylated()) {
                        cpGSiteMap.get(pos).addMethylCount(1);
                    } else {
                        cpGSiteMap.get(pos).addNonMethylCount(1);
                    }
                } else {
                    throw new RuntimeException("sequence contains cpgsite not in ref");
                }
            }
        }
        patternResult.setCpGList(new ArrayList<>(cpGSiteMap.values()));
        patternResult.setCount(pattern.getSequenceMap().size());
        patternResult.setPercent(pattern.getSequenceMap().size() / (double) totalCount);
        if (pattern.getPatternType() == Pattern.PatternType.ALLELE) {
            patternResult.addAllele(Integer.parseInt(pattern.getPatternString().split("-")[0]));
        } else if (pattern.getPatternType() != Pattern.PatternType.NONALLELE) {
            throw new RuntimeException("only support convert allele and non-allele Pattern to PatternResult");
        }
        return patternResult;
    }

    private Pattern generateNonAllelePattern(Pattern allelePattern, List<Sequence> seqGroup) {
        Pattern nonAllelePattern = new Pattern("", Pattern.PatternType.NONALLELE);
        if (allelePattern != null) {
            for (Sequence sequence : seqGroup) {
                nonAllelePattern.getSequenceMap().put(sequence.getId(), sequence);
            }
            for (String key : allelePattern.getSequenceMap().keySet()) {
                nonAllelePattern.getSequenceMap().remove(key);
            }
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

    /**
     * generate memu pattern.
     *
     * @param seqGroup
     * @param methylationPatternList
     * @param mutationPatternList
     * @return
     */
    private List<Pattern> getMeMuPatern(List<Sequence> seqGroup, List<Pattern> methylationPatternList,
                                        List<Pattern> mutationPatternList) {
        Map<String, Pattern> patternMap = new HashMap<>();
        for (Sequence sequence : seqGroup) {
            int meID = -1, muID = -1;
            for (Pattern methylPattern : methylationPatternList) {
                if (methylPattern.getSequenceMap().containsKey(sequence.getId())) {
                    meID = methylPattern.getPatternID();
                }
            }
            for (Pattern mutationPattern : mutationPatternList) {
                if (mutationPattern.getSequenceMap().containsKey(sequence.getId())) {
                    muID = mutationPattern.getPatternID();
                }
            }
            // seq not included in either MethylPattern or MutationPattern
            if (meID == -1 || muID == -1) {
                continue;
            }
            String key = String.format("%d-%d", meID, muID);
            if (patternMap.containsKey(key)) {
                patternMap.get(key).addSequence(sequence);
            } else {
                // new memu pattern
                Pattern memuPatern = new Pattern("", Pattern.PatternType.MEMU);
                memuPatern.setPatternString(sequence.getMeMuString());
                memuPatern.setMethylationParentID(meID);
                memuPatern.setMutationParentID(muID);
                memuPatern.addSequence(sequence);
                patternMap.put(key, memuPatern);
            }
        }
        return new ArrayList<>(patternMap.values());
    }

    private Pattern filterAllelePatterns(List<Pattern> allelePatternList, int totalSeqCount, Constant constant) {
        List<Pattern> qualifiedAllelePatternList = new ArrayList<>();
        for (Pattern pattern : allelePatternList) {
            double percentage = (double) pattern.getCount() / totalSeqCount;
            if (percentage >= constant.mutationPatternThreshold) {
                qualifiedAllelePatternList.add(pattern);
            }
        }
        Collections.sort(qualifiedAllelePatternList, new PatternByCountComparator());
        // only keep most significant allele pattern
        if (qualifiedAllelePatternList.size() > 0) {
            return qualifiedAllelePatternList.get(0);
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
        double criticalZ;
        try {
            criticalZ = nd.inverseCumulativeProbability(1 - constant.criticalValue / totalSeqCount);
        } catch (MathException e) {
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
    private List<Sequence> cutAndFilterSequence(String region, Map<String, List<Sequence>> sequenceGroupMap,
                                                Map<String, Coordinate> refCoorMap,
                                                Map<String, Coordinate> targetCoorMap,
                                                Map<String, String> referenceSeqs) throws IOException {
        List<Sequence> CpGBoundSequenceList = new ArrayList<>();
        Coordinate targetCoor = targetCoorMap.get(region);
        Coordinate refCoor = refCoorMap.get(region);
        if (targetCoor == null) {
            throw new RuntimeException("can not find target coordinate for region " + region);
        }
        List<Sequence> sequenceGroup = sequenceGroupMap.get(region);
        Iterator<Sequence> sequenceIterator = sequenceGroup.iterator();
        // cut reference seq
        int refStart = (int) (targetCoor.getStart() - refCoor.getStart());
        int refEnd = (int) (targetCoor.getEnd() - refCoor.getStart());
        referenceSeqs.put(region, referenceSeqs.get(region).substring(refStart, refEnd + 1));
        while (sequenceIterator.hasNext()) {
            Sequence sequence = sequenceIterator.next();
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
                        cpGSite.setPosition(cpGSite.getPosition() - refStart);
                    } else {
                        cpGSiteIterator.remove();
                    }
                }
                sequence.setStartPos(1);
            } else {
                // filter out
                sequenceIterator.remove();
                // recheck if sequence cover all CpGs in ref
                // seqs here are uncutted. Should be cutted in getMethylPattern
                int startCpGPos = referenceSeqs.get(region).indexOf("CG") + refStart;
                int endCpGPos = referenceSeqs.get(region).lastIndexOf("CG") + refStart;
                if (sequence.getStartPos() <= startCpGPos && sequence.getEndPos() >= (endCpGPos + 1)) {
                    CpGBoundSequenceList.add(sequence);
                }
            }
        }
        return CpGBoundSequenceList;
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
    private Pair<List<Sequence>, List<Sequence>> filterSequences(List<Sequence> seqList,
                                                                 Constant constant) throws IOException {
        LOGGER.info("Filter Sequences: before filter count:\t" + seqList.size());
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
        LOGGER.info("Filter Sequences: after filter count:\t" + qualifiedSeqList.size());
        return new ImmutablePair<>(qualifiedSeqList, unQualifiedSeqList);
    }

    private List<Pattern> getMethylPattern(List<Sequence> seqList, List<Sequence> CpGBoundSequenceList,
                                           String referenceSeq) {
        List<Sequence> combinedSequenceList = new ArrayList<>();
        combinedSequenceList.addAll(seqList);
        combinedSequenceList.addAll(CpGBoundSequenceList);

        // only keep CpG bound methylation string
        final int startCpGPos = referenceSeq.indexOf("CG");
        final int endCpGPos = referenceSeq.lastIndexOf("CG");

        List<Pattern> methylationPatterns = new ArrayList<>();
        // group sequences by methylationString, distribute each seq into one pattern
        Map<String, List<Sequence>> patternMap = groupSeqsByKey(combinedSequenceList, new GetKeyFunction() {
            @Override
            public String apply(Sequence seq) {
                return seq.getMethylationString().substring(startCpGPos, endCpGPos + 2);
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
