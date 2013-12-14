package BSPAT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;

import DataType.Constant;
import DataType.Coordinate;
import DataType.CpGSite;
import DataType.Pattern;
import DataType.Sequence;
import DataType.SequenceComparatorMM;
import DataType.SequenceComparatorMethylation;
import DataType.SequenceComparatorMutations;
import DataType.SequenceComparatorRegion;

/**
 * Bisulfite sequences analysis. Include obtaining methylation string, mutation
 * string, methyl&mutation string. And group sequences by pattern.
 * 
 * 
 * @author Ke
 * 
 */
public class BSSeqAnalysis {

	private Report report;
	private Hashtable<String, String> referenceSeqs = new Hashtable<String, String>();
	private ArrayList<Sequence> sequencesList = new ArrayList<Sequence>();
	private Constant constant;
	private HashMap<String, Coordinate> coordinates;

	public String execute(String inputFolder, String outputFolder, String sampleName, Constant constant) throws Exception {
		String html = "";
		this.constant = constant;
		ImportBismarkResult importBismarkResult = new ImportBismarkResult(constant.originalRefPath, inputFolder);
		referenceSeqs = importBismarkResult.getReferenceSeqs();
		sequencesList = importBismarkResult.getSequencesList();
		if (sequencesList.size() == 0) {
			throw  new Exception("mapping result is empty, please double check input!");
		}

		// 0. retreive SNP info
		if (constant.coorReady == true) {
			coordinates = IO.readCoordinates(constant.coorFilePath, constant.coorFileName);
		}else {
			throw new Exception("coordinates file is not ready!");
		}
		// first sort seqs by region
		Collections.sort(sequencesList, new SequenceComparatorRegion());
		String region = "";
		ArrayList<ArrayList<Sequence>> sequenceGroups = new ArrayList<ArrayList<Sequence>>();
		ArrayList<Sequence> sequenceGroup = null;
		for (Sequence seq : sequencesList) {
			if (seq.getRegion().equals(region)) {
				// add to same group
				sequenceGroup.add(seq);
			} else {
				// new group
				if (sequenceGroup != null) {
					sequenceGroups.add(sequenceGroup);
				}
				sequenceGroup = new ArrayList<Sequence>();
				sequenceGroup.add(seq);
				region = seq.getRegion();
			}
		}
		if (sequenceGroup.size() != 0) {
			sequenceGroups.add(sequenceGroup);
		}

		// then generate report for each region
		for (ArrayList<Sequence> seqGroup : sequenceGroups) {
			if (seqGroup.size() < 10) {
				continue;
			}
			region = seqGroup.get(0).getRegion();

			ArrayList<Sequence> Fgroup = new ArrayList<Sequence>();
			ArrayList<Sequence> Rgroup = new ArrayList<Sequence>();
			for (Sequence sequence : seqGroup) {
				if (sequence.getFRstate().equals("F")) {
					Fgroup.add(sequence);
				} else if (sequence.getFRstate().equals("R")) {
					Rgroup.add(sequence);
				}
			}
			// remove mis-mapped sequences
			Fgroup = removeMisMap(Fgroup);
			Rgroup = removeMisMap(Rgroup);
			// check F R for each region, overlap or not
			Sequence Fseq = null, Rseq = null;
			if (Fgroup != null && Rgroup != null) {
				Fseq = Fgroup.get(0);
				Rseq = Rgroup.get(0);
			}
			if ((Fseq != null) && (Rseq != null) && (Fseq.getStartPos() == Rseq.getStartPos())
					&& (Fseq.getOriginalSeq().length() == Rseq.getOriginalSeq().length())) {
				// F R are totally overlapped combine F R group together
				ArrayList<Sequence> FRgroup = new ArrayList<Sequence>();
				FRgroup.addAll(Fgroup);
				FRgroup.addAll(Rgroup);
				int totalCount = FRgroup.size();
				ArrayList<Pattern> methylationPatterns = new ArrayList<Pattern>();
				ArrayList<Pattern> mutationPatterns = new ArrayList<Pattern>();

				getMethylString(FRgroup);
				FRgroup = filterSequences(FRgroup);
				getMethylPattern(FRgroup, methylationPatterns);
				getMutationPattern(FRgroup, mutationPatterns);

				ReportSummary reportSummary = new ReportSummary();
				System.out.println(region + " Report");
				report = new Report("FR", region, outputFolder, FRgroup, methylationPatterns, mutationPatterns, referenceSeqs, totalCount,
						reportSummary, constant);
				/** it is better to include those function in constructor. **/
				report.writeResult();
				report.writeStatistics();
				report.writeMethylationPatterns();
				report.writeMutationPatterns();
				System.out.println(region + " Draw figure");
				if (constant.coorReady == true) {
					System.out.println("start drawing -- BSSeqAnalysis -- execute");
					DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion, constant.toolsPath);
					drawFigureLocal.drawMethylPattern(region, outputFolder, sampleName, "FR",
							reportSummary, coordinates);
					drawFigureLocal.drawMethylPatternWithAllele(region, outputFolder, sampleName,
							"FR", reportSummary, coordinates);
				}
				System.out.println("finished DrawSingleFigure");
				reportSummary.replacePath(constant.diskRootPath.toString(), constant.webRootPath, constant.coorReady, constant.host);
				html += reportSummary.generateHTML(region, constant.coorReady, "FR");
			} else {
				// consider F and R seperately like two regions
				// F
				if (Fgroup != null) {
					int totalCount = Fgroup.size();
					ArrayList<Pattern> methylationPatterns = new ArrayList<Pattern>();
					ArrayList<Pattern> mutationPatterns = new ArrayList<Pattern>();

					getMethylString(Fgroup);
					Fgroup = filterSequences(Fgroup);
					getMethylPattern(Fgroup, methylationPatterns);
					getMutationPattern(Fgroup, mutationPatterns);

					ReportSummary reportSummary = new ReportSummary();
					report = new Report("F", region, outputFolder, Fgroup, methylationPatterns, mutationPatterns, referenceSeqs, totalCount,
							reportSummary, constant);
					/** it is better to include those function in constructor. **/
					report.writeResult();
					report.writeStatistics();
					report.writeMethylationPatterns();
					report.writeMutationPatterns();
					if (constant.coorReady == true) {
						DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion, constant.toolsPath);
						drawFigureLocal.drawMethylPattern(region, outputFolder, sampleName,
								"F", reportSummary, coordinates);
						drawFigureLocal.drawMethylPatternWithAllele(region, outputFolder,
								sampleName, "F", reportSummary, coordinates);
					}
					reportSummary.replacePath(constant.diskRootPath.toString(), constant.webRootPath, constant.coorReady, constant.host);
					html += reportSummary.generateHTML(region, constant.coorReady, "F");
				}
				// R
				if (Rgroup != null) {
					int totalCount = Rgroup.size();
					ArrayList<Pattern> methylationPatterns = new ArrayList<Pattern>();
					ArrayList<Pattern> mutationPatterns = new ArrayList<Pattern>();

					getMethylString(Rgroup);
					Rgroup = filterSequences(Rgroup);
					getMethylPattern(Rgroup, methylationPatterns);
					getMutationPattern(Rgroup, mutationPatterns);

					ReportSummary reportSummary = new ReportSummary();
					report = new Report("R", region, outputFolder, Rgroup, methylationPatterns, mutationPatterns, referenceSeqs, totalCount,
							reportSummary, constant);
					/** it is better to include those function in constructor. **/
					report.writeResult();
					report.writeStatistics();
					report.writeMethylationPatterns();
					report.writeMutationPatterns();
					if (constant.coorReady == true) {
						DrawPattern drawFigureLocal = new DrawPattern(constant.figureFormat, constant.refVersion, constant.toolsPath);
						drawFigureLocal.drawMethylPattern(region, outputFolder, sampleName,
								"R", reportSummary, coordinates);
						drawFigureLocal.drawMethylPatternWithAllele(region, outputFolder,
								sampleName, "R", reportSummary, coordinates);
					}
					reportSummary.replacePath(constant.diskRootPath.toString(), constant.webRootPath, constant.coorReady, constant.host);
					html += reportSummary.generateHTML(region, constant.coorReady, "R");
				}
			}

		}
		return html;
	}

	/**
	 * get methylation String & methylation string with mutations; calculate
	 * conversion rate, methylation rate
	 * 
	 */

	public void getMethylString(ArrayList<Sequence> seqList) {
		char[] methylationString;
		char[] methylationStringWithMutations;
		char[] mutationString;
		String originalSeq;
		double countofUnConvertedC;
		double countofMethylatedCpG;
		double unequalNucleotide;
		String convertedReferenceSeq;
		int countofCinRef;
		String referenceSeq = referenceSeqs.get(seqList.get(0).getRegion());

		for (Sequence seq : seqList) {
			// convert reference sequence and count C in non-CpG context.
			convertedReferenceSeq = "";
			countofCinRef = 0;// count C in non-CpG context.
			for (int i = 0; i < referenceSeq.length(); i++) {
				if (i != referenceSeq.length() - 1 && referenceSeq.charAt(i) == 'C' && referenceSeq.charAt(i + 1) != 'G') {//non CpG context
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
				methylationString[i] = '-';
				methylationStringWithMutations[i] = '-';
				mutationString[i] = '-';
			}
			originalSeq = seq.getOriginalSeq();
			for (int i = 0; i < originalSeq.length(); i++) {
				if (originalSeq.charAt(i) != convertedReferenceSeq.charAt(i + seq.getStartPos() - 1)) {
					if (i != originalSeq.length() - 1 && originalSeq.charAt(i) == 'C' && originalSeq.charAt(i + 1) != 'G') {//non CpG context
						countofUnConvertedC++;
					}
					unequalNucleotide++;
					methylationStringWithMutations[i + seq.getStartPos() - 1] = originalSeq.charAt(i); // with mutations
					mutationString[i + seq.getStartPos() - 1] = originalSeq.charAt(i);
				}
			}
			for (CpGSite cpg : seq.getCpGSites()) {
				if (cpg.getMethylLabel() == true) {
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
			seq.setSequenceIdentity(1 - (unequalNucleotide - countofMethylatedCpG) / (originalSeq.length() - seq.getCpGSites().size()));
			seq.setMethylationString(new String(methylationString));
			seq.setMethylationStringWithMutations(new String(methylationStringWithMutations));
			seq.setMutationString(new String(mutationString));
		}
	}

	private ArrayList<Sequence> filterSequences(ArrayList<Sequence> seqList) {
		// fill sequence list filtered by threshold.
		ArrayList<Sequence> tempSeqList = new ArrayList<Sequence>();
		for (Sequence seq : seqList) {
			// filter unqualified reads
			if (seq.getBisulConversionRate() >= constant.conversionRateThreshold && seq.getSequenceIdentity() >= constant.sequenceIdentityThreshold) {
				tempSeqList.add(seq);
			}
		}
		return tempSeqList;
	}

	/**
	 * get methylation pattern
	 */
	public void getMethylPattern(ArrayList<Sequence> seqList, ArrayList<Pattern> methylationPatterns) {
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
				if (!sequence.getMethylationStringWithMutations().equals(methylationPatternWithMutations.getPatternString())) {
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
	public void getMutationPattern(ArrayList<Sequence> seqList, ArrayList<Pattern> mutationPatterns) {
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
				if (!sequence.getMethylationStringWithMutations().equals(mutationpatternWithMethylation.getPatternString())) {
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

	private ArrayList<Sequence> removeMisMap(ArrayList<Sequence> seqList) {
		ArrayList<Sequence> newList = null;
		if (seqList.size() != 0) {
			// count sequence with same start position
			HashMap<Integer, Integer> positionMap = new HashMap<Integer, Integer>();
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
			newList = new ArrayList<Sequence>();
			for (Sequence sequence : seqList) {
				if (sequence.getStartPos() == maxKey) {
					newList.add(sequence);
				}
			}
		}
		return newList;
	}

}
