package edu.cwru.cbc.BSPAT.core;

import edu.cwru.cbc.BSPAT.DataType.PatternLink;
import edu.cwru.cbc.BSPAT.DataType.SNP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ReportSummary implements Serializable {
    public static final String targetBoundedText = "A. Sequences cover whole target region:";
    public static final String targetAfterFilterText = "A. After quality control:";
    public static final String cpgBoundedText = "B. Sequences don't cover whole target but cover all CpGs:";
    public static final String cpgAfterFilterText = "B. After quality control:";
    public static final String othersText = "C. Sequences cover neither target nor all CpGs:";
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String statTextLink;
    private HashMap<String, PatternLink> patternHash = new HashMap<>();
    private String ASMFigureLink;
    private String ASMGBLink;
    private boolean hasASM;
    private SNP ASMsnp;
    private String id;
    private int seqTargetBounded;
    private int seqTargetAfterFilter;
    private int seqCpGBounded;
    private int seqCpGAfterFilter;
    private int seqOthers;

    public ReportSummary(String id) {
        this.id = id;
    }

    public void addPatternLink(String patternType) {
        patternHash.put(patternType, new PatternLink(patternType));
    }

    public void replacePath(String diskPath, String webPath, boolean hasFigure, String host) {
        statTextLink = statTextLink.replace(diskPath, webPath);
        for (PatternLink patternLink : patternHash.values()) {
            patternLink.setTextResultLink(patternLink.getTextResultLink().replace(diskPath, webPath));
        }
        if (hasFigure) {
            for (PatternLink patternLink : patternHash.values()) {
                patternLink.setFigureResultLink(patternLink.getFigureResultLink().replace(diskPath, webPath));
                patternLink.setGBResultLink(host + patternLink.getGBResultLink().replace(diskPath, webPath));
            }
            if (hasASM) {
                ASMFigureLink = ASMFigureLink.replace(diskPath, webPath);
                ASMGBLink = host + ASMGBLink.replace(diskPath, webPath);
            }
        }
    }

    public PatternLink getPatternLink(String patternType) {
        return this.patternHash.get(patternType);
    }

    public Collection<PatternLink> getPatternLinks() {
	    List<PatternLink> patternLinkList = new ArrayList<>();
	    if (patternHash.containsKey(PatternLink.METHYLATION)) {
		    patternLinkList.add(patternHash.get(PatternLink.METHYLATION));
	    }
	    if (patternHash.containsKey(PatternLink.METHYLATIONWITHMUTATION)) {
		    patternLinkList.add(patternHash.get(PatternLink.METHYLATIONWITHMUTATION));
	    }
	    return patternLinkList;
    }

    public int getSeqCpGBounded() {
        return seqCpGBounded;
    }

    public void setSeqCpGBounded(int seqCpGBounded) {
        this.seqCpGBounded = seqCpGBounded;
    }

    public int getSeqCpGAfterFilter() {
        return seqCpGAfterFilter;
    }

    public void setSeqCpGAfterFilter(int seqCpGAfterFilter) {
        this.seqCpGAfterFilter = seqCpGAfterFilter;
    }

    public int getSeqOthers() {
        return seqOthers;
    }

    public void setSeqOthers(int seqOthers) {
        this.seqOthers = seqOthers;
    }

    public int getSeqTargetBounded() {
        return seqTargetBounded;
    }

    public void setSeqTargetBounded(int seqTargetBounded) {
        this.seqTargetBounded = seqTargetBounded;
    }

    public int getSeqTargetAfterFilter() {
        return seqTargetAfterFilter;
    }

    public void setSeqTargetAfterFilter(int seqTargetAfterFilter) {
        this.seqTargetAfterFilter = seqTargetAfterFilter;
    }

    public String getStatTextLink() {
        return statTextLink;
    }

    public void setStatTextLink(String statTextLink) {
        this.statTextLink = statTextLink;
    }

    public String getASMFigureLink() {
        return ASMFigureLink;
    }

    public void setASMFigureLink(String ASMFigureLink) {
        this.ASMFigureLink = ASMFigureLink;
    }

    public String getASMGBLink() {
        return ASMGBLink;
    }

    public void setASMGBLink(String ASMGBLink) {
        this.ASMGBLink = ASMGBLink;
    }

    public boolean hasASM() {
        return hasASM;
    }

    public void setHasASM(boolean hasASM) {
        this.hasASM = hasASM;
    }

    public SNP getASMsnp() {
        return ASMsnp;
    }

    public void setASMsnp(SNP aSMsnp) {
        ASMsnp = aSMsnp;
    }

    public String getId() {
        return id;
    }
}
