package edu.cwru.cbc.BSPAT; /**
 * Created by kehu on 8/26/14.
 */

import edu.cwru.cbc.BSPAT.DataType.ExtensionFilter;
import edu.cwru.cbc.BSPAT.Servlet.AnalysisServlet;
import edu.cwru.cbc.BSPAT.Servlet.MappingServlet;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class IntegrationTest {
    private final static Logger LOGGER = Logger.getLogger(IntegrationTest.class.getName());
	public String rootPath = new File(".").getAbsolutePath();
	public final String testPath = rootPath + "/out/artifacts/BSPAT_war_exploded";
	public String jobID;

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
        }

        @Override
        protected void succeeded(Description description) {
            File resultFolder = new File(testPath + "/Job" + jobID);
            try {
                FileUtils.deleteDirectory(resultFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            LOGGER.info("test result cleaned!");
        }
    };

    class MyAnswer implements Answer<Object> {
        public edu.cwru.cbc.BSPAT.DataType.Constant resultConstant;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            String jobID = (String) args[1];
            edu.cwru.cbc.BSPAT.DataType.Constant constant = edu.cwru.cbc.BSPAT.DataType.Constant.readConstant(jobID);
            constant.targetFileName = constant.coorFileName;
            resultConstant = constant;
            FileUtils.copyFileToDirectory(new File(constant.coorFilePath + constant.coorFileName),
                                          new File(constant.targetPath));
            constant.writeConstant();
            return args[1];
        }
    }

    @Test
    public void testBSPAT() throws IOException, ServletException {
        jobID = testMapping();
        testAnalysis(jobID);
    }

    private String testMapping() throws ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn(testPath);
        when(request.getParameter("demo")).thenReturn("false");
        when(request.getParameter("test")).thenReturn("true");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/BSPAT/mapping"));
        when(request.getParameter("refVersion")).thenReturn("hg18");
        when(request.getParameter("qualsType")).thenReturn("phred33");
        when(request.getParameter("maxmis")).thenReturn("2");
        when(request.getParameter("email")).thenReturn("");
        when(request.getContextPath()).thenReturn("");
        MyAnswer myAnswer = new MyAnswer();
        doAnswer(myAnswer).when(request).setAttribute(anyString(), anyObject());
        when(request.getRequestDispatcher("mappingResult.jsp")).thenReturn(requestDispatcher);

        MappingServlet mappingServlet = new MappingServlet();
        mappingServlet.init(servletConfig);
        mappingServlet.doPost(request, response);

        verify(request, Mockito.times(1)).getRequestDispatcher("mappingResult.jsp");


        assertEquals("Sequences analysed in total", 1676, myAnswer.resultConstant.mappingSummary.getSeqAnalysed());
        assertEquals("Sequences with a unique best hit", 1584,
                     myAnswer.resultConstant.mappingSummary.getUniqueBestHit());
        assertEquals("Sequences without any alignment", 92, myAnswer.resultConstant.mappingSummary.getNoAlignment());
        assertEquals("Sequences did not map uniquely", 0, myAnswer.resultConstant.mappingSummary.getNotUnique());
        assertEquals("Invalid sequences", 0, myAnswer.resultConstant.mappingSummary.getNotExtracted());
        assertEquals("Mapping efficiency", "0.945",
                     myAnswer.resultConstant.mappingSummary.getMappingEfficiencyString());
        assertTrue("Mapping phase running time", myAnswer.resultConstant.mappingTime >= 1);

        assertNotNull("Reference coordinates", myAnswer.resultConstant.getAbsolutePathCoorFile());
        assertNotNull("Mapping result", myAnswer.resultConstant.mappingResultLink);

        return myAnswer.resultConstant.getJobID();
    }

    private void testAnalysis(String jobID) throws ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);

        // TODO using target coordinates, test CpG bounded methylation pattern

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn(testPath);
        when(request.getParameter("jobID")).thenReturn(jobID);
        when(request.getParameter("figureFormat")).thenReturn("png");
        when(request.getParameter("minp0text")).thenReturn("0.02");
        when(request.getParameter("criticalValue")).thenReturn("0.05");
        when(request.getParameter("mutationpatternThreshold")).thenReturn("0.2");
        when(request.getParameter("conversionRateThreshold")).thenReturn("0.9");
        when(request.getParameter("sequenceIdentityThreshold")).thenReturn("0.9");
        MyAnswer myAnswer = new MyAnswer();
        doAnswer(myAnswer).when(request).setAttribute(anyString(), anyObject());
        when(request.getRequestDispatcher("analysisResult.jsp")).thenReturn(requestDispatcher);

        AnalysisServlet analysisServlet = new AnalysisServlet();
        analysisServlet.init(servletConfig);
        analysisServlet.doPost(request, response);

        verify(request, Mockito.times(1)).getRequestDispatcher("analysisResult.jsp");

        assertEquals("Sequence number cover target region", 1584,
                     myAnswer.resultConstant.seqCountSummary.getSeqTargetBounded());
        assertEquals("Sequence number after filtering", 1540,
                     myAnswer.resultConstant.seqCountSummary.getSeqTargetAfterFilter());
        assertTrue("Analysis phase running time", myAnswer.resultConstant.analysisTime >= 1);

        assertNotNull("Anslysis result", myAnswer.resultConstant.analysisResultLink);

        assertEquals("Number of Experiments", 2, myAnswer.resultConstant.experiments.size());
        assertEquals("Number of Regions", 1, myAnswer.resultConstant.experiments.get(0).getReportSummaries().size());
        assertTrue("Has ASM pattern", myAnswer.resultConstant.experiments.get(0).getReportSummaries().get(0).hasASM());
        assertEquals("Number of Regions", 1, myAnswer.resultConstant.experiments.get(1).getReportSummaries().size());
        assertTrue("Has ASM pattern", myAnswer.resultConstant.experiments.get(1).getReportSummaries().get(0).hasASM());

        // check pattern result files
        File testFileFolder = new File("/home/kehu/IdeaProjects/BSPAT/testFiles");
        File resultFolder = new File(String.format("%s/Job%s/pattern_result/", testPath, jobID));
        checkResultFiles(testFileFolder, resultFolder);
    }

    private void checkResultFiles(File testFileFolder, File resultFolder){
        if (testFileFolder.exists() && resultFolder.exists()) {
            File[] files = testFileFolder.listFiles(new ExtensionFilter(".txt"));
            if (files != null) {
                for (File file : files) {
                    junitx.framework.FileAssert.assertEquals(file.getName(), file, new File(
                            resultFolder.getAbsolutePath() + "/" + file.getName()));
                }
            }
        }
    }
}
