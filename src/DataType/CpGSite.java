package DataType;

public class CpGSite {
    private int position;
    private boolean methylLabel;
    private int methylCount = 0;
    private int nonMethylCount = 0;

    public CpGSite(int position, boolean methylLabel) {
        this.methylLabel = methylLabel;
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean getMethylLabel() {
        return methylLabel;
    }

    public int getMethylCount() {
        return methylCount;
    }

    public int getNonMethylCount() {
        return nonMethylCount;
    }

    public void addMethylCount(int count) {
        this.methylCount += count;
    }

    public void addNonMethylCount(int count) {
        this.nonMethylCount += count;
    }

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
