package DataType;

import java.util.ArrayList;

public class PatternResult {
	private ArrayList<CpGSite> CpGList;
	private int count;
	private double percent;
	private int alleleLocus = -1;
	private boolean hasAllele = false;
	
	public PatternResult(PatternResult patternResult){
		this.CpGList = new ArrayList<CpGSite>();
		for (CpGSite cpg : patternResult.getCpGList()) {
			this.CpGList.add(new CpGSite(cpg.getPosition(), false));
		}
		this.count = 0;
		this.hasAllele = patternResult.hasAllele;
		this.alleleLocus = patternResult.getAlleleLocus();
	}
	
	public PatternResult(){
		CpGList = new ArrayList<CpGSite>();
	}
	
	public void addCpG(CpGSite cpg){
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
	
	public ArrayList<CpGSite> getCpGList() {
		return CpGList;
	}
	
	public void addAlleleLocus(int locus){
		this.alleleLocus = locus;
		this.hasAllele = true;
	}
	
	public int getAlleleLocus() {
		return alleleLocus;
	}

	public boolean hasAllele() {
		return hasAllele;
	}
	
	
}
