package BSPAT;

import DataType.PatternLink;
import DataType.SNP;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class ReportSummary implements Serializable {
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
    private int seqBeforeFilter;
    private int seqAfterFilter;

    public ReportSummary(String id) {
        this.id = id;
        patternHash.put(PatternLink.METHYLATION, new PatternLink(PatternLink.METHYLATION));
        patternHash.put(PatternLink.MUTATION, new PatternLink(PatternLink.MUTATION));
        patternHash.put(PatternLink.METHYLATIONWITHMUTATION, new PatternLink(PatternLink.METHYLATIONWITHMUTATION));
        patternHash.put(PatternLink.MUTATIONWITHMETHYLATION, new PatternLink(PatternLink.MUTATIONWITHMETHYLATION));
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
        PatternLink[] patternLinks = new PatternLink[4];
        for (PatternLink patternLink : patternHash.values()) {
            switch (patternLink.getPatternType()) {
                case PatternLink.METHYLATION:
                    patternLinks[0] = patternLink;
                    break;
                case PatternLink.METHYLATIONWITHMUTATION:
                    patternLinks[1] = patternLink;
                    break;
                case PatternLink.MUTATION:
                    patternLinks[2] = patternLink;
                    break;
                case PatternLink.MUTATIONWITHMETHYLATION:
                    patternLinks[3] = patternLink;
                    break;
            }
        }
        return Arrays.asList(patternLinks);
    }

    public int getSeqBeforeFilter() {
        return seqBeforeFilter;
    }

    public void setSeqBeforeFilter(int seqBeforeFilter) {
        this.seqBeforeFilter = seqBeforeFilter;
    }

    public int getSeqAfterFilter() {
        return seqAfterFilter;
    }

    public void setSeqAfterFilter(int seqAfterFilter) {
        this.seqAfterFilter = seqAfterFilter;
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
