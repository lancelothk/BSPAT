package web;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import BSPAT.CallBismark;
import BSPAT.Utilities;
import DataType.Constant;
import DataType.Experiment;

public class ExecuteMapping implements Runnable {
	private boolean completed = false;
	private Constant constant;

	public ExecuteMapping(String runID) {
		super();
		constant = Constant.readConstant(runID);
	}

	public synchronized void run() {
		try {
			BufferedWriter jspBufferedWriter = new BufferedWriter(new FileWriter(constant.randomDir + "/mappingResult.jsp"));
			// head
			jspBufferedWriter
					.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">");
			jspBufferedWriter.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"../style.css\" />");
			jspBufferedWriter.write("<div id=\"container\">");
			jspBufferedWriter.write("<%@ include file=\"../menu.html\"%>");
			jspBufferedWriter.write("<div id=\"content\"><div id=\"content_top\"></div><div id=\"content_main\">");

			// result description table
			jspBufferedWriter.write("<table id=\"mappingResultDescription\">");

			// dispaly experiment names
			jspBufferedWriter.write("<tr><td>Mapping Summary</td></tr><tr><td>" + "Experiment(" + constant.experiments.size() + "):</td><td>");
			for (Experiment experiment : constant.experiments) {
				jspBufferedWriter.write("" + experiment.getName() + "&nbsp&nbsp&nbsp");
			}
			jspBufferedWriter.write("</td></tr>");

			// mapping
			double mappingRuntime = mapping();
			jspBufferedWriter.write("<tr><td>" + "mapping running time:</td><td>" + mappingRuntime + "s</td></tr>");
			jspBufferedWriter.write("<tr><td>" + "Result ID: </td><td>" + constant.runID + "</td></tr>");

			// compress mapping result and provide mapping result link
			String zipFileName = constant.randomDir + "/" + "mappingResult.zip";
			Utilities.zipFolder(constant.mappingResultPath, zipFileName);
			jspBufferedWriter.write("<tr><td>" + "Mapping result: </td>" + "<td><a href="
					+ zipFileName.replace(constant.diskRootPath.toString(), constant.webRootPath) + ">mappingResult.zip</a></td></tr>");
			jspBufferedWriter.write("</table><p class=\"dottedline\"></p>");

			// display analysis options
			displayAnalysisOptions(jspBufferedWriter);

			// write js script
			writeJS(jspBufferedWriter);

			// bottom
			jspBufferedWriter.write("</div><div id=\"content_bottom\"></div></div><%@ include file=\"../footer.html\"%></div>");

			jspBufferedWriter.close();
			completed = true;

