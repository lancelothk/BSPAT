package edu.cwru.cbc.BSPAT.Servlet;

import edu.cwru.cbc.BSPAT.DataType.Constant;
import edu.cwru.cbc.BSPAT.core.CallBismark;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class ExecuteMapping implements Callable<Object> {
	private final static Logger LOGGER = Logger.getLogger(ExecuteMapping.class.getName());
	private Constant constant;
	private String experimentName;
	private CallBismark callBismark;

	public ExecuteMapping(CallBismark callBismark, String experimentName, Constant constant) {
		super();
		this.constant = constant;
		this.experimentName = experimentName;
		this.callBismark = callBismark;
	}

	@Override
	public Object call() throws Exception {
		LOGGER.info("Start mapping-" + experimentName);
		// run bismark and extract result
		callBismark.execute(constant.seqsPath + experimentName, constant.mappingResultPath + experimentName + "/",
				constant.logPath);
		LOGGER.info("Finished mapping-" + experimentName);
		return null;
	}
}
