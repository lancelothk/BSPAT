package web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import BSPAT.Utilities;
import DataType.Constant;

/**
 * Servlet implementation class resultRetrieveServlet
 */
@WebServlet(name = "/resultRetrieve", urlPatterns = { "/resultRetrieve" })
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
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		response.setContentType("text/html");
		String rootPath = this.getServletContext().getRealPath("");
		Constant.WEBAPPFOLDER = rootPath;
		String runID = request.getParameter("runID");
		constant = Constant.readConstant(runID);
		if (constant == null){
			Utilities.showAlertWindow(response, "Can not read result");
			return;
		}
		boolean found = false;
		if (!runID.equals("")) {
			File rootFolder = new File(rootPath);
			String[] folders = rootFolder.list();
			for (String folder : folders) {
				if (folder.endsWith(runID)) {
					FileWriter jspWriter = new FileWriter(constant.randomDir + "/resultRetrieve.jsp");
					BufferedWriter jspBufferedWriter = new BufferedWriter(jspWriter);
					File mappingResult = new File(rootPath + "/" + folder + "/mappingResult.jsp");
					File analysisResult = new File(rootPath + "/" + folder + "/analysisResult.jsp");
					
					jspBufferedWriter.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">");
					jspBufferedWriter.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"../style.css\" />");
					jspBufferedWriter.write("<div id=\"container\">");
					jspBufferedWriter.write("<%@ include file=\"../menu.html\"%>");
					jspBufferedWriter
							.write("<div id=\"content\"><div id=\"content_top\"></div><div id=\"content_main\">");

					// result retrieve option
					jspBufferedWriter
							.write("<form action=\"../resultRetrieve\" enctype=\"multipart/form-data\" method=\"post\">");
					jspBufferedWriter.write("Run ID: <input type=\"text\" name=\"runID\" value=\"" + runID
							+ "\" /> <input type=\"submit\" value=\"Submit\" /></form></br>");

					if (mappingResult.exists()) {
						// write mapping result link
						String mappingRelativePath = mappingResult.getAbsolutePath().replace(
								constant.diskRootPath.toString(), constant.webRootPath);
						jspBufferedWriter.write("<a href=\"" + mappingRelativePath + "\" >Mapping Result</a></br>");
					}
					if (analysisResult.exists()) {
						String analysisRelativePath = analysisResult.getAbsolutePath().replace(
								constant.diskRootPath.toString(), constant.webRootPath);
						jspBufferedWriter.write("<a href=\"" + analysisRelativePath + "\" >Analysis Result</a></br>");
					}
					jspBufferedWriter
							.write("</div><div id=\"content_bottom\"></div></div><%@ include file=\"../footer.html\"%></div>");
					jspBufferedWriter.close();
					response.sendRedirect(constant.randomDir.replace(constant.diskRootPath.toString(),
							constant.webRootPath) + "/resultRetrieve.jsp");
					found = true;
				} else {
					continue;
				}
			}
			if (found == false){
				Utilities.showAlertWindow(response, "ID does not exist!");
			}
		}else {
			Utilities.showAlertWindow(response, "ID is empty!");
		}
	}

}
