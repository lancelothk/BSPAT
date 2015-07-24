package edu.cwru.cbc.BSPAT.DataType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Sequence {
	private String id; // 1
	//    private String flag; // 2
	// 16 is reverse-complementary string(bottom)
	private String strand; // TOP same to ref, BOTTOM reverse-complementary to ref.
	private String region; // 3
	private int startPos; // 4 offset from first bp of reference seq. 0-based
	// 5 MAPQ
	// 6 CIGAR
	// 7 RNEXT
	// 8 PNEXT
	// 9 TLEN
	private String originalSeq; // 10
	//    private String qualityScore; // 11
	//    private String editDist; // 12 edit distance to the reference
	//    private String mismatchString; // 13 base-by-base mismatches to the reference, not including indels
	//    private String methylCall; // 14 methylation call string
	//    private String readConvState; // 15 read conversion state for the alignment
	//	private String XGtag; // 16 genome conversion state for the alignment
	private List<CpGSite> CpGSites;
	private String methylationString;
	private String memuString;
	private double methylationRate;
	private double bisulConversionRate;
	private double sequenceIdentity;
	private String refSeq;

	public Sequence(String id, String strand, String region, int startPos, String originalSeq) {
		this.id = id;
		this.strand = strand;
		this.region = region;
		this.startPos = startPos;
		this.originalSeq = originalSeq;
		this.CpGSites = new ArrayList<>();
	}

	public int getEndPos() {
		return startPos + length() - 1; // 0-based
	}

	public double getSequenceIdentity() {
		return sequenceIdentity;
	}

	public void setSequenceIdentity(double sequenceIdentity) {
		checkValueRange(sequenceIdentity);
		this.sequenceIdentity = sequenceIdentity;
	}

	public double getMethylationRate() {
		return methylationRate;
	}

	public void setMethylationRate(double methylationRate) {
		checkValueRange(methylationRate);
		this.methylationRate = methylationRate;
	}

	public double getBisulConversionRate() {
		return bisulConversionRate;
	}

	public void setBisulConversionRate(double bisulConversionRate) {
		checkValueRange(bisulConversionRate);
		this.bisulConversionRate = bisulConversionRate;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOriginalSeq() {
		return originalSeq;
	}

	public void setOriginalSeq(String originalSeq) {
		this.originalSeq = originalSeq;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public int getStartPos() {
		return startPos;
	}

	public void setStartPos(int startPos) {
		this.startPos = startPos;
	}

	public void addCpG(CpGSite cpg) {
		CpGSites.add(cpg);
	}

	public List<CpGSite> getCpGSites() {
		return CpGSites;
	}

	public String getMethylationString() {
		return methylationString;
	}

	public void setMethylationString(String methylationString) {
		this.methylationString = methylationString;
	}

	public int length() {
		return originalSeq.length();
	}

	public String getMeMuString() {
		return memuString;
	}

	public void setMeMuString(PotentialSNP snp, int targetStart, int targetEnd) {
		char[] patternArray = methylationString.toCharArray();
		if (snp.getNucleotide() != '-') {
			patternArray[snp.getPosition() - startPos] = snp.getNucleotide();
		}
		this.memuString = new String(patternArray).substring(targetStart - startPos, targetEnd - startPos + 1);
	}

	public String getStrand() {
		return strand;
	}

	public void setStrand(String strand) {
		this.strand = strand;
	}

	public boolean isInSeq(int pos) {
		return pos >= this.startPos && pos <= this.getEndPos();
	}

	/**
	 * generate methylation pattern, calculate conversion rate, methylation rate
	 */
	public void processSequence(String referenceSeq) {
		this.refSeq = referenceSeq.substring(startPos, this.getEndPos() + 1);
		StringBuilder convertedReferenceSeq = bisulfiteRefSeq();
		int countOfNonCpGC = StringUtils.countMatches(refSeq, "C") - CpGSites.size();
		double countOfMethylatedCpG = 0;
		for (CpGSite cpGSite : CpGSites) {
			if (cpGSite.isMethylated()) {
				countOfMethylatedCpG++;
			}
		}
		double countOfUnConvertedC = 0;
		double unequalNucleotide = 0;
		for (int i = 0; i < this.getOriginalSeq().length(); i++) {
			if (strand.equals("TOP")) {
				// meet unequal element
				if (this.getOriginalSeq().charAt(i) != convertedReferenceSeq.charAt(i)) {
					if (isCpGSite(i)) {
						if (!(this.getOriginalSeq().charAt(i) == 'T' && convertedReferenceSeq.charAt(i) == 'C') &&
								!(this.getOriginalSeq().charAt(i) == 'C' && convertedReferenceSeq.charAt(i) == 'T')) {
							unequalNucleotide++;
						}
					} else {
						if (this.getOriginalSeq().charAt(i) == 'C' && refSeq.charAt(i) == 'C') {
							countOfUnConvertedC++;
						} else {
							unequalNucleotide++;
						}
					}
				}
			} else {
				// meet unequal element
				if (this.getOriginalSeq().charAt(i) != convertedReferenceSeq.charAt(i)) {
					if (isCpGSite(i)) {
						if (!(this.getOriginalSeq().charAt(i) == 'G' && convertedReferenceSeq.charAt(i) == 'A') &&
								!(this.getOriginalSeq().charAt(i) == 'A' && convertedReferenceSeq.charAt(i) == 'G')) {
							unequalNucleotide++;
						}
					} else {
						if (this.getOriginalSeq().charAt(i) == 'G' && refSeq.charAt(i) == 'G') {
							countOfUnConvertedC++;
						} else {
							unequalNucleotide++;
						}
					}
				}
			}
		}
		// fill sequence content including calculation fo bisulfite
		// conversion rate and methylation rate for each sequence.
		this.setBisulConversionRate(1 - countOfUnConvertedC / countOfNonCpGC);
		this.setMethylationRate(countOfMethylatedCpG / CpGSites.size());
		this.setSequenceIdentity(
				1 - unequalNucleotide / (this.getOriginalSeq().length() - this.getCpGSites().size()));
		this.setMethylationString(generateMethylString());
	}

	private StringBuilder bisulfiteRefSeq() {
		// convert reference sequence and count C in non-CpG context.
		StringBuilder convertedReferenceSeq = new StringBuilder();
		for (int i = 0; i < this.length(); i++) {
			if (strand.equals("TOP") && refSeq.charAt(i) == 'C') {
				convertedReferenceSeq.append('T');
			} else if (strand.equals("BOTTOM") && refSeq.charAt(i) == 'G') {
				convertedReferenceSeq.append('A');
			} else {
				convertedReferenceSeq.append(refSeq.charAt(i));
			}
		}
		convertedReferenceSeq.trimToSize();
		return convertedReferenceSeq;
	}

	private String generateMethylString() {
		char[] methylationString = new char[this.length()];
		for (int i = 0; i < this.getOriginalSeq().length(); i++) {
			methylationString[i] = '-';
		}
		for (CpGSite cpg : this.getCpGSites()) {
			int cpgPos = cpg.getPosition();
			if (this.getStrand().equals("BOTTOM")) {
				cpgPos--;
			}
			if (cpg.isMethylated()) {
				// methylated CpG site represent by @@
				methylationString[cpgPos - this.getStartPos()] = '@';
				if (this.isInSeq(cpgPos + 1)) {
					methylationString[cpgPos - this.getStartPos() + 1] = '@';
				}
			} else {
				// un-methylated CpG site represent by **.
				methylationString[cpgPos - this.getStartPos()] = '*';
				if (this.isInSeq(cpgPos + 1)) {
					methylationString[cpgPos - this.getStartPos() + 1] = '*';
				}
			}
		}
		return new String(methylationString);
	}

	private void checkValueRange(double value) {
		if (value > 1 || value < 0) {
			throw new RuntimeException("invalid value out of range [0,1]");
		}
	}

	public boolean isCpGSite(int pos) {
		for (CpGSite cpGSite : this.CpGSites) {
			int cpgPos = cpGSite.getPosition();
			if (this.getStrand().equals("BOTTOM")) {
				cpgPos--;
			}
			if ((pos + startPos) == cpgPos) {
				return true;
			}
		}
		return false;
	}
}
