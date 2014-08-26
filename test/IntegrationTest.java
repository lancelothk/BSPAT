/**
 * Created by kehu on 8/26/14.
 */

import Servlet.AnalysisServlet;
import Servlet.MappingServlet;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
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

import static org.mockito.Mockito.*;


public class IntegrationTest {

    class MyAnswer implements Answer<Object> {
        public String jobID;

        public String getJobID() {
            return jobID;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            DataType.Constant constant = (DataType.Constant) args[1];
            constant.targetFileName = constant.coorFileName;
            jobID = constant.jobID;
            FileUtils.copyFileToDirectory(new File(constant.coorFilePath + constant.coorFileName),
                                          new File(constant.targetPath));
            constant.writeConstant();
            return args[1];
        }
    }

    @Test
    public void testBSPAT() throws IOException, ServletException {
//        String jobID = testMapping();
        testAnalysis("7477341046127505759");

//        verify(request, atLeast(1)).getParameter("username"); // only if you want to verify username was called...
    }

    private String testMapping() throws ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn("/home/kehu/IdeaProjects/BSPAT/out/artifacts/BSPAT_exploded");

        when(request.getParameter("demo")).thenReturn("true");
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

        return myAnswer.jobID;
    }

    private void testAnalysis(String jobID) throws ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getRealPath("")).thenReturn("/home/kehu/IdeaProjects/BSPAT/out/artifacts/BSPAT_exploded");

        when(request.getParameter("jobID")).thenReturn(jobID);
        when(request.getParameter("figureFormat")).thenReturn("png");
        when(request.getParameter("minp0text")).thenReturn("0.02");
        when(request.getParameter("criticalValue")).thenReturn("0.05");
        when(request.getParameter("mutationpatternThreshold")).thenReturn("0.2");
        when(request.getParameter("conversionRateThreshold")).thenReturn("0.9");
        when(request.getParameter("sequenceIdentityThreshold")).thenReturn("0.9");
        when(request.getRequestDispatcher("analysisResult.jsp")).thenReturn(requestDispatcher);

        AnalysisServlet analysisServlet = new AnalysisServlet();
        analysisServlet.init(servletConfig);
        analysisServlet.doPost(request, response);
    }

}
