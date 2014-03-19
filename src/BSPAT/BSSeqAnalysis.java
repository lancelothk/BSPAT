package BSPAT;

import DataType.*;

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
        Report report;
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

        // 4. generate report for each region
        for (String region : sequenceGroupMap.keySet()) {
            List<Sequence> seqGroup = sequenceGroupMap.get(region);

            int totalCount = seqGroup.size();

            getMethylString(region, seqGroup);
            seqGroup = filterSequences(seqGroup);
            if (seqGroup.size() == 0) {
                return reportSummaries;
            }
            List<Pattern> methylationPatterns = getMethylPattern(seqGroup);
            List<Pattern> mutationPatterns = getMutationPattern(seqGroup);

            ReportSummary reportSummary = new ReportSummary(region, "F");
            report = new Report("F", region, outputFolder, seqGroup, methylationPatterns, mutationPatterns,
                                referenceSeqs, totalCount, reportSummary, constant);
            /** TODO it is better to include those function in constructor. **/
            report.writeResult();
            report.writeStatistics();
            report.writeMethylationPatterns();
            report.writeMutationPatterns();
            if (constant.coorReady) {
                System.out.println("start drawing -- BSSeqAnalysis -- execute");
                DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
                                                              constant.toolsPath);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.METHYLATION), experimentName,
                                                  "F", reportSummary, coordinates);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.MUTATION), experimentName,
                                                  "F", reportSummary, coordinates);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.MUTATIONWITHMETHYLATION),
                                                  experimentName, "F", reportSummary, coordinates);
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.METHYLATIONWITHMUTATION),
                                                  experimentName, "F", reportSummary, coordinates);
                drawFigureLocal.drawMethylPatternWithAllele(region, outputFolder, reportSummary.getPatternLink(
                        PatternLink.MUTATIONWITHMETHYLATION), experimentName, "F", reportSummary, coordinates);
            }
            reportSummary.replacePath(Constant.DISKROOTPATH, constant.webRootPath, constant.coorReady, constant.host);
            reportSummaries.add(reportSummary);

        }
        return reportSummaries;
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
                        Constant.REFEXTENSIONLENGTH + refSeq.length() - 1;
                if (sequence.getStartPos() <= refStart && sequence.getEndPos() >= refEnd) {
                    // cut sequence to suit reference
                    sequence.setOriginalSeq(sequence.getOriginalSeq().substring(refStart - sequence.getStartPos(),
                                                                                refEnd - sequence.getStartPos()));
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
     * @param sequencesList
     * @param getKey function parameter to return String key.
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
     * get methylation String & methylation string with mutations; calculate
     * conversion rate, methylation rate
     */

    public void getMethylString(String region, List<Sequence> seqList) {
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
                if (originalSeq.charAt(i) != convertedReferenceSeq.charAt(i + seq.getStartPos() - 1)) {
                    if (i != originalSeq.length() - 1 && originalSeq.charAt(i) == 'C') {
                        if (originalSeq.charAt(i + 1) != 'G') {// non CpG context
                            countofUnConvertedC++;
                        }
                        //else CpG context, do nothing
                    } else {
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
                    // un-methylated CpG site represent by **
                    methylationString[cpg.getPosition() - 1] = '*';
                    if (cpg.getPosition() + 1 <= methylationString.length) {
                        methylationString[cpg.getPosition()] = '*';
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
        List<Sequence> tempSeqList = new ArrayList<>();
        for (Sequence seq : seqList) {
            // filter unqualified reads
            if (seq.getBisulConversionRate() >= constant.conversionRateThreshold &&
                    seq.getSequenceIdentity() >= constant.sequenceIdentityThreshold) {
                tempSeqList.add(seq);
            }
        }
        return tempSeqList;
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
