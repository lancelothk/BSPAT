package DataType;

public class CpGSite {
    private int position;
    private boolean methylLabel;
    private int methylCount = 0;
    private int totalCount = 0;

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

    public void methylCountPlus(int count) {
        this.methylCount += count;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void totalCountPlus(int count) {
        this.totalCount += count;
    }

    public double getMethylLevel() {
        return methylCount / (double) totalCount;
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
