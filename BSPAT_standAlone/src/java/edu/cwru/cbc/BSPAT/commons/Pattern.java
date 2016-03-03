package edu.cwru.cbc.BSPAT.commons;

import java.util.HashMap;
import java.util.Map;

public class Pattern {
	private int patternCount;
	private String patternString;
	private Map<String, Sequence> sequenceMap;
	private int patternID;
	private int methylationParentID;
	private PatternType patternType;

	public Pattern(String patternString, PatternType patternType) {
		this.patternString = patternString;
		this.patternType = patternType;
		sequenceMap = new HashMap<>();
	}

	public void assignPatternID(int i) {
		this.patternID = i;
	}

	public int getPatternID() {
		return patternID;
	}

	public void addSequence(Sequence seq) {
		sequenceMap.put(seq.getId(), seq);
	}

	public PatternType getPatternType() {
		return patternType;
	}

	public int getCount() {
		return sequenceMap.size();
	}

	public String getPatternString() {
		return patternString;
	}

	public void setPatternString(String patternString) {
		this.patternString = patternString;
	}

	public Map<String, Sequence> getSequenceMap() {
		return sequenceMap;
	}

	public int getMethylationParentID() {
		return methylationParentID;
	}

	public void setMethylationParentID(int methylationParentID) {
		this.methylationParentID = methylationParentID;
	}

	public enum PatternType {
		METHYLATION, MEMU, ALLELE, NONALLELE
	}
}
