package edu.cwru.cbc.BSPAT.commons;

/**
 * Created by kehu on 9/11/14.
 * CpG site stored in sequence
 */
public class CpGSite {
	private int position;
	private boolean methylLabel;

	public CpGSite(int position, boolean methylLabel) {
		this.methylLabel = methylLabel;
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	public boolean isMethylated() {
		return methylLabel;
	}

	public void reverse(int refLength) {
		this.position = refLength - this.position - 1;
	}
	public int compareTo(CpGSite o) {
		return this.getPosition() - o.getPosition();
	}
}
