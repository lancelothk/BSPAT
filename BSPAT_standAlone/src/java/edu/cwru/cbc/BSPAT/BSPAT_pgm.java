package edu.cwru.cbc.BSPAT;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by lancelothk on 2/9/16.
 * Command line entry of BSPAT.
 */
@SuppressWarnings("Duplicates")
public class BSPAT_pgm {

	public static final String METHYLATION = "Methylation";
	public static final String METHYLATIONWITHSNP = "MethylationWithSNP";

	public static void main(String[] args) throws IOException, InterruptedException, ParseException {
		double bisulfiteConversionRate = 0.9, sequenceIdentityThreshold = 0.9, criticalValue = 0.01,
				methylPatternThreshold = 0.01, memuPatternThreshold = 0.1, snpThreshold = 0.2;
		String referencePath, bismarkResultPath, outputPath, targetRegionFile;

		Options options = new Options();
		// Require all input path to be directory. File is not allowed.
		options.addOption(Option.builder("r").hasArg().desc("Reference Path").required().build());
		options.addOption(Option.builder("i").hasArg().desc("Bismark result Path").required().build());
		options.addOption(Option.builder("t").hasArg().desc("Target region file").required().build());
		options.addOption(Option.builder("o").hasArg().desc("Output Path").required().build());
		options.addOption(Option.builder("b").hasArg().desc("Bisulfite Conversion Rate").build());
		options.addOption(Option.builder("s").hasArg().desc("Sequence Identity Threshold").build());
		options.addOption(Option.builder("m").hasArg().desc("Methylation pattern Threshold").build());
		options.addOption(Option.builder("p").hasArg().desc("significant SNP Threshold").build());
		options.addOption(Option.builder("c").hasArg().desc("Critical Value").build());

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		referencePath = validatePath(cmd.getOptionValue("r"));
		bismarkResultPath = validatePath(cmd.getOptionValue("i"));
		outputPath = validatePath(cmd.getOptionValue("o"));
		targetRegionFile = cmd.getOptionValue("t");

		if (cmd.hasOption("b")) {
			bisulfiteConversionRate = Double.parseDouble(cmd.getOptionValue("b"));
		}
		if (cmd.hasOption("s")) {
			sequenceIdentityThreshold = Double.parseDouble(cmd.getOptionValue("s"));
		}
		if (cmd.hasOption("m")) {
			methylPatternThreshold = Double.parseDouble(cmd.getOptionValue("m"));
		}
		if (cmd.hasOption("p")) {
			snpThreshold = Double.parseDouble(cmd.getOptionValue("p"));
		}
		if (cmd.hasOption("c")) {
			criticalValue = Double.parseDouble(cmd.getOptionValue("c"));
		}

		generatePatterns(referencePath, bismarkResultPath, outputPath, Utils.readBedFile(targetRegionFile),
				bisulfiteConversionRate, sequenceIdentityThreshold, criticalValue, methylPatternThreshold, snpThreshold,
				memuPatternThreshold);
	}

	public static String validatePath(String path) {
		if (!path.endsWith("/")) {
			return path + "/";
		} else {
			return path;
		}
	}

	private static void generatePatterns(String referencePath, String bismarkResultPath, String outputPath,
	                                     Map<String, List<BedInterval>> bedIntervalMap, double bisulfiteConversionRate,
	                                     double sequenceIdentityThreshold, double criticalValue,
	                                     double methylPatternThreshold, double snpThreshold, double memuPatternThreshold) throws
			IOException {
		ImportBismarkResult importBismarkResult = new ImportBismarkResult(referencePath, bismarkResultPath);
		Map<String, String> referenceSeqs = importBismarkResult.getReferenceSeqs();
		List<Sequence> sequencesList = importBismarkResult.getSequencesList();
		for (Map.Entry<String, List<BedInterval>> chromosomeEntry : bedIntervalMap.entrySet()) {
			String refSeq = referenceSeqs.get(chromosomeEntry.getKey());
			for (BedInterval bedInterval : chromosomeEntry.getValue()) {
				List<Sequence> seqGroup = new ArrayList<>();
				// TODO use indexed bam to speed up query
				for (Sequence sequence : sequencesList) {
					if (sequence.getStartPos() <= bedInterval.getStart() && sequence.getEndPos() >= bedInterval.getEnd()) {
						seqGroup.add(sequence);
					}
				}
				generatePatternsSingleGroup(outputPath, bisulfiteConversionRate, sequenceIdentityThreshold,
						criticalValue, methylPatternThreshold, snpThreshold, memuPatternThreshold, bedInterval,
						seqGroup, refSeq);
			}
		}
	}

