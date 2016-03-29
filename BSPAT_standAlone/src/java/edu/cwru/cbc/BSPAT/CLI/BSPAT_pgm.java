package edu.cwru.cbc.BSPAT.CLI;

import edu.cwru.cbc.BSPAT.MethylFigure.CpG;
import edu.cwru.cbc.BSPAT.MethylFigure.CpGSitePattern;
import edu.cwru.cbc.BSPAT.MethylFigure.CpGStatistics;
import edu.cwru.cbc.BSPAT.MethylFigure.PatternResult;
import edu.cwru.cbc.BSPAT.commons.*;
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
				bisulfiteConversionRate, sequenceIdentityThreshold, criticalValue, methylPatternThreshold,
				memuPatternThreshold, snpThreshold);
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
	                                     double methylPatternThreshold, double memuPatternThreshold,
	                                     double snpThreshold) throws
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
	                                                double methylPatternThreshold, double memuPatternThreshold,
	                                                double snpThreshold, BedInterval bedInterval,
	                                                List<Sequence> seqGroup, String refSeq) throws IOException {
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

		List<CpGStatistics> cpgStatList = writeStatistics(
				String.format("%s%s_bismark.analysis_report.txt", outputPath, bedInterval.toString()),
				sequencePassedQualityFilter, mismatchStat, targetStart, targetRefSeq, bisulfiteConversionRate,
				sequenceIdentityThreshold, criticalValue, memuPatternThreshold, memuPatternThreshold, snpThreshold,
				seqGroup.size());

		Pair<Pattern, Pattern> allelePatterns = getAllelePatterns(seqGroup, potentialSNP);
		Pattern allelePattern = allelePatterns.getLeft();
		Pattern nonAllelePattern = allelePatterns.getRight();
		if (allelePattern.getSequenceMap().size() != 0 && nonAllelePattern.getSequenceMap().size() != 0) {
			PatternResult patternWithAllele = patternToPatternResult(allelePattern, cpgStatList, seqGroup.size(),
					targetStart);
			PatternResult patternWithoutAllele = patternToPatternResult(nonAllelePattern, cpgStatList, seqGroup.size(),
					targetStart);
			writeASMReport(String.format("%s%s_bismark.analysis_ASMreport.txt", outputPath, bedInterval.toString()),
					targetRefSeq, patternWithAllele, patternWithoutAllele);
		}
	}

	private static void writeASMReport(String outputFileName, String targetRefSeq, PatternResult patternWithAllele,
	                                   PatternResult patternWithoutAllele) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFileName))) {
			targetRefSeq = " " + targetRefSeq + " ";// extend reference in display in case of half CpG at the end of reference
			bufferedWriter.write("ASM report\tcount\tpercentage\tAllele\n");
			bufferedWriter.write(targetRefSeq + "\tref\n");
			assembleAllelePattern(targetRefSeq, patternWithAllele, bufferedWriter);
			assembleAllelePattern(targetRefSeq, patternWithoutAllele, bufferedWriter);
			bufferedWriter.close();
		}
	}

	private static void assembleAllelePattern(String targetRefSeq, PatternResult patternWithAllele,
	                                          BufferedWriter bufferedWriter) throws
			IOException {
		StringBuilder withAlleleString = new StringBuilder(StringUtils.repeat("-", targetRefSeq.length()));
		withAlleleString.setCharAt(0, ' ');
		withAlleleString.setCharAt(withAlleleString.length() - 1, ' ');
		StringBuilder withAllelePercent = new StringBuilder(StringUtils.repeat(" ", targetRefSeq.length()));
		for (CpGSitePattern cpGSitePattern : patternWithAllele.getCpGList()) {
			int cpgPos = cpGSitePattern.getPosition();
			String percent = String.format("%2.0f", cpGSitePattern.getMethylLevel() * 100);
			withAlleleString.setCharAt(cpgPos - 1, '*');
			withAllelePercent.setCharAt(cpgPos - 1, percent.charAt(0));
			withAlleleString.setCharAt(cpgPos, '*');
			withAllelePercent.setCharAt(cpgPos, percent.charAt(1));
		}
		if (patternWithAllele.getSnp() != null) {
			withAlleleString.setCharAt(patternWithAllele.getSnp().getPosition() - 1,
					patternWithAllele.getSnp().getNucleotide());
		}
		withAlleleString.append("\t")
				.append(patternWithAllele.getCount())
				.append('(')
				.append(patternWithAllele.getPercent())
				.append(')')
				.append("\n");
		withAllelePercent.append("\n");
		bufferedWriter.write(withAlleleString.toString());
		bufferedWriter.write(withAllelePercent.toString());
	}

	private static PatternResult patternToPatternResult(Pattern pattern, List<CpGStatistics> cpGStatisticsList,
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

	private static List<CpGStatistics> writeStatistics(String reportFileName,
	                                                   List<Sequence> sequencePassedQualityFilter,
	                                                   int[][] mutationStat, int targetStart, String targetRefSeq,
	                                                   double bisulfiteConversionRate, double sequenceIdentityThreshold,
	                                                   double criticalValue, double methylPatternThreshold,
	                                                   double memuPatternThreshold, double snpThreshold,
	                                                   int sequenceNumber) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reportFileName))) {
			Hashtable<Integer, CpGStatistics> cpgStatHashTable = new Hashtable<>();
			// collect information for calculating methylation rate for each CpG site.
			for (Sequence seq : sequencePassedQualityFilter) {
				for (CpGSite cpg : seq.getCpGSites()) {
					if (!cpgStatHashTable.containsKey(cpg.getPosition())) {
						CpGStatistics cpgStat = new CpGStatistics(cpg.getPosition());
						cpgStat.allSitePlus();
						if (cpg.isMethylated()) {
							cpgStat.methylSitePlus();
						}
						cpgStatHashTable.put(cpg.getPosition(), cpgStat);
					} else {
						CpGStatistics cpgStat = cpgStatHashTable.get(cpg.getPosition());
						cpgStat.allSitePlus();
						if (cpg.isMethylated()) {
							cpgStat.methylSitePlus();
						}
					}
				}
			}

			List<CpGStatistics> cpgStatList = new ArrayList<>();
			for (CpGStatistics cpgStat : cpgStatHashTable.values()) {
				if (cpgStat.getPosition() == targetStart - 1) { // display half cpg in the beginning of pattern.
					cpgStatList.add(cpgStat);
				} else if (cpgStat.getPosition() >= targetStart && cpgStat.getPosition() <= targetStart + targetRefSeq.length() - 1) {
					cpgStatList.add(cpgStat);
				}
			}

			cpgStatList.sort(CpG::compareTo);
			bufferedWriter.write("target region start position:\t" + targetStart + "\n");
			bufferedWriter.write("target region length:\t" + targetRefSeq.length() + "\n");
			bufferedWriter.write("Bisulfite conversion rate threshold:\t" + bisulfiteConversionRate + "\n");
			bufferedWriter.write("Sequence identity threshold:\t" + sequenceIdentityThreshold + "\n");
			bufferedWriter.write("Critival value:\t" + criticalValue + "\n");
			bufferedWriter.write("Methylation pattern threshold:\t" + methylPatternThreshold + "\n");
			bufferedWriter.write("Memu pattern threshold:\t" + memuPatternThreshold + "\n");
			bufferedWriter.write("SNP threshold:\t" + snpThreshold + "\n");
			bufferedWriter.write("Sequences covers whole target region:\t" + sequenceNumber + "\n");
			bufferedWriter.write("Sequences passed quality filtering:\t" + sequencePassedQualityFilter.size() + "\n");
			bufferedWriter.write("methylation rate for each CpG site:\n");
			bufferedWriter.write("pos\trate" + "\n");

			for (CpGStatistics cpgStat : cpgStatList) {
				cpgStat.calcMethylLevel();
				bufferedWriter.write(cpgStat.getPosition() + "\t" + cpgStat.getMethylLevel() + "\n");
			}

			bufferedWriter.write("mismatch stat:\n");
			bufferedWriter.write(String.format("index\tref\tA\tC\tG\tT\tN\ttotal\tcoverage\n"));
			for (int i = 0; i < mutationStat.length; i++) {
				// 1 based position
				int total = mutationStat[i][0] + mutationStat[i][1] + mutationStat[i][2] + mutationStat[i][3] +
						mutationStat[i][4];
				int coverage = mutationStat[i][0] + mutationStat[i][1] + mutationStat[i][2] + mutationStat[i][3] +
						mutationStat[i][4] + mutationStat[i][5];
				bufferedWriter.write(
						String.format("%d\t%c\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n", i + 1, targetRefSeq.charAt(i),
								mutationStat[i][0], mutationStat[i][1], mutationStat[i][2], mutationStat[i][3],
								mutationStat[i][4], total, coverage));
			}
			return cpgStatList;
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
