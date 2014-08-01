package Servlet;

import BSPAT.CallBismark;
import DataType.Constant;

import java.io.IOException;

public class ExecuteMapping implements Runnable {
	private Constant constant;
	private String experimentName;
	private CallBismark callBismark;

	public ExecuteMapping(CallBismark callBismark, String experimentName, Constant constant) {
		super();
		this.constant = constant;
		this.experimentName = experimentName;
		this.callBismark = callBismark;
	}

	public synchronized void run() {
		System.out.println("Start mapping-" + experimentName);
		// run bismark and extract result
		try {
			callBismark.execute(constant.seqsPath + experimentName, constant.mappingResultPath + experimentName + "/");
		} catch (IOException e) {
			// TODO throw exception to main thread
			throw new RuntimeException("");
		}
		System.out.println("Finished mapping-" + experimentName);
	}
}
