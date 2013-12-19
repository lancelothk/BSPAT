<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<link rel="stylesheet" type="text/css" href="style.css" />
<title>BS-PAT</title>
</head>

<body>
	<div id="container">
		<%@ include file="menu.html"%>
		<div id="content">
			<div id="content_top"></div>
			<div id="content_main">
				<h2>Manual</h2>
				<table>
					<tr>
						<td>This manual is divided into four parts:</td>
					</tr>
					<tr>
						<td>1. <a href="quickGuide.jsp">Quick Guide</a>;</td>
					</tr>
					<tr>
						<td>2. <a href="manualMapping.jsp">Mapping phase</a>;</td>
					</tr>
					<tr>
						<td>3. <a href="manualAnalysis.jsp">Analysis phase</a>;</td>
					</tr>
					<tr>
						<td>4. <a href="manualResult.jsp">Result phase</a>;</td>
					</tr>
				</table>
			</div>
			<div id="content_bottom"></div>
		</div>
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
