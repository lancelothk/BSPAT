package web;

import BSPAT.CallBismark;
import BSPAT.IO;
import BSPAT.Utilities;
import DataType.*;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Servlet implementation class uploadServlet
 */
@WebServlet(name = "/uploadServlet", urlPatterns = {"/mapping"})
@MultipartConfig
public class mappingServlet extends HttpServlet {
	private static final long serialVersionUID = 6078331324800268609L;
	private Constant constant = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public mappingServlet() {
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
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		initializeConstant(request); // initialize constant, which is a singleton
		cleanRootFolder(); // release root folder space
		List<Experiment> experiments = new ArrayList<>();
		boolean isDemo = Boolean.parseBoolean(request.getParameter("demo"));

		if (isDemo) {
			experiments.add(new Experiment(1, "demoExperiment"));
			FileUtils.copyFileToDirectory(new File(constant.demoPath + "demoReference.fasta"),
										  new File(constant.originalRefPath));
			FileUtils.copyFileToDirectory(new File(constant.demoPath + "demoSequence.fastq"),
										  new File(constant.seqsPath + "demoExperiment"));
		} else {
			addExperiment(request, response, experiments);
			handleUploadedFiles(request, response, experiments);
		}

		long start = System.currentTimeMillis();
		// set other parameters
		constant.refVersion = request.getParameter("refVersion");
		constant.coorFileName = "coordinates.coor";
		constant.qualsType = request.getParameter("qualsType");
		constant.maxmis = Integer.valueOf(request.getParameter("maxmis"));
		constant.experiments = experiments;
		constant.email = request.getParameter("email");
		// execute BLAT query
		blatQuery();
		// append xx to both end of reference
		try {
			modifyRef(constant.originalRefPath, constant.modifiedRefPath);
		} catch (Exception e) {
			Utilities.showAlertWindow(response, "error in modifying reference");
			e.printStackTrace();
		}
		// bismark indexing
		CallBismark callBismark = null;
		try {
			callBismark = new CallBismark(constant.modifiedRefPath, constant.toolsPath, constant.qualsType,
										  constant.maxmis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// multiple threads to execute bismark mapping
		ExecutorService executor = Executors.newCachedThreadPool();
		for (Experiment experiment : constant.experiments) {
			executor.execute(new ExecuteMapping(callBismark, experiment.getName(), constant));
		}
		executor.shutdown();
		// Wait until all threads are finish
		try {
			executor.awaitTermination(Constant.MAXEXECUTIONDAY, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// read mapping summary report
		MappingSummary mappingSummary = IO.readMappingSummary(constant.mappingResultPath);

		// compress result folder
		String zipFileName = constant.randomDir + "/mappingResult.zip";
		Utilities.zipFolder(constant.mappingResultPath, zipFileName);

		// passing JSTL parameters
		constant.mappingSummary = mappingSummary;
		constant.mappingTime = (System.currentTimeMillis() - start) / 1000;
		constant.mappingTime = constant.mappingTime < 1 ? 1 : constant.mappingTime;
		constant.mappingResultLink = zipFileName.replace(Constant.DISKROOTPATH, constant.webRootPath);
		constant.finishedMapping = true;
		// save constant object to file
		Constant.writeConstant();
		// send email to inform user
		Utilities.sendEmail(constant.email, constant.runID,
							"Mapping has finished.\n" + "Your runID is " + constant.runID +
									"\nPlease go to cbc.case.edu/BSPAT/result.jsp to retrieve your result.");
		//redirect page
		request.setAttribute("constant", constant);
		request.getRequestDispatcher("mappingResult.jsp").forward(request, response);
	}

	private void addExperiment(HttpServletRequest request, HttpServletResponse response, List<Experiment> experiments) {
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
	}

	private void handleUploadedFiles(HttpServletRequest request, HttpServletResponse response,
									 List<Experiment> experiments) throws IOException, ServletException {
		Collection<Part> parts = request.getParts(); // get submitted data
		// for each part, add it into corresponding experiment
		boolean refReady = false;
		for (Part part : parts) {
			String fieldName = Utilities.getField(part, "name");
			String fileName = Utilities.getField(part, "filename");
			if (fieldName.equals("ref")) { // deal with uploaded ref file
				// save ref file in ref folder
				if (IO.saveFileToDisk(part, constant.originalRefPath, fileName)) {
					refReady = true;
				} else {
					Utilities.showAlertWindow(response, "reference file is blank!");
					return;
				}
			} else if (fieldName.startsWith("seqFile")) { // deal with uploaded seq file
				int seqFileIndex = Integer.valueOf(part.getName().replace("seqFile", ""));
				for (Experiment experiment : experiments) { // match file index and seq file index
					if (experiment.getIndex() == seqFileIndex) {
						experiment.setSeqFile(fileName);
						if (IO.saveFileToDisk(part, constant.seqsPath + experiment.getName(), fileName)) {
							experiment.setSeqReady(true);
						} else {
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
				if (!expt.isSeqReady()) {
					Utilities.showAlertWindow(response, "Experiment " + expt.getIndex() + " sequence file missing!");
					return;
				}
			}
		} else {
			Utilities.showAlertWindow(response, "Reference file missing!");
			return;
		}
	}

	/**
	 * initialize constant singleton
	 *
	 * @param request
	 * @throws IOException
	 */
	private void initializeConstant(HttpServletRequest request) throws IOException {
		constant = Constant.getInstance();
		// DISKROOTPATH is absolute disk path.
		Constant.DISKROOTPATH = this.getServletContext().getRealPath("");
		// webPath is relative path to root
		constant.webRootPath = request.getContextPath();
		constant.randomDir = Files.createTempDirectory(Paths.get(Constant.DISKROOTPATH), "Run").toString();
		constant.runID = constant.randomDir.split("Run")[1];
		constant.mappingResultPath = constant.randomDir + "/bismark_result/";
		IO.createFolder(constant.mappingResultPath);
		constant.patternResultPath = constant.randomDir + "/pattern_result/";
		IO.createFolder(constant.patternResultPath);
		constant.coorFilePath = constant.randomDir + "/coor/";
		IO.createFolder(constant.coorFilePath);
		constant.targetPath = constant.randomDir + "/target/";
		IO.createFolder(constant.targetPath);
		constant.originalRefPath = constant.randomDir + "/origianlRef/";
		IO.createFolder(constant.originalRefPath);
		constant.modifiedRefPath = constant.randomDir + "/modifiedRef/";
		IO.createFolder(constant.modifiedRefPath);
		constant.seqsPath = constant.randomDir + "/seqs/";
		IO.createFolder(constant.seqsPath);
		constant.toolsPath = Constant.DISKROOTPATH + "/tools/";
		IO.createFolder(constant.toolsPath);
		constant.demoPath = Constant.DISKROOTPATH + "/demo/";
		URL domain = new URL(request.getRequestURL().toString());
		constant.host = domain.getHost() + ":" + domain.getPort();
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
		if (!refFolder.isDirectory()) {// if ref directory do not exist, make
			// one
			refFolder.mkdirs();
		}
		File originalRefPathFile = new File(originalRefPath);
		String[] fileNames;
		fileNames = originalRefPathFile.list(new ExtensionFilter(new String[]{".txt", "fasta", "fa"}));
		for (String str : fileNames) {
			BufferedReader reader = new BufferedReader(new FileReader(originalRefPath + str));
			BufferedWriter writer = new BufferedWriter(new FileWriter(modifiedRefPath + str));
			String line;
			StringBuilder ref = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(">")) {
					if (ref.length() > 0) {
						// append XX to both end of reference
						writer.write("XX" + ref.toString() + "XX\n");
						ref = new StringBuilder();
					}
					writer.write(line + "\n");
				} else {
					ref.append(line);
				}
			}
			if (ref.length() > 0) {
				// append XX to both end of reference
				writer.write("XX" + ref.toString() + "XX\n");
			}
			reader.close();
			writer.close();
		}
	}

	/**
	 * clean old data to release space in server
	 *
	 * @throws IOException
	 */
	private void cleanRootFolder() throws IOException {
		File rootDirectory = new File(Constant.DISKROOTPATH);
		long rootFolderSize = FileUtils.sizeOfDirectory(rootDirectory) / 1024 / 1024;
		long excess = rootFolderSize - Constant.SPACETHRESHOLD;
		System.out.println("root folder space occupation:\t" + rootFolderSize + "M");
		if (rootFolderSize >= Constant.SPACETHRESHOLD) { // if exceed threshold, clean
			File[] subFolders = rootDirectory.listFiles();
			Arrays.sort(subFolders, new FileDateComparator()); // sort by date.
			if (subFolders != null) {
				for (File folder : subFolders) {
					if (folder.getName().startsWith("Run") && folder.isDirectory() &&
							folder != subFolders[subFolders.length - 1]) {
						if (excess >= 0) { // only clean exceed part
							long length = FileUtils.sizeOfDirectory(folder) / 1024 / 1024;
							// delete directory recursively
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

	/**
	 * execute blat query
	 */
	private void blatQuery() {
		File refFolder = new File(constant.originalRefPath);
		String[] files = refFolder.list();
		String blatQueryPath = constant.toolsPath + "BlatQuery/";
		for (String name : files) {
			try {
				System.out.println("start blat query for " + name);
				String blatQuery = String.format("%sBlatQuery.sh %s %s %s %s", blatQueryPath, blatQueryPath,
												 constant.refVersion, constant.originalRefPath, name);
				Utilities.callCMD(blatQuery, new File(constant.coorFilePath), null);
				System.out.println("blat query is finished for " + name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Utilities.convertPSLtoCoorPair(constant.coorFilePath, constant.coorFileName, constant.refVersion);
			constant.coorReady = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("blat result converted");
	}

}
