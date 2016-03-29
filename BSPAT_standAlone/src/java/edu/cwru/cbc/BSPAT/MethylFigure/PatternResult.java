package edu.cwru.cbc.BSPAT.MethylFigure;

import edu.cwru.cbc.BSPAT.commons.PotentialSNP;

import java.util.ArrayList;
import java.util.List;

public class PatternResult {
	public static int targetRegionLength;
	private List<CpGSitePattern> CpGList;
	private int count;
	private double percent;
	private PotentialSNP snp = null;

	public PatternResult() {
		this.CpGList = new ArrayList<>();
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

	public PotentialSNP getSnp() {
		return snp;
	}

	public void setSnp(PotentialSNP snp) {
		this.snp = snp;
	}

	public void countPlus(int count) {
		this.count += count;
	}

}
