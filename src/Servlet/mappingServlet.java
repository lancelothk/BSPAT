package Servlet;

import BSPAT.CallBismark;
import BSPAT.IO;
import BSPAT.Utilities;
import DataType.*;
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
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
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
			modifyRef(constant.originalRefPath, constant.modifiedRefPath);

			// bismark indexing
			CallBismark callBismark = null;
			callBismark = new CallBismark(constant.modifiedRefPath, constant.toolsPath, constant.qualsType,
										  constant.maxmis);

			// multiple threads to execute bismark mapping
			ExecutorService executor = Executors.newCachedThreadPool();
			for (Experiment experiment : constant.experiments) {
				executor.execute(new ExecuteMapping(callBismark, experiment.getName(), constant));
			}
			executor.shutdown();
			// Wait until all threads are finish
			executor.awaitTermination(Constant.MAXEXECUTIONDAY, TimeUnit.DAYS);

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
			Utilities.sendEmail(constant.email, constant.jobID,
								"Mapping has finished.\n" + "Your jobID is " + constant.jobID +
										"\nPlease go to cbc.case.edu/BSPAT/result.jsp to retrieve your result.");
			//redirect page
			request.setAttribute("constant", constant);
			request.getRequestDispatcher("mappingResult.jsp").forward(request, response);
		} catch (UserNoticeException e) {
			e.printStackTrace();
			throw e;
		} catch (InterruptedException | ServletException | IOException | MessagingException | RuntimeException e) {
			e.printStackTrace();
			throw new UserNoticeException("Server error!");
		}
	}

	private void addExperiment(HttpServletRequest request, HttpServletResponse response, List<Experiment> experiments) {
		// add each experiment in list
		String experimentName;
		int index = 1;
		while ((experimentName = request.getParameter("experiment" + index)) != null) {
			experiments.add(new Experiment(index, experimentName));
			index++; // separate ++ operation makes code clear.
		}
	}

	private void handleUploadedFiles(HttpServletRequest request, HttpServletResponse response,
									 List<Experiment> experiments) {
		Collection<Part> parts = null; // get submitted data
		try {
			parts = request.getParts();
		} catch (IOException | ServletException e) {
			throw new UserNoticeException("Network error!");
		}
		// for each part, add it into corresponding experiment
		for (Part part : parts) {
			String fieldName = Utilities.getField(part, "name");
			String fileName = Utilities.getField(part, "filename");
			if (fieldName.equals("ref")) { // deal with uploaded ref file
				// save ref file in ref folder
				IO.saveFileToDisk(part, constant.originalRefPath, fileName);
			} else if (fieldName.startsWith("seqFile")) { // deal with uploaded seq file
				int seqFileIndex = Integer.valueOf(part.getName().replace("seqFile", ""));
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

	private final SecureRandom random = new SecureRandom();

	/**
	 * generate random suffix folder with given path and prefix.
	 *
	 * @param path
	 * @param prefix
	 * @return random folder
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
		if (!refFolder.exists()) {// if ref directory do not exist, make one
			refFolder.mkdirs();
		}
		File originalRefPathFile = new File(originalRefPath);
		String[] fileNames;
		fileNames = originalRefPathFile.list(new ExtensionFilter(new String[]{".txt", "fasta", "fa"}));
		for (String str : fileNames) {
			try (
					BufferedReader reader = new BufferedReader(new FileReader(originalRefPath + str));
					BufferedWriter writer = new BufferedWriter(new FileWriter(modifiedRefPath + str));
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
	}

	/**
	 * execute blat query
	 */
	private void blatQuery() throws IOException, InterruptedException {
		File refFolder = new File(constant.originalRefPath);
		String[] files = refFolder.list();
		String blatQueryPath = constant.toolsPath + "BlatQuery/";
		for (String name : files) {
			System.out.println("start blat query for " + name);
			String blatQuery = String.format("%sBlatQuery.sh %s %s %s %s", blatQueryPath, blatQueryPath,
											 constant.refVersion, constant.originalRefPath, name);
			Utilities.callCMD(blatQuery, new File(constant.coorFilePath), null);
			System.out.println("blat query is finished for " + name);
		}
		Utilities.convertPSLtoCoorPair(constant.coorFilePath, constant.coorFileName, constant.refVersion);
		constant.coorReady = true;
		System.out.println("blat result converted");
	}

}
