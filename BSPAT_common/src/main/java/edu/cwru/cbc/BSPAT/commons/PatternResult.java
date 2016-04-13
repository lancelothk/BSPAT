package edu.cwru.cbc.BSPAT.commons;

import java.util.ArrayList;
import java.util.List;

public class PatternResult {
	public static int targetRegionLength;
	private List<CpGStatistics> CpGList;
	private int count;
	private double percent;
	private PotentialSNP snp = null;

	public PatternResult() {
		this.CpGList = new ArrayList<>();
	}

	public void addCpG(CpGStatistics cpg) {
		CpGList.add(cpg);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public double getPercent() {
		return percent;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	public List<CpGStatistics> getCpGList() {
		return CpGList;
	}

	public void setCpGList(List<CpGStatistics> cpGSitePatternList) {
		this.CpGList = cpGSitePatternList;
	}

	public PotentialSNP getSnp() {
		return snp;
	}

	public void setSnp(PotentialSNP snp) {
		this.snp = snp;
	}
}
