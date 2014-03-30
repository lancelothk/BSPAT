<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet" type="text/css" href="style.css"/>
    <title>BSPAT</title>
</head>

<%@ page import="BSPAT.Utilities, DataType.Constant" %>
<%@ page import="java.io.IOException" %>

<body>
<div id="container">
    <%@ include file="menu.html" %>
    <div id="content">
        <div id="content_top"></div>
        <div id="content_main">
            <form action="result.jsp" enctype="multipart/form-data" method="get">
                Result ID: <label>
                <input type="text" name="runID"/>
            </label> <input type="submit" value="Query"/>
            </form>

            <%
                String runID = request.getParameter("runID");
                if (runID != null) {
                    if (runID.startsWith("Run") || runID.startsWith("run")) {
                        runID = runID.substring(3);
                    }
                    Constant.DISKROOTPATH = this.getServletConfig().getServletContext().getRealPath("");
                    Constant constant;
                    try {
                        constant = Constant.readConstant(runID);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Utilities.showAlertWindow(response, "can not find Result ID:\t" + runID);
                        return;
                    }
                    if (constant == null) {
                        Utilities.showAlertWindow(response, "Can not read result");
                        return;
                    }%>
            <table>
                <% if (constant.finishedMapping) { %>
                <tr>
                    <td>Run ID:</td>
                    <td><%=runID %>
                    </td>
                </tr>
                <tr>
                    <td>Mapping Result</td>
                    <td>
                        <form action="resultRetrieve" enctype="multipart/form-data" method="post">
                            <input type="hidden" name="runID" value="<%=runID %>"/>
                            <input type="hidden" name="mPage" value="mappingResult.jsp"/>
                            <input type="submit" value="Check"/>
                        </form>
                    </td>
                </tr>
                <% if (constant.finishedAnalysis) { %>
                <tr>
                    <td>Analysis Result</td>
                    <td>
                        <form action="resultRetrieve" enctype="multipart/form-data" method="post">
                            <input type="hidden" name="runID" value="<%=runID %>"/>
                            <input type="hidden" name="aPage" value="analysisResult.jsp"/>
                            <input type="submit" value="Check"/>
                        </form>
                    </td>
                </tr>
                <% }%>
                <% }%>
            </table>
            <% }%>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
</body>
</html>
