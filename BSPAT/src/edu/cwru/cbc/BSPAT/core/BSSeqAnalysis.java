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
 * Bisulfite sequences analysis. Include obtaining methylation string, methyl&SNP string. And group sequences by pattern.
 *
 * @author Ke
 */
public class BSSeqAnalysis {
	public static final int DEFAULT_TARGET_LENGTH = 70;
	public static final double ASM_PATTERN_THRESHOLD = 0.2;
	public static final double ASM_MIN_METHYL_DIFFERENCE = 0.2;
	public static final double MEMU_PATTERN_THRESHOLD = 0.1;

	private static boolean isFirstBPCpGSite(int pos, List<CpGSite> cpGSiteList) {
		for (CpGSite cpGSite : cpGSiteList) {
			if (pos == cpGSite.getPosition()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * generate methylation pattern, calculate conversion rate, methylation rate
	 */
	public static void processSequence(String referenceSeq, final List<Sequence> seqList) {
		// convert reference sequence and count C in non-CpG context.
		StringBuilder convertedReferenceSeq = new StringBuilder();
		// count C in non-CpG context.  Maybe not efficient enough since scan twice.
		int countOfNonCpGC = StringUtils.countMatches(referenceSeq, "C") - StringUtils.countMatches(referenceSeq, "CG");
		for (int i = 0; i < referenceSeq.length(); i++) {
			if (referenceSeq.charAt(i) == 'C' || referenceSeq.charAt(i) == 'c') {
				convertedReferenceSeq.append('T');
			} else {
				convertedReferenceSeq.append(referenceSeq.charAt(i));
			}
		}
		for (Sequence seq : seqList) {
			char[] methylationString = new char[convertedReferenceSeq.length()];
			// fill read to reference length
			double countOfUnConvertedC = 0;
			double countOfMethylatedCpG = 0;
			double unequalNucleotide = 0;

			for (int i = 0; i < seq.getOriginalSeq().length(); i++) {
				methylationString[i] = '-';
			}
			for (int i = 0; i < seq.getOriginalSeq().length(); i++) {
				// meet unequal element
				if (seq.getOriginalSeq().charAt(i) != convertedReferenceSeq.charAt(i)) {
					if (isFirstBPCpGSite(i, seq.getCpGSites())) {
						if (!(seq.getOriginalSeq().charAt(i) == 'T' && convertedReferenceSeq.charAt(i) == 'C') &&
								!(seq.getOriginalSeq().charAt(i) == 'C' && convertedReferenceSeq.charAt(i) == 'T')) {
							unequalNucleotide++;
						}
					} else {
						if (seq.getOriginalSeq().charAt(i) == 'C' && referenceSeq.charAt(i) == 'C') {
							countOfUnConvertedC++;
						} else {
							unequalNucleotide++;
						}
					}
				}
			}
			for (CpGSite cpg : seq.getCpGSites()) {
				int pos = cpg.getPosition();
				if (seq.getStrand().equals("BOTTOM")) {
					pos--;
				}
				if (seq.isInSeq(pos)) {
					if (cpg.isMethylated()) {
						countOfMethylatedCpG++;
						// methylated CpG site represent by @@
						methylationString[pos] = '@';
						if (pos + 1 <= methylationString.length) {
							methylationString[pos + 1] = '@';
						}
					} else {
						// un-methylated CpG site represent by **. Exclude mutation in CpG site.
						if (pos != referenceSeq.length()) {
							methylationString[pos] = '*';
							if (pos + 1 <= methylationString.length) {
								methylationString[pos + 1] = '*';
							}
						}
					}
				}
			}
			// fill sequence content including calculation fo bisulfite
			// conversion rate and methylation rate for each sequence.
			seq.setBisulConversionRate(1 - (countOfUnConvertedC / countOfNonCpGC));
			seq.setMethylationRate(countOfMethylatedCpG / seq.getCpGSites().size());
			seq.setSequenceIdentity(1 - unequalNucleotide / (seq.getOriginalSeq().length() - seq.getCpGSites().size()));
			seq.setMethylationString(new String(methylationString));
		}
	}

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
		Map<String, List<Sequence>> sequenceGroupMap = groupSeqsByKey(sequencesList, new GetKeyFunction() {
			@Override
			public String getKey(Sequence seq) {
				return seq.getRegion();
			}
		});

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
					targetStart = (int) (targetCoor.getStart() - refCoor.getStart()); // 0-based
					targetEnd = (int) (targetCoor.getEnd() - refCoor.getStart()) - 1; // 0-based
					break;
				case "-":
					targetStart = (int) (refCoor.getEnd() - targetCoor.getEnd()); // 0-based
					targetEnd = (int) (refCoor.getEnd() - targetCoor.getStart()) - 1; // 0-based
					break;
				default:
					throw new RuntimeException("invalid reference strand!");
			}
			if (targetStart < 0 || targetEnd < 0 || targetStart >= targetEnd) {
				throw new RuntimeException("invalid target coordinates!");
			}
			// cut reference seq
			String targetRefSeq = refSeq.substring(targetStart, targetEnd + 1);

			// calculate mismatch stat based on all sequences in reference region.
			int[][] mismatchStat = calculateMismatchStat(targetRefSeq, targetStart, targetEnd, seqGroup);
			// declare SNP
			PotentialSNP potentialSNP = declareSNP(constant, seqGroup.size(), mismatchStat);

			// seqs in seqGroup got changed in updateTargetSequences().
			Pair<List<Sequence>, List<Sequence>> filteredCuttingResultPair = updateTargetSequences(seqGroup,
					targetRefSeq, targetStart, targetEnd);
			List<Sequence> CpGBoundSequenceList = filteredCuttingResultPair.getLeft();
			List<Sequence> otherSequenceList = filteredCuttingResultPair.getRight();
			reportSummary.setSeqOthers(otherSequenceList.size());

			// generate methylation pattern, calculate conversion rate, methylation rate
			processSequence(targetRefSeq, seqGroup);
			processSequence(getBoundedSeq("CG", targetRefSeq), CpGBoundSequenceList);

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

			// if no sequence exist after filtering, return empty reportSummary
			if (seqGroup.size() == 0 && CpGBoundSequenceList.size() == 0) {
				reportSummaries.add(reportSummary);
				continue;
			}

			// write report
			Report report = new Report(region, outputFolder, targetRefSeq, constant, reportSummary);
			report.writeReport(filteredTargetSequencePair, filteredCpGSequencePair, mismatchStat);

			DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion,
					constant.toolsPath, region, outputFolder, experimentName, targetCoorMap, targetRefSeq, targetStart);

			// generate methyl pattern output
			List<Pattern> methylationPatternList = getMethylPattern(seqGroup, CpGBoundSequenceList, targetRefSeq);
			System.out.println(experimentName + "\t" + region);
			methylationPatternList = filterMethylationPatterns(methylationPatternList,
					seqGroup.size() + CpGBoundSequenceList.size(), StringUtils.countMatches(targetRefSeq, "CG"),
					constant);
			if (methylationPatternList.size() == 0) {
				reportSummaries.add(reportSummary);
				continue;
			}
			Pattern.resetPatternCount();
			sortAndAssignPatternID(methylationPatternList);
			reportSummary.addPatternLink(PatternLink.METHYLATION);
			List<Sequence> allMethylSequences = new ArrayList<>();
			allMethylSequences.addAll(seqGroup);
			allMethylSequences.addAll(CpGBoundSequenceList);
			report.writePatterns(methylationPatternList, PatternLink.METHYLATION, allMethylSequences);
			drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.METHYLATION));

			// generate memu pattern output
			List<Pattern> meMuPatternList = getMeMuPatern(seqGroup, methylationPatternList, potentialSNP);
			meMuPatternList = filterPatternsByThreshold(meMuPatternList, seqGroup.size(), MEMU_PATTERN_THRESHOLD);
			if (meMuPatternList.size() != 0) {
				reportSummary.addPatternLink(PatternLink.METHYLATIONWITHSNP);
				report.writePatterns(meMuPatternList, PatternLink.METHYLATIONWITHSNP, seqGroup);
				drawFigureLocal.drawPattern(reportSummary.getPatternLink(PatternLink.METHYLATIONWITHSNP));
			}

			// ASM
			if (potentialSNP != null) {
				Pair<Pattern, Pattern> allelePatterns = getAllelePatterns(seqGroup, potentialSNP);
				Pattern allelePattern = allelePatterns.getLeft();
				Pattern nonAllelePattern = allelePatterns.getRight();
				if (allelePattern.getSequenceMap().size() != 0 && nonAllelePattern.getSequenceMap().size() != 0) {
					PatternResult patternWithAllele = patternToPatternResult(allelePattern, report.getCpgStatList(),
							seqGroup.size());
					PatternResult patternWithoutAllele = patternToPatternResult(nonAllelePattern,
							report.getCpgStatList(), seqGroup.size());

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
				long firstPos = refString.indexOf("CG");
				// no CpG found in ref seq, use ref start and DEFAULT_TARGET_LENGTH.
				if (firstPos == -1) {
					targetCoorMap.put(key, new Coordinate(refCoorMap.get(key).getId(), refCoorMap.get(key).getChr(),
							refCoorMap.get(key).getStrand(), refCoorMap.get(key).getStart(),
							refCoorMap.get(key).getStart() + DEFAULT_TARGET_LENGTH));
				} else {
					// from first CpG to min(ref end, fisrt CpG + DEFAULT_TARGET_LENGTH)
					long endPos = refCoorMap.get(key).getStart() + firstPos + DEFAULT_TARGET_LENGTH;
					targetCoorMap.put(key, new Coordinate(refCoorMap.get(key).getId(), refCoorMap.get(key).getChr(),
							refCoorMap.get(key).getStrand(), refCoorMap.get(key).getStart() + firstPos,
							endPos < refCoorMap.get(key).getEnd() ? endPos : refCoorMap.get(key).getEnd()));
				}
			}
		}
		return targetCoorMap;
	}

	private PotentialSNP declareSNP(Constant constant, int totalSequenceCount, int[][] mismatchStat) {
		List<PotentialSNP> potentialSNPList = new ArrayList<>();
		double threshold = totalSequenceCount * constant.SNPThreshold;
		for (int i = 0; i < mismatchStat.length; i++) {
			int count = 0;
			if (mismatchStat[i][0] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i, 'A'));
				count++;
			}
			if (mismatchStat[i][1] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i, 'C'));
				count++;
			}
			if (mismatchStat[i][2] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i, 'G'));
				count++;
			}
			if (mismatchStat[i][3] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i, 'T'));
				count++;
			}
			if (mismatchStat[i][4] >= threshold) {
				potentialSNPList.add(new PotentialSNP(i, 'N'));
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
			patternResult.addAllele(Integer.parseInt(pattern.getPatternString().split(":")[0]));
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
			if (sequence.getOriginalSeq().charAt(potentialSNP.getPosition()) == potentialSNP.getNucleotide()) {
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
		for (Pattern pattern : patternList) {
			pattern.assignPatternID();
		}
	}

	/**
	 * generate memu pattern.
	 */
	private List<Pattern> getMeMuPatern(List<Sequence> seqGroup, List<Pattern> methylationPatternList,
	                                    PotentialSNP potentialSNP) {
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
			if (sequence.getOriginalSeq().charAt(potentialSNP.getPosition()) == potentialSNP.getNucleotide()) {
				snp = potentialSNP;
			} else {
				snp = new PotentialSNP(potentialSNP.getPosition(), '-');
			}
			String key = String.format("%d:%d:%s", meID, snp.getPosition(), snp.getNucleotide());
			if (patternMap.containsKey(key)) {
				patternMap.get(key).addSequence(sequence);
			} else {
				// new memu pattern
				Pattern memuPatern = new Pattern("", Pattern.PatternType.MEMU);
				sequence.setMeMuString(snp);
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
	private Pair<List<Sequence>, List<Sequence>> updateTargetSequences(List<Sequence> sequenceGroup,
	                                                                   String targetRefSeq,
	                                                                   int targetStart,
	                                                                   int targetEnd) throws IOException {
		List<Sequence> CpGBoundSequenceList = new ArrayList<>();
		List<Sequence> otherSequenceList = new ArrayList<>();
		Iterator<Sequence> sequenceIterator = sequenceGroup.iterator();
		while (sequenceIterator.hasNext()) {
			Sequence sequence = sequenceIterator.next();
			if (sequence.getStartPos() <= targetStart && sequence.getEndPos() >= targetEnd) {
				// cut sequence to suit reference
				sequence.setOriginalSeq(sequence.getOriginalSeq()
						.substring(targetStart - sequence.getStartPos(), targetEnd - sequence.getStartPos() + 1));
				updateCpGPosition(targetStart, targetStart, targetEnd, sequence);
				sequence.setStartPos(1);
			} else {
				// filter out
				sequenceIterator.remove();
				// recheck if sequence cover all CpGs in ref
				int startCpGPos = targetRefSeq.indexOf("CG") + targetStart;
				int endCpGPos = targetRefSeq.lastIndexOf("CG") + targetStart;
				if (sequence.getStartPos() <= startCpGPos && sequence.getEndPos() >= (endCpGPos + 1)) {
					CpGBoundSequenceList.add(sequence);
					sequence.setOriginalSeq(sequence.getOriginalSeq()
							.substring(startCpGPos - sequence.getStartPos(), endCpGPos + 2 - sequence.getStartPos()));
					sequence.setStartPos(startCpGPos);
					updateCpGPosition(sequence.getStartPos(), sequence.getStartPos(), endCpGPos + 1, sequence);
				} else {
					// not cover whole target or all CpGs.
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
				// CpGbounded seqs
				if (seq.getOriginalSeq().length() == (endCpGPos - startCpGPos + 2)) {
					return seq.getMethylationString();
				} else {
					return seq.getMethylationString().substring(startCpGPos, endCpGPos + 2);
				}
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

	private int[][] calculateMismatchStat(String targetRefSeq, int targetStart, int targetEnd,
	                                      List<Sequence> targetSequencesList) {
		int[][] mismatchStat;
		mismatchStat = new int[targetRefSeq.length()][5]; // 5 possible values.
		// TODO double check and refactor
		for (Sequence seq : targetSequencesList) {
			char[] seqArray = seq.getOriginalSeq().toCharArray();
			Arrays.fill(seqArray, '-');
			for (int i = 0; i < seqArray.length; i++) {
				if (seq.getStartPos() + i >= targetStart && seq.getStartPos() + i <= targetEnd) {
					int offset = seq.getStartPos() + i - targetStart;
					if (isFirstBPCpGSite(i + seq.getStartPos(), seq.getCpGSites())) {
						if (seq.getOriginalSeq().charAt(i) != 'T' && seq.getOriginalSeq().charAt(i) != 'C') {
							seqArray[i] = seq.getOriginalSeq().charAt(i);
						}
					} else if (targetRefSeq.charAt(offset) == 'C') {
						if (seq.getOriginalSeq().charAt(i) != 'C' && seq.getOriginalSeq().charAt(i) != 'T') {
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
							// do nothing
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
