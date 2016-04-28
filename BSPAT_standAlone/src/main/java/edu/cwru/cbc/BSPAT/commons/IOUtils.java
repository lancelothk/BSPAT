package edu.cwru.cbc.BSPAT.commons;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by kehu on 4/8/16.
 * IO methods for reading input and writing output
 */
public class IOUtils {
	public static final String refExtension = "";

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

	public static void writeFastaFile(Map<String, String> referenceSeqs, File outputFile) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
		for (Map.Entry<String, String> entry : referenceSeqs.entrySet()) {
			bufferedWriter.write(">" + entry.getKey() + "\n");
			bufferedWriter.write(entry.getValue() + "\n");
		}
		bufferedWriter.close();

	}

	public static void writeASMPattern(String outputFileName, String targetRefSeq, PatternResult patternWithAllele,
	                                   PatternResult patternWithoutAllele) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFileName))) {
			bufferedWriter.write("ASM\tcount\tpercentage\n");
			bufferedWriter.write(targetRefSeq + "\tref\n");
			bufferedWriter.write(assembleAllelePattern(targetRefSeq, patternWithoutAllele, '*') + "\n");
			bufferedWriter.write(assembleAllelePattern(targetRefSeq, patternWithAllele, '@') + "\n");
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


	private static String assembleAllelePattern(String targetRefSeq, PatternResult patternWithAllele,
	                                            char cpgLabel) throws
			IOException {
		StringBuilder withAlleleString = new StringBuilder(StringUtils.repeat("-", targetRefSeq.length()));
		for (CpGStatistics cpGSitePattern : patternWithAllele.getCpGList()) {
			int cpgPos = cpGSitePattern.getPosition();
			if (cpgPos >= 0) { // check for half CpG
				withAlleleString.setCharAt(cpgPos, cpgLabel);
			}
			if (cpgPos + 1 < withAlleleString.length()) {
				withAlleleString.setCharAt(cpgPos + 1, cpgLabel);
			}
		}
		// check for half CpG
		if (patternWithAllele.getSnp() != null && patternWithAllele.getSnp()
				.getPosition() >= 0 && patternWithAllele.getSnp().getPosition() < withAlleleString.length()) {
			withAlleleString.setCharAt(patternWithAllele.getSnp().getPosition(),
					patternWithAllele.getSnp().getNucleotide());
		}
		withAlleleString.append("\t")
				.append(patternWithAllele.getCount())
				.append("\t")
				.append(patternWithAllele.getPercent());
		return withAlleleString.toString();
	}

	public static void writeReport(String reportFileName,
	                               List<Sequence> sequencePassedQualityFilter,
	                               int[][] mutationStat, int targetStart, String targetRefSeq,
	                               double bisulfiteConversionRate,
	                               double sequenceIdentityThreshold,
	                               double criticalValue, double methylPatternThreshold,
	                               double memuPatternThreshold, double snpThreshold,
	                               int sequenceNumber, List<CpGStatistics> cpgStatList) throws
			IOException {
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reportFileName))) {
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

	/**
	 * read sequences into bed regions they covered.
	 */
	private static void readBismarkAlignmentResults(Map<String, List<BedInterval>> targetRegionMap,
	                                                File bismarkResultFile) {
		final SamReader reader = SamReaderFactory.makeDefault().open(bismarkResultFile);
		for (final SAMRecord samRecord : reader) {
			// TODO use indexed bam to speed up query
			List<BedInterval> targetList = targetRegionMap.get(samRecord.getReferenceName());
			if (targetList != null) {
				for (BedInterval targetRegion : targetList) {
					// 0-based start. Same to all CpG site positions.
					int startPos = samRecord.getStart() - 1 - refExtension.length();
					// 0-based end. Also subtract extended reference length
					int endPos = startPos + samRecord.getReadLength() - 1 - refExtension.length();
					if (startPos <= targetRegion.getStart() && endPos >= targetRegion.getEnd()) {
						Sequence seq = new Sequence(samRecord.getReadName(),
								(samRecord.getFlags() & 0x10) == 0x10 ? "BOTTOM" : "TOP", samRecord.getReferenceName(),
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

	public static Map<String, List<BedInterval>> readBedFile(String bedFile) throws IOException {
		return Files.asCharSource(new File(bedFile), Charsets.UTF_8)
				.readLines(new LineProcessor<Map<String, List<BedInterval>>>() {
					private final Splitter tabSplitter = Splitter.on("\t");
					private Map<String, List<BedInterval>> bedIntervalMap = new HashMap<>();

					@Override
					public boolean processLine(String line) throws IOException {
						List<String> itemList = tabSplitter.splitToList(line);
						if (itemList.size() == 5) {
							boolean isMinusStrand;
							switch (itemList.get(4)) {
								case "+":
									isMinusStrand = false;
									break;
								case "-":
									isMinusStrand = true;
									break;
								default:
									throw new RuntimeException(
											"invalid strand symbol in target region file: " + itemList.get(4));
							}
							//  require bed file position 0-based.
							BedInterval bedInterval = new BedInterval(itemList.get(0),
									Integer.parseInt(itemList.get(1)), Integer.parseInt(itemList.get(2)),
									itemList.get(3), isMinusStrand);
							List<BedInterval> bedIntervalList = bedIntervalMap.get(itemList.get(0));
							if (bedIntervalList == null) {
								bedIntervalList = new ArrayList<>();
								bedIntervalList.add(bedInterval);
								bedIntervalMap.put(itemList.get(0), bedIntervalList);
							} else {
								bedIntervalList.add(bedInterval);
							}
							return true;
						} else {
							throw new RuntimeException(
									"in valid target region file! Should contain 5 columns: <Ref_name> <start_position>   <end _position>    <region_name>   <stand(+/-)>");
						}
					}

					@Override
					public Map<String, List<BedInterval>> getResult() {
						return bedIntervalMap;
					}
				});
	}

	public static void extendReference(String referencePath, File modifiedRefFile) throws IOException {
		Map<String, String> referenceMap = readReference(referencePath);
		for (Map.Entry<String, String> entry : referenceMap.entrySet()) {
			entry.setValue(refExtension + entry.getValue() + refExtension);
		}
		writeFastaFile(referenceMap, modifiedRefFile);
	}

	public static void writeLDReport(String LDreportFileName, List<CpGStatistics> cpgStatList, List<Sequence> sequencePassedQualityFilter) throws
			IOException {
		BufferedWriter LDreportWriter = new BufferedWriter(new FileWriter(LDreportFileName));
		DecimalFormat formatter = new DecimalFormat("0.000000");
		// write col names
		LDreportWriter.write("LDreport\t");
		for (CpGStatistics cpGStatistics : cpgStatList) {
			if (cpGStatistics != cpgStatList.get(cpgStatList.size() - 1)) {
				LDreportWriter.write(String.format("%8d\t", cpGStatistics.getPosition()));
			} else {
				LDreportWriter.write(String.format("%8d\n", cpGStatistics.getPosition()));
			}
		}
		for (int i = 0; i < cpgStatList.size(); i++) {
			LDreportWriter.write(
					String.format("%8d\t", cpgStatList.get(i).getPosition()) + StringUtils.repeat("        \t", i + 1));
			for (int j = i + 1; j < cpgStatList.size(); j++) {
				LDreportWriter.write(formatter.format(
						calcLDofCpGPair(cpgStatList.get(i), cpgStatList.get(j), sequencePassedQualityFilter)) + "\t");
			}
			LDreportWriter.write("\n");
		}
		LDreportWriter.close();
	}

	private static double calcLDofCpGPair(CpGStatistics cpgA, CpGStatistics cpgB, List<Sequence> sequencePassedQualityFilter) {
		double mAmB = 0, mAnB = 0, nAmB = 0, nAnB = 0;
		for (Sequence sequence : sequencePassedQualityFilter) {
			switch (sequence.checkCpGPair(cpgA.getPosition(), cpgB.getPosition())) {
				case mAmB:
					mAmB++;
					break;
				case mAnB:
					mAnB++;
					break;
				case nAmB:
					nAmB++;
					break;
				case nAnB:
					nAnB++;
					break;
				case notCoverBoth:
					break;
				default:
					throw new RuntimeException("unknown type of CpGPairStatus");
			}
		}
		double total = mAmB + mAnB + nAmB + nAnB;
		return calcRSquare((mAmB + mAnB) / total, (mAmB + nAmB) / total, mAmB / total);
	}

	private static double calcRSquare(double pa, double pb, double pab) {
		double one = 1 - 1e-16;
		double zero = Double.MIN_VALUE;
		pa = Double.max(zero, pa);
		pa = Double.min(one, pa);
		pb = Double.max(zero, pb);
		pb = Double.min(one, pb);
		pab = Double.max(zero, pab);
		pab = Double.min(one, pab);
		double d = (pab - pa * pb);
		double b = (pa * (1 - pa) * pb * (1 - pb));
		double rsquare = Math.pow((pab - pa * pb), 2) / (pa * (1 - pa) * pb * (1 - pb));
		return rsquare;
	}
}
