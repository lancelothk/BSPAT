package web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;

import BSPAT.Utilities;
import DataType.Constant;
import DataType.Experiment;
import DataType.ExtensionFilter;
import DataType.FileDateComparator;

/**
 * Servlet implementation class uploadServlet
 */
@WebServlet(name = "/uploadServlet", urlPatterns = { "/mapping" })
@MultipartConfig
public class mappingServlet extends HttpServlet {
	private static final long serialVersionUID = 6078331324800268609L;
	private Constant constant = null;
	private final long SPACETHRESHOLD = 4000;// maximum allow 4000MB space

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public mappingServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		initializeConstant(request); // initialize constant, which is a singleton
		System.out.println("diskpath:\t" + this.getServletContext().getRealPath("")); // print disk path to console
		cleanRootFolder(); // release root folder space
		Collection<Part> parts = request.getParts(); // get submitted data
		ArrayList<Experiment> experiments = new ArrayList<Experiment>();
		// add each experiment in list
		String experimentName;
		int index = 1;
		while ((experimentName = request.getParameter("experiment" + index)) != null) {
			if (experimentName.equals("")) {
				response.setContentType("text/html");
				Utilities.showAlertWindow(response, "Experiment " + index + " name is empty!");
				return;
			} else {
				experiments.add(new Experiment(index, experimentName));
				index++; // separate ++ operation makes code clear.
			}
		}
		// for each part, add it into corresponding experiment
		boolean refReady = false;
		for (Part part : parts) {
			String fieldName = Utilities.getField(part, "name");
			String fileName = Utilities.getField(part, "filename");
			if (fieldName.equals("ref")) { // deal with uploaded ref file
				File refFolder = new File(constant.originalRefPath);
				if (!refFolder.isDirectory()) { // if ref directory do not exist, make one
					refFolder.mkdirs();
				}
				// save ref file in ref folder
				if (Utilities.saveFileToDisk(part, refFolder.getAbsolutePath(), fileName)) {
					refReady = true;
				}else {
					Utilities.showAlertWindow(response, "reference file is blank!");
					return;
				}
			} else if (fieldName.startsWith("seqFile")) { // deal with uploaded seq file
				int seqFileIndex = Integer.valueOf(part.getName().replace("seqFile", ""));
				for (Experiment experiment : experiments) { // match file index and seq file index
					if (experiment.getIndex() == seqFileIndex) {
						experiment.setSeqFile(fileName);
						File seqFolder = new File(constant.seqsPath + experiment.getName());
						if (!seqFolder.isDirectory()) { // if sequence directory do not exist, make one
							seqFolder.mkdirs();
						}
						if (Utilities.saveFileToDisk(part, seqFolder.getAbsolutePath(), fileName)) {
							experiment.setSeqReady(true);
						}else {
							Utilities.showAlertWindow(response, "seq file is blank!");
							return;
						}
						break;
					}
				}
			}
		}
		// check file uploading status
		if (refReady) {
			for (Experiment expt : experiments) {
				if (expt.isSeqReady() == false) {
					Utilities.showAlertWindow(response, "Experiment " + expt.getIndex() + " sequence file missing!");
					return;
				}
			}
		} else {
			Utilities.showAlertWindow(response, "Reference file missing!");
			return;
		}
		// set other parameters
		constant.refVersion = request.getParameter("refVersion"); // set reference genome version
		constant.qualsType = request.getParameter("qualsType"); // set quality score type parameter
		constant.maxmis = Integer.valueOf(request.getParameter("maxmis")); // set maximum permitted mismatches
		constant.experiments = experiments;
		constant.email = request.getParameter("email");// set email address
		Constant.writeConstant(constant.runID, constant); // save constant object in file
		// append xx to both end of reference
		try {
			modifyRef(constant.originalRefPath, constant.modifiedRefPath);
		} catch (Exception e) {
			Utilities.showAlertWindow(response, "error in modifying reference");
			e.printStackTrace();
		}
		// initialize executeMappingThread
		ExecuteMapping executeMapping = new ExecuteMapping(constant.runID);
		Thread executeMappingThread = new Thread(executeMapping);
		executeMappingThread.start();
		// initialize executeBlatQueryThread
		ExecuteBlatQuery executeBlatQuery = new ExecuteBlatQuery(constant);
		Thread executeBlatQueryThread = new Thread(executeBlatQuery);
		executeBlatQueryThread.start();
		// display progress.html
		FileReader progressHTMLFileReader = new FileReader(constant.diskRootPath + "/progress.html");
		BufferedReader progressHTMLBufferedReader = new BufferedReader(progressHTMLFileReader);
		String line;
		while ((line = progressHTMLBufferedReader.readLine()) != null) {
			response.getWriter().write(line);
		}
		progressHTMLBufferedReader.close();
		response.getWriter().flush();
		// wait for executeMappingThread finish
		while (executeMappingThread.isAlive() || executeBlatQueryThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Utilities.showAlertWindow(response, "executeMappingThread sleep interrupted");
				e.printStackTrace();
				return;
			}
		}
		// write response page into disk
		response.getWriter().write(
				"<script type=\"text/javascript\"> document.location=\""
						+ constant.randomDir.toString().replace(constant.diskRootPath.toString(), constant.webRootPath)
						+ "/mappingResult.jsp" + "\";</script> ");
	}

	/**
	 * initialize constant singleton
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
	private Constant initializeConstant(HttpServletRequest request) throws IOException {
		constant = Constant.getInstance();
		constant.diskRootPath = this.getServletContext().getRealPath("");
		constant.webRootPath = request.getContextPath();
		Constant.WEBAPPFOLDER = constant.diskRootPath;
		// randomDir is absolute disk path.
		constant.randomDir = Files.createTempDirectory(Paths.get(constant.diskRootPath), "Run").toString();
		constant.runID = constant.randomDir.split("Run")[1];
		constant.mappingResultPath = constant.randomDir + "/bismark_result/";
		Utilities.createFolder(constant.mappingResultPath);
		constant.patternResultPath = constant.randomDir + "/pattern_result/";
		Utilities.createFolder(constant.patternResultPath);
		constant.coorFilePath = constant.randomDir + "/coor/";
		Utilities.createFolder(constant.coorFilePath);
		constant.originalRefPath = constant.randomDir + "/origianlRef/";
		Utilities.createFolder(constant.originalRefPath);
		constant.modifiedRefPath = constant.randomDir + "/modifiedRef/";
		Utilities.createFolder(constant.modifiedRefPath);
		constant.seqsPath = constant.randomDir + "/seqs/";
		Utilities.createFolder(constant.seqsPath);
		constant.toolsPath = constant.diskRootPath + "/tools/";
		Utilities.createFolder(constant.toolsPath);
		URL domain = new URL(request.getRequestURL().toString());
		constant.host = domain.getHost() + ":" + domain.getPort();
		return constant;
	}

	/**
	 * append xx to both ends of original reference. Save result as modified
	 * reference
	 * 
	 * @param originalRefPath
	 * @param modifiedRefPath
	 * @throws IOException
	 */
	private void modifyRef(String originalRefPath, String modifiedRefPath) throws IOException {
		File refFolder = new File(modifiedRefPath);
		if (!refFolder.isDirectory()) {// if ref directory do not exist, make one
			refFolder.mkdirs();
		}
		File originalRefPathFile = new File(originalRefPath);
		String[] fileNames = null;
		fileNames = originalRefPathFile.list(new ExtensionFilter(new String[] { ".txt", "fasta", "fa" }));
		for (String str : fileNames) {
			BufferedReader reader = new BufferedReader(new FileReader(originalRefPath + str));
			BufferedWriter writer = new BufferedWriter(new FileWriter(modifiedRefPath + str));
			String line = null;
			StringBuilder ref = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(">")){
					if (ref.length() > 0){
						writer.write("XX" + ref.toString() + "XX\n");// append XX to both end of reference
						ref = new StringBuilder();
					}
					writer.write(line + "\n");
				}else{
					ref.append(line);
				}
			}
			if (ref.length() > 0){
				writer.write("XX" + ref.toString() + "XX\n");// append XX to both end of reference
			}
			reader.close();
			writer.close();
		}
	}

	/**
	 * clean old data to release space in server
	 * @param rootDirectory
	 * @param excess
	 * @throws IOException
	 */
	private void cleanRootFolder() throws IOException {
		File rootDirectory = new File(constant.diskRootPath);
		long rootFolderSize = FileUtils.sizeOfDirectory(rootDirectory) / 1024 / 1024; // size in bytes. need divided by 1024*1024 to convert to MB
		long excess = rootFolderSize - SPACETHRESHOLD;
		System.out.println("root folder space occupation:\t" + rootFolderSize);
		if (rootFolderSize >= SPACETHRESHOLD) { // if exceed threshold, clean
			File[] subFolders = rootDirectory.listFiles();
			Arrays.sort(subFolders, new FileDateComparator()); // sort by date. 
			for (File folder : subFolders) {
				if (folder.getName().startsWith("Run") && folder.isDirectory() && folder != subFolders[subFolders.length-1]) {
					if (excess >= 0) { // only clean exceed part
						long length = FileUtils.sizeOfDirectory(folder) / 1024 / 1024;
						FileUtils.deleteDirectory(folder);
						excess -= length;
					} else {
						return;
					}
				}
			}
		}
	}

}
