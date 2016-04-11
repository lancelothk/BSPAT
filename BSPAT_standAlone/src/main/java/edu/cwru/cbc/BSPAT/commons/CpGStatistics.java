package edu.cwru.cbc.BSPAT.commons;

public class CpGStatistics implements CpG {

	private int methylCount;
	private int nonMethylCount;
	private double methylLevel;
	private int position;
	private boolean methylLabel;

	public CpGStatistics(int position, boolean methylLabel) {
		this.methylLevel = 0;
		this.position = position;
		this.methylLabel = methylLabel;
	}

	public boolean isMethylated() {
		return methylLabel;
	}

	public void calcMethylLevel() {
		methylLevel = methylCount / (double) (methylCount + nonMethylCount);
	}

	@Override
	public double getMethylLevel() {
		return methylLevel;
	}

	public void setMethylLevel(double methylLevel) {
		this.methylLevel = methylLevel;
	}

	@Override
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int getCountOfAll() {
		return methylCount + nonMethylCount;
	}

	@Override
	public int getNonMethylCount() {
		return nonMethylCount;
	}

	@Override
	public int getMethylCount() {
		return methylCount;
	}

	@Override
	public int compareTo(CpG o) {
		return this.getPosition() - o.getPosition();
	}

	public void addNonMethylCount(int count) {
		this.nonMethylCount += count;
	}

	public void addMethylCount(int count) {
		this.methylCount += count;
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

	public void reverse(int refLength) {
		this.position = refLength - this.getPosition();
	}
}