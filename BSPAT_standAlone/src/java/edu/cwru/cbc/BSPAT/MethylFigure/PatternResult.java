package edu.cwru.cbc.BSPAT.MethylFigure;

import java.util.ArrayList;
import java.util.List;

public class PatternResult {
	public static int targetRegionLength;
	private List<CpGSitePattern> CpGList;
	private int count;
	private double percent;
	private List<Integer> alleleList;

	public PatternResult() {
		this.CpGList = new ArrayList<>();
		this.alleleList = new ArrayList<>();
	}

	public void addCpG(CpGSitePattern cpg) {
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

	public List<CpGSitePattern> getCpGList() {
		return CpGList;
	}

	public void setCpGList(List<CpGSitePattern> cpGSitePatternList) {
		this.CpGList = cpGSitePatternList;
	}

	public void addAllele(int locus) {
		this.alleleList.add(locus);
	}

	public List<Integer> getAlleleList() {
		return alleleList;
	}

	public boolean hasAllele() {
		return alleleList.size() != 0;
	}

	public void countPlus(int count) {
		this.count += count;
	}

}
