package edu.cwru.cbc.BSPAT.core;

import edu.cwru.cbc.BSPAT.DataType.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import java.io.IOException;
import java.util.*;

/**
 * Bisulfite sequences analysis. Include obtaining methylation string, methyl&SNP string. And group sequences by pattern.
 *
 * @author Ke
 */
public class BSSeqAnalysis {
	public static final int DEFAULT_TARGET_LENGTH = 70;
	public static final double ASM_PATTERN_THRESHOLD = 0.2;
	public static final double ASM_MIN_METHYL_DIFFERENCE = 0.2;
	public static final double MEMU_PATTERN_THRESHOLD = 0.1;

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

		// 1. read refCoorMap / targetCoorMap
		Map<String, Coordinate> refCoorMap = IO.readCoordinates(constant.coorFilePath, constant.coorFileName);
		Map<String, Coordinate> targetCoorMap = getStringCoordinateMap(constant, referenceSeqs, refCoorMap);

		// 2. group seqs by region
		Map<String, List<Sequence>> sequenceGroupMap = groupSeqsByKey(sequencesList, Sequence::getRegion);

		// 3. perform analysis for each region
		for (String region : sequenceGroupMap.keySet()) {
			ReportSummary reportSummary = new ReportSummary(region);
			List<Sequence> seqGroup = sequenceGroupMap.get(region);
			String refSeq = referenceSeqs.get(region);
			Coordinate refCoor = refCoorMap.get(region);
			Coordinate targetCoor = targetCoorMap.get(region);
			if (!refCoor.getStrand().equals(targetCoor.getStrand())) {
				throw new RuntimeException("target strand is not same to reference!");
			}

			// calculate target offset to reference
			int targetStart, targetEnd;
			switch (refCoor.getStrand()) {
				case "+":
					targetStart = targetCoor.getStart() - refCoor.getStart(); // 0-based
					targetEnd = targetCoor.getEnd() - refCoor.getStart(); // 0-based
					break;
				case "-":
					targetStart = refCoor.getEnd() - targetCoor.getEnd(); // 0-based
					targetEnd = refCoor.getEnd() - targetCoor.getStart(); // 0-based
					break;
				default:
					throw new RuntimeException("invalid reference strand!");
			}
			if (targetStart < 0 || targetEnd < 0 || targetStart >= targetEnd || targetStart >= refSeq.length() || targetEnd >= refSeq
					.length()) {
				throw new RuntimeException("invalid target coordinates! out of reference!");
			}
			String tmpTargetRefSeq;
			if (targetStart != 0 && targetEnd != refSeq.length() - 1) {
				tmpTargetRefSeq = refSeq.substring(targetStart - 1,
						targetEnd + 2); // used to include one more bp on both ends to detect CpG sites.
			} else {
				tmpTargetRefSeq = refSeq.substring(targetStart,
						targetEnd + 1);
			}
			int startCpGPos = tmpTargetRefSeq.indexOf("CG") + targetStart - 1;
			int endCpGPos = tmpTargetRefSeq.lastIndexOf("CG") + targetStart - 1;
			String targetRefSeq = refSeq.substring(targetStart, targetEnd + 1);
			boolean isStartCpGPartial = (startCpGPos == targetStart - 1);
			boolean isEndCpGPartial = (endCpGPos == targetEnd);
			int cpgBoundedStart = isStartCpGPartial ? startCpGPos + 1 : startCpGPos;
			int cpgBoundedEnd = isEndCpGPartial ? endCpGPos : endCpGPos + 1;
			String cpgRefSeq = refSeq.substring(cpgBoundedStart, cpgBoundedEnd + 1);

			// processing sequences
			for (Sequence sequence : seqGroup) {
				sequence.processSequence(refSeq);
			}

			// seqs in seqGroup got changed in updateTargetSequences().
			Pair<List<Sequence>, List<Sequence>> filteredCuttingResultPair = updateTargetSequences(seqGroup,
					targetStart, targetEnd, cpgBoundedStart, cpgBoundedEnd);
			List<Sequence> CpGBoundSequenceList = filteredCuttingResultPair.getLeft();
			List<Sequence> otherSequenceList = filteredCuttingResultPair.getRight();
			reportSummary.setSeqOthers(otherSequenceList.size());

			// sequence quality filter for CpGBoundSequenceList
			reportSummary.setSeqCpGBounded(CpGBoundSequenceList.size());
			Pair<List<Sequence>, List<Sequence>> filteredCpGSequencePair = filterSequences(CpGBoundSequenceList,
					constant);
			CpGBoundSequenceList = filteredCpGSequencePair.getLeft();
			reportSummary.setSeqCpGAfterFilter(CpGBoundSequenceList.size());

			// sequence quality filter for seqGroup
			reportSummary.setSeqTargetBounded(seqGroup.size());
			Pair<List<Sequence>, List<Sequence>> filteredTargetSequencePair = filterSequences(seqGroup, constant);
			seqGroup = filteredTargetSequencePair.getLeft();
			reportSummary.setSeqTargetAfterFilter(seqGroup.size());

			List<Sequence> allMethylSequences = new ArrayList<>();
			allMethylSequences.addAll(seqGroup);
			allMethylSequences.addAll(CpGBoundSequenceList);

			// if no sequence exist after filtering, return empty reportSummary
			if (allMethylSequences.size() == 0) {
				reportSummaries.add(reportSummary);
				continue;
			}

			// calculate mismatch stat based on all sequences in reference region.
			int[][] mismatchStat = calculateMismatchStat(targetRefSeq, targetStart, targetEnd, allMethylSequences);

			// declare SNP
			PotentialSNP potentialSNP = declareSNP(constant, allMethylSequences.size(),
					mismatchStat, targetStart);

			// write report
			Report report = new Report(region, outputFolder, targetRefSeq, targetStart, cpgRefSeq, constant,
					reportSummary);
			report.writeReport(filteredTargetSequencePair, filteredCpGSequencePair, mismatchStat);

			DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
					constant.toolsPath, region, outputFolder, experimentName, targetCoorMap);

