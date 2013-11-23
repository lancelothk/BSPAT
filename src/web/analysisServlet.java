package web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import BSPAT.Utilities;
import DataType.Constant;

/**
 * Servlet implementation class analysisServlet
 */
@WebServlet(name = "/analysisServlet", urlPatterns = { "/analysis" })
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
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		response.setContentType("text/html");
		String runID = request.getParameter("runID");
		constant = Constant.readConstant(runID);

		Collection<Part> parts = request.getParts();
		boolean coorReady = false;
		for (Part part : parts) {
			String fieldName = Utilities.getField(part, "name");
			String fileName = Utilities.getField(part, "filename");
			if (fieldName.equals("coor")) {// deal with uploaded coordinates file
				File coorFolder = new File(constant.coorFilePath);
				if (!coorFolder.isDirectory()) {// if coordinates directory do not exist, make one
					coorFolder.mkdirs();
				}
				// save coordinates file in coordinates folder
				if (Utilities.saveFileToDisk(part, coorFolder.getAbsolutePath(), fileName)) {
					coorReady = true;
					constant.coorFileName = fileName;
				}
			}
		}
		
		constant.coorReady = coorReady;
		if (request.getParameter("minp0text") != null) {
			constant.minP0Threshold = Double.valueOf(request.getParameter("minp0text"));
			constant.minMethylThreshold = -1;
			// value check
			if (constant.minP0Threshold < 0 || constant.minP0Threshold > 1) {
				Utilities.showAlertWindow(response, "p0 threshold invalid!!");
				return;
			}
		} else if (request.getParameter("minmethyltext") != null) {
			constant.minMethylThreshold = Double.valueOf(request.getParameter("minmethyltext"));
			constant.minP0Threshold = -1;
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

		//	update constant file on disk
		Constant.writeConstant(constant.runID, constant);

		// save constant object in request
		ExecuteAnalysis executeAnalysis = new ExecuteAnalysis(constant.runID);
		// start analysis thread
		Thread executeAnalysisThread = new Thread(executeAnalysis);
		executeAnalysisThread.start();
		FileReader progressHTMLFileReader = new FileReader(constant.diskRootPath + "/progress.html");
		BufferedReader progressHTMLBufferedReader = new BufferedReader(progressHTMLFileReader);
		String line;
		while ((line = progressHTMLBufferedReader.readLine()) != null) {
			response.getWriter().write(line);
		}
		progressHTMLBufferedReader.close();
		response.getWriter().flush();
		
		while (executeAnalysisThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Utilities.showAlertWindow(response, "executeAnalysisThread sleep interrupted");
				e.printStackTrace();
			}
		}
		response.getWriter().write(
				"<script type=\"text/javascript\"> document.location=\""
						+ constant.randomDir.toString().replace(constant.diskRootPath.toString(), constant.webRootPath)
						+ "/analysisResult.jsp" + "\";</script> ");

	}

	
}
