package Servlet;

import BSPAT.CallBismark;
import DataType.Constant;

import java.util.concurrent.Callable;

public class ExecuteMapping implements Callable<Object> {
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
        System.out.println("Start mapping-" + experimentName);
        // run bismark and extract result
        callBismark.execute(constant.seqsPath + experimentName, constant.mappingResultPath + experimentName + "/",
                            constant.logPath);
        System.out.println("Finished mapping-" + experimentName);
        return null;
    }
}
