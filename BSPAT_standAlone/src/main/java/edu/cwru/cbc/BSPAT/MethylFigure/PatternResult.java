package edu.cwru.cbc.BSPAT.MethylFigure;

import edu.cwru.cbc.BSPAT.commons.PotentialSNP;

import java.util.ArrayList;
import java.util.List;

public class PatternResult {
	public static int targetRegionLength;
	private List<CpGStatistics> CpGList;
	private int count;
	private double percent;
	private PotentialSNP snp = null;
	private boolean reversePattern = false;

	public PatternResult() {
		this.CpGList = new ArrayList<>();
	}

	public void setReversePattern(boolean reversePattern) {
		this.reversePattern = reversePattern;
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

	public void reversePattern(int refLength) {
		for (CpGStatistics cpg : CpGList) {
			cpg.reverse(refLength);
		}
	}
}
