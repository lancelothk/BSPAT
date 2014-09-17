package Servlet;

import BSPAT.IO;
import BSPAT.ReportSummary;
import BSPAT.Utilities;
import DataType.Constant;
import DataType.Experiment;
import DataType.SeqCountSummary;
import org.apache.commons.io.FileUtils;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Servlet implementation class analysisServlet
 */
@WebServlet(name = "/analysisServlet", urlPatterns = {"/analysis"})
@MultipartConfig
public class AnalysisServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = Logger.getLogger(AnalysisServlet.class.getName());

    /**
     * @see HttpServlet#HttpServlet()
	 */
    public AnalysisServlet() {
        super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response) throws ServletException, IOException {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 * response)
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        Constant constant = null;
        try {
            long start = System.currentTimeMillis();
			response.setContentType("text/html");
			String jobID = request.getParameter("jobID");
            Constant.DISKROOTPATH = this.getServletContext().getRealPath("");
            constant = Constant.readConstant(jobID);
            Collection<Part> parts = null; // get submitted data
			try {
				parts = request.getParts();
			} catch (IOException | ServletException e) {
                throw new RuntimeException("Network error!", e);
            }
			for (Part part : parts) {
				String fieldName = Utilities.getField(part, "name");
				String fileName = Utilities.getField(part, "filename");
				if (fieldName.equals("target")) { // deal with uploaded target file
					File targetFolder = new File(constant.targetPath);
					if (!targetFolder.isDirectory()) { // if target directory do not exist, make one
						targetFolder.mkdirs();
					}
					// save target file in target folder
					if (fileName != null && !fileName.isEmpty()) {
						IO.saveFileToDisk(part, targetFolder.getAbsolutePath(), fileName);
						constant.targetFileName = fileName;
					} else {
						// copy original coor file to target folder
						FileUtils.copyFileToDirectory(new File(constant.coorFilePath + constant.coorFileName),
													  new File(constant.targetPath));
						constant.targetFileName = constant.coorFileName;
					}
				}
			}
			constant.figureFormat = request.getParameter("figureFormat"); // set figure format
			if (request.getParameter("minp0text") != null) {
				constant.minP0Threshold = Double.valueOf(request.getParameter("minp0text"));
				constant.criticalValue = Double.valueOf(request.getParameter("criticalValue"));
			} else if (request.getParameter("minmethyltext") != null) {
				constant.minMethylThreshold = Double.valueOf(request.getParameter("minmethyltext"));
				constant.minP0Threshold = -1;
				constant.criticalValue = -1;
			}

			constant.mutationPatternThreshold = Double.valueOf(request.getParameter("mutationpatternThreshold"));
			constant.conversionRateThreshold = Double.valueOf(request.getParameter("conversionRateThreshold"));
			constant.sequenceIdentityThreshold = Double.valueOf(request.getParameter("sequenceIdentityThreshold"));

			// clean up result directory and result zip file
			Utilities.deleteFolderContent(constant.patternResultPath);
			File resultZip = new File(constant.randomDir + "/" + "analysisResult.zip");
			if (resultZip.exists()) {
				resultZip.delete();
			}

			ExecutorService executor = Executors.newSingleThreadExecutor();
			List<Future<List<ReportSummary>>> futureList = new ArrayList<>();
			for (Experiment experiment : constant.experiments) {
				Future<List<ReportSummary>> future = executor.submit(
						new ExecuteAnalysis(experiment.getName(), constant));
				futureList.add(future);
			}

			constant.seqCountSummary = new SeqCountSummary();
			for (int i = 0; i < constant.experiments.size(); i++) {
				constant.experiments.get(i).reportSummaries = futureList.get(i).get();
                LOGGER.info(constant.getJobID() + "\t" + constant.experiments.get(i).getName() + "\tfinished!");
                for (ReportSummary reportSummary : constant.experiments.get(i).reportSummaries) {
					constant.seqCountSummary.addSeqBeforeFilter(reportSummary.getSeqBeforeFilter());
					constant.seqCountSummary.addSeqAfterFilter(reportSummary.getSeqAfterFilter());
				}
			}

			// compress result folder
			String zipFileName = constant.randomDir + "/" + "analysisResult.zip";
			Utilities.zipFolder(constant.patternResultPath, zipFileName);
			// send email to inform user
			Utilities.sendEmail(constant.email, constant.jobID,
								"Analysis has finished.\n" + "Your jobID is " + constant.jobID +
										"\nPlease go to cbc.case.edu/BSPAT/result.jsp to retrieve your result.");

			// passing JSTL parameters
			constant.analysisTime = (System.currentTimeMillis() - start) / 1000;
			constant.analysisTime = constant.analysisTime < 1 ? 1 : constant.analysisTime;
			constant.analysisResultLink = zipFileName.replace(Constant.DISKROOTPATH, constant.webRootPath);
			constant.finishedAnalysis = true;
			// update constant file on disk
            constant.writeConstant();
            request.setAttribute("jobID",constant.jobID);
			request.getRequestDispatcher("analysisResult.jsp").forward(request, response);
        } catch (InterruptedException | ServletException | IOException | MessagingException | ExecutionException | RuntimeException e) {
            Utilities.handleServletException(e, constant);
        }
    }

}