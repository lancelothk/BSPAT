package edu.cwru.cbc.BSPAT.commons;

/**
 * Created by lancelothk on 3/2/16.
 */
public class BedInterval {
	private String chr;
	private int start;
	private int end;
	private String name;

	public BedInterval(String chr, int start, int end, String name) {
		this.chr = chr;
		this.start = start;
		this.end = end;
		this.name = name;
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

	@Override
	public String toString() {
		return String.format("%s-%d-%d-%s", chr, start, end, name);
	}
}
