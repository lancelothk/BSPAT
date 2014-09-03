package Servlet;

import BSPAT.Utilities;
import DataType.Constant;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet implementation class resultRetrieveServlet
 */
@WebServlet(name = "/resultRetrieve", urlPatterns = {"/resultRetrieve"})
@MultipartConfig
public class resultRetrieveServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Constant constant = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public resultRetrieveServlet() {
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
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        Constant.DISKROOTPATH = this.getServletContext().getRealPath("");
        String jobID = request.getParameter("jobID");
        try {
            constant = Constant.readConstant(jobID);
        } catch (IOException e) {
            Utilities.showAlertWindow(response, "can not find Result ID:\t" + jobID);
            return;
        }
        if (constant != null) {
            String mPage = request.getParameter("mPage");
            String aPage = request.getParameter("aPage");
            if (mPage != null) {
                request.setAttribute("constant", constant);
                request.getRequestDispatcher("mappingResult.jsp").forward(request, response);
                return;
            }
            if (aPage != null) {
                request.setAttribute("constant", constant);
                request.getRequestDispatcher("analysisResult.jsp").forward(request, response);
                return;
            }
            throw new RuntimeException("both mPage and aPage are empty!");
        } else {
            System.err.println("Can not load Result ID:\t" + jobID);
            Utilities.showAlertWindow(response, "Can not load Result ID:\t" + jobID);
            return;
        }
    }

}
