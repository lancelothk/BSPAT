package edu.cwru.cbc.BSPAT.commons;

import edu.cwru.cbc.BSPAT.MethylFigure.CpG;
import edu.cwru.cbc.BSPAT.MethylFigure.CpGStatistics;
import edu.cwru.cbc.BSPAT.MethylFigure.PatternResult;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

/**
 * Created by kehu on 4/8/16.
 * IO methods for reading input and writing output
 */
public class IOUtils {
	// TODO replace reference reading code with the one used in ASM project. Or replace with using htsjdk
	public static Map<String, String> readReference(String refPath) throws IOException {
		Map<String, String> referenceSeqs = new HashMap<>();
		File refPathFile = new File(refPath);
		if (refPathFile.isDirectory()) {
			File[] files = refPathFile.listFiles(new ExtensionFilter(new String[]{".txt", "fasta", "fa", "fna"}));
			for (File file : files) {
				referenceSeqs = readFastaFile(file);
			}
		} else {
			referenceSeqs = readFastaFile(refPathFile);
		}
		return referenceSeqs;
	}

	private static Map<String, String> readFastaFile(File file) throws IOException {
		Map<String, String> referenceSeqs = new HashMap<>();
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
		return referenceSeqs;
	}

	public static void writeASMPattern(String outputFileName, String targetRefSeq, PatternResult patternWithAllele,
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

	public static void writeStatistics(String reportFileName,
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

	public static void writePatterns(String patternFileName, String refSeq, List<Pattern> patternList,
	                                 String patternType, double sequenceCount) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(patternFileName))) {
			switch (patternType) {
				case Pattern.METHYLATION:
					bufferedWriter.write(String.format("%s\tcount\tpercentage\tPatternID\n", patternType));
					bufferedWriter.write(String.format("%s\tref\n", refSeq));
					for (Pattern pattern : patternList) {
						String origin = pattern.getPatternString();
						bufferedWriter.write(String.format("%s\t%d\t%f\t%d\n", origin, pattern.getCount(),
								pattern.getCount() / sequenceCount, pattern.getPatternID()));
					}
					break;
				case Pattern.METHYLATIONWITHSNP:
					bufferedWriter.write(
							String.format("%s\tcount\tpercentage\tMethylParent\n", Pattern.METHYLATIONWITHSNP));
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

	public static void writeAnalysedSequences(String sequenceFile, List<Sequence> sequencesList, int refLength,
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

	public static void readBismarkAlignmentResults(String inputFolder,
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
}
