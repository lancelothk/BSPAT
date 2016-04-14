<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet" type="text/css" href="style.css"/>
    <title>BSPAT</title>
</head>

<body>
<div id="container">
    <%@ include file="menu.html" %>
    <div id="content">
        <div id="content_top"></div>
        <div id="content_main">
            <h2>Manual</h2>

            <p class="dottedline"></p>
            <table>
                <tr>
                    <td>This manual is divided into three parts:</td>
                </tr>
                <tr>
                    <td>1. <a href="manualMapping.jsp">Mapping</a></td>
                </tr>
                <tr>
                    <td>2. <a href="manualAnalysis.jsp">Analysis</a></td>
                </tr>
                <tr>
                    <td>3. <a href="manualResult.jsp">Result</a></td>
                </tr>
                <tr>
                    <td>Also we have a one-click <a href="demo.jsp">demo</a> to show the workflow of BSPAT.</td>
                </tr>
            </table>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
</body>
</html>
