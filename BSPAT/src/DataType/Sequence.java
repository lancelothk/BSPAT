package DataType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Sequence {
    private String id; // 1
//    private String flag; // 2
    private String region; // 3
    private int startPos; // 4
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
    private String mutationString;
    private double methylationRate;
    private double bisulConversionRate;
    private double sequenceIdentity;
    private String FRstate = "";
    private List<String> allelePosList;

    public Sequence(String id, String region, int startPos, String originalSeq) {
        this.id = id;
        this.region = region;
        this.startPos = startPos;
        this.originalSeq = originalSeq;
        this.CpGSites = new ArrayList<>();
        this.allelePosList = new ArrayList<>();
    }

    private static boolean isFirstBPCpGSite(int pos, List<CpGSite> cpGSiteList) {
        for (CpGSite cpGSite : cpGSiteList) {
            if (pos == cpGSite.getPosition()) {
                return true;
            }
        }
        return false;
    }

    /**
     * generate methylation and mutation String; calculate
     * conversion rate, methylation rate
     */
	public static void processSequence(String referenceSeq, final List<Sequence> seqList) {
		// convert reference sequence and count C in non-CpG context.
        String convertedReferenceSeq = "";
        // count C in non-CpG context.  Maybe not efficient enough since scan twice.
        int countOfNonCpGC = StringUtils.countMatches(referenceSeq, "C") - StringUtils.countMatches(referenceSeq, "CG");
        for (int i = 0; i < referenceSeq.length(); i++) {
            if (referenceSeq.charAt(i) == 'C' || referenceSeq.charAt(i) == 'c') {
                convertedReferenceSeq += 'T';
            } else {
                convertedReferenceSeq += referenceSeq.charAt(i);
            }
        }
        for (Sequence seq : seqList) {
            char[] methylationString;
            char[] mutationString;
            // fill read to reference length
            double countOfUnConvertedC = 0;
            double countOfMethylatedCpG = 0;
            double unequalNucleotide = 0;
            methylationString = new char[convertedReferenceSeq.length()];
            mutationString = new char[convertedReferenceSeq.length()];

            for (int i = 0; i < convertedReferenceSeq.length(); i++) {
                methylationString[i] = ' ';
                mutationString[i] = ' ';
            }
            for (int i = 0; i < seq.getOriginalSeq().length(); i++) {
                methylationString[i] = '-';
                mutationString[i] = '-';
            }
            for (int i = 0; i < seq.getOriginalSeq().length(); i++) {
                // meet unequal element
                if (seq.getOriginalSeq().charAt(i) != convertedReferenceSeq.charAt(i)) {
                    if (isFirstBPCpGSite(i, seq.getCpGSites())) {
                        if (!(seq.getOriginalSeq().charAt(i) == 'T' && convertedReferenceSeq.charAt(i) == 'C') &&
                                !(seq.getOriginalSeq().charAt(i) == 'C' && convertedReferenceSeq.charAt(i) == 'T')) {
                            unequalNucleotide++;
                            mutationString[i] = seq.getOriginalSeq().charAt(i);
                            seq.addAllele(String.format("%d-%s", i, seq.getOriginalSeq().charAt(i)));
                        }
                    } else {
                        if (seq.getOriginalSeq().charAt(i) == 'C' && referenceSeq.charAt(i) == 'C') {
                            countOfUnConvertedC++;
                        } else {
                            unequalNucleotide++;
                            mutationString[i] = seq.getOriginalSeq().charAt(i);
                            seq.addAllele(String.format("%d-%s", i, seq.getOriginalSeq().charAt(i)));
                        }
                    }
                }
            }
            for (CpGSite cpg : seq.getCpGSites()) {
                if (cpg.isMethylated()) {
                    countOfMethylatedCpG++;
                    // methylated CpG site represent by @@
                    methylationString[cpg.getPosition()] = '@';
                    if (cpg.getPosition() + 1 <= methylationString.length) {
                        methylationString[cpg.getPosition() + 1] = '@';
                    }
                    // mutation
                    mutationString[cpg.getPosition()] = '-';
                } else {
                    // un-methylated CpG site represent by **. Exclude mutation in CpG site.
                    if (cpg.getPosition() != mutationString.length) {
                        methylationString[cpg.getPosition()] = '*';
                        if (cpg.getPosition() + 1 <= methylationString.length) {
                            methylationString[cpg.getPosition() + 1] = '*';
                        }
                    }
                }
            }
            // fill sequence content including calculation fo bisulfite
            // conversion rate and methylation rate for each sequence.
            seq.setBisulConversionRate(1 - (countOfUnConvertedC / countOfNonCpGC));
            seq.setMethylationRate(countOfMethylatedCpG / seq.getCpGSites().size());
            seq.setSequenceIdentity(1 - unequalNucleotide / (seq.getOriginalSeq().length() - seq.getCpGSites().size()));
            seq.setMethylationString(StringInternPool.intern(new String(methylationString)));
            seq.setMutationString(StringInternPool.intern(new String(mutationString)));
        }
    }

    /**
     * reverse and complement Sequence
     *
     * @param str
     * @return
     */
    public String complementarySequence(String str) {
        StringBuffer strBuff = new StringBuffer(str);
        String result = "";
        //strBuff.reverse(); // for reverse sequence
        for (int i = 0; i < strBuff.length(); i++) {
            if (strBuff.charAt(i) == 'A') {
                result += 'T';
            } else if (strBuff.charAt(i) == 'T') {
                result += 'A';
            } else if (strBuff.charAt(i) == 'C') {
                result += 'G';
            } else if (strBuff.charAt(i) == 'G') {
                result += 'C';
            } else {
                result += strBuff.charAt(i);
            }
        }
        return result;
    }

    public int getEndPos() {
        return startPos + length() - 1;
    }

    public void setFRstate(String fRstate) {
        FRstate = fRstate;
    }

    public String getFRstate() {
        return FRstate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public void setOriginalSeq(String originalSeq) {
        this.originalSeq = originalSeq;
    }

    public void setMethylationString(String methylationString) {
        this.methylationString = methylationString;
    }

    public void setBisulConversionRate(double bisulConversionRate) {
        this.bisulConversionRate = bisulConversionRate;
    }

    public void setMethylationRate(double methylationRate) {
        this.methylationRate = methylationRate;
    }

    public void setSequenceIdentity(double sequenceIdentity) {
        this.sequenceIdentity = sequenceIdentity;
    }

    public void setMutationString(String mutationString) {
        this.mutationString = mutationString;
    }

    public String getMutationString() {
        return mutationString;
    }

    public double getSequenceIdentity() {
        return sequenceIdentity;
    }

    public double getMethylationRate() {
        return methylationRate;
    }

    public double getBisulConversionRate() {
        return bisulConversionRate;
    }

    public String getId() {
        return id;
    }

    public String getOriginalSeq() {
        return originalSeq;
    }

    public String getRegion() {
        return region;
    }

    public int getStartPos() {
        return startPos;
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
		// TODO if we can make sure MeMuString won't change, then we can cache it.
		StringBuilder meMuBuilder = new StringBuilder();
        for (int i = 0; i < originalSeq.length(); i++) {
            if (methylationString.charAt(i) != '-' && mutationString.charAt(i) != '-') {
                // if both methyl and mutation occurs, use mutation first
                meMuBuilder.append(mutationString.charAt(i));
            } else if (methylationString.charAt(i) != '-') {
                meMuBuilder.append(methylationString.charAt(i));
            } else if (mutationString.charAt(i) != '-') {
                meMuBuilder.append(mutationString.charAt(i));
            } else {
                meMuBuilder.append('-');
            }
        }
        return meMuBuilder.toString();
    }
}