	private static void generatePatternsSingleGroup(String outputPath, double bisulfiteConversionRate,
	                                                double sequenceIdentityThreshold, double criticalValue,
	                                                double methylPatternThreshold, double snpThreshold,
	                                                double memuPatternThreshold, BedInterval bedInterval, List<Sequence> seqGroup,
	                                                String refSeq) throws IOException {
		int targetStart = bedInterval.getStart(), targetEnd = bedInterval.getEnd(); // 0-based
		String targetRefSeq = refSeq.substring(targetStart, targetEnd + 1);// 0-based

		// processing sequences
		for (Sequence sequence : seqGroup) {
			sequence.processSequence(refSeq);
		}

		// quality filtering
		Pair<List<Sequence>, List<Sequence>> qualityFilterSequencePair = filterSequences(
				seqGroup, bisulfiteConversionRate, sequenceIdentityThreshold);

		List<Sequence> sequencePassedQualityFilter = qualityFilterSequencePair.getLeft();
		writeAnalysedSequences(outputPath + bedInterval.toString() + "_bismark.analysis.txt",
				sequencePassedQualityFilter);

		// generate methyl pattern output
		List<Pattern> methylationPatternList = getMethylPattern(sequencePassedQualityFilter, targetStart, targetEnd);

		methylationPatternList = filterMethylationPatterns(methylationPatternList,
				sequencePassedQualityFilter.size(), StringUtils.countMatches(targetRefSeq, "CG"), criticalValue,
				methylPatternThreshold);

		// sort pattern and assign id index
		methylationPatternList.sort((p1, p2) -> Integer.compare(p2.getCount(), p1.getCount()));
		for (int i = 0; i < methylationPatternList.size(); i++) {
			methylationPatternList.get(i).assignPatternID(i);
		}

		writePatterns(String.format("%s%s_bismark.analysis_%s.txt", outputPath, bedInterval.toString(), METHYLATION),
				targetRefSeq,
				methylationPatternList, METHYLATION, sequencePassedQualityFilter.size());

		// calculate mismatch stat based on all sequences in reference region.
		int[][] mismatchStat = calculateMismatchStat(targetRefSeq, targetStart, targetEnd,
				sequencePassedQualityFilter);

		// declare SNP
		PotentialSNP potentialSNP = declareSNP(snpThreshold, sequencePassedQualityFilter.size(),
				mismatchStat, targetStart);

		if (potentialSNP != null) {
			// generate memu pattern output
			List<Pattern> meMuPatternList = getMeMuPatern(sequencePassedQualityFilter, methylationPatternList,
					potentialSNP, targetStart, targetEnd);
			meMuPatternList = filterPatternsByThreshold(meMuPatternList, sequencePassedQualityFilter.size(),
					memuPatternThreshold);
			writePatterns(String.format("%s%s_bismark.analysis_%s.txt", outputPath, bedInterval.toString(),
					METHYLATIONWITHSNP),
					targetRefSeq, meMuPatternList, METHYLATIONWITHSNP, sequencePassedQualityFilter.size());
		}
	}

	/**
	 * group sequences by given key function
	 *
	 * @param getKey function parameter to return String key.
	 * @return HashMap contains <key function return value, grouped sequence list>
	 */
	private static Map<String, List<Sequence>> groupSeqsByKey(List<Sequence> sequencesList, GetKeyFunction getKey) {
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

	public static void writePatterns(String patternFileName, String refSeq, List<Pattern> patternList,
	                                 String patternType, double sequenceCount) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(patternFileName))) {
			switch (patternType) {
				case METHYLATION:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tPatternID\n", patternType));
					bufferedWriter.write(String.format("%s\tref\n", refSeq));
					for (Pattern pattern : patternList) {
						bufferedWriter.write(
								String.format("%s\t%d\t%f\t%d\n", pattern.getPatternString(), pattern.getCount(),
										pattern.getCount() / sequenceCount, pattern.getPatternID()));
					}
					break;
				case METHYLATIONWITHSNP:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tMethylParent\n", METHYLATIONWITHSNP));
					bufferedWriter.write(String.format("%s\tref\n", refSeq));
					Collections.sort(patternList,
							(p1, p2) -> p1.getMethylationParentID() == p2.getMethylationParentID() ? Integer.compare(
									p1.getCount(), p2.getCount()) : Integer.compare(p1.getMethylationParentID(),
									p2.getMethylationParentID()));
					for (Pattern pattern : patternList) {
						bufferedWriter.write(
								String.format("%s\t%d\t%f\t%d\n", pattern.getPatternString(), pattern.getCount(),
										pattern.getCount() / sequenceCount, pattern.getMethylationParentID()));
					}
					break;
				default:
					throw new RuntimeException("unknown pattern type!");
			}
		}
	}

	private static void writeAnalysedSequences(String sequenceFile, List<Sequence> sequencesList) throws IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(sequenceFile))) {
			bufferedWriter.write(
					"start\tend\tmethylationString\tID\toriginalSequence\tBisulfiteConversionRate\tmethylationRate\tsequenceIdentity\n");
			for (Sequence seq : sequencesList) {
				bufferedWriter.write(seq.getStartPos() + "\t" + seq.getEndPos() + "\t" +
						seq.getMethylationString() + "\t" + seq.getId() + "\t" + seq.getOriginalSeq() + "\t" +
								seq.getBisulConversionRate() + "\t" + seq.getMethylationRate() + "\t" +
								seq.getSequenceIdentity() + "\n");
			}
		}
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
