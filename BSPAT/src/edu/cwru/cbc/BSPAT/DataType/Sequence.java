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
		this.sequenceIdentity = sequenceIdentity;
	}

	public double getMethylationRate() {
		return methylationRate;
	}

	public void setMethylationRate(double methylationRate) {
		this.methylationRate = methylationRate;
	}

	public double getBisulConversionRate() {
		return bisulConversionRate;
	}

	public void setBisulConversionRate(double bisulConversionRate) {
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
		String currRefSeq = referenceSeq.substring(startPos, this.getEndPos() + 1);
		// convert reference sequence and count C in non-CpG context.
		StringBuilder convertedReferenceSeq = new StringBuilder();
		// count C in non-CpG context.  Maybe not efficient enough since scan twice.
		int countOfNonCpGC = StringUtils.countMatches(currRefSeq, "C") - StringUtils.countMatches(currRefSeq, "CG");
		for (int i = 0; i < this.length(); i++) {
			if (currRefSeq.charAt(i) == 'C' || currRefSeq.charAt(i) == 'c') {
				convertedReferenceSeq.append('T');
			} else {
				convertedReferenceSeq.append(currRefSeq.charAt(i));
			}
		}
		convertedReferenceSeq.trimToSize();
		char[] methylationString = new char[this.length()];
		// fill read to reference length
		double countOfUnConvertedC = 0;
		double countOfMethylatedCpG = 0;
		double unequalNucleotide = 0;
		double countOfCpGSite = 0;

		for (int i = 0; i < this.getOriginalSeq().length(); i++) {
			methylationString[i] = '-';
		}
		for (int i = 0; i < this.getOriginalSeq().length(); i++) {
			// meet unequal element
			if (this.getOriginalSeq().charAt(i) != convertedReferenceSeq.charAt(i)) {
				if (isFirstBPCpGSite(i)) {
					if (!(this.getOriginalSeq().charAt(i) == 'T' && convertedReferenceSeq.charAt(i) == 'C') &&
							!(this.getOriginalSeq().charAt(i) == 'C' && convertedReferenceSeq.charAt(i) == 'T')) {
						unequalNucleotide++;
					}
				} else {
					if (this.getOriginalSeq().charAt(i) == 'C' && currRefSeq.charAt(i) == 'C') {
						countOfUnConvertedC++;
					} else {
						unequalNucleotide++;
					}
				}
			}
		}
		for (CpGSite cpg : this.getCpGSites()) {
			int pos = cpg.getPosition();
			if (this.getStrand().equals("BOTTOM")) {
				pos--;
			}
			if (this.isInSeq(pos)) {
				countOfCpGSite++;
				if (cpg.isMethylated()) {
					countOfMethylatedCpG++;
					// methylated CpG site represent by @@
					methylationString[pos - this.getStartPos()] = '@';
					if (this.isInSeq(pos + 1)) {
						methylationString[pos - this.getStartPos() + 1] = '@';
					}
				} else {
					// un-methylated CpG site represent by **.
					methylationString[pos - this.getStartPos()] = '*';
					if (this.isInSeq(pos + 1)) {
						methylationString[pos - this.getStartPos() + 1] = '*';
					}
				}
			}
		}
		// fill sequence content including calculation fo bisulfite
		// conversion rate and methylation rate for each sequence.
		this.setBisulConversionRate(1 - (countOfUnConvertedC - countOfCpGSite) / countOfNonCpGC);
		this.setMethylationRate(countOfMethylatedCpG / this.getCpGSites().size());
		this.setSequenceIdentity(
				1 - unequalNucleotide / (this.getOriginalSeq().length() - this.getCpGSites().size()));
		this.setMethylationString(new String(methylationString));
	}

	public boolean isFirstBPCpGSite(int pos) {
		for (CpGSite cpGSite : this.CpGSites) {
			if (pos == cpGSite.getPosition()) {
				return true;
			}
		}
		return false;
	}
}
