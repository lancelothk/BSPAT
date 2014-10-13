package edu.cwru.cbc.BSPAT_analysis.QueryRefGenome;

/**
 * Created by kehu on 9/9/14.
 */
public class CpGIsland {
    private String chrom;
    private int chromStart;
    private int chromEnd;
    private String name;
    private int cpgNum;

    public CpGIsland() {
    }

    public CpGIsland(String chrom, int chromStart, int chromEnd, String name, int cpgNum) {
        this.chrom = chrom;
        this.chromStart = chromStart;
        this.chromEnd = chromEnd;
        this.name = name;
        this.cpgNum = cpgNum;
    }

    public String getChrom() {
        return chrom;
    }

    public void setChrom(String chrom) {
        this.chrom = chrom;
    }

    public int getChromStart() {
        return chromStart;
    }

    public void setChromStart(int chromStart) {
        this.chromStart = chromStart;
    }

    public int getChromEnd() {
        return chromEnd;
    }

    public void setChromEnd(int chromEnd) {
        this.chromEnd = chromEnd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCpgNum() {
        return cpgNum;
    }

    public void setCpgNum(int cpgNum) {
        this.cpgNum = cpgNum;
    }

    public long length(){
        return chromEnd - chromStart;
    }
}
