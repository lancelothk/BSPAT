<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
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
				<h2>Step 2: Analysis:</h2>
				<p class="dottedline"></p>
				<img id="loader" src="images/progress.gif" alt="progress" style="display: none;"  />
			<form action="analysis" enctype="multipart/form-data" method="post">
				<input type="hidden" name="runID" value="<c:out value="${constant.runID}" />">
				<table id="analysisTable">
					<tr><td>Mapping Summary</td></tr>
					<tr><td>Experiment(<c:out value="${constant.experiments.size()}" />):</td>
					<c:forEach items="${constant.experiments}" var="experiment">
						<td><c:out value="${experiment.getName()}" /></td>
						</c:forEach>
					</tr>
					<tr><td>mapping part running time:</td><td><c:out value="${constant.mappingTime}" />s</td></tr>
					<tr><td>Result ID: </td><td><c:out value="${constant.runID}" /></td></tr>
					<tr><td>Mapping result: </td><td><a href=<c:out value="${constant.mappingResultLink}" />>mappingResult.zip</a></td></tr>
				</table>
				<p class="dottedline"></p>
				<table id="parameters">
					<tr><td>Analysis Parameters: </td><td>Values must be in (0,1)</td></tr>
					<tr><td>Bisulfite conversion rate threshold</td><td><input type="text" name="conversionRateThreshold" value=0.95 /></td></tr>
					<tr><td>Sequence identity threshold</td><td><input type="text" name="sequenceIdentityThreshold" value=0.9 /></td></tr>
					<tr><td><input type="radio" name="par" id="minp0" value="minp0" checked="yes" onclick="check()" />P0 threshold</td>
						<td><input type="text" id="minp0text" name="minp0text" value=0.02 /></td></tr>
					<tr><td><input type="radio" name="par" id="minmethyl" value="minmethyl" onclick="check()"  />Methylation pattern threshold</td>
						<td><input type="text" id="minmethyltext" name="minmethyltext" value=0.1 disabled="true" /></td></tr>
					<tr><td>Mutation pattern threshold</td><td><input type="text" name="mutationpatternThreshold" value=0.3 /></td></tr>
					<tr><td>Figure Format</td><td><select name="figureFormat"><option value="png">PNG</option><option value="eps">EPS</option></select></td></tr>
					<tr><td><input type="submit" value="Execute" onclick="document.getElementById('loader').style.display = 'block';document.getElementById('analysisTable').style.display = 'none';document.getElementById('parameters').style.display = 'none';setFooter();" /></td></tr>
				</table>
			</form>
			</div>
			<div id="content_bottom"></div>
		</div><%@ include file="../footer.html"%>
	</div>
</body>

<script language="javascript">
function check(){
	if(document.getElementById('minp0').checked) {
		document.getElementById('minp0text').disabled = false;
		document.getElementById('minmethyltext').disabled = true;
	}else if(document.getElementById('minmethyl').checked) {
		document.getElementById('minp0text').disabled = true;
		document.getElementById('minmethyltext').disabled = false;
	}
}
</script>
</html>