package edu.cwru.cbc.BSPAT.CLI;

import edu.cwru.cbc.BSPAT.commons.*;
import htsjdk.samtools.util.SequenceUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * Created by lancelothk on 2/9/16.
 * Command line entry of BSPAT.
 */
@SuppressWarnings("Duplicates")
public class BSPAT_pgm {

	// use 0.2 as threshold to filter out unequal patterns. ASM pattern should be roughly equal.
	private static final double ASM_PATTERN_THRESHOLD = 0.2;
	private static final double ASM_MIN_METHYL_DIFFERENCE = 0.2;

	public static void main(String[] args) throws IOException, InterruptedException, ParseException {
		Options options = new Options();
		// Require all input path to be directory. File is not allowed.
		options.addOption(Option.builder("r")
				.desc("Reverse result. Display result in complementary strand, when minus strand reference used")
				.build());
		options.addOption(Option.builder("o").hasArg().desc("Output Path").build());
		options.addOption(Option.builder("b").hasArg().desc("Bisulfite Conversion Rate").build());
		options.addOption(Option.builder("i").hasArg().desc("Sequence Identity Threshold").build());
		options.addOption(Option.builder("m").hasArg().desc("Methylation pattern Threshold").build());
		options.addOption(Option.builder("n").hasArg().desc("MethylationWithSNP pattern Threshold").build());
		options.addOption(Option.builder("s").hasArg().desc("significant SNP Threshold").build());
		options.addOption(Option.builder("c").hasArg().desc("Critical Value").build());
		options.addOption(Option.builder("h").desc("Help").build());

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"BSPAT [options] <reference file Path or file> <bismark result path or file> <target region file>",
					options);
			System.exit(1);
		}

		String referencePath, bismarkResultPath, targetRegionFile;
		if (cmd.getArgList().size() != 3) {
			throw new RuntimeException(
					"Incorrect number of arguments! BSPAT [options] <reference file Path or file> <bismark result path or file> <target region file>");
		} else {
			referencePath = cmd.getArgList().get(0);
			bismarkResultPath = cmd.getArgList().get(1);
			targetRegionFile = cmd.getArgList().get(2);
			System.out.println("Reference path is " + referencePath);
			System.out.println("Bismark result path is " + bismarkResultPath);
			System.out.println("Target region file is " + targetRegionFile);
		}

		String outputPath = cmd.getOptionValue("o", getPath(bismarkResultPath));
		File outputPathFile = new File(outputPath);
		if (!outputPathFile.exists()) {
			if (!outputPathFile.mkdirs()) {
				throw new RuntimeException("cannot create output path folder!");
			}
		}
		outputPath = outputPathFile.getAbsolutePath();
		double bisulfiteConversionRate = Double.parseDouble(cmd.getOptionValue("b", "0.9"));
		double sequenceIdentityThreshold = Double.parseDouble(cmd.getOptionValue("i", "0.9"));
		double methylPatternThreshold = Double.parseDouble(cmd.getOptionValue("m", "0.01"));
		double memuPatternThreshold = Double.parseDouble(cmd.getOptionValue("m", "0.1"));
		double snpThreshold = Double.parseDouble(cmd.getOptionValue("s", "0.2"));
		double criticalValue = Double.parseDouble(cmd.getOptionValue("c", "0.01"));

		System.out.println(
				"Caution: Bismark attempts to extract 2 additional bps from end of reference sequence to be able to determine the sequence context (CG, CHG or CHH). " +
						"So reads align to exact end of reference sequence will be rejected in Bismark result. " +
						"To avoid exclusion of those reads, it is recommended to add 2 additional bps in both ends of reference sequence.");

		generatePatterns(referencePath, bismarkResultPath, outputPath, IOUtils.readBedFile(targetRegionFile),
				bisulfiteConversionRate, sequenceIdentityThreshold, criticalValue, methylPatternThreshold,
				memuPatternThreshold, snpThreshold);
	}

	private static String getPath(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			return file.getPath();
		} else {
			return file.getParent();
		}
	}

	private static void generatePatterns(String referencePath, String bismarkResultPath, String outputPath,
	                                     Map<String, List<BedInterval>> targetRegionMap, double bisulfiteConversionRate,
	                                     double sequenceIdentityThreshold, double criticalValue,
	                                     double methylPatternThreshold, double memuPatternThreshold,
	                                     double snpThreshold) throws
			IOException {
		Map<String, String> referenceMap = IOUtils.readReference(referencePath);
		// reference names of target regions should be subset of reference names in reference file.
		for (String name : targetRegionMap.keySet()) {
			if (!referenceMap.keySet().contains(name)) {
				throw new RuntimeException(
						name + " in target regions is not a valid reference name in reference file!");
			}
		}
		IOUtils.readBismarkAlignmentResults(bismarkResultPath, targetRegionMap);
		for (Map.Entry<String, List<BedInterval>> chromosomeEntry : targetRegionMap.entrySet()) {
			String refSeq = referenceMap.get(chromosomeEntry.getKey());
			for (BedInterval targetRegion : chromosomeEntry.getValue()) {
				generatePatternsSingleGroup(outputPath, bisulfiteConversionRate, sequenceIdentityThreshold,
						criticalValue, methylPatternThreshold, snpThreshold, memuPatternThreshold, targetRegion,
						refSeq);
			}
		}
	}

	private static void generatePatternsSingleGroup(String outputPath, double bisulfiteConversionRate,
	                                                double sequenceIdentityThreshold, double criticalValue,
	                                                double methylPatternThreshold, double memuPatternThreshold,
	                                                double snpThreshold, BedInterval targetRegion,
	                                                String refSeq) throws
			IOException {
		System.out.printf("analyse region %s-%d-%d\n", targetRegion.getChr(), targetRegion.getStart(),
				targetRegion.getEnd());
		if (targetRegion.isMinusStrand()) {
			targetRegion.reverse(refSeq.length());
			refSeq = SequenceUtil.reverseComplement(refSeq);
			for (Sequence sequence : targetRegion.getSequenceList()) {
				sequence.reverse(refSeq.length());
			}
		}
		String targetRefSeq = refSeq.substring(targetRegion.getStart(), targetRegion.getEnd() + 1);
		String targetRefSeqExtend = refSeq.substring(Math.max(0, targetRegion.getStart() - 1),
				Math.min(refSeq.length() - 1, targetRegion.getEnd() + 2));
		List<Sequence> seqGroup = targetRegion.getSequenceList();
		if (seqGroup.size() == 0) {
			System.err.printf("target %s-%s-%d-%d isn't fully covered by any sequence.\n", targetRegion.getName(),
					targetRegion.getChr(), targetRegion.getStart(), targetRegion.getEnd());
			return;
		}
		// processing sequences
		for (Sequence sequence : seqGroup) {
			sequence.processSequence(refSeq);
		}

		// quality filtering
		Pair<List<Sequence>, List<Sequence>> qualityFilterSequencePair = filterSequences(
				seqGroup, bisulfiteConversionRate, sequenceIdentityThreshold);

		List<Sequence> sequencePassedQualityFilter = qualityFilterSequencePair.getLeft();
		IOUtils.writeAnalysedSequences(outputPath + "/" + targetRegion.toString() + "_bismark.analysis.txt",
				sequencePassedQualityFilter, refSeq.length(), targetRegion.isMinusStrand());

		// generate methyl pattern output
		List<Pattern> methylationPatternList = getMethylPattern(sequencePassedQualityFilter, targetRegion.getStart(),
				targetRegion.getEnd());

		methylationPatternList = filterMethylationPatterns(methylationPatternList,
				sequencePassedQualityFilter.size(), StringUtils.countMatches(targetRefSeqExtend, "CG"), criticalValue,
				methylPatternThreshold);

		// sort pattern and assign id index
		methylationPatternList.sort((p1, p2) -> Integer.compare(p2.getCount(), p1.getCount()));
		for (int i = 0; i < methylationPatternList.size(); i++) {
			methylationPatternList.get(i).assignPatternID(i);
		}

		if (methylationPatternList.size() != 0) {
			IOUtils.writePatterns(String.format("%s/%s_bismark.analysis_%s.txt", outputPath, targetRegion.toString(),
					Pattern.METHYLATION),
					targetRefSeq, methylationPatternList, Pattern.METHYLATION, sequencePassedQualityFilter.size());
		}

		// calculate mismatch stat based on all sequences in reference region.
		int[][] mismatchStat = calculateMismatchStat(targetRefSeq, targetRegion.getStart(), targetRegion.getEnd(),
				sequencePassedQualityFilter);

		// declare SNP
		PotentialSNP potentialSNP = declareSNP(snpThreshold, sequencePassedQualityFilter.size(),
				mismatchStat, targetRegion.getStart());
		if (potentialSNP != null) {
			// generate memu pattern output
			List<Pattern> meMuPatternList = getMeMuPatern(sequencePassedQualityFilter, methylationPatternList,
					potentialSNP, targetRegion.getStart(), targetRegion.getEnd());
			meMuPatternList = filterPatternsByThreshold(meMuPatternList, sequencePassedQualityFilter.size(),
					memuPatternThreshold);
			IOUtils.writePatterns(String.format("%s/%s_bismark.analysis_%s.txt", outputPath, targetRegion.toString(),
					Pattern.METHYLATIONWITHSNP),
					targetRefSeq, meMuPatternList, Pattern.METHYLATIONWITHSNP, sequencePassedQualityFilter.size());

			Pair<Pattern, Pattern> allelePatterns = getAllelePatterns(seqGroup, potentialSNP);
			Pattern allelePattern = allelePatterns.getLeft();
			Pattern nonAllelePattern = allelePatterns.getRight();
			if (allelePattern.getCount() != 0 && nonAllelePattern.getCount() != 0) {
				PatternResult patternWithAllele = patternToPatternResult(allelePattern, seqGroup.size(),
						targetRegion.getStart(),
						targetRegion.getEnd());
				PatternResult patternWithoutAllele = patternToPatternResult(nonAllelePattern, seqGroup.size(),
						targetRegion.getStart(),
						targetRegion.getEnd());
				if (hasASM(patternWithAllele, patternWithoutAllele)) {
					IOUtils.writeASMPattern(
							String.format("%s/%s_bismark.analysis_ASM.txt", outputPath, targetRegion.toString()),
							targetRefSeq, patternWithAllele, patternWithoutAllele);
				}
			}
		}
		IOUtils.writeStatistics(String.format("%s/%s_bismark.analysis_report.txt", outputPath, targetRegion.toString()),
				sequencePassedQualityFilter, mismatchStat, targetRegion.getStart(), targetRefSeq,
				bisulfiteConversionRate, sequenceIdentityThreshold, criticalValue, memuPatternThreshold,
				memuPatternThreshold, snpThreshold, seqGroup.size());
	}

	private static boolean hasASM(PatternResult patternWithAllele, PatternResult patternWithoutAllele) {
		if (patternWithAllele.getPercent() < ASM_PATTERN_THRESHOLD ||
				patternWithoutAllele.getPercent() < ASM_PATTERN_THRESHOLD) {
			return false;
		}
		List<CpGStatistics> cgListWithAllele = patternWithAllele.getCpGList();
		List<CpGStatistics> cgListWithoutAllele = patternWithoutAllele.getCpGList();
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

	private static PatternResult patternToPatternResult(Pattern pattern, int totalCount, int targetStart,
	                                                    int targetEnd) {
		PatternResult patternResult = new PatternResult();
		Map<Integer, CpGStatistics> cpGSiteMap = new HashMap<>();
		for (Sequence sequence : pattern.getSequenceMap().values()) {
			for (CpGSite cpGSite : sequence.getCpGSites()) {
				int pos = cpGSite.getPosition();
				if (pos >= targetStart - 1 && pos <= targetEnd + 1) {
					if (!cpGSiteMap.containsKey(pos)) {
						cpGSiteMap.put(pos, new CpGStatistics(pos, false));
					}
					if (cpGSite.isMethylated()) {
						cpGSiteMap.get(pos).addMethylCount(1);
					} else {
						cpGSiteMap.get(pos).addNonMethylCount(1);
					}
				}
			}
		}
		for (CpGStatistics cpGSitePattern : cpGSiteMap.values()) {
			cpGSitePattern.setPosition(cpGSitePattern.getPosition() - targetStart);
			cpGSitePattern.calcMethylLevel();
		}
		patternResult.setCpGList(new ArrayList<>(cpGSiteMap.values()));
		patternResult.setCount(pattern.getSequenceMap().size());
		patternResult.setPercent(pattern.getSequenceMap().size() / (double) totalCount);
		if (pattern.getPatternType() == Pattern.PatternType.ALLELE) {
			patternResult.setSnp(
					new PotentialSNP(Integer.parseInt(pattern.getPatternString().split(":")[0]) - targetStart,
							pattern.getPatternString().split(":")[1].charAt(0)));
		} else if (pattern.getPatternType() != Pattern.PatternType.NONALLELE) {
			throw new RuntimeException("only support convert allele and non-allele Pattern to PatternResult");
		}
		return patternResult;
	}

	private static Pair<Pattern, Pattern> getAllelePatterns(List<Sequence> seqGroup, PotentialSNP potentialSNP) {
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

	private static Map<String, List<Sequence>> groupSeqsByKey(List<Sequence> sequencesList,
	                                                          Function<Sequence, String> getKey) {
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

	private static List<Pattern> filterMethylationPatterns(List<Pattern> methylationPatterns, double totalSeqCount,
	                                                       int refCpGCount, double criticalValue,
	                                                       double methylPatternThreshold) {
		if (methylationPatterns.size() != 0 && totalSeqCount != 0) {
			if (criticalValue != -1 && refCpGCount > 3) {
				return filterMethylPatternsByP0Threshold(methylationPatterns, totalSeqCount, refCpGCount,
						criticalValue);
			} else {
				return filterPatternsByThreshold(methylationPatterns, totalSeqCount, methylPatternThreshold);
			}
		}
		// return empty list.
		return new ArrayList<>();
	}

	private static List<Pattern> filterPatternsByThreshold(List<Pattern> patterns, double totalSeqCount,
	                                                       double threshold) {
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

	private static List<Pattern> filterMethylPatternsByP0Threshold(List<Pattern> methylationPatterns,
	                                                               double totalSeqCount,
	                                                               int refCpGCount, double criticalValue) {
		List<Pattern> qualifiedMethylationPatternList = new ArrayList<>();
		NormalDistribution nd = new NormalDistribution(0, 1);

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
			if (pZ <= (criticalValue / methylationPatterns.size())) {
				qualifiedMethylationPatternList.add(methylationPattern);
			}
		}
		System.out.println("methylationPattern count:\t" + methylationPatterns.size());
		System.out.println("nonNoisePatternCount\t" + nonNoisePatternCount);
		System.out.println("qualifiedMethylationPatternList:\t" + qualifiedMethylationPatternList.size());
		return qualifiedMethylationPatternList;
	}

	private static List<Pattern> getMethylPattern(List<Sequence> allMethylSequences, int targetStart, int targetEnd) {
		List<Pattern> methylationPatterns = new ArrayList<>();
		// group sequences by methylationString, distribute each seq into one pattern
		Map<String, List<Sequence>> patternMap = groupSeqsByKey(allMethylSequences, seq -> seq.getMethylationString()
				.substring(targetStart - seq.getStartPos(), targetEnd - seq.getStartPos() + 1));
		for (String methylString : patternMap.keySet()) {
			List<Sequence> patternSeqList = patternMap.get(methylString);
			Pattern methylationPattern = new Pattern(methylString, Pattern.PatternType.METHYLATION);
			patternSeqList.forEach(methylationPattern::addSequence);
			methylationPatterns.add(methylationPattern);
		}
		return methylationPatterns;
	}

	private static List<Pattern> getMeMuPatern(List<Sequence> seqGroup, List<Pattern> methylationPatternList,
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

	/**
	 * fill sequence list filtered by threshold.
	 */
	private static Pair<List<Sequence>, List<Sequence>> filterSequences(List<Sequence> seqList,
	                                                                    double bisulfiteConversionRate,
	                                                                    double sequenceIdentityThreshold) throws
			IOException {
		List<Sequence> qualifiedSeqList = new ArrayList<>();
		List<Sequence> unQualifiedSeqList = new ArrayList<>();
		for (Sequence seq : seqList) {
			// filter unqualified reads
			if (seq.getBisulConversionRate() >= bisulfiteConversionRate && seq.getSequenceIdentity() >= sequenceIdentityThreshold) {
				qualifiedSeqList.add(seq);
			} else {
				unQualifiedSeqList.add(seq);
			}
		}
		return new ImmutablePair<>(qualifiedSeqList, unQualifiedSeqList);
	}

	private static int[][] calculateMismatchStat(String targetRefSeq, int targetStart, int targetEnd,
	                                             List<Sequence> targetSequencesList) {
		int[][] mismatchStat = new int[targetRefSeq.length()][6]; // 6 possible values.
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

	// TODO add logging for exception cases.
	private static PotentialSNP declareSNP(double snpThreshold, int totalTargetSeqenceCount, int[][] mismatchStat,
	                                       int targetStart) {
		List<PotentialSNP> potentialSNPList = new ArrayList<>();
		double threshold = totalTargetSeqenceCount * snpThreshold;
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
}
