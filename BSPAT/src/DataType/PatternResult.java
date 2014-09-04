package DataType;

import java.util.ArrayList;
import java.util.List;

public class PatternResult {
    private List<CpGSite> CpGList;
    private int count;
	private double percent;
    private List<Integer> alleleList;

	public PatternResult() {
        CpGList = new ArrayList<>();
        this.alleleList = new ArrayList<>();
	}

	public void addCpG(CpGSite cpg) {
		CpGList.add(cpg);
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	public int getCount() {
		return count;
	}

	public double getPercent() {
		return percent;
	}

    public List<CpGSite> getCpGList() {
        return CpGList;
	}

	public void addAllele(int locus) {
		this.alleleList.add(locus);
	}

    public void setCpGList(List<CpGSite> cpGSiteList) {
        this.CpGList = cpGSiteList;
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
