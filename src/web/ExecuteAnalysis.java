package web;

import BSPAT.BSSeqAnalysis;
import DataType.Constant;
import DataType.Experiment;

public class ExecuteAnalysis implements Runnable {
	private Constant constant;

	public ExecuteAnalysis(Constant constant) {
		super();
		this.constant = constant;
	}

	public synchronized void run() {
		try {
			System.out.println("Pattern analysi begin:");
			for (Experiment experiment : constant.experiments) {
				BSSeqAnalysis bsSeqAnalysis = new BSSeqAnalysis();
				experiment.reportSummaries = bsSeqAnalysis.execute(experiment.getName(), constant);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
