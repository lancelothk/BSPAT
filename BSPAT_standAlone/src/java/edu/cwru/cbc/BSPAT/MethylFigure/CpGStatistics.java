package edu.cwru.cbc.BSPAT.MethylFigure;

public class CpGStatistics implements CpG {

	// count of all CpG sites
	private int countOfAll;
	// count of methylated CpG sites
	private int methylCount;
	private double methylLevel;
	private int position;

	public CpGStatistics(int position) {
		this.countOfAll = 0;
		this.methylCount = 0;
		this.methylLevel = 0;
		this.position = position;
	}

	public CpGStatistics(CpGStatistics cpg) {
		this.countOfAll = cpg.getCountOfAll();
		this.methylCount = cpg.getMethylCount();
		this.methylLevel = cpg.getMethylLevel();
		this.position = cpg.getPosition();
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
}