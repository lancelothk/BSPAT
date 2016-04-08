package edu.cwru.cbc.BSPAT.CLI;

import edu.cwru.cbc.BSPAT.MethylFigure.CpG;
import edu.cwru.cbc.BSPAT.MethylFigure.CpGStatistics;
import edu.cwru.cbc.BSPAT.MethylFigure.PatternResult;
import edu.cwru.cbc.BSPAT.commons.*;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.SequenceUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.*;
import java.util.*;

/**
 * Created by lancelothk on 2/9/16.
 * Command line entry of BSPAT.
 */
@SuppressWarnings("Duplicates")
public class BSPAT_pgm {

	private static final String METHYLATION = "Methylation";
	private static final String METHYLATIONWITHSNP = "MethylationWithSNP";
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

		generatePatterns(referencePath, bismarkResultPath, outputPath, Utils.readBedFile(targetRegionFile),
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
		Map<String, String> referenceMap = readReference(referencePath);
		// reference names of target regions should be subset of reference names in reference file.
		for (String name : targetRegionMap.keySet()) {
			if (!referenceMap.keySet().contains(name)) {
				throw new RuntimeException(
						name + " in target regions is not a valid reference name in reference file!");
			}
		}
		readBismarkAlignmentResults(bismarkResultPath, targetRegionMap);
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
		if (targetRegion.isMinusStrand()) {
			targetRegion.reverse(refSeq.length());
			refSeq = SequenceUtil.reverseComplement(refSeq);
			for (Sequence sequence : targetRegion.getSequenceList()) {
				sequence.reverse(refSeq.length());
			}
		}
		String targetRefSeq = refSeq.substring(targetRegion.getStart(), targetRegion.getEnd() + 1);
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
		writeAnalysedSequences(outputPath + "/" + targetRegion.toString() + "_bismark.analysis.txt",
				sequencePassedQualityFilter, refSeq.length(), targetRegion.isMinusStrand());

		// generate methyl pattern output
		List<Pattern> methylationPatternList = getMethylPattern(sequencePassedQualityFilter, targetRegion.getStart(),
				targetRegion.getEnd());

		methylationPatternList = filterMethylationPatterns(methylationPatternList,
				sequencePassedQualityFilter.size(), StringUtils.countMatches(targetRefSeq, "CG"), criticalValue,
				methylPatternThreshold);

		// sort pattern and assign id index
		methylationPatternList.sort((p1, p2) -> Integer.compare(p2.getCount(), p1.getCount()));
		for (int i = 0; i < methylationPatternList.size(); i++) {
			methylationPatternList.get(i).assignPatternID(i);
		}

		writePatterns(String.format("%s/%s_bismark.analysis_%s.txt", outputPath, targetRegion.toString(), METHYLATION),
				targetRefSeq, methylationPatternList, METHYLATION, sequencePassedQualityFilter.size());

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
			writePatterns(String.format("%s/%s_bismark.analysis_%s.txt", outputPath, targetRegion.toString(),
					METHYLATIONWITHSNP),
					targetRefSeq, meMuPatternList, METHYLATIONWITHSNP, sequencePassedQualityFilter.size());

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
					writeASMPattern(
							String.format("%s/%s_bismark.analysis_ASM.txt", outputPath, targetRegion.toString()),
							targetRefSeq, patternWithAllele, patternWithoutAllele);
				}
			}
		}
		writeStatistics(String.format("%s/%s_bismark.analysis_report.txt", outputPath, targetRegion.toString()),
				sequencePassedQualityFilter, mismatchStat, targetRegion.getStart(), targetRefSeq,
				bisulfiteConversionRate, sequenceIdentityThreshold, criticalValue, memuPatternThreshold,
				memuPatternThreshold, snpThreshold, seqGroup.size());
	}

	private static void readBismarkAlignmentResults(String inputFolder,
	                                                Map<String, List<BedInterval>> targetRegionMap) throws
			IOException {
		File inputFile = new File(inputFolder);
		if (inputFile.isDirectory()) {
			File[] inputFiles = inputFile.listFiles(new ExtensionFilter(new String[]{"_bismark.sam", "_bismark.bam"}));
			for (File file : inputFiles) {
				readBismarkAlignmentResults(targetRegionMap, file);
			}
		} else {
			readBismarkAlignmentResults(targetRegionMap, inputFile);
		}
	}

	private static void readBismarkAlignmentResults(Map<String, List<BedInterval>> targetRegionMap,
	                                                File bismarkBamFile) {
		final SamReader reader = SamReaderFactory.makeDefault().open(bismarkBamFile);
		for (final SAMRecord samRecord : reader) {
			// TODO use indexed bam to speed up query
			List<BedInterval> targetList = targetRegionMap.get(samRecord.getReferenceName());
			if (targetList != null) {
				for (BedInterval targetRegion : targetList) {
					int startPos = samRecord.getStart() - 1;// 0-based start. Same to all CpG site positions.
					int endPos = startPos + samRecord.getReadLength() - 1; // 0-based end
					if (startPos <= targetRegion.getStart() && endPos >= targetRegion.getEnd()) {
						Sequence seq = new Sequence(samRecord.getReadName(),
								(samRecord.getFlags() & 0x10) == 0x10 ? "BOTTOM" : "TOP",
								samRecord.getReferenceName(),
								startPos, samRecord.getReadString());
						String methylString = samRecord.getStringAttribute("XM");
						for (int i = 0; i < methylString.length(); i++) {
							switch (methylString.charAt(i)) {
								case 'Z':
									CpGSite cpg = new CpGSite((seq.getStrand().equals("BOTTOM") ? i - 1 : i) + startPos,
											true);
									seq.addCpG(cpg);
									break;
								case 'z':
									cpg = new CpGSite((seq.getStrand().equals("BOTTOM") ? i - 1 : i) + startPos, false);
									seq.addCpG(cpg);
									break;
								default:
							}
						}
						targetRegion.getSequenceList().add(seq);
					}
				}
			}
		}
	}

	// TODO replace reference reading code with the one used in ASM project. Or replace with using htsjdk
	private static Map<String, String> readReference(String refPath) throws IOException {
		Map<String, String> referenceSeqs = new HashMap<>();
		File refPathFile = new File(refPath);
		if (refPathFile.isDirectory()) {
			File[] files = refPathFile.listFiles(new ExtensionFilter(new String[]{".txt", "fasta", "fa", "fna"}));
			for (File file : files) {
				readFastaFile(referenceSeqs, file);
			}
		} else {
			readFastaFile(referenceSeqs, refPathFile);
		}
		return referenceSeqs;
	}

	private static void readFastaFile(Map<String, String> referenceSeqs, File file) throws IOException {
		try (BufferedReader buffReader = new BufferedReader(new FileReader(file))) {
			String line, name = null;
			StringBuilder ref = new StringBuilder();
			while ((line = buffReader.readLine()) != null) {
				if (line.length() != 0 && line.charAt(0) == '>') {
					if (ref.length() > 0) {
						referenceSeqs.put(name, ref.toString().toUpperCase());
						ref = new StringBuilder();
					}
					name = line.substring(1, line.length());
				} else {
					ref.append(line);
				}
			}
			if (ref.length() > 0) {
				referenceSeqs.put(name, ref.toString().toUpperCase());
			}
		}
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

	private static void writeASMPattern(String outputFileName, String targetRefSeq, PatternResult patternWithAllele,
	                                    PatternResult patternWithoutAllele) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFileName))) {
			bufferedWriter.write("ASM\tcount\tpercentage\n");
			bufferedWriter.write(targetRefSeq + "\tref\n");
			assembleAllelePattern(targetRefSeq, patternWithoutAllele, bufferedWriter, '*');
			assembleAllelePattern(targetRefSeq, patternWithAllele, bufferedWriter, '@');
			patternWithoutAllele.getCpGList().sort(CpG::compareTo);
			patternWithAllele.getCpGList().sort(CpG::compareTo);
			bufferedWriter.write("Methylation level in reads with reference allele:\n");
			bufferedWriter.write("Relative_CpG_position\tMethyl_Level" + "\n");
			for (CpGStatistics cpgStat : patternWithoutAllele.getCpGList()) {
				cpgStat.calcMethylLevel();
				bufferedWriter.write(cpgStat.getPosition() + "\t" + cpgStat.getMethylLevel() + "\n");
			}
			bufferedWriter.write("Methylation level in reads with alternative allele:\n");
			bufferedWriter.write("Relative_CpG_position\tMethyl_Level" + "\n");
			for (CpGStatistics cpgStat : patternWithAllele.getCpGList()) {
				cpgStat.calcMethylLevel();
				bufferedWriter.write(cpgStat.getPosition() + "\t" + cpgStat.getMethylLevel() + "\n");
			}
			bufferedWriter.close();
		}
	}

	private static void assembleAllelePattern(String targetRefSeq, PatternResult patternWithAllele,
	                                          BufferedWriter bufferedWriter, char cpgLabel) throws
			IOException {
		StringBuilder withAlleleString = new StringBuilder(StringUtils.repeat("-", targetRefSeq.length()));
		for (CpGStatistics cpGSitePattern : patternWithAllele.getCpGList()) {
			int cpgPos = cpGSitePattern.getPosition();
			withAlleleString.setCharAt(cpgPos, cpgLabel);
			withAlleleString.setCharAt(cpgPos + 1, cpgLabel);
		}
		if (patternWithAllele.getSnp() != null) {
			withAlleleString.setCharAt(patternWithAllele.getSnp().getPosition(),
					patternWithAllele.getSnp().getNucleotide());
		}
		withAlleleString.append("\t")
				.append(patternWithAllele.getCount())
				.append("\t")
				.append(patternWithAllele.getPercent())
				.append("\n");
		bufferedWriter.write(withAlleleString.toString());
	}

	private static PatternResult patternToPatternResult(Pattern pattern, int totalCount, int targetStart,
	                                                    int targetEnd) {
		PatternResult patternResult = new PatternResult();
		Map<Integer, CpGStatistics> cpGSiteMap = new HashMap<>();
		for (Sequence sequence : pattern.getSequenceMap().values()) {
			for (CpGSite cpGSite : sequence.getCpGSites()) {
				int pos = cpGSite.getPosition();
				if (pos >= targetStart - 1 && pos <= targetEnd + 1) {
					if (cpGSiteMap.containsKey(pos)) {
						if (cpGSite.isMethylated()) {
							cpGSiteMap.get(pos).methylSitePlus();
							cpGSiteMap.get(pos).allSitePlus();
						} else {
							cpGSiteMap.get(pos).allSitePlus();
						}
					} else {
						cpGSiteMap.put(pos, new CpGStatistics(pos, false));
						if (cpGSite.isMethylated()) {
							cpGSiteMap.get(pos).methylSitePlus();
							cpGSiteMap.get(pos).allSitePlus();
						} else {
							cpGSiteMap.get(pos).allSitePlus();
						}
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

	private static void writeStatistics(String reportFileName,
	                                    List<Sequence> sequencePassedQualityFilter,
	                                    int[][] mutationStat, int targetStart, String targetRefSeq,
	                                    double bisulfiteConversionRate,
	                                    double sequenceIdentityThreshold,
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
						CpGStatistics cpgStat = new CpGStatistics(cpg.getPosition(), false);
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
			bufferedWriter.write(
					"target region start position:\t" + targetStart + "\n");
			bufferedWriter.write("target region length:\t" + targetRefSeq.length() + "\n");
			bufferedWriter.write("Bisulfite conversion rate threshold:\t" + bisulfiteConversionRate + "\n");
			bufferedWriter.write("Sequence identity threshold:\t" + sequenceIdentityThreshold + "\n");
			bufferedWriter.write("Critival value:\t" + criticalValue + "\n");
			bufferedWriter.write("Methylation pattern threshold:\t" + methylPatternThreshold + "\n");
			bufferedWriter.write("Memu pattern threshold:\t" + memuPatternThreshold + "\n");
			bufferedWriter.write("SNP threshold:\t" + snpThreshold + "\n");
			bufferedWriter.write("Sequences covers whole target region:\t" + sequenceNumber + "\n");
			bufferedWriter.write("Sequences passed quality filtering:\t" + sequencePassedQualityFilter.size() + "\n");
			bufferedWriter.write("Methylation level in target region:\n");
			bufferedWriter.write("Absolute_CpG_position\tMethyl_Level" + "\n");

			for (CpGStatistics cpgStat : cpgStatList) {
				cpgStat.calcMethylLevel();
				bufferedWriter.write(cpgStat.getPosition() + "\t" + cpgStat.getMethylLevel() + "\n");
			}

			bufferedWriter.write("mismatch stat:\n");
			bufferedWriter.write("index\tref\tA\tC\tG\tT\tN\ttotal\tcoverage\n");
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

	private static void writePatterns(String patternFileName, String refSeq, List<Pattern> patternList,
	                                  String patternType, double sequenceCount) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(patternFileName))) {
			switch (patternType) {
				case METHYLATION:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tPatternID\n", patternType));
					bufferedWriter.write(String.format("%s\tref\n", refSeq));
					for (Pattern pattern : patternList) {
						String origin = pattern.getPatternString();
						bufferedWriter.write(String.format("%s\t%d\t%f\t%d\n", origin, pattern.getCount(),
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
						String origin = pattern.getPatternString();
						bufferedWriter.write(String.format("%s\t%d\t%f\t%d\n", origin, pattern.getCount(),
								pattern.getCount() / sequenceCount, pattern.getMethylationParentID()));
					}
					break;
				default:
					throw new RuntimeException("unknown pattern type!");
			}
		}
	}

	private static void writeAnalysedSequences(String sequenceFile, List<Sequence> sequencesList, int refLength,
	                                           boolean reverse) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(sequenceFile))) {
			bufferedWriter.write(
					"start\tend\tmethylationString\tID\toriginalSequence\tBisulfiteConversionRate\tmethylationRate\tsequenceIdentity\n");
			for (Sequence seq : sequencesList) {
				int start = seq.getStartPos();
				int end = seq.getEndPos();
				String originalString = seq.getOriginalSeq();
				String methylString = seq.getMethylationString();
				if (reverse) {
					start = refLength - seq.getEndPos();
					end = refLength - seq.getStartPos();
					methylString = StringUtils.reverse(seq.getMethylationString());
					originalString = StringUtils.reverse(seq.getOriginalSeq());
				}
				bufferedWriter.write(
						start + "\t" + end + "\t" + methylString + "\t" + seq.getId() + "\t" + originalString + "\t" + seq
								.getBisulConversionRate() + "\t" + seq.getMethylationRate() + "\t" + seq.getSequenceIdentity() + "\n");
			}
		}
		System.out.println("wrote file " + sequenceFile);
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
