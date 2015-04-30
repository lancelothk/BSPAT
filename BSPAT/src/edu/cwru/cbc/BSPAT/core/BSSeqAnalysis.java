package edu.cwru.cbc.BSPAT.core;

import edu.cwru.cbc.BSPAT.DataType.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.IOException;
import java.util.*;

import static edu.cwru.cbc.BSPAT.core.Utilities.getBoundedSeq;

/**
 * Bisulfite sequences analysis. Include obtaining methylation string, mutation
 * string, methyl&mutation string. And group sequences by pattern.
 *
 * @author Ke
 */
public class BSSeqAnalysis {
    public static final int DEFAULT_TARGET_LENGTH = 70;

    /**
     * Execute analysis.
     *
     * @return List of ReportSummary
     */
    public List<ReportSummary> execute(String experimentName,
                                       Constant constant) throws IOException, InterruptedException, MathException {
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
            // get position of first CpG and generate DEFAULT_TARGET_LENGTH bp region.
            targetCoorMap = new HashMap<>();
            for (String key : refCoorMap.keySet()) {
                String refString = referenceSeqs.get(key);
                long firstPos = refString.indexOf("CG");
                // no CpG found in ref seq
                if (firstPos == -1) {
                    targetCoorMap.put(key, new Coordinate(refCoorMap.get(key).getId(), refCoorMap.get(key).getChr(),
                            refCoorMap.get(key).getStrand(),
                            refCoorMap.get(key).getStart(),
                            refCoorMap.get(key).getStart() + DEFAULT_TARGET_LENGTH));
                } else {
                    long endPos = refCoorMap.get(key).getStart() + firstPos + DEFAULT_TARGET_LENGTH;
                    targetCoorMap.put(key, new Coordinate(refCoorMap.get(key).getId(), refCoorMap.get(key).getChr(),
                            refCoorMap.get(key).getStrand(),
                            refCoorMap.get(key).getStart() + firstPos,
                            endPos < refCoorMap.get(key).getEnd() ? endPos : refCoorMap.get(
                                    key).getEnd()));
                }
            }
        } else {
            targetCoorMap = IO.readCoordinates(constant.targetPath, constant.targetFileName);
        }
        // 2. group seqs by region
        Map<String, List<Sequence>> sequenceGroupMap = groupSeqsByKey(sequencesList, new GetKeyFunction() {
            @Override
            public String getKey(Sequence seq) {
                return seq.getRegion();
            }
        });

