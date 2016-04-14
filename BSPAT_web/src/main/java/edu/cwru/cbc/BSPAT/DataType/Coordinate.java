package edu.cwru.cbc.BSPAT.DataType;

public class Coordinate {
	private String id;
	private int start;
	private int end;
	private String chr;
	private String strand;

	public Coordinate(String id, String chr, String strand, int start, int end) {
		super();
		this.id = id;
		this.start = start;
		this.end = end;
		this.chr = chr;
		this.strand = strand;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public String getChr() {
		return chr;
	}

	public String getStrand() {
		return strand;
	}

	public String getId() {
		return id;
	}

}
