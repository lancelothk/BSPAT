package web;

import java.io.IOException;

import BSPAT.CallBismark;
import DataType.Constant;
import DataType.Experiment;

public class ExecuteMapping implements Runnable {
	private Constant constant;

	public ExecuteMapping(Constant constant) {
		super();
		this.constant = constant;
	}

	public synchronized void run() {
		try {
			// mapping
			CallBismark callBismark = null;
			callBismark = new CallBismark(constant.modifiedRefPath, constant.toolsPath, constant.qualsType, constant.maxmis);
			for (Experiment experiment : constant.experiments) {
				System.out.println("Start mapping" + experiment.getName());
				// run bismark and extract result
				try {
					callBismark.execute(constant.seqsPath + experiment.getName(), constant.mappingResultPath + experiment.getName() + "/");
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Finished mapping" + experiment.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
}
