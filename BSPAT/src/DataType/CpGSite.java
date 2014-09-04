package DataType;

public class CpGSite implements CpG {
    private int position;
    private boolean methylLabel;
    private int methylCount = 0;
    private int nonMethylCount = 0;

    public CpGSite(int position, boolean methylLabel) {
        this.methylLabel = methylLabel;
        this.position = position;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isMethylated() {
        return methylLabel;
    }

    @Override
    public int getMethylCount() {
        return methylCount;
    }

    @Override
    public int getNonMethylCount() {
        return nonMethylCount;
    }

    @Override
    public int getCountOfAll() {
        return methylCount + nonMethylCount;
    }

    public void addMethylCount(int count) {
        this.methylCount += count;
    }

    public void addNonMethylCount(int count) {
        this.nonMethylCount += count;
    }

    @Override
    public double getMethylLevel() {
        return methylCount / (double) (methylCount + nonMethylCount);
    }

    public char getMethylType() {
        if (getMethylLevel() >= 0.8) {
            return 'H';
        } else if (getMethylLevel() <= 0.2) {
            return 'L';
        } else {
            return 'M';
        }
    }
}
