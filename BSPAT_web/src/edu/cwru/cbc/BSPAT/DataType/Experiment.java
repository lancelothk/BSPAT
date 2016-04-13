package edu.cwru.cbc.BSPAT.DataType;

import edu.cwru.cbc.BSPAT.core.ReportSummary;

import java.io.Serializable;
import java.util.List;

public class Experiment implements Serializable {
	private static final long serialVersionUID = 1L;
	public List<ReportSummary> reportSummaries;
	private int index;
	private String name;
	private String seqFile;
	private String inputType;

	public Experiment(int index, String name) {
		super();
		this.index = index;
		this.name = name;
	}

	public String getInputType() {
		return inputType;
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSeqFile() {
		return seqFile;
	}

	public void setSeqFile(String seqFile) {
		this.seqFile = seqFile;
	}

	public List<ReportSummary> getReportSummaries() {
		return reportSummaries;
	}
}
