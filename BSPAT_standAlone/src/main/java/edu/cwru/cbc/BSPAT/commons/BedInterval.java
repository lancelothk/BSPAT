package edu.cwru.cbc.BSPAT.commons;

import java.util.ArrayList;
import java.util.List;

public class BedInterval {
	private String chr;
	private int start;
	private int end;
	private String name;
	private boolean isMinusStrand;
	private List<Sequence> sequenceList;

	public BedInterval(String chr, int start, int end, String name, boolean isMinusStrand) {
		this.chr = chr;
		this.start = start;
		this.end = end;
		this.name = name;
		this.isMinusStrand = isMinusStrand;
		this.sequenceList = new ArrayList<>();
	}

	public static BedInterval newInstance(BedInterval bedInterval){
		BedInterval newInstance = new BedInterval(bedInterval.chr, bedInterval.start, bedInterval.end, bedInterval.name, bedInterval.isMinusStrand);
		for (Sequence sequence : bedInterval.sequenceList) {
			newInstance.sequenceList.add(Sequence.newInstance(sequence));
		}
		return newInstance;
	}

	public boolean isMinusStrand() {
		return isMinusStrand;
	}

	public String getChr() {
		return chr;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getName() {
		return name;
	}

	public List<Sequence> getSequenceList() {
		return sequenceList;
	}

	@Override
	public String toString() {
		return String.format("%s-%d-%d-%s-%s", chr, start, end, name, isMinusStrand?"minus":"plus");
	}

	public void reverse(int refLength) {
		int tmp = this.start;
		this.start = refLength - this.end - 1;
		this.end = refLength - tmp - 1;
	}
}
