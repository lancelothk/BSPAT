package edu.cwru.cbc.BSPAT.DataType;

import java.util.ArrayList;
import java.util.List;

public class Sequence {
	private String id; // 1
	//    private String flag; // 2
	private String region; // 3
	private int startPos; // 4 offset from first bp of reference seq.
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
	private List<String> allelePosList;

	public Sequence(String id, String region, int startPos, String originalSeq) {
		this.id = id;
		this.region = region;
		this.startPos = startPos;
		this.originalSeq = originalSeq;
		this.CpGSites = new ArrayList<>();
		this.allelePosList = new ArrayList<>();
	}

	public int getEndPos() {
		return startPos + length() - 1;
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

	public List<String> getAlleleList() {
		return allelePosList;
	}

	public void addAllele(String allele) {
		this.allelePosList.add(allele);
	}

	public String getMeMuString() {
		return memuString;
	}

	public void setMeMuString(PotentialSNP snp) {
		char[] patternArray = methylationString.toCharArray();
		if (snp.getNucleotide() != '-') {
			patternArray[snp.getPosition()] = snp.getNucleotide();
		}
		this.memuString = new String(patternArray);
	}
}