			// send email to inform user
			Utilities.sendEmail(constant.email, constant.runID, "Mapping has finished.\n" + "Your runID is " + constant.runID
					+ "\nPlease go to cbc.case.edu/BS-PAT/result.jsp to retrieve your result.");
		} catch (Exception e) {
			Utilities.sendEmail(constant.email, constant.runID, "Mapping encounter errors. Please check you input and parameters");
			e.printStackTrace();
			completed = true;
			return;
		}
	}

	public boolean isCompleted() {
		return completed;
	}

	private double mapping() {
		long startMapping = System.currentTimeMillis();
		CallBismark callBismark = null;
		try {
			callBismark = new CallBismark(constant.modifiedRefPath, constant.toolsPath, constant.qualsType, constant.maxmis);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// and
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
		return (System.currentTimeMillis() - startMapping) / 1000;
	}

	private void displayAnalysisOptions(BufferedWriter jspBufferedWriter) {
		// use hidden field to store runID
		try {
			jspBufferedWriter.write("<form action=\"../analysis\" enctype=\"multipart/form-data\" method=\"post\">\n");
			jspBufferedWriter.write("<input type=\"hidden\" name=\"runID\" value=\"" + constant.runID + "\">\n");
//			jspBufferedWriter.write("<table id=\"coor\">\n");
//			jspBufferedWriter
//					.write("<tr><td>Corresponding Coordinates File(Optional):</td><td><input type=\"file\" name=\"coor\" multiple=\"multiple\" /></td></tr>\n");
//			jspBufferedWriter.write("<td>Reference Genome Version</td><td><select name=\"refVersion\"><option value=\"hg19\">phred33</option><option value=\"hg18\">phred64</option></select></td>");
			jspBufferedWriter.write("<p class=\"dottedline\"></p>\n");
			jspBufferedWriter.write("<table id=\"parameters\">\n");
			jspBufferedWriter.write("<tr><td>Analysis Parameters: </td><td>Values must be in (0,1)</td></tr>\n");
			jspBufferedWriter
					.write("<tr><td>Bisulfite conversion rate threshold</td><td><input type=\"text\" name=\"conversionRateThreshold\" value=0.95 /></td></tr>\n");
			jspBufferedWriter
					.write("<tr><td>Sequence identity threshold</td><td><input type=\"text\" name=\"sequenceIdentityThreshold\" value=0.9 /></td></tr>\n");
			jspBufferedWriter
					.write("<tr><td><input type=\"radio\" name=\"par\" id=\"minp0\" value=\"minp0\" checked=\"yes\" onclick=\"check()\" />P0 threshold</td><td><input type=\"text\" id=\"minp0text\" name=\"minp0text\" value=0.02 /></td></tr>\n");
			jspBufferedWriter
					.write("<tr><td><input type=\"radio\" name=\"par\" id=\"minmethyl\" value=\"minmethyl\" onclick=\"check()\"  />Methylation pattern threshold</td><td><input type=\"text\" id=\"minmethyltext\" name=\"minmethyltext\" value=0.1 disabled=\"true\" /></td></tr>\n");
			jspBufferedWriter
					.write("<tr><td>Mutation pattern threshold</td><td><input type=\"text\" name=\"mutationpatternThreshold\" value=0.3 /></td></tr>\n");
			jspBufferedWriter.write("<td>Figure Format</td><td><select name=\"figureFormat\"><option value=\"png\">PNG</option><option value=\"eps\">EPS</option></select></td>");
			jspBufferedWriter.write("</table><p class=\"dottedline\"></p></br>\n");
			jspBufferedWriter.write("<p id=\"submitParagraph\"><input type=\"submit\" value=\"Submit\" id=\"submitButton\" /></p>\n");
			jspBufferedWriter.write("</form>\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeJS(BufferedWriter jspBufferedWriter) {
		try {
			jspBufferedWriter.write("<script language=\"javascript\">\n");
			jspBufferedWriter.write("function check(){\n");
			jspBufferedWriter.write("if(document.getElementById('minp0').checked) {\n");
			jspBufferedWriter.write("document.getElementById('minp0text').disabled = false;\n");
			jspBufferedWriter.write("document.getElementById('minmethyltext').disabled = true;\n");
			jspBufferedWriter.write("}else if(document.getElementById('minmethyl').checked) {\n");
			jspBufferedWriter.write("document.getElementById('minp0text').disabled = true;\n");
			jspBufferedWriter.write("document.getElementById('minmethyltext').disabled = false;\n");
			jspBufferedWriter.write("}\n");
			jspBufferedWriter.write("}\n");
			// disable methyl , enable p0
			// jspBufferedWriter.write("function lockmethyl(checkbox) {");
			// jspBufferedWriter.write("var methylBox = document.getElementById(\"minmethylbox\");");
			// jspBufferedWriter.write("var methylText = document.getElementById(\"minmethyltext\");");
			// jspBufferedWriter.write("var p0Text = document.getElementById(\"minp0text\");");
			// jspBufferedWriter.write("if (checkbox.checked==true){");
			// jspBufferedWriter.write("methylText.disabled = true;");
			// jspBufferedWriter.write("p0Text.disabled = false;");
			// jspBufferedWriter.write("methylBox.disabled = true;");
			// jspBufferedWriter.write("}else{p0Text.disabled = true;methylBox.disabled = false;}}");
			// // disable p0 , enable methyl
			// jspBufferedWriter.write("function lockp0(checkbox) {");
			// jspBufferedWriter.write("var methylText = document.getElementById(\"minmethyltext\");");
			// jspBufferedWriter.write("var p0Text = document.getElementById(\"minp0text\");");
			// jspBufferedWriter.write("var p0Box = document.getElementById(\"minp0box\");");
			// jspBufferedWriter.write("if (checkbox.checked==true){");
			// jspBufferedWriter.write("methylText.disabled = false;");
			// jspBufferedWriter.write("p0Text.disabled = true;");
			// jspBufferedWriter.write("p0Box.disabled = true;");
			// jspBufferedWriter.write("}else{methylText.disabled = true;p0Box.disabled = false;}}");
			jspBufferedWriter.write("</script>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
