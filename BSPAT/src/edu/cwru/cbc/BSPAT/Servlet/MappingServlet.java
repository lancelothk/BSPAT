package edu.cwru.cbc.BSPAT.Servlet;

import com.google.common.io.Files;
import edu.cwru.cbc.BSPAT.DataType.*;
import edu.cwru.cbc.BSPAT.core.CallBismark;
import edu.cwru.cbc.BSPAT.core.IO;
import edu.cwru.cbc.BSPAT.core.Utilities;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Servlet implementation class uploadServlet
 */
@WebServlet(name = "/uploadServlet", urlPatterns = {"/mapping"})
@MultipartConfig
public class MappingServlet extends HttpServlet {
	private static final long serialVersionUID = 6078331324800268609L;
	private final static Logger LOGGER = Logger.getLogger(MappingServlet.class.getName());
	private final SecureRandom random = new SecureRandom();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MappingServlet() {
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
			constant = new Constant();
			response.setContentType("text/html");
			initializeConstant(constant, request); // initialize constant, which is a singleton
			cleanRootFolder(constant.getJobID()); // release root folder space
			List<Experiment> experiments = new ArrayList<>();
			boolean isDemo = Boolean.parseBoolean(request.getParameter("demo"));

			if (isDemo) {
				experiments.add(new Experiment(1, "demoExperiment"));
				FileUtils.copyFileToDirectory(new File(constant.demoPath + "demoReference.fasta"),
						new File(constant.originalRefPath));
				FileUtils.copyFileToDirectory(new File(constant.demoPath + "demoSequence.fastq"),
						new File(constant.seqsPath + "demoExperiment"));
			} else if (Boolean.parseBoolean(request.getParameter("test"))) {
				FileUtils.copyFileToDirectory(new File(constant.testPath + "testReference.fasta"),
						new File(constant.originalRefPath));
				addTestExperiment(constant, experiments, "experiment1", 1);
				addTestExperiment(constant, experiments, "experiment2", 2);
			} else {
				addExperiment(request, experiments);
				handleUploadedFiles(constant, request, experiments);
			}

			long start = System.currentTimeMillis();
			// set other parameters
			constant.refVersion = request.getParameter("refVersion");
			constant.coorFileName = "coordinates.coor";
			constant.qualsType = request.getParameter("qualsType");
			constant.maxmis = Integer.parseInt(request.getParameter("maxmis"));
			constant.experiments = experiments;
			constant.email = request.getParameter("email");
			// execute BLAT query
			List<String> blatWarnings = blatQuery(constant);
			// append xx to both end of reference
			modifyRef(constant.originalRefPath, constant.modifiedRefPath);

			// load configurations
			Properties properties = new Properties();
			properties.load(new FileInputStream(Constant.DISKROOTPATH + Constant.propertiesFileName));

			// bismark indexing
			CallBismark callBismark;
			callBismark = new CallBismark(constant.modifiedRefPath, properties.getProperty("bismarkPath"),
					properties.getProperty("bowtiePath"), constant.logPath, constant.qualsType, constant.maxmis);

			// multiple threads to execute bismark mapping
			ExecutorService executor;
			if (Boolean.parseBoolean(properties.getProperty("useSingleThread"))) {
				executor = Executors.newSingleThreadExecutor(); // single thread
			} else {
				executor = Executors.newCachedThreadPool(); // multiple threads
			}

			List<Future<Object>> futureList = new ArrayList<>();
			for (Experiment experiment : constant.experiments) {
				Future<Object> future = executor.submit(
						new ExecuteMapping(callBismark, experiment.getName(), constant));
				futureList.add(future);
			}

			for (Future future : futureList) {
				future.get();
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
			constant.blatWarnings = blatWarnings;
			// save constant object to file
			constant.writeConstant();
			// send email to inform user
			Utilities.sendEmail(constant.email, constant.jobID,
					"Mapping has finished.\n" + "Your jobID is " + constant.jobID +
							"\nPlease go to cbc.case.edu/BSPAT/result.jsp to retrieve your result.");
			//redirect page
			request.setAttribute("jobID", constant.jobID);
			request.getRequestDispatcher("mappingResult.jsp").forward(request, response);
		} catch (InterruptedException | ServletException | IOException | MessagingException | ExecutionException | RuntimeException e) {
			Utilities.handleServletException(e, constant);
		}
	}

	private void addTestExperiment(Constant constant, List<Experiment> experiments, String experimentName,
	                               int experimentIndex) throws IOException {
		experiments.add(new Experiment(experimentIndex, experimentName));
		File[] seqs = new File(constant.testPath + experimentName).listFiles(new ExtensionFilter(".fastq"));
		for (File seq : seqs) {
			FileUtils.copyFileToDirectory(seq, new File(constant.seqsPath + "/" + experimentName));
		}
	}

	private void addExperiment(HttpServletRequest request, List<Experiment> experiments) {
		// add each experiment in list
		String experimentName;
		int index = 1;
		while ((experimentName = request.getParameter("experiment" + index)) != null) {
			experiments.add(new Experiment(index, experimentName));
			index++; // separate ++ operation makes code clear.
		}
	}

