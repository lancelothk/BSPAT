package edu.cwru.cbc.BSPAT.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lancelothk on 3/2/16.
 */
public class BedInterval {
	private String chr;
	private int start;
	private int end;
	private String name;
	private boolean isPlusStrand;
	private List<Sequence> sequenceList;

	public BedInterval(String chr, int start, int end, String name, boolean isPlusStrand) {
		this.chr = chr;
		this.start = start;
		this.end = end;
		this.name = name;
		this.isPlusStrand = isPlusStrand;
		this.sequenceList = new ArrayList<>();
	}

	public boolean isPlusStrand() {
		return isPlusStrand;
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
		return String.format("%s-%d-%d-%s", chr, start, end, name);
	}
}