			// generate methyl pattern output
			List<Pattern> methylationPatternList = getMethylPattern(allMethylSequences, cpgBoundedStart,
					cpgBoundedEnd);
			System.out.println(experimentName + "\t" + region);
			methylationPatternList = filterMethylationPatterns(methylationPatternList,
					allMethylSequences.size(), StringUtils.countMatches(targetRefSeq, "CG"),
					constant);
			if (methylationPatternList.size() == 0) {
				reportSummaries.add(reportSummary);
				reportSummary.replacePath(Constant.DISKROOTPATH, constant.webRootPath, constant.host);
				continue;
			}
			sortAndAssignPatternID(methylationPatternList);
			reportSummary.addPatternLink(PatternLink.METHYLATION);
			report.writePatterns(methylationPatternList, PatternLink.METHYLATION, allMethylSequences);
			drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.METHYLATION));

			if (potentialSNP != null) {
				// generate memu pattern output
				List<Pattern> meMuPatternList = getMeMuPatern(seqGroup, methylationPatternList, potentialSNP,
						targetStart, targetEnd);
				meMuPatternList = filterPatternsByThreshold(meMuPatternList, seqGroup.size(),
						MEMU_PATTERN_THRESHOLD);
				if (meMuPatternList.size() != 0) {
					reportSummary.addPatternLink(PatternLink.METHYLATIONWITHSNP);
					report.writePatterns(meMuPatternList, PatternLink.METHYLATIONWITHSNP, seqGroup);
					drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.METHYLATIONWITHSNP));
				}

				// ASM
				Pair<Pattern, Pattern> allelePatterns = getAllelePatterns(seqGroup, potentialSNP);
				Pattern allelePattern = allelePatterns.getLeft();
				Pattern nonAllelePattern = allelePatterns.getRight();
				if (allelePattern.getSequenceMap().size() != 0 && nonAllelePattern.getSequenceMap().size() != 0) {
					PatternResult patternWithAllele = patternToPatternResult(allelePattern, report.getCpgStatList(),
							seqGroup.size(), targetStart);
					PatternResult patternWithoutAllele = patternToPatternResult(nonAllelePattern,
							report.getCpgStatList(), seqGroup.size(), targetStart);

					if (!hasASM(patternWithAllele, patternWithoutAllele)) {
						reportSummary.setHasASM(false);
					} else {
						reportSummary.setHasASM(true);
						drawFigureLocal.drawASMPattern(reportSummary, patternWithAllele, patternWithoutAllele,
								constant.logPath);
					}
				}
			}

			reportSummary.replacePath(Constant.DISKROOTPATH, constant.webRootPath, constant.host);
			reportSummaries.add(reportSummary);
		}
		return reportSummaries;
	}

	private Map<String, Coordinate> getStringCoordinateMap(Constant constant, Map<String, String> referenceSeqs,
	                                                       Map<String, Coordinate> refCoorMap) {
		Map<String, Coordinate> targetCoorMap = IO.readCoordinates(constant.targetPath, constant.targetFileName);
		for (String key : refCoorMap.keySet()) {
			// if no given targetCoor, get position of first CpG and generate DEFAULT_TARGET_LENGTH bp region.
			if (!targetCoorMap.containsKey(key)) {
				String refString = referenceSeqs.get(key);
				int firstPos = refString.indexOf("CG");
				// no CpG found in ref seq, use ref start and DEFAULT_TARGET_LENGTH.
				if (firstPos == -1) {
					targetCoorMap.put(key, new Coordinate(refCoorMap.get(key).getId(), refCoorMap.get(key).getChr(),
							refCoorMap.get(key).getStrand(), refCoorMap.get(key).getStart(),
							refCoorMap.get(key).getStart() + DEFAULT_TARGET_LENGTH));
				} else {
					// from first CpG to min(ref end, fisrt CpG + DEFAULT_TARGET_LENGTH)
					int endPos = refCoorMap.get(key).getStart() + firstPos + DEFAULT_TARGET_LENGTH;
					targetCoorMap.put(key, new Coordinate(refCoorMap.get(key).getId(), refCoorMap.get(key).getChr(),
							refCoorMap.get(key).getStrand(), refCoorMap.get(key).getStart() + firstPos,
							endPos < refCoorMap.get(key).getEnd() ? endPos : refCoorMap.get(key).getEnd()));
				}
			}
		}
		return targetCoorMap;
	}

	private PotentialSNP declareSNP(Constant constant, int totalTargetSeqenceCount, int[][] mismatchStat,
	                                int targetStart) {
		List<PotentialSNP> potentialSNPList = new ArrayList<>();
		double threshold = totalTargetSeqenceCount * constant.SNPThreshold;
		for (int i = 0; i < mismatchStat.length; i++) {
			int count = 0;
			if (mismatchStat[i][0] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i + targetStart, 'A'));
				count++;
			}
			if (mismatchStat[i][1] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i + targetStart, 'C'));
				count++;
			}
			if (mismatchStat[i][2] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i + targetStart, 'G'));
				count++;
			}
			if (mismatchStat[i][3] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i + targetStart, 'T'));
				count++;
			}
			if (mismatchStat[i][4] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i + targetStart, 'N'));
				count++;
			}
			if (count >= 2) {
				//more than two SNP allele in same position!
				return null;
			}
		}
		switch (potentialSNPList.size()) {
			case 0:
				return null;
			case 1:
				return potentialSNPList.get(0);
			default:
				// More than 1 SNP in the region!
				return null;
		}
	}

	private boolean hasASM(PatternResult patternWithAllele, PatternResult patternWithoutAllele) {
		// use 0.2 as threshold to filter out unequal patterns. ASM pattern should be roughly equal.
		if (patternWithAllele.getPercent() < ASM_PATTERN_THRESHOLD ||
				patternWithoutAllele.getPercent() < ASM_PATTERN_THRESHOLD) {
			return false;
		}
		List<CpGSitePattern> cgListWithAllele = patternWithAllele.getCpGList();
		List<CpGSitePattern> cgListWithoutAllele = patternWithoutAllele.getCpGList();
		// if there is at least one cpg site with different methyl type and the different bigger than 0.2, it is ASM
		for (int i = 0; i < cgListWithAllele.size(); i++) {
			if (cgListWithAllele.get(i).getMethylType() != cgListWithoutAllele.get(i).getMethylType() &&
					Math.abs(cgListWithAllele.get(i).getMethylLevel() - cgListWithoutAllele.get(i).getMethylLevel()) >=
							ASM_MIN_METHYL_DIFFERENCE) {
				return true;
			}
		}
		return false;
	}

	private PatternResult patternToPatternResult(Pattern pattern, List<CpGStatistics> cpGStatisticsList,
	                                             int totalCount, int targetStart) {
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
				}
			}
		}
		for (CpGSitePattern cpGSitePattern : cpGSiteMap.values()) {
			cpGSitePattern.setPosition(cpGSitePattern.getPosition() - targetStart);
		}
		patternResult.setCpGList(new ArrayList<>(cpGSiteMap.values()));
		patternResult.setCount(pattern.getSequenceMap().size());
		patternResult.setPercent(pattern.getSequenceMap().size() / (double) totalCount);
		if (pattern.getPatternType() == Pattern.PatternType.ALLELE) {
			patternResult.addAllele(Integer.parseInt(pattern.getPatternString().split(":")[0]) - targetStart);
		} else if (pattern.getPatternType() != Pattern.PatternType.NONALLELE) {
			throw new RuntimeException("only support convert allele and non-allele Pattern to PatternResult");
		}
		return patternResult;
	}

	private Pair<Pattern, Pattern> getAllelePatterns(List<Sequence> seqGroup, PotentialSNP potentialSNP) {
		// generate allele pattern for each unique(position and character) allele
		Pattern allelePattern = new Pattern(potentialSNP.toString(), Pattern.PatternType.ALLELE);
		Pattern nonAllelePattern = new Pattern(potentialSNP.getPosition() + ":-", Pattern.PatternType.NONALLELE);
		for (Sequence sequence : seqGroup) {
			if (sequence.getOriginalSeq()
					.charAt(potentialSNP.getPosition() - sequence.getStartPos()) == potentialSNP.getNucleotide()) {
				allelePattern.addSequence(sequence);
			} else {
				nonAllelePattern.addSequence(sequence);
			}
		}
		return new ImmutablePair<>(allelePattern, nonAllelePattern);
	}

	private void sortAndAssignPatternID(List<Pattern> patternList) {
		// sort methylation pattern.
		Collections.sort(patternList, new PatternByCountComparator());
		// assign pattern id after sorting. So id is associated with order. Smaller id has large count.
		for (int i = 0; i < patternList.size(); i++) {
			patternList.get(i).assignPatternID(i);
		}
	}

	/**
	 * generate memu pattern.
	 */
	private List<Pattern> getMeMuPatern(List<Sequence> seqGroup, List<Pattern> methylationPatternList,
	                                    PotentialSNP potentialSNP, int targetStart, int targetEnd) {
		// return no memu pattern if there is no snp
		if (potentialSNP == null) {
			return new ArrayList<>();
		}
		Map<String, Pattern> patternMap = new HashMap<>();
		for (Sequence sequence : seqGroup) {
			int meID = -1;
			for (Pattern methylPattern : methylationPatternList) {
				if (methylPattern.getSequenceMap().containsKey(sequence.getId())) {
					meID = methylPattern.getPatternID();
				}
			}
			// seq not included in MethylPattern
			if (meID == -1) {
				continue;
			}
			PotentialSNP snp;
			if (sequence.getOriginalSeq()
					.charAt(potentialSNP.getPosition() - sequence.getStartPos()) == potentialSNP.getNucleotide()) {
				snp = potentialSNP;// declared SNP
			} else {
				snp = new PotentialSNP(potentialSNP.getPosition(), '-'); // reference allele
			}
			String key = String.format("%d:%d:%s", meID, snp.getPosition(), snp.getNucleotide());
			if (patternMap.containsKey(key)) {
				patternMap.get(key).addSequence(sequence);
			} else {
				// new memu pattern
				Pattern memuPatern = new Pattern("", Pattern.PatternType.MEMU);
				sequence.setMeMuString(snp, targetStart, targetEnd);
				memuPatern.setPatternString(sequence.getMeMuString());
				memuPatern.setMethylationParentID(meID);
				memuPatern.addSequence(sequence);
				patternMap.put(key, memuPatern);
			}
		}
		return new ArrayList<>(patternMap.values());
	}

	private List<Pattern> filterMethylationPatterns(List<Pattern> methylationPatterns, double totalSeqCount,
	                                                int refCpGCount, Constant constant) throws MathException {
		if (methylationPatterns.size() != 0 && totalSeqCount != 0) {
			if (constant.criticalValue != -1 && refCpGCount > 3) {
				return filterMethylPatternsByP0Threshold(methylationPatterns, totalSeqCount, refCpGCount, constant);
			} else {
				return filterPatternsByThreshold(methylationPatterns, totalSeqCount, constant.minMethylThreshold);
			}
		}
		// return empty list.
		return new ArrayList<>();
	}

	private List<Pattern> filterPatternsByThreshold(List<Pattern> patterns, double totalSeqCount, double threshold) {
		List<Pattern> qualifiedPatternList = new ArrayList<>();
		// use percentage pattern threshold
		for (Pattern pattern : patterns) {
			double percentage = pattern.getCount() / totalSeqCount;
			if (percentage >= threshold) {
				qualifiedPatternList.add(pattern);
			}
		}
		return qualifiedPatternList;
	}

	private List<Pattern> filterMethylPatternsByP0Threshold(List<Pattern> methylationPatterns, double totalSeqCount,
	                                                        int refCpGCount, Constant constant) throws MathException {
		List<Pattern> qualifiedMethylationPatternList = new ArrayList<>();
		NormalDistributionImpl nd = new NormalDistributionImpl(0, 1);

		int nonNoisePatternCount = 0;
		for (Pattern methylationPattern : methylationPatterns) {
			if (methylationPattern.getCount() != 1) {
				nonNoisePatternCount++;
			}
		}
		double p0 = 1.0 / Math.min(nonNoisePatternCount, Math.pow(2, refCpGCount));

		// significant pattern selection
		for (Pattern methylationPattern : methylationPatterns) {
			double ph = methylationPattern.getCount() / totalSeqCount;
			double z = (ph - p0) / Math.sqrt(ph * (1 - ph) / totalSeqCount);
			double pZ = 1 - nd.cumulativeProbability(z);
			if (pZ <= (constant.criticalValue / methylationPatterns.size())) {
				qualifiedMethylationPatternList.add(methylationPattern);
			}
		}
		System.out.println("methylationPattern count:\t" + methylationPatterns.size());
		System.out.println("nonNoisePatternCount\t" + nonNoisePatternCount);
		System.out.println("qualifiedMethylationPatternList:\t" + qualifiedMethylationPatternList.size());
		return qualifiedMethylationPatternList;
	}

	/**
	 * cutting mapped sequences to reference region and filter reads without covering whole reference seq
	 */
	private Pair<List<Sequence>, List<Sequence>> updateTargetSequences(List<Sequence> sequenceGroup, int targetStart,
	                                                                   int targetEnd, int cpgBoundedStart,
	                                                                   int cpgBoundedEnd) throws
			IOException {
		List<Sequence> CpGBoundSequenceList = new ArrayList<>();
		List<Sequence> otherSequenceList = new ArrayList<>();
		Iterator<Sequence> sequenceIterator = sequenceGroup.iterator();
		while (sequenceIterator.hasNext()) {
			Sequence sequence = sequenceIterator.next();
			if (!(sequence.getStartPos() <= targetStart && sequence.getEndPos() >= targetEnd)) {
				// filter out
				sequenceIterator.remove();
				// recheck if sequence cover all CpGs in ref
				if (sequence.getStartPos() <= cpgBoundedStart && sequence.getEndPos() >= cpgBoundedEnd) {
					CpGBoundSequenceList.add(sequence);
				} else {
					// not cover whole target or all CpGs.
					otherSequenceList.add(sequence);
				}
			}
		}
		return new ImmutablePair<>(CpGBoundSequenceList, otherSequenceList);
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

	private List<Pattern> getMethylPattern(List<Sequence> allMethylSequences,
	                                       final int cpgBoundedStart, final int cpgBoundedEnd) {

		List<Pattern> methylationPatterns = new ArrayList<>();
		// group sequences by methylationString, distribute each seq into one pattern
		Map<String, List<Sequence>> patternMap = groupSeqsByKey(allMethylSequences, seq -> seq.getMethylationString()
				.substring(cpgBoundedStart - seq.getStartPos(), cpgBoundedEnd - seq.getStartPos() + 1));

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

	private int[][] calculateMismatchStat(String targetRefSeq, int targetStart, int targetEnd,
	                                      List<Sequence> targetSequencesList) {
		int[][] mismatchStat = new int[targetRefSeq.length()][6]; // 6 possible values.
		// TODO double check and refactor
		for (Sequence seq : targetSequencesList) {
			char[] seqArray = seq.getOriginalSeq().toCharArray();
			Arrays.fill(seqArray, '-');
			char originalC, bisulfiteC;
			if (seq.getStrand().equals("TOP")) {
				originalC = 'C';
				bisulfiteC = 'T';
			} else {
				originalC = 'G';
				bisulfiteC = 'A';
			}
			for (int i = 0; i < seqArray.length; i++) {
				if (seq.getStartPos() + i >= targetStart && seq.getStartPos() + i <= targetEnd) {
					int offset = seq.getStartPos() + i - targetStart;
					if (seq.isCpGSite(i)) {
						if (seq.getOriginalSeq().charAt(i) != bisulfiteC && seq.getOriginalSeq()
								.charAt(i) != originalC) {
							seqArray[i] = seq.getOriginalSeq().charAt(i);
						}
					} else if (targetRefSeq.charAt(offset) == originalC) {
						if (seq.getOriginalSeq().charAt(i) != originalC && seq.getOriginalSeq()
								.charAt(i) != bisulfiteC) {
							seqArray[i] = seq.getOriginalSeq().charAt(i);
						}
					} else if (seq.getOriginalSeq().charAt(i) != targetRefSeq.charAt(offset)) {
						seqArray[i] = seq.getOriginalSeq().charAt(i);
					}
					switch (seqArray[i]) {
						case 'A':
							mismatchStat[offset][0]++;
							break;
						case 'C':
							mismatchStat[offset][1]++;
							break;
						case 'G':
							mismatchStat[offset][2]++;
							break;
						case 'T':
							mismatchStat[offset][3]++;
							break;
						case 'N':
							mismatchStat[offset][4]++;
							break;
						case '-':
							mismatchStat[offset][5]++;
							break;
						default:
							throw new RuntimeException("unknown nucleotide:" + seqArray[i]);
					}
				}
			}
		}
		return mismatchStat;
	}
}
