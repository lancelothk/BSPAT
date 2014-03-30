package web;

import BSPAT.CallBismark;
import BSPAT.IO;
import BSPAT.Utilities;
import DataType.*;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        handleUploadedFiles(request, response, experiments);

        long start = System.currentTimeMillis();
        // set other parameters
        constant.refVersion = request.getParameter("refVersion");
        constant.coorFileName = "coordinates";
        constant.qualsType = request.getParameter("qualsType");
        constant.maxmis = Integer.valueOf(request.getParameter("maxmis"));
        constant.experiments = experiments;
        constant.email = request.getParameter("email");
        // execute BLAT query
        blatQuery();
        // fetch extended reference sequence
        try {
            fetchRefSeq(Constant.REFEXTENSIONLENGTH);
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
            System.err.println("fetch ref seq fails!");
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

    private void handleUploadedFiles(HttpServletRequest request, HttpServletResponse response,
                                     List<Experiment> experiments) throws IOException, ServletException {
        Collection<Part> parts = request.getParts(); // get submitted data
        // for each part, add it into corresponding experiment
        boolean refReady = false;
        for (Part part : parts) {
            String fieldName = Utilities.getField(part, "name");
            String fileName = Utilities.getField(part, "filename");
            if (fieldName.equals("ref")) { // deal with uploaded ref file
                File refFolder = new File(constant.originalRefPath);
                if (!refFolder.isDirectory()) { // if ref directory do not
                    // exist, make one
                    refFolder.mkdirs();
                }
                // save ref file in ref folder
                if (IO.saveFileToDisk(part, refFolder.getAbsolutePath(), fileName)) {
                    refReady = true;
                } else {
                    Utilities.showAlertWindow(response, "reference file is blank!");
                    return;
                }
            } else if (fieldName.startsWith("seqFile")) { // deal with uploaded
                // seq file
                int seqFileIndex = Integer.valueOf(part.getName().replace("seqFile", ""));
                for (Experiment experiment : experiments) { // match file index
                    // and seq file
                    // index
                    if (experiment.getIndex() == seqFileIndex) {
                        experiment.setSeqFile(fileName);
                        File seqFolder = new File(constant.seqsPath + experiment.getName());
                        if (!seqFolder.isDirectory()) { // if sequence directory
                            // do not exist, make
                            // one
                            seqFolder.mkdirs();
                        }
                        if (IO.saveFileToDisk(part, seqFolder.getAbsolutePath(), fileName)) {
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
     * fetch extended reference sequence from UCSC DAS server
     * @param refExtensionLength Extension length
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void fetchRefSeq(int refExtensionLength) throws IOException, ParserConfigurationException, SAXException {
        Map<String, Coordinate> coordinatesMap = IO.readCoordinates(constant.coorFilePath, constant.coorFileName);
        StringBuilder dasQuery = new StringBuilder("http://genome.ucsc.edu/cgi-bin/das/" + constant.refVersion + "/dna?");
        // build query url
        List<String> coordinateKeyList = new ArrayList<>();
        for (String key : coordinatesMap.keySet()) {
            coordinateKeyList.add(key);
            Coordinate coordinate = coordinatesMap.get(key);
            dasQuery.append(String.format("segment=%s:%d,%d;", coordinate.getChr(), coordinate.getStart() - refExtensionLength, coordinate.getEnd() + refExtensionLength));
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new URL(dasQuery.toString()).openStream());
        doc.getDocumentElement().normalize();
        NodeList seqList = doc.getElementsByTagName("SEQUENCE");
        for (int i = 0; i < seqList.getLength(); i++) {
            Element item = (Element) seqList.item(i);
            coordinatesMap.get(coordinateKeyList.get(i)).setRefSeq(item.getElementsByTagName("DNA").item(0).getTextContent().replace("\n", ""));
        }
        writeRef(coordinatesMap, constant.modifiedRefPath);
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
        constant.originalRefPath = constant.randomDir + "/origianlRef/";
        IO.createFolder(constant.originalRefPath);
        constant.modifiedRefPath = constant.randomDir + "/modifiedRef/";
        IO.createFolder(constant.modifiedRefPath);
        constant.seqsPath = constant.randomDir + "/seqs/";
        IO.createFolder(constant.seqsPath);
        constant.toolsPath = Constant.DISKROOTPATH + "/tools/";
        IO.createFolder(constant.toolsPath);
        URL domain = new URL(request.getRequestURL().toString());
        constant.host = domain.getHost() + ":" + domain.getPort();
    }

    /**
     * write modified reference into file
     * @param coordinateMap Key is ref seq ID, Value is Coordinate object
     * @param modifiedRefPath
     * @throws IOException
     */

    private void writeRef(Map<String, Coordinate> coordinateMap, String modifiedRefPath) throws IOException {
        File refFolder = new File(modifiedRefPath);
        if (!refFolder.isDirectory()) {// if ref directory do not exist, make one
            refFolder.mkdirs();
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(modifiedRefPath + "modifiedReference.fa"));
        for (Coordinate coordinate : coordinateMap.values()) {
            writer.write(String.format(">%s\n%s\n", coordinate.getId(), coordinate.getRefSeq().toUpperCase()));
        }
        writer.close();
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
                    if (folder.getName().startsWith("Run") && folder.isDirectory() && folder != subFolders[subFolders.length - 1]) {
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
                String blatQuery = String.format("%sBlatQuery.sh %s %s %s %s", blatQueryPath, blatQueryPath, constant.refVersion, constant.originalRefPath, name);
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
