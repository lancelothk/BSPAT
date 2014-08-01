<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet" type="text/css" href="style.css"/>
    <title>BSPAT</title>
</head>
<script language="javascript">
    function checkEmptyValue(id, msg) {
        if (document.getElementById(id).value == "") {
            window.alert(msg);
            return true;
        } else {
            return false;
        }
    }

    function validateInput() {
        if (checkEmptyValue("jobID", "Job id is empty!")) {
            return false;
        }
        return true;
    }
</script>
<%@ page import="BSPAT.Utilities, DataType.Constant" %>

<body>
<div id="container">
    <%@ include file="menu.html" %>
    <div id="content">
        <div id="content_top"></div>
        <div id="content_main">
            <form action="result.jsp" onsubmit="return validateInput()" enctype="multipart/form-data" method="get">
                Job ID: <label>
                <input type="text" id="jobID" name="jobID"/>
            </label> <input type="submit" value="Query"/>
            </form>

            <%
                String jobID = request.getParameter("jobID");
                if (jobID != null) {
                    if (jobID.toLowerCase().startsWith(Constant.JOB_FOLDER_PREFIX.toLowerCase())) {
                        jobID = jobID.substring(3);
                    }
                    Constant.DISKROOTPATH = this.getServletConfig().getServletContext().getRealPath("");
                    Constant constant;
                    try {
                        constant = Constant.readConstant(jobID);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Utilities.showAlertWindow(response, "can not find Result ID:\t" + jobID);
                        return;
                    }
                    if (constant == null) {
                        System.err.println("Can not load Result ID:\t" + jobID);
                        Utilities.showAlertWindow(response, "Can not load Result ID:\t" + jobID);
                        return;
                    }%>
            <table>
                <% if (constant.finishedMapping) { %>
                <tr>
                    <td>Job ID:</td>
                    <td><%=jobID %>
                    </td>
                </tr>
                <tr>
                    <td>Mapping Result</td>
                    <td>
                        <form action="resultRetrieve" enctype="multipart/form-data" method="post">
                            <input type="hidden" name="jobID" value="<%=jobID %>"/>
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
                            <input type="hidden" name="jobID" value="<%=jobID %>"/>
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