        // 3. generate report for each region criticalZ
        for (String region : sequenceGroupMap.keySet()) {
            ReportSummary reportSummary = new ReportSummary(region);
            Pair<List<Sequence>, List<Sequence>> filteredCuttingResultPair = cutAndFilterSequence(region,
                    sequenceGroupMap,
                    refCoorMap,
                    targetCoorMap,
                    referenceSeqs);
            String refSeq = referenceSeqs.get(region);

            List<Sequence> CpGBoundSequenceList = filteredCuttingResultPair.getLeft();
            List<Sequence> otherSequenceList = filteredCuttingResultPair.getRight();
            reportSummary.setSeqOthers(otherSequenceList.size());

            List<Sequence> seqGroup = sequenceGroupMap.get(region);
            Sequence.processSequence(refSeq, seqGroup);
            Sequence.processSequence(getBoundedSeq("CG", refSeq), CpGBoundSequenceList);

            reportSummary.setSeqCpGBounded(CpGBoundSequenceList.size());
            Pair<List<Sequence>, List<Sequence>> filteredCpGSequencePair = filterSequences(CpGBoundSequenceList,
                    constant);
            CpGBoundSequenceList = filteredCpGSequencePair.getLeft();
            reportSummary.setSeqCpGAfterFilter(CpGBoundSequenceList.size());

            reportSummary.setSeqTargetBounded(seqGroup.size());
            // sequence quality filter
            Pair<List<Sequence>, List<Sequence>> filteredTargetSequencePair = filterSequences(seqGroup, constant);
            seqGroup = filteredTargetSequencePair.getLeft();
            reportSummary.setSeqTargetAfterFilter(seqGroup.size());

            // calculate mutation stat
            int[][] mutationStat = calculateMutationStat(refSeq, seqGroup);

            // declare SNP and update mutationString
            List<PotentialSNP> potentialSNPList = declareSNP(constant, seqGroup.size(), mutationStat);
            updateMutationString(seqGroup, potentialSNPList);

            // if no sequence exist after filtering, return empty reportSummary
            if (seqGroup.size() == 0 && CpGBoundSequenceList.size() == 0) {
                continue;
            }
            List<Pattern> methylationPatternList = getMethylPattern(seqGroup, CpGBoundSequenceList,
                    refSeq);
            List<Pattern> mutationPatternList = getMutationPattern(seqGroup);
            List<Pattern> allelePatternList = getAllelePattern(seqGroup);

            System.out.println(experimentName + "\t" + region);
            methylationPatternList = filterMethylationPatterns(methylationPatternList,
                    seqGroup.size() + CpGBoundSequenceList.size(),
                    StringUtils.countMatches(refSeq,
                            "CG"), constant);

            Pattern.resetPatternCount();
            sortAndAssignPatternID(methylationPatternList);
            sortAndAssignPatternID(mutationPatternList);

            // generate memu pattern after filtering me & mu pattern since filtered non-significant patterns won't contribute to memu result.
            List<Pattern> meMuPatternList = getMeMuPatern(seqGroup, methylationPatternList, mutationPatternList);
            meMuPatternList = filterMeMuPatterns(meMuPatternList, seqGroup.size(), constant);

            Pattern allelePattern = filterAllelePatterns(allelePatternList, seqGroup.size(), constant);
            Pattern nonAllelePattern = generateNonAllelePattern(allelePattern, seqGroup);

            Report report = new Report(region, outputFolder, refSeq, constant, reportSummary);
            report.writeReport(filteredTargetSequencePair, filteredCpGSequencePair, methylationPatternList,
                    mutationPatternList, meMuPatternList, mutationStat);

            if (constant.coorReady) {
                DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
                        constant.toolsPath, region, outputFolder, experimentName,
                        targetCoorMap, refSeq);
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

    private void updateMutationString(List<Sequence> seqGroup, List<PotentialSNP> potentialSNPList) {
        for (Sequence sequence : seqGroup) {
            char[] newMutationString = new char[sequence.length()];
            Arrays.fill(newMutationString, '-');
            for (PotentialSNP potentialSNP : potentialSNPList) {
                if (sequence.getOriginalSeq().charAt(potentialSNP.getPosition()) == potentialSNP.getNucleotide()) {
                    newMutationString[potentialSNP.getPosition()] = potentialSNP.getNucleotide();
                }
            }
            sequence.setMutationString(new String(newMutationString));
        }
    }

    private List<PotentialSNP> declareSNP(Constant constant, int totalSequenceCount, int[][] mutationStat) {
        List<PotentialSNP> potentialSNPList = new ArrayList<>();
        double threshold = totalSequenceCount * constant.mutationPatternThreshold;
        for (int i = 0; i < mutationStat.length; i++) {
            int count = 0;
            if (mutationStat[i][0] >= threshold) {
                potentialSNPList.add(new PotentialSNP(i, 'A'));
                count++;
            }
            if (mutationStat[i][1] >= threshold) {
                potentialSNPList.add(new PotentialSNP(i, 'C'));
                count++;
            }
            if (mutationStat[i][2] >= threshold) {
                potentialSNPList.add(new PotentialSNP(i, 'G'));
                count++;
            }
            if (mutationStat[i][3] >= threshold) {
                potentialSNPList.add(new PotentialSNP(i, 'T'));
                count++;
            }
            if (mutationStat[i][4] >= threshold) {
                potentialSNPList.add(new PotentialSNP(i, 'N'));
                count++;
            }
            if (count >= 2) {
                throw new RuntimeException("more than two SNP allele in same position!");
            }
        }
        return potentialSNPList;
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

    private List<Pattern> filterMeMuPatterns(List<Pattern> memuPatterns, int totalSeqCount, Constant constant) {
        List<Pattern> qualifiedMutationPatternList = new ArrayList<>();
        for (Pattern memuPattern : memuPatterns) {
            double percentage = (double) memuPattern.getCount() / totalSeqCount;
            if (percentage >= constant.mutationPatternThreshold) {
                qualifiedMutationPatternList.add(memuPattern);
            }
        }
        return qualifiedMutationPatternList;
    }

    private List<Pattern> filterMethylationPatterns(List<Pattern> methylationPatterns, double totalSeqCount,
                                                    int refCpGCount, Constant constant) throws MathException {
        if (methylationPatterns.size() != 0 && totalSeqCount != 0) {
            if (constant.minP0Threshold != -1 && refCpGCount > 3) {
                return filterMethylPatternsByP0Threshold(methylationPatterns, totalSeqCount, refCpGCount, constant);
            } else {
                return filterMethylPatternsByMethylThreshold(methylationPatterns, totalSeqCount,
                        constant);
            }
        }
        // return empty list.
        return new ArrayList<>();
    }

    private List<Pattern> filterMethylPatternsByMethylThreshold(List<Pattern> methylationPatterns, double totalSeqCount,
                                                                Constant constant) {
        List<Pattern> qualifiedMethylationPatternList = new ArrayList<>();
        // use percentage pattern threshold
        for (Pattern methylationPattern : methylationPatterns) {
            double percentage = methylationPattern.getCount() / totalSeqCount;
            if (percentage >= constant.minMethylThreshold) {
                qualifiedMethylationPatternList.add(methylationPattern);
            }
        }
        return qualifiedMethylationPatternList;
    }

    private List<Pattern> filterMethylPatternsByP0Threshold(List<Pattern> methylationPatterns, double totalSeqCount, double refCpGCount,
                                                            Constant constant) throws MathException {
        List<Pattern> qualifiedMethylationPatternList = new ArrayList<>();
        NormalDistributionImpl nd = new NormalDistributionImpl(0, 1);
        // 2 means two states(M/N). The expected portion of each random pattern will be n/2^#CpG.
        double avgPatternSeqCount = totalSeqCount / Math.pow(2, refCpGCount);
        methylationPatterns.sort(new Comparator<Pattern>() {
            @Override
            public int compare(Pattern o1, Pattern o2) {
                return o2.getCount() - o1.getCount();
            }
        });
        double lowerBoundaryCount = 0;
        for (Pattern methylationPattern : methylationPatterns) {
            if (methylationPattern.getCount() < avgPatternSeqCount) {
                lowerBoundaryCount = methylationPattern.getCount();
                break;
            }
        }
        // significant pattern selection
        for (Pattern methylationPattern : methylationPatterns) {
            double ph = methylationPattern.getCount() / totalSeqCount;
            double p0 = lowerBoundaryCount / totalSeqCount;
            double z = (ph - p0) / Math.sqrt(ph * (1 - ph) / totalSeqCount);
            double pZ = 1 - nd.cumulativeProbability(z);
            if (pZ <= (constant.criticalValue / totalSeqCount)) {
                qualifiedMethylationPatternList.add(methylationPattern);
            }
        }
        System.out.println("methylationPattern count:\t" + methylationPatterns.size());
        System.out.println("qualifiedMethylationPatternList:\t" + qualifiedMethylationPatternList.size());
        return qualifiedMethylationPatternList;
    }

    /**
     * cutting mapped sequences to reference region and filter reads without covering whole reference seq
     */
    private Pair<List<Sequence>, List<Sequence>> cutAndFilterSequence(String region,
                                                                      Map<String, List<Sequence>> sequenceGroupMap,
                                                                      Map<String, Coordinate> refCoorMap,
                                                                      Map<String, Coordinate> targetCoorMap,
                                                                      Map<String, String> referenceSeqs) throws IOException {
        String refSeq = referenceSeqs.get(region);
        List<Sequence> CpGBoundSequenceList = new ArrayList<>();
        List<Sequence> otherSequenceList = new ArrayList<>();
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
        referenceSeqs.put(region, refSeq.substring(refStart, refEnd + 1));
        while (sequenceIterator.hasNext()) {
            Sequence sequence = sequenceIterator.next();
            if (sequence.getStartPos() <= refStart && sequence.getEndPos() >= refEnd) {
                // cut sequence to suit reference
                sequence.setOriginalSeq(sequence.getOriginalSeq().substring(refStart - sequence.getStartPos(),
                        refEnd - sequence.getStartPos() + 1));
                updateCpGPosition(refStart, refStart, refEnd, sequence);
                sequence.setStartPos(1);
            } else {
                // filter out
                sequenceIterator.remove();
                // recheck if sequence cover all CpGs in ref
                int startCpGPos = refSeq.indexOf("CG") + refStart;
                int endCpGPos = refSeq.lastIndexOf("CG") + refStart;
                if (sequence.getStartPos() <= startCpGPos && sequence.getEndPos() >= (endCpGPos + 1)) {
                    CpGBoundSequenceList.add(sequence);
                    sequence.setOriginalSeq(sequence.getOriginalSeq().substring(startCpGPos - sequence.getStartPos(),
                            endCpGPos + 2 -
                                    sequence.getStartPos()));
                    updateCpGPosition(refStart, startCpGPos, endCpGPos + 1, sequence);
                } else {
                    otherSequenceList.add(sequence);
                }
            }
        }
        return new ImmutablePair<>(CpGBoundSequenceList, otherSequenceList);
    }

    private void updateCpGPosition(int refStart, int leftBound, int rightBound, Sequence sequence) {
        // leftBound should be bigger than or equal to refStart
        assert leftBound >= refStart;
        // update CpG sites
        Iterator<CpGSite> cpGSiteIterator = sequence.getCpGSites().iterator();
        while (cpGSiteIterator.hasNext()) {
            CpGSite cpGSite = cpGSiteIterator.next();
            // only keep CpG site wholly sit in ref.
            if (cpGSite.getPosition() >= leftBound && cpGSite.getPosition() + 1 <= rightBound) {
                cpGSite.setPosition(cpGSite.getPosition() - refStart);
            } else {
                cpGSiteIterator.remove();
            }
        }
    }

    /**
     * group sequences by given key function
     *
     * @param getKey function parameter to return String key.
     * @return HashMap contains <key function return value, grouped sequence list>
     */
    private Map<String, List<Sequence>> groupSeqsByKey(List<Sequence> sequencesList, GetKeyFunction getKey) {
        Map<String, List<Sequence>> sequenceGroupMap = new HashMap<>();
        for (Sequence seq : sequencesList) {
            if (sequenceGroupMap.containsKey(getKey.getKey(seq))) {
                sequenceGroupMap.get(getKey.getKey(seq)).add(seq);
            } else {
                List<Sequence> sequenceGroup = new ArrayList<>();
                sequenceGroup.add(seq);
                sequenceGroupMap.put(getKey.getKey(seq), sequenceGroup);
            }
        }
        return sequenceGroupMap;
    }

    /**
     * fill sequence list filtered by threshold.
     */
    private Pair<List<Sequence>, List<Sequence>> filterSequences(List<Sequence> seqList,
                                                                 Constant constant) throws IOException {
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
        return new ImmutablePair<>(qualifiedSeqList, unQualifiedSeqList);
    }

    private List<Pattern> getMethylPattern(List<Sequence> seqList, List<Sequence> CpGBoundSequenceList,
                                           String referenceSeq) {
        List<Sequence> combinedSequenceList = new ArrayList<>();
        combinedSequenceList.addAll(seqList);
        combinedSequenceList.addAll(CpGBoundSequenceList);

        List<Pattern> methylationPatterns = new ArrayList<>();
        final int startCpGPos = referenceSeq.indexOf("CG");
        final int endCpGPos = referenceSeq.lastIndexOf("CG");
        // group sequences by methylationString, distribute each seq into one pattern
        Map<String, List<Sequence>> patternMap = groupSeqsByKey(combinedSequenceList, new GetKeyFunction() {
            @Override
            public String getKey(Sequence seq) {
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
            public String getKey(Sequence seq) {
                return seq.getMutationString();
            }
        });

        for (String mutationString : patternMap.keySet()) {
            List<Sequence> patternSeqList = patternMap.get(mutationString);
            Pattern mutationPattern = new Pattern(mutationString, Pattern.PatternType.MUTATION);
            for (Sequence seq : patternSeqList) {
                mutationPattern.addSequence(seq);
            }
            mutationPatterns.add(mutationPattern);
        }
        return mutationPatterns;
    }

    private int[][] calculateMutationStat(String referenceSeq, List<Sequence> targetSequencesList) {
        int[][] mutationStat;
        mutationStat = new int[referenceSeq.length()][5]; // 5 possible values.
        for (Sequence seq : targetSequencesList) {
            char[] mutationArray = seq.getMutationString().toCharArray();
            for (int i = 0; i < mutationArray.length; i++) {
                switch (mutationArray[i]) {
                    case 'A':
                        mutationStat[i][0]++;
                        break;
                    case 'C':
                        mutationStat[i][1]++;
                        break;
                    case 'G':
                        mutationStat[i][2]++;
                        break;
                    case 'T':
                        mutationStat[i][3]++;
                        break;
                    case 'N':
                        mutationStat[i][4]++;
                        break;
                    case '-':
                        // do nothing
                        break;
                    default:
                        throw new RuntimeException("unknown nucleotide:" + mutationArray[i]);
                }
            }
        }
        return mutationStat;
    }
}
