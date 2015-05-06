package edu.cwru.cbc.BSPAT.DataType;

import java.io.Serializable;

public class PatternLink implements Serializable {
	public static final String STAT = "Stat";
	public static final String ASM = "ASM";
	public static final String METHYLATION = "Methylation";
	public static final String METHYLATIONWITHMUTATION = "MethylationWithMutation";
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	protected String patternType;
	private String textResultLink;
	private String figureResultLink;
	private String GBResultLink;

	public PatternLink(String patternType) {
		this.patternType = patternType;
	}

	public String getPatternType() {
		return patternType;
	}

	public void setPatternType(String patternType) {
		this.patternType = patternType;
	}

	public String getTextResultLink() {
		return textResultLink;
	}

	public void setTextResultLink(String textResultLink) {
		this.textResultLink = textResultLink;
	}

	public String getFigureResultLink() {
		return figureResultLink;
	}

	public void setFigureResultLink(String figureResultLink) {
		this.figureResultLink = figureResultLink;
	}

	public String getGBResultLink() {
		return GBResultLink;
	}

	public void setGBResultLink(String gBResultLink) {
		GBResultLink = gBResultLink;
	}
}
