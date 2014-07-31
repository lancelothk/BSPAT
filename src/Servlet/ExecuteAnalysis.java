package Servlet;

import BSPAT.BSSeqAnalysis;
import BSPAT.ReportSummary;
import DataType.Constant;

import java.util.List;
import java.util.concurrent.Callable;

public class ExecuteAnalysis implements Callable<List<ReportSummary>> {
	private Constant constant;
	private String experimentName;

	public ExecuteAnalysis(String experimentName, Constant constant) {
		super();
		this.constant = constant;
		this.experimentName = experimentName;
	}

	@Override
	public List<ReportSummary> call() throws Exception {
		System.out.println("Pattern analysi begin:");
		BSSeqAnalysis bsSeqAnalysis = new BSSeqAnalysis();
		return bsSeqAnalysis.execute(experimentName, constant);
	}
}
