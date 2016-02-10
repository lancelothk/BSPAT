package edu.cwru.cbc.BSPAT;

/**
 * Created by kehu on 9/11/14.
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

	public void setPosition(int position) {
		this.position = position;
	}

	public boolean isMethylated() {
		return methylLabel;
	}
}
