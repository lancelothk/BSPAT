package DataType;

import java.util.ArrayList;
import java.util.List;

public class Sequence {
    private String id; // 1
    private String flag; // 2
    private String region; // 3
    private int startPos; // 4
    // 5 MAPQ
    // 6 CIGAR
    // 7 RNEXT
    // 8 PNEXT
    // 9 TLEN
    private String originalSeq; // 10
    private String qualityScore; // 11
    private String editDist; // 12 edit distance to the reference
    private String mismatchString; // 13 base-by-base mismatches to the reference, not including indels
    private String methylCall; // 14 methylation call string
    private String readConvState; // 15 read conversion state for the alignment
    //	private String XGtag; // 16 genome conversion state for the alignment
    private List<CpGSite> CpGSites;
    private String methylationString;
    private String mutationString;
    private double methylationRate;
    private double bisulConversionRate;
    private double sequenceIdentity;
    private String FRstate = "";
    private List<String> allelePosList;

    public Sequence(String id, String flag, String region, int startPos, String originalSeq, String qualityScore,
                    String editDist, String mismatchString, String methylCall, String readConvState) {
        this.id = id;
        this.flag = flag;
        this.region = region;
        this.startPos = startPos;
        this.originalSeq = originalSeq;
        this.qualityScore = qualityScore;
        this.editDist = editDist;
        this.mismatchString = mismatchString;
        this.methylCall = methylCall;
        this.CpGSites = new ArrayList<>();
        this.readConvState = readConvState;
        this.allelePosList = new ArrayList<>();
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

    public void setFlag(String flag) {
        this.flag = flag;
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

    public void setMethylCall(String methylCall) {
        this.methylCall = methylCall;
    }

    public void setQualityScore(String qualityScore) {
        this.qualityScore = qualityScore;
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


    public void setEditDist(String editDist) {
        this.editDist = editDist;
    }

    public void setMismatchString(String mismatchString) {
        this.mismatchString = mismatchString;
    }

    public void setReadConvState(String readConvState) {
        this.readConvState = readConvState;
    }

    public void setMutationString(String mutationString) {
        this.mutationString = mutationString;
    }

    public String getMutationString() {
        return mutationString;
    }

    public String getReadConvState() {
        return readConvState;
    }

    public String getEditDist() {
        return editDist;
    }

    public String getMismatchString() {
        return mismatchString;
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

    public String getMethylCall() {
        return methylCall;
    }

    public String getId() {
        return id;
    }

    public String getFlag() {
        return flag;
    }

    public String getOriginalSeq() {
        return originalSeq;
    }

    public String getQualityScore() {
        return qualityScore;
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
        StringBuilder meMuBuilder = new StringBuilder();
        for (int i = 0; i < originalSeq.length(); i++) {
            if (methylationString.charAt(i) != '-' && mutationString.charAt(i) != '-') {
				// if both methyl and mutation occurs, use mutation first
				meMuBuilder.append(mutationString.charAt(i));
                System.err.println("mismatch happens in G of CpG site!");
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
