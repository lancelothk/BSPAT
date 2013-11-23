package web;

import java.io.BufferedWriter;
import java.io.FileWriter;

import BSPAT.BSSeqAnalysis;
import BSPAT.Utilities;
import DataType.Constant;

public class ExecuteAnalysis implements Runnable {
	private boolean completed = false;
	private Constant constant;

	public ExecuteAnalysis(String runID) {
		super();
		constant = Constant.readConstant(runID);
	}

	public synchronized void run() {
		try {
			callBSPAT();
			completed = true;
			// send email to inform user
			Utilities.sendEmail(constant.email, constant.runID, "Analysis has finished.\n" +  "Your runID is " + constant.runID
						+ "\nPlease go to cbc.case.edu/BS-PAT/result.jsp to retrieve your result.");
		} catch (Exception e) {
			System.err.println("BS-PAT thread error\n");
			e.printStackTrace();
			Utilities.sendEmail(constant.email, constant.runID, "Analysis encounter errors. Please check you input and parameters\n");
			completed = true;
		}
	}

	public boolean isCompleted() {
		return completed;
	}

	private void callBSPAT() throws Exception {
		FileWriter jspWriter = new FileWriter(constant.randomDir + "/analysisResult.jsp");
		BufferedWriter jspBufferedWriter = new BufferedWriter(jspWriter);

		String[] patternResultPaths = new String[constant.experiments.size()];
		String[] bismarkResultPaths = new String[constant.experiments.size()];
		for (int i = 0; i < constant.experiments.size(); i++) {
				bismarkResultPaths[i] = constant.mappingResultPath + constant.experiments.get(i).getName() + "/";
				patternResultPaths[i] = constant.patternResultPath + constant.experiments.get(i).getName() + "/";
		}

		String htmlPage;
		long startBSPAT = System.currentTimeMillis();
		htmlPage = runBSSeqAnalysis(bismarkResultPaths, patternResultPaths);
		long endBSPAT = System.currentTimeMillis() - startBSPAT;

		jspBufferedWriter.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">");
		jspBufferedWriter.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"../style.css\" />");
		jspBufferedWriter.write("<div id=\"container\">");
		jspBufferedWriter.write("<%@ include file=\"../menu.html\"%>");
		jspBufferedWriter.write("<div id=\"content\"><div id=\"content_top\"></div><div id=\"content_main\">");

		//result summary table
		jspBufferedWriter.write("<table id=\"summary\">");

		// display running summary
		jspBufferedWriter.write("<p class=\"dottedline\"></p>");
		jspBufferedWriter.write("<tr><td>Result Summary:</td></tr>");
		jspBufferedWriter.write("<tr><td>RunID:</td><td>" + constant.runID + "</td></tr>");
		jspBufferedWriter.write("<tr><td>Bisulfite conversion rate threshold:</td><td>"
				+ constant.conversionRateThreshold + "</td></tr>");
		jspBufferedWriter.write("<tr><td>Sequence identity threshold:</td><td>" + constant.sequenceIdentityThreshold
				+ "</td></tr>");
		if (constant.minP0Threshold != -1) {
			jspBufferedWriter.write("<tr><td>p0 threshold:</td><td>" + constant.minP0Threshold + "</td></tr>");
		} else if (constant.minMethylThreshold != -1) {
			jspBufferedWriter.write("<tr><td>methylation pattern threshold:</td><td>" + constant.minMethylThreshold
					+ "</td></tr>");
		}
		jspBufferedWriter.write("<tr><td>Mutation pattern threshold:</td><td>" + constant.mutationPatternThreshold
				+ "</td></tr>");
		jspBufferedWriter.write("<tr><td>Analysis running time:</td><td>" + endBSPAT / 1000
				+ "s</td></tr></table><p class=\"dottedline\"></p>");
		jspBufferedWriter.write(htmlPage);

		// compress mapping result and provide mapping result link
		String zipFileName = constant.randomDir + "/" + "analysisResult.zip";
		Utilities.zipFolder(constant.patternResultPath, zipFileName);
		jspBufferedWriter.write("<table><tr><td>" + "Zipped analysis result: </td>" + "<td><a href="
				+ zipFileName.replace(constant.diskRootPath.toString(), constant.webRootPath)
				+ ">analysisResult.zip</a></td></tr></table>");
		jspBufferedWriter.write("</br><p class=\"dottedline\"></br></p><A HREF=\"mappingResult.jsp\">Back</A>");
		jspBufferedWriter
				.write("</div><div id=\"content_bottom\"></div></div><%@ include file=\"../footer.html\"%></div>");
		jspBufferedWriter.close();
	}

	private String runBSSeqAnalysis(String[] bismarkResults, String[] patternResults) throws Exception {
		String htmlPage = "";
		System.out.println("Pattern analysi begin:");
		for (int i = 0; i < constant.experiments.size(); i++) {
			htmlPage += ("<table id=\"analysisResult\"><tr><td>Experiment:</td><td>"
					+ constant.experiments.get(i).getName() + "</td></tr>");
				BSSeqAnalysis bsSeqAnalysis = new BSSeqAnalysis();
				htmlPage  += bsSeqAnalysis.execute(bismarkResults[i],
						patternResults[i], constant.experiments.get(i).getName(), constant);
		}
		htmlPage += "</table>";
		return htmlPage;
	}
}
