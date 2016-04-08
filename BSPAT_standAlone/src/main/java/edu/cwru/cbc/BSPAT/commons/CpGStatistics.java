package edu.cwru.cbc.BSPAT.commons;

public class CpGStatistics implements CpG {

	// count of all CpG sites
	private int countOfAll;
	// count of methylated CpG sites
	private int methylCount;
	private double methylLevel;
	private int position;
	private boolean methylLabel;

	public CpGStatistics(int position, boolean methylLabel) {
		this.countOfAll = 0;
		this.methylCount = 0;
		this.methylLevel = 0;
		this.position = position;
		this.methylLabel = methylLabel;
	}

	public CpGStatistics(CpGStatistics cpg) {
		this.countOfAll = cpg.getCountOfAll();
		this.methylCount = cpg.getMethylCount();
		this.methylLevel = cpg.getMethylLevel();
		this.position = cpg.getPosition();
	}

	public boolean isMethylated() {
		return methylLabel;
	}

	public void calcMethylLevel() {
		methylLevel = methylCount / (double) countOfAll;
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

	@Override
	public int compareTo(CpG o) {
		return this.getPosition() - o.getPosition();
	}

	public void addNonMethylCount(int count) {
		this.countOfAll += count;
	}

	public void addMethylCount(int count) {
		this.methylCount += count;
		this.countOfAll += count;
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