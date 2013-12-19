package DataType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

import BSPAT.ReportSummary;

public class Experiment implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int index;
	private String name;
	private String seqFile;
	private boolean seqReady;
	private String inputType;
	public Queue<ReportSummary> reportSummaries;

	public Experiment(int index, String name) {
		super();
		this.index = index;
		this.name = name;
		this.seqReady = false;
		this.reportSummaries = new PriorityQueue<>();
	}
	
	public void setInputType(String inputType) {
		this.inputType = inputType;
	}
	
	public String getInputType() {
		return inputType;
	}
	
	public void setSeqReady(boolean seqReady) {
		this.seqReady = seqReady;
	}
	
	public boolean isSeqReady() {
		return seqReady;
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
	
	public Queue<ReportSummary> getReportSummaries() {
		return reportSummaries;
	}
}
