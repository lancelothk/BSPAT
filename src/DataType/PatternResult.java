package DataType;

import java.util.ArrayList;

public class PatternResult {
	private ArrayList<CpGSite> CpGList;
	private int count;
	private double percent;
	private boolean hasAllele = false;
	private ArrayList<Integer> alleleList;
	public PatternResult(PatternResult patternResult){
		this.CpGList = new ArrayList<CpGSite>();
		this.alleleList = new ArrayList<>(); 
		for (CpGSite cpg : patternResult.getCpGList()) {
			this.CpGList.add(new CpGSite(cpg.getPosition(), false));
		}
		this.hasAllele = patternResult.hasAllele;
		this.alleleList = patternResult.getAlleleList();
	}
	
	public PatternResult(){
		CpGList = new ArrayList<CpGSite>();
		this.alleleList = new ArrayList<>(); 
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
	
	public void addAllele(int locus){
		this.alleleList.add(locus);
		this.hasAllele = true;
	}
	
	public ArrayList<Integer> getAlleleList() {
		return alleleList;
	}

	public boolean hasAllele() {
		return hasAllele;
	}
	
	public void countPlus(int count){
		this.count += count;
	}
}
