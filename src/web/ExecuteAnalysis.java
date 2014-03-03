package web;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import BSPAT.BSSeqAnalysis;
import BSPAT.ReportSummary;
import DataType.Constant;

public class ExecuteAnalysis implements Callable<ArrayList<ReportSummary>> {
	private Constant constant;
	private String experimentName;

	public ExecuteAnalysis(String experimentName, Constant constant) {
		super();
		this.constant = constant;
		this.experimentName = experimentName;
	}

	@Override
	public ArrayList<ReportSummary> call() throws Exception {
		System.out.println("Pattern analysi begin:");
		BSSeqAnalysis bsSeqAnalysis = new BSSeqAnalysis();
		return bsSeqAnalysis.execute(experimentName, constant);
	}
}
