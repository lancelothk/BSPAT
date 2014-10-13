package edu.cwru.cbc.BSPAT.Servlet;

import edu.cwru.cbc.BSPAT.DataType.Constant;
import edu.cwru.cbc.BSPAT.core.BSSeqAnalysis;
import edu.cwru.cbc.BSPAT.core.ReportSummary;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class ExecuteAnalysis implements Callable<List<ReportSummary>> {
    private final static Logger LOGGER = Logger.getLogger(ExecuteAnalysis.class.getName());

    private Constant constant;
    private String experimentName;

    public ExecuteAnalysis(String experimentName, Constant constant) {
        super();
        this.constant = constant;
        this.experimentName = experimentName;
    }

    @Override
    public List<ReportSummary> call() throws Exception {
        LOGGER.info("Pattern analysi begin\t" + experimentName);
        BSSeqAnalysis bsSeqAnalysis = new BSSeqAnalysis();
        List<ReportSummary> reportSummaryList = bsSeqAnalysis.execute(experimentName, constant);
        LOGGER.info("Pattern analysi finished\t" + experimentName);
        return reportSummaryList;
    }
}
