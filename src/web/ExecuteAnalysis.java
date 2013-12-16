package web;

import BSPAT.BSSeqAnalysis;
import DataType.Constant;

public class ExecuteAnalysis implements Runnable {
	private Constant constant;

	public ExecuteAnalysis(Constant constant) {
		super();
		this.constant = constant;
	}

	public synchronized void run() {
		try {
			String[] patternResultPaths = new String[constant.experiments.size()];
			String[] bismarkResultPaths = new String[constant.experiments.size()];
			for (int i = 0; i < constant.experiments.size(); i++) {
				bismarkResultPaths[i] = constant.mappingResultPath + constant.experiments.get(i).getName() + "/";
				patternResultPaths[i] = constant.patternResultPath + constant.experiments.get(i).getName() + "/";
			}
			System.out.println("Pattern analysi begin:");
			for (int i = 0; i < constant.experiments.size(); i++) {
				BSSeqAnalysis bsSeqAnalysis = new BSSeqAnalysis();
				constant.reportSummaries.add(bsSeqAnalysis.execute(bismarkResultPaths[i], patternResultPaths[i], constant.experiments.get(i).getName(), constant));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
