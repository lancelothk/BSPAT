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
        Map<String, List<Sequence>> sequenceGroupMap = groupSeqsByRegion(sequencesList);

        // 3. cut and filter sequences
        cutAndFilterSequence(sequenceGroupMap);

        // 4. generate report for each region
        for (String region : sequenceGroupMap.keySet()) {
            List<Sequence> seqGroup = sequenceGroupMap.get(region);

            int totalCount = seqGroup.size();
            List<Pattern> methylationPatterns = new ArrayList<>();
            List<Pattern> mutationPatterns = new ArrayList<>();

            getMethylString(region, seqGroup);
            seqGroup = filterSequences(seqGroup);
            getMethylPattern(seqGroup, methylationPatterns);
            getMutationPattern(seqGroup, mutationPatterns);

            ReportSummary reportSummary = new ReportSummary(region, "F");
            report = new Report("F", region, outputFolder, seqGroup, methylationPatterns, mutationPatterns,
                                referenceSeqs, totalCount, reportSummary, constant
            );
            /** TODO it is better to include those function in constructor. **/
            report.writeResult();
            report.writeStatistics();
            report.writeMethylationPatterns();
            report.writeMutationPatterns();
            if (constant.coorReady) {
                System.out.println("start drawing -- BSSeqAnalysis -- execute");
                DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
                                                              constant.toolsPath
                );
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.METHYLATION), experimentName,
                                                  "F", reportSummary, coordinates
                                                 );
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.MUTATION), experimentName,
                                                  "F", reportSummary, coordinates
                                                 );
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.MUTATIONWITHMETHYLATION
                                                                              ), experimentName, "F", reportSummary,
                                                  coordinates
                                                 );
                drawFigureLocal.drawMethylPattern(region, outputFolder,
                                                  reportSummary.getPatternLink(PatternLink.METHYLATIONWITHMUTATION
                                                                              ), experimentName, "F", reportSummary,
                                                  coordinates
                                                 );
                drawFigureLocal.drawMethylPatternWithAllele(region, outputFolder, reportSummary.getPatternLink(
                        PatternLink.MUTATIONWITHMETHYLATION
                                                                                                              ),
                                                            experimentName, "F", reportSummary, coordinates
                                                           );
            }
            reportSummary.replacePath(Constant.DISKROOTPATH, constant.webRootPath, constant.coorReady, constant.host
                                     );
            reportSummaries.add(reportSummary);

        }
        return reportSummaries;
    }

    /**
     * cutting mapped sequences to reference region and filter reads without covering whole reference seq
     * @param sequenceGroupMap
     */
    private void cutAndFilterSequence(Map<String, List<Sequence>> sequenceGroupMap) {
        for (String region : sequenceGroupMap.keySet()) {
            for (Sequence sequence : sequenceGroupMap.get(region)) {
                String refSeq = referenceSeqs.get(region);
                int refStart = Constant.REFEXTENSIONLENGTH, refEnd = Constant.REFEXTENSIONLENGTH + refSeq.length() - 1;
                if (sequence.getStartPos() <= refStart && sequence.getEndPos() >= refEnd) {
                    // cut sequence to suit reference
                    sequence.setOriginalSeq(sequence.getOriginalSeq().substring(refStart - sequence.getStartPos(),
                                                                                refEnd - sequence.getStartPos()
                                                                               )
                                           );
                    sequence.setStartPos(0);
                } else {
                    // filter out
                    sequenceGroupMap.get(region).remove(sequence);
                }
            }
        }
    }

    /**
     * group sequences by region
     *
     * @param sequencesList
     * @return a HashMap with Key:String region, Value:List<Sequence>
     */
    private Map<String, List<Sequence>> groupSeqsByRegion(List<Sequence> sequencesList) {
        Map<String, List<Sequence>> sequenceGroupMap = new HashMap<>();
        for (Sequence seq : sequencesList) {
            if (sequenceGroupMap.containsKey(seq.getRegion())) {
                sequenceGroupMap.get(seq.getRegion()).add(seq);
            } else {
                List<Sequence> sequenceGroup = new ArrayList<>();
                sequenceGroup.add(seq);
                sequenceGroupMap.put(seq.getRegion(), sequenceGroup);
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
        char[] methylationStringWithMutations;
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
            methylationStringWithMutations = new char[convertedReferenceSeq.length()];
            mutationString = new char[convertedReferenceSeq.length()];

            for (int i = 0; i < convertedReferenceSeq.length(); i++) {
                methylationString[i] = ' ';
                methylationStringWithMutations[i] = ' ';
                mutationString[i] = ' ';
            }
            originalSeq = seq.getOriginalSeq();
            for (int i = seq.getStartPos() - 1; i < seq.getStartPos() - 1 + originalSeq.length(); i++) {
                methylationString[i] = '-';
                methylationStringWithMutations[i] = '-';
                mutationString[i] = '-';
            }
            for (int i = 0; i < originalSeq.length(); i++) {
                if (originalSeq.charAt(i) != convertedReferenceSeq.charAt(i + seq.getStartPos() - 1)) {
                    if (i != originalSeq.length() - 1 && originalSeq.charAt(i) == 'C' &&
                            originalSeq.charAt(i + 1) != 'G') {// non
                        // CpG
                        // context
                        countofUnConvertedC++;
                    } else {
                        unequalNucleotide++;
                        methylationStringWithMutations[i + seq.getStartPos() - 1] = originalSeq.charAt(i); // with
                        // mutations
                        mutationString[i + seq.getStartPos() - 1] = originalSeq.charAt(i);
                    }
                }
            }
            for (CpGSite cpg : seq.getCpGSites()) {
                if (cpg.getMethylLabel()) {
                    countofMethylatedCpG++;
                    // methylated CpG site represent by @@
                    methylationString[cpg.getPosition() - 1] = '@';
                    if (cpg.getPosition() + 2 <= methylationString.length) {
                        methylationString[cpg.getPosition()] = '@';
                    }
                    methylationStringWithMutations[cpg.getPosition() - 1] = '@';
                    if (cpg.getPosition() + 2 <= methylationStringWithMutations.length) {
                        methylationStringWithMutations[cpg.getPosition()] = '@';
                    }
                    // mutation
                    mutationString[cpg.getPosition() - 1] = '-';
                } else {
                    // un-methylated CpG site represent by **
                    methylationString[cpg.getPosition() - 1] = '*';
                    if (cpg.getPosition() + 2 <= methylationString.length) {
                        methylationString[cpg.getPosition()] = '*';
                    }
                    methylationStringWithMutations[cpg.getPosition() - 1] = '*';
                    if (cpg.getPosition() + 2 <= methylationStringWithMutations.length) {
                        methylationStringWithMutations[cpg.getPosition()] = '*';
                    }
                }
            }
            // fill sequence content including calculation fo bisulfite
            // conversion rate and methylation rate for each sequence.
            seq.setBisulConversionRate(1 - (countofUnConvertedC / countofCinRef));
            seq.setMethylationRate(countofMethylatedCpG / seq.getCpGSites().size());
            seq.setSequenceIdentity(
                    1 - (unequalNucleotide - countofMethylatedCpG) / (originalSeq.length() - seq.getCpGSites().size())
                                   );
            seq.setMethylationString(new String(methylationString));
            seq.setMethylationStringWithMutations(new String(methylationStringWithMutations));
            seq.setMutationString(new String(mutationString));
        }
    }

    private List<Sequence> filterSequences(List<Sequence> seqList) {
        // fill sequence list filtered by threshold.
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

    /**
     * get methylation pattern
     */
    public void getMethylPattern(List<Sequence> seqList, List<Pattern> methylationPatterns) {
        // sort sequences by methylationString first. Character order.
        Collections.sort(seqList, new SequenceComparatorMethylation());

        // group sequences by methylationString, distribute each seq into one
        // pattern
        Pattern methylationPattern = new Pattern("");
        Pattern methylationPatternWithMutations;
        for (Sequence sequence : seqList) {
            if (!sequence.getMethylationString().equals(methylationPattern.getPatternString())) {
                if (!methylationPattern.getPatternString().equals("")) {
                    methylationPatterns.add(methylationPattern);
                }
                methylationPattern = new Pattern(sequence.getMethylationString());
                methylationPattern.setCGcount(sequence.getCpGSites().size());
                methylationPattern.setParrentPatternID(methylationPatterns.size());
                methylationPattern.addSequence(sequence);
            } else {
                methylationPattern.addSequence(sequence);
            }
        }
        methylationPatterns.add(methylationPattern);

        for (Pattern pattern : methylationPatterns) {
            // sort sequences by methylationStringMutations first. Character
            // order.
            Collections.sort(pattern.sequenceList(), new SequenceComparatorMM());

            methylationPatternWithMutations = new Pattern("");
            for (Sequence sequence : pattern.sequenceList()) {
                if (!sequence.getMethylationStringWithMutations().equals(
                        methylationPatternWithMutations.getPatternString()
                                                                        )) {
                    if (!methylationPatternWithMutations.getPatternString().equals("")) {
                        pattern.addChildPattern(methylationPatternWithMutations);
                    }
                    methylationPatternWithMutations = new Pattern(sequence.getMethylationStringWithMutations());
                    methylationPatternWithMutations.setParrentPatternID(pattern.getParrentPatternID());
                    methylationPatternWithMutations.addSequence(sequence);
                } else {
                    methylationPatternWithMutations.addSequence(sequence);
                }
            }
            pattern.addChildPattern(methylationPatternWithMutations);
        }
    }

    /**
     * get mutation pattern
     */
    public void getMutationPattern(List<Sequence> seqList, List<Pattern> mutationPatterns) {
        // sort sequences by methylationString first. Character order.
        Collections.sort(seqList, new SequenceComparatorMutations());

        // group sequences by methylationString, distribute each seq into one
        // pattern
        Pattern mutationpattern = new Pattern("");
        Pattern mutationpatternWithMethylation;
        for (Sequence sequence : seqList) {
            if (!sequence.getMutationString().equals(mutationpattern.getPatternString())) {
                if (!mutationpattern.getPatternString().equals("")) {
                    mutationPatterns.add(mutationpattern);
                }
                mutationpattern = new Pattern(sequence.getMutationString());
                mutationpattern.setCGcount(sequence.getCpGSites().size());
                mutationpattern.setParrentPatternID(mutationPatterns.size());
                mutationpattern.addSequence(sequence);
            } else {
                mutationpattern.addSequence(sequence);
            }
        }
        mutationPatterns.add(mutationpattern);

        for (Pattern pattern : mutationPatterns) {
            // sort sequences by methylationStringMutations first. Character
            // order.
            Collections.sort(pattern.sequenceList(), new SequenceComparatorMM());

            mutationpatternWithMethylation = new Pattern("");
            for (Sequence sequence : pattern.sequenceList()) {
                if (!sequence.getMethylationStringWithMutations().equals(
                        mutationpatternWithMethylation.getPatternString()
                                                                        )) {
                    if (!mutationpatternWithMethylation.getPatternString().equals("")) {
                        pattern.addChildPattern(mutationpatternWithMethylation);
                    }
                    mutationpatternWithMethylation = new Pattern(sequence.getMethylationStringWithMutations());
                    mutationpatternWithMethylation.setParrentPatternID(pattern.getParrentPatternID());
                    mutationpatternWithMethylation.addSequence(sequence);
                    mutationpatternWithMethylation.setCGcount(pattern.getCGcount());
                } else {
                    mutationpatternWithMethylation.addSequence(sequence);
                }
            }
            pattern.addChildPattern(mutationpatternWithMethylation);
        }
    }

    private List<Sequence> removeMisMap(List<Sequence> seqList) {
        List<Sequence> newList = null;
        if (seqList.size() != 0) {
            // count sequence with same start position
            HashMap<Integer, Integer> positionMap = new HashMap<>();
            for (Sequence sequence : seqList) {
                if (!positionMap.containsKey(sequence.getStartPos())) {
                    positionMap.put(sequence.getStartPos(), 1);
                } else {
                    positionMap.put(sequence.getStartPos(), positionMap.get(sequence.getStartPos()) + 1);
                }
            }
            // select majority position
            int max = 0;
            int maxKey = 0;
            for (Integer key : positionMap.keySet()) {
                if (positionMap.get(key) >= max) {
                    max = positionMap.get(key);
                    maxKey = key;
                }
            }
            // remove minority
            newList = new ArrayList<>();
            for (Sequence sequence : seqList) {
                if (sequence.getStartPos() == maxKey) {
                    newList.add(sequence);
                }
            }
        }
        return newList;
    }

}
