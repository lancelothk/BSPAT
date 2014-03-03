package web;

import BSPAT.IO;
import BSPAT.ReportSummary;
import BSPAT.Utilities;
import DataType.AnalysisSummary;
import DataType.Constant;
import DataType.Experiment;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Servlet implementation class analysisServlet
 */
@WebServlet(name = "/analysisServlet", urlPatterns = {"/analysis"})
@MultipartConfig
public class analysisServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Constant constant = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public analysisServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		long start = System.currentTimeMillis();
		response.setContentType("text/html");
		String runID = request.getParameter("runID");
		constant = Constant.readConstant(runID);
		constant.figureFormat = request.getParameter("figureFormat"); // set
		// figure
		// format
		if (request.getParameter("minp0text") != null) {
			constant.minP0Threshold = Double.valueOf(request.getParameter("minp0text"));
			constant.criticalValue = Double.valueOf(request.getParameter("criticalValue"));
			constant.minMethylThreshold = -1;
			// value check
			if (constant.minP0Threshold < 0 || constant.minP0Threshold > 1) {
				Utilities.showAlertWindow(response, "alpha threshold invalid!!");
				return;
			}
			// value check
			if (constant.criticalValue < 0 || constant.criticalValue > 1) {
				Utilities.showAlertWindow(response, "critical value invalid!!");
				return;
			}
		} else if (request.getParameter("minmethyltext") != null) {
			constant.minMethylThreshold = Double.valueOf(request.getParameter("minmethyltext"));
			constant.minP0Threshold = -1;
			constant.criticalValue = -1;
			// value check
			if (constant.minMethylThreshold < 0 || constant.minMethylThreshold > 1) {
				Utilities.showAlertWindow(response, "methylation pattern threshold invalid!!");
				return;
			}
		}

		constant.mutationPatternThreshold = Double.valueOf(request.getParameter("mutationpatternThreshold"));
		constant.conversionRateThreshold = Double.valueOf(request.getParameter("conversionRateThreshold"));
		constant.sequenceIdentityThreshold = Double.valueOf(request.getParameter("sequenceIdentityThreshold"));

		// value check
		if (constant.mutationPatternThreshold < 0 || constant.mutationPatternThreshold > 1) {
			Utilities.showAlertWindow(response, "mutation pattern threshold invalid!!");
			return;
		}
		if (constant.conversionRateThreshold < 0 || constant.conversionRateThreshold > 1) {
			Utilities.showAlertWindow(response, "conversion rate threshold invalid!!");
			return;
		}
		if (constant.sequenceIdentityThreshold < 0 || constant.sequenceIdentityThreshold > 1) {
			Utilities.showAlertWindow(response, "sequence identity threshold invalid!!");
			return;
		}

		// clean up result directory and result zip file
		Utilities.deleteFolderContent(constant.patternResultPath);
		File resultZip = new File(constant.randomDir + "/" + "analysisResult.zip");
		if (resultZip.exists()) {
			resultZip.delete();
		}

		ExecutorService executor = Executors.newCachedThreadPool();
		ArrayList<Future<ArrayList<ReportSummary>>> futureList = new ArrayList<>();
		for (Experiment experiment : constant.experiments) {
			Future<ArrayList<ReportSummary>> future = executor.submit(new ExecuteAnalysis(experiment.getName(), constant));
			futureList.add(future);
		}

		for (int i = 0; i < constant.experiments.size(); i++) {
			try {
				constant.experiments.get(i).reportSummaries = futureList.get(i).get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// read analysis summary report
		AnalysisSummary analysisSummary = IO.readAnalysisSummary(constant.patternResultPath);

		// compress result folder
		String zipFileName = constant.randomDir + "/" + "analysisResult.zip";
		Utilities.zipFolder(constant.patternResultPath, zipFileName);
		// send email to inform user
		Utilities.sendEmail(constant.email, constant.runID, "Analysis has finished.\n" + "Your runID is " + constant.runID
				+ "\nPlease go to cbc.case.edu/BSPAT/result.jsp to retrieve your result.");

		// passing JSTL parameters
		constant.analysisSummary = analysisSummary;
		constant.analysisTime = (System.currentTimeMillis() - start) / 1000;
		constant.analysisResultLink = zipFileName.replace(constant.diskRootPath, constant.webRootPath);
		constant.finishedAnalysis = true;
		// update constant file on disk
		Constant.writeConstant();
		request.setAttribute("constant", constant);
		request.getRequestDispatcher("analysisResult.jsp").forward(request, response);
	}

}
