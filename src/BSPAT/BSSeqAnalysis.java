package BSPAT;

import DataType.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.util.*;

/**
 * Bisulfite sequences analysis. Include obtaining methylation string, mutation
 * string, methyl&mutation string. And group sequences by pattern.
 *
 * @author Ke
 */
public class BSSeqAnalysis {


    private Hashtable<String, String> referenceSeqs = new Hashtable<>();
    private Constant constant;

    /**
     * Execute analysis.
     *
     * @param experimentName
     * @param constant
     * @return List of ReportSummary
     * @throws Exception
     */
    public List<ReportSummary> execute(String experimentName, Constant constant) throws Exception {
        List<ReportSummary> reportSummaries = new ArrayList<>();
        this.constant = constant;
        String inputFolder = constant.mappingResultPath + experimentName + "/";
        String outputFolder = constant.patternResultPath + experimentName + "/";
        ImportBismarkResult importBismarkResult = new ImportBismarkResult(constant.originalRefPath, inputFolder);
        referenceSeqs = importBismarkResult.getReferenceSeqs();
        List<Sequence> sequencesList = importBismarkResult.getSequencesList();
        if (sequencesList.size() == 0) {
            throw new Exception("mapping result is empty, please double check input!");
        }

        // 1. read coordinates
        HashMap<String, Coordinate> coordinates;
        if (constant.coorReady) {
            coordinates = IO.readCoordinates(constant.coorFilePath, constant.coorFileName);
        } else {
            throw new Exception("coordinates file is not ready!");
        }

        // 2. group seqs by region
        Map<String, List<Sequence>> sequenceGroupMap = groupSeqsByKey(sequencesList, new GetKeyFunction() {
            @Override
            public String apply(Sequence seq) {
                return seq.getRegion();
            }
        });

        // 3. cut and filter sequences
        cutAndFilterSequence(sequenceGroupMap);
        // TODO handle case which no sequence remain after cut.

        // 4. generate report for each region
        for (String region : sequenceGroupMap.keySet()) {
            ReportSummary reportSummary = new ReportSummary(region);
            List<Sequence> seqGroup = sequenceGroupMap.get(region);
            reportSummary.setSeqBeforeFilter(seqGroup.size());

            processSequence(region, seqGroup);
            seqGroup = filterSequences(seqGroup);
            reportSummary.setSeqAfterFilter(seqGroup.size());
            // if no sequence exist after filtering, return empty reportSummary
            if (seqGroup.size() == 0) {
                continue;
            }
            List<Pattern> methylationPatternList = getMethylPattern(seqGroup);
            List<Pattern> mutationPatternList = getMutationPattern(seqGroup);

            methylationPatternList = filterMethylationPatterns(methylationPatternList, seqGroup.size());
            mutationPatternList = filterMutationPatterns(mutationPatternList, seqGroup.size());

            sortAndAssignPatternID(methylationPatternList);
            sortAndAssignPatternID(mutationPatternList);

            List<Pattern> meMuPatternList = getMeMuPatern(methylationPatternList, mutationPatternList);

            Report report = new Report(region, outputFolder, seqGroup, referenceSeqs.get(region), constant,
                                       reportSummary);
            report.writeReport(methylationPatternList, mutationPatternList, meMuPatternList);

            if (constant.coorReady) {
                System.out.println("start drawing -- BSSeqAnalysis -- execute");
                DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
                                                              constant.toolsPath);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.METHYLATION), experimentName,
                                                  reportSummary, coordinates);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.MUTATION), experimentName,
                                                  reportSummary, coordinates);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.MUTATIONWITHMETHYLATION),
                                                  experimentName, reportSummary, coordinates);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.METHYLATIONWITHMUTATION),
                                                  experimentName, reportSummary, coordinates);
                drawFigureLocal.drawMethylPatternWithAllele(region, outputFolder, reportSummary.getPatternLink(
                        PatternLink.MUTATIONWITHMETHYLATION), experimentName, reportSummary, coordinates);
            }
            reportSummary.replacePath(Constant.DISKROOTPATH, constant.webRootPath, constant.coorReady, constant.host);
            reportSummaries.add(reportSummary);

        }
        return reportSummaries;
    }

    private void sortAndAssignPatternID(List<Pattern> patternList) {
        // sort methylation pattern.
        Collections.sort(patternList, new PatternComparator());
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

    private List<Pattern> filterMutationPatterns(List<Pattern> mutationPatterns, int totalSeqCount) {
        List<Pattern> qualifiedMutationPattern = new ArrayList<>();
        for (Pattern mutationPattern : mutationPatterns) {
            double percentage = (double) mutationPattern.getCount() / totalSeqCount;
            if (percentage >= constant.mutationPatternThreshold) {
                qualifiedMutationPattern.add(mutationPattern);
            }
        }
        return qualifiedMutationPattern;
    }

    private List<Pattern> filterMethylationPatterns(List<Pattern> methylationPatterns, double totalSeqCount) {
        List<Pattern> qualifiedMethylationPattern = new ArrayList<>();
        // use percentage pattern threshold
        if (constant.minP0Threshold == -1) {
            for (Pattern methylationPattern : methylationPatterns) {
                double percentage = methylationPattern.getCount() / totalSeqCount;
                if (percentage >= constant.minMethylThreshold) {
                    qualifiedMethylationPattern.add(methylationPattern);
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
                criticalZ = nd.inverseCumulativeProbability(1 - constant.criticalValue / totalSeqCount);
            } catch (MathException e) {
                e.printStackTrace();
            }
            // significant pattern selection
            for (Pattern methylationPattern : methylationPatterns) {
                ph = methylationPattern.getCount() / totalSeqCount;
                p0 = Math.max(constant.minP0Threshold, Math.pow(p, methylationPattern.getCGcount()));
                z = (ph - p0) / Math.sqrt(ph * (1 - ph) / totalSeqCount);
                if (z > criticalZ) {
                    qualifiedMethylationPattern.add(methylationPattern);
                }
            }
        }
        return qualifiedMethylationPattern;
    }

    /**
     * cutting mapped sequences to reference region and filter reads without covering whole reference seq
     *
     * @param sequenceGroupMap
     */
    private void cutAndFilterSequence(Map<String, List<Sequence>> sequenceGroupMap) {
        for (String region : sequenceGroupMap.keySet()) {
            List<Sequence> sequenceGroup = sequenceGroupMap.get(region);
            Iterator<Sequence> sequenceIterator = sequenceGroup.iterator();
            while (sequenceIterator.hasNext()) {
                Sequence sequence = sequenceIterator.next();
                String refSeq = referenceSeqs.get(region);
                int refStart = Constant.REFEXTENSIONLENGTH - 1, refEnd =
                        Constant.REFEXTENSIONLENGTH + refSeq.length() - 2;
                if (sequence.getStartPos() <= refStart && sequence.getEndPos() >= refEnd) {
                    // cut sequence to suit reference
                    sequence.setOriginalSeq(sequence.getOriginalSeq().substring(refStart - sequence.getStartPos(),
                                                                                refEnd - sequence.getStartPos() + 1));
                    // update CpG sites
                    Iterator<CpGSite> cpGSiteIterator = sequence.getCpGSites().iterator();
                    while (cpGSiteIterator.hasNext()) {
                        CpGSite cpGSite = cpGSiteIterator.next();
                        if (cpGSite.getPosition() >= refStart && cpGSite.getPosition() <= refEnd) {
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
     * generate methylation and mutation String; calculate
     * conversion rate, methylation rate
     */

    public void processSequence(String region, List<Sequence> seqList) {
        char[] methylationString;
        char[] mutationString;
        String originalSeq;
        double countofUnConvertedC;
        double countofMethylatedCpG;
        double unequalNucleotide;
        String convertedReferenceSeq;
        int countofCinRef;
        String referenceSeq = referenceSeqs.get(region);

        for (Sequence seq : seqList) {
            // convert reference sequence and count C in non-CpG context.
            convertedReferenceSeq = "";
            countofCinRef = 0;// count C in non-CpG context.
            for (int i = 0; i < referenceSeq.length(); i++) {
                if (i != referenceSeq.length() - 1 && referenceSeq.charAt(i) == 'C' &&
                        referenceSeq.charAt(i + 1) != 'G') {// non
                    // CpG
                    // context
                    countofCinRef++;
                }
                if (referenceSeq.charAt(i) == 'C' || referenceSeq.charAt(i) == 'c') {
                    convertedReferenceSeq += 'T';
                } else {
                    convertedReferenceSeq += referenceSeq.charAt(i);
                }
            }
            // fill read to reference length
            countofUnConvertedC = 0;
            countofMethylatedCpG = 0;
            unequalNucleotide = 0;
            methylationString = new char[convertedReferenceSeq.length()];
            mutationString = new char[convertedReferenceSeq.length()];

            for (int i = 0; i < convertedReferenceSeq.length(); i++) {
                methylationString[i] = ' ';
                mutationString[i] = ' ';
            }
            originalSeq = seq.getOriginalSeq();
            for (int i = seq.getStartPos() - 1; i < seq.getStartPos() - 1 + originalSeq.length(); i++) {
                methylationString[i] = '-';
                mutationString[i] = '-';
            }
            for (int i = 0; i < originalSeq.length(); i++) {
                // meet unequal element
                if (originalSeq.charAt(i) != convertedReferenceSeq.charAt(i + seq.getStartPos() - 1)) {
                    // unequal pair is 'C/T'. Two possible cases: 1. CpG context, methylation; 2. non-CpG context, unconverted C.
                    if (originalSeq.charAt(i) == 'C' &&
                            convertedReferenceSeq.charAt(i + seq.getStartPos() - 1) == 'T') {
                        // next char is not 'G' --- non-CpG context
                        if (i != originalSeq.length() - 1 && originalSeq.charAt(i + 1) != 'G') {
                            countofUnConvertedC++;
                        }
                        // else: next char is 'G' --- CpG context. Do nothing.
                    } else {
                        // non C/T inequality.
                        unequalNucleotide++;
                        mutationString[i + seq.getStartPos() - 1] = originalSeq.charAt(i);
                    }
                }
            }
            for (CpGSite cpg : seq.getCpGSites()) {
                if (cpg.getMethylLabel()) {
                    countofMethylatedCpG++;
                    // methylated CpG site represent by @@
                    methylationString[cpg.getPosition() - 1] = '@';
                    if (cpg.getPosition() + 1 <= methylationString.length) {
                        methylationString[cpg.getPosition()] = '@';
                    }
                    // mutation
                    mutationString[cpg.getPosition() - 1] = '-';
                } else {
                    // un-methylated CpG site represent by **. Exclude mutation in CpG site.
                    if (cpg.getPosition() - 1 != mutationString.length &&
                            originalSeq.charAt(cpg.getPosition() - 1) == 'T' &&
                            originalSeq.charAt(cpg.getPosition()) == 'G') {
                        methylationString[cpg.getPosition() - 1] = '*';
                        if (cpg.getPosition() + 1 <= methylationString.length) {
                            methylationString[cpg.getPosition()] = '*';
                        }
                    }
                }
            }
            // fill sequence content including calculation fo bisulfite
            // conversion rate and methylation rate for each sequence.
            seq.setBisulConversionRate(1 - (countofUnConvertedC / countofCinRef));
            seq.setMethylationRate(countofMethylatedCpG / seq.getCpGSites().size());
            seq.setSequenceIdentity(1 - unequalNucleotide / (originalSeq.length() - seq.getCpGSites().size()));
            seq.setMethylationString(new String(methylationString));
            seq.setMutationString(new String(mutationString));
        }
    }

    /**
     * fill sequence list filtered by threshold.
     *
     * @param seqList
     * @return
     */
    private List<Sequence> filterSequences(List<Sequence> seqList) {
        System.out.println("Filter Sequences: before filter count:\t" + seqList.size());
        List<Sequence> qualifiedSeqList = new ArrayList<>();
        for (Sequence seq : seqList) {
            // filter unqualified reads
            if (seq.getBisulConversionRate() >= constant.conversionRateThreshold &&
                    seq.getSequenceIdentity() >= constant.sequenceIdentityThreshold) {
                qualifiedSeqList.add(seq);
            }
        }
        System.out.println("Filter Sequences: after filter count:\t" + qualifiedSeqList.size());
        return qualifiedSeqList;
    }

    public List<Pattern> getMethylPattern(List<Sequence> seqList) {
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

    public List<Pattern> getMutationPattern(List<Sequence> seqList) {
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
