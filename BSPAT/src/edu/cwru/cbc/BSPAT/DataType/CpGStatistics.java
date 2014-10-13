package edu.cwru.cbc.BSPAT.DataType;

public class CpGStatistics implements CpG {

    // count of all CpG sites
    private int countOfAll;
    // count of methylated CpG sites
    private int methylCount;
    private double methylLevel;
    private int position;


    public CpGStatistics(int position) {
        countOfAll = 0;
        methylCount = 0;
        methylLevel = 0;
        this.position = position;
    }

    public void setMethylLevel(double methylLevel) {
        this.methylLevel = methylLevel;
    }

    public void calcMethylLevel() {
        methylLevel = methylCount / (double) countOfAll;
    }

    @Override
    public double getMethylLevel() {
        return methylLevel;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void allSitePlus() {
        countOfAll++;
    }

    public void methylSitePlus() {
        methylCount++;
    }

    @Override
    public int getCountOfAll() {
        return countOfAll;
    }

    @Override
    public int getNonMethylCount() {
        return countOfAll - methylCount;
    }

    @Override
    public int getMethylCount() {
        return methylCount;
    }
}