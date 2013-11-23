package DataType;

public class Coordinate {
	private long start;
	private long end;
	private long starthg19;
	private long endhg19;
	private String chr;
	public Coordinate(long start, long end, String chr) {
		super();
		this.start = start;
		this.end = end;
		this.chr = chr;
	}
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public long getEnd() {
		return end;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	public String getChr() {
		return chr;
	}
	public void setChr(String chr) {
		this.chr = chr;
	}
	public void setgh19(int start, int end){
		this.starthg19 = start;
		this.endhg19 = end;
	}
	public long getStarthg19() {
		return starthg19;
	}
	public long getEndhg19() {
		return endhg19;
	}
}