	private void handleUploadedFiles(Constant constant, HttpServletRequest request, List<Experiment> experiments) {
		Collection<Part> parts; // get submitted data
		try {
			parts = request.getParts();
		} catch (IOException | ServletException e) {
			throw new RuntimeException("Network error!", e);
		}
		// for each part, add it into corresponding experiment
		for (Part part : parts) {
			String fieldName = Utilities.getField(part, "name");
			String fileName = Utilities.getField(part, "filename");
			assert fieldName != null;
			if (fieldName.equals("ref")) { // deal with uploaded ref file
				// save ref file in ref folder
				IO.saveFileToDisk(part, constant.originalRefPath, fileName);
			} else if (fieldName.startsWith("seqFile")) { // deal with uploaded seq file
				int seqFileIndex = Integer.parseInt(part.getName().replace("seqFile", ""));
				for (Experiment experiment : experiments) { // match file index and seq file index
					if (experiment.getIndex() == seqFileIndex) {
						experiment.setSeqFile(fileName);
						IO.saveFileToDisk(part, constant.seqsPath + experiment.getName(), fileName);
						break;
					}
				}
			}
		}
	}

	/**
	 * generate random suffix folder with given path and prefix.
	 */
	private File generateRandomDirectory(String path, String prefix) throws IOException {
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		long n;
		File dir;
		do {
			n = random.nextLong();
			n = (n == Long.MIN_VALUE) ? 0 : Math.abs(n);
			dir = new File(path + prefix + Long.toString(n));
		} while (dir.exists());
		if (!dir.mkdir()) {
			throw new IOException("failed to create random folder in " + path + " with prefix: " + prefix);
		}
		return dir;
	}

	/**
	 * initialize constant singleton
	 */
	private void initializeConstant(Constant constant, HttpServletRequest request) throws IOException {
		// DISKROOTPATH is absolute disk path.
		Constant.DISKROOTPATH = this.getServletContext().getRealPath("");
		// webPath is relative path to root
		constant.webRootPath = request.getContextPath().endsWith("\\") ? request.getContextPath().replace("\\", "/") :
				request.getContextPath() + "/";
		constant.randomDir = generateRandomDirectory(Constant.DISKROOTPATH,
				Constant.JOB_FOLDER_PREFIX).getAbsolutePath();
		constant.jobID = constant.randomDir.split(Constant.JOB_FOLDER_PREFIX)[1];
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
		constant.logPath = constant.randomDir + "/log/";
		IO.createFolder(constant.logPath);

		// under root path
		constant.toolsPath = Constant.DISKROOTPATH + "/tools/";
		IO.createFolder(constant.toolsPath);
		constant.demoPath = Constant.DISKROOTPATH + "/demo/";
		constant.testPath = Constant.DISKROOTPATH + "/test/";
		URL domain = new URL(request.getRequestURL().toString());
		constant.host = domain.getHost() + ":" + domain.getPort();
	}

	/**
	 * append xx to both ends of original reference. Save result as modified
	 * reference
	 */
	private void modifyRef(String originalRefPath, String modifiedRefPath) throws IOException {
		File refFolder = new File(modifiedRefPath);
		if (!refFolder.exists()) {// if ref directory do not exist, make one
			refFolder.mkdirs();
		}
		File originalRefPathFile = new File(originalRefPath);
		String[] fileNames;
		fileNames = originalRefPathFile.list(new ExtensionFilter(new String[]{".txt", "fasta", "fa"}));
		for (String str : fileNames) {
			try (
					BufferedReader reader = new BufferedReader(new FileReader(originalRefPath + str));
					BufferedWriter writer = new BufferedWriter(new FileWriter(modifiedRefPath + str))
			) {
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
			}
		}
	}

	/**
	 * clean old data to release space in server
	 *
	 * @throws IOException
	 */
	private void cleanRootFolder(String jobId) throws IOException {
		File rootDirectory = new File(Constant.DISKROOTPATH);
		long rootFolderSize = FileUtils.sizeOfDirectory(rootDirectory) / 1024 / 1024;
		long excess = rootFolderSize - Constant.SPACETHRESHOLD;
		LOGGER.info(jobId + "\troot folder space occupation:\t" + rootFolderSize + "M");
		if (rootFolderSize >= Constant.SPACETHRESHOLD) { // if exceed threshold, clean
			File[] subFolders = rootDirectory.listFiles();
			assert subFolders != null;
			Arrays.sort(subFolders, new FileDateComparator()); // sort by date.
			for (File folder : subFolders) {
				if (folder.getName().startsWith(Constant.JOB_FOLDER_PREFIX) && folder.isDirectory() &&
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

	/**
	 * execute blat query
	 */
	private List<String> blatQuery(Constant constant) throws IOException, InterruptedException {
		File refFolder = new File(constant.originalRefPath);
		String[] files = refFolder.list();
		String blatQueryPath = constant.toolsPath + "BlatQuery/";
		for (String name : files) {
			LOGGER.info(constant.getJobID() + "\tstart blat query for " + name);
			List<String> cmdList = Arrays.asList(blatQueryPath + "/BlatQuery.sh", blatQueryPath, constant.refVersion,
					constant.originalRefPath, name);
			if (Utilities.callCMD(cmdList, new File(constant.coorFilePath), constant.logPath + "/blat.log") > 0) {
				throw new RuntimeException(
						"blat query error! Please double check your reference file. <br> blat logs:<br>" +
								Files.toString(new File(constant.logPath + "/blat.log"), Charsets.UTF_8)
										.replace("\n", "<br>"));
			}
			LOGGER.info(constant.getJobID() + "\tblat query is finished for " + name);
		}
		List<String> warnings = Utilities.convertPSLtoCoorPair(constant.coorFilePath, constant.coorFileName);
		LOGGER.info(constant.getJobID() + "\tblat result converted");
		warnings.forEach(LOGGER::info);
		return warnings;
	}

}
