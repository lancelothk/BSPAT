package DataType;

public class Coordinate {
	private String id;
	private long start;
	private long end;
	private String chr;
	private String strand;

	public Coordinate(String id, String chr, String strand, long start, long end) {
		super();
		this.id = id;
		this.start = start;
		this.end = end;
		this.chr = chr;
		this.strand = strand;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
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
