<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
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
            <h2>Step 2: Analysis:</h2>

            <p class="dottedline"></p>
            <img id="loader" src="images/progress.gif" alt="progress"
                 style="display: none;"/>

            <form action="analysis" onsubmit="return validateInput()" enctype="multipart/form-data" method="post">
                <input type="hidden" name="jobID"
                       value="<c:out value="${constant.jobID}" />">
                <table id="analysisTable">
                    <tr>
                        <td>Mapping Summary<sup><a
                                href="manualAnalysis.jsp#mappingSummary">?</a></sup></td>
                    </tr>
                    <tr>
                        <td>Result ID:</td>
                        <td><c:out value="${constant.jobID}"/></td>
                    </tr>
                    <tr>
                        <td>Experiment(<c:out value="${constant.experiments.size()}"/>):
                        </td>
                        <td><c:forEach items="${constant.experiments}"
                                       var="experiment">
                            <c:out value="${experiment.getName()}"/>&nbsp;&nbsp;
                        </c:forEach></td>
                    </tr>
                    <tr>
                        <td>Sequences analysed in total:</td>
                        <td><c:out value="${constant.mappingSummary.getSeqAnalysed()}"/></td>
                    </tr>
                    <tr>
                        <td>Sequences with a unique best hit:</td>
                        <td><c:out value="${constant.mappingSummary.getUniqueBestHit()}"/></td>
                    </tr>
                    <tr>
                        <td>Sequences without any alignment:</td>
                        <td><c:out value="${constant.mappingSummary.getNoAlignment()}"/></td>
                    </tr>
                    <tr>
                        <td>Sequences did not map uniquely:</td>
                        <td><c:out value="${constant.mappingSummary.getNotUnique()}"/></td>
                    </tr>
                    <tr>
                        <td>Invalid sequences:</td>
                        <td><c:out value="${constant.mappingSummary.getNotExtracted()}"/></td>
                    </tr>
                    <tr>
                        <td>Mapping efficiency:</td>
                        <td><c:out value="${constant.mappingSummary.getMappingEfficiencyString()}"/></td>
                    </tr>
                    <tr>
                        <td>Mapping phase running time:</td>
                        <td><c:out value="${constant.mappingTime}"/>s</td>
                    </tr>
                    <tr>
                        <td>Reference coordinates:</td>
                        <td><a href=<c:out value="${constant.getAbsolutePathCoorFile()}"/>>coordinates.coor</a>
                        </td>
                    </tr>
                    <tr>
                        <td>Mapping result:</td>
                        <td><a href=<c:out value="${constant.mappingResultLink}"/>>mappingResult.zip</a></td>
                    </tr>
                </table>
                <p class="dottedline"></p>
                <table id="parameters">
                    <tr>
                        <td>Analysis Parameters</td>
                    </tr>
                    <tr>
                        <td>Target Coordinates:<sup><a href="manualAnalysis.jsp#targetCoordinates">?</a></sup></td>
                        <td><input type="file" name="target" id="target" multiple/></td>
                    </tr>
                    <tr>
                        <td>Bisulfite conversion rate:<sup><a
                                href="manualAnalysis.jsp#conversionRate">?</a></sup></td>
                        <td><label>
                            <input type="text" id="conversionRateThreshold" name="conversionRateThreshold"
                                   value=0.9>
                        </label></td>
                    </tr>
                    <tr>
                        <td>Sequence identity:<sup><a
                                href="manualAnalysis.jsp#seqIdentity">?</a></sup></td>
                        <td><label>
                            <input type="text" id="sequenceIdentityThreshold" name="sequenceIdentityThreshold"
                                   value=0.9>
                        </label></td>
                    </tr>
                    <tr>
                        <td><label for="minp0"></label><input type="radio" name="par" id="minp0" value="minp0"
                                                              checked="yes" onclick="check()"/>&alpha; threshold:<sup><a
                                href="manualAnalysis.jsp#alphaThreshold">?</a></sup></td>
                        <td><label for="minp0text"></label>
                            <input type="text" id="minp0text" name="minp0text" value=0.02></td>
                    </tr>
                    <tr>
                        <td>&nbsp;&nbsp;&nbsp;Critical Value:<sup><a
                                href="manualAnalysis.jsp#alphaThreshold">?</a></sup></td>
                        <td><label for="criticalValue"></label><input type="text" id="criticalValue"
                                                                      name="criticalValue" value=0.05></td>
                    </tr>
                    <tr>
                        <td><label for="minmethyl"></label><input type="radio" name="par" id="minmethyl"
                                                                  value="minmethyl" onclick="check()"/>Methylation
                            pattern
                            threshold:<sup><a
                                    href="manualAnalysis.jsp#methylPatternThreshold">?</a></sup></td>
                        <td><label for="minmethyltext"></label><input type="text" id="minmethyltext"
                                                                      name="minmethyltext" value=0.1 disabled="true"/>
                        </td>
                    </tr>
                    <tr>
                        <td>Mutation threshold:<sup><a
                                href="manualAnalysis.jsp#mutationPatternThreshold">?</a></sup></td>
                        <td><label>
                            <input type="text" id="mutationpatternThreshold" name="mutationpatternThreshold"
                                   value=0.2>
                        </label></td>
                    </tr>
                    <tr>
                        <td>Figure Format:<sup><a
                                href="manualAnalysis.jsp#figureFormat">?</a></sup></td>
                        <td><label>
                            <select name="figureFormat">
                                <option value="png">PNG</option>
                                <option value="eps">EPS</option>
                            </select>
                        </label></td>
                    </tr>
                    <tr>
                        <td><input type="submit" value="Execute"/>
                        </td>
                    </tr>
                </table>
            </form>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
<script language="javascript" type="" src="inputValidation.js"></script>
<script src="jquery-1.11.1.min.js"></script>
<script language="javascript" type="">
    function check() {
        if (document.getElementById('minp0').checked) {
            document.getElementById('minp0text').disabled = false;
            document.getElementById('criticalValue').disabled = false;
            document.getElementById('minmethyltext').disabled = true;
        } else if (document.getElementById('minmethyl').checked) {
            document.getElementById('minp0text').disabled = true;
            document.getElementById('criticalValue').disabled = true;
            document.getElementById('minmethyltext').disabled = false;
        }
    }

    function validateInput() {
        removeErrorMsg();
        var valid = true;
        if (containBlank("target")) {
            valid = false;
        }
        if (isEmptyValue("conversionRateThreshold", "Bisulfite conversion rate is empty!")
                || isInvalidValue("conversionRateThreshold", "Bisulfite conversion rate is invalid!")) {
            valid = false;
        }
        if (isEmptyValue("sequenceIdentityThreshold", "Sequence identity is empty!")
                || isInvalidValue("sequenceIdentityThreshold", "Sequence identity is invalid!")) {
            valid = false;
        }
        if (isEmptyValue("minp0text", "α threshold is empty!")
                || isInvalidValue("minp0text", "α threshold is invalid!")) {
            valid = false;
        }
        if (isEmptyValue("criticalValue", "Critical Value is empty!")
                || isInvalidValue("criticalValue", "Critical Value is invalid!")) {
            valid = false;
        }
        if (isEmptyValue("minmethyltext", "Methylation pattern threshold is empty!")
                || isInvalidValue("minmethyltext", "Methylation pattern threshold is invalid!")) {
            valid = false;
        }
        if (isEmptyValue("mutationpatternThreshold", "Mutation threshold is empty!")
                || isInvalidValue("mutationpatternThreshold", "Mutation threshold is invalid!")) {
            valid = false;
        }
        if (valid == true) {
            resetPage();
        }
        return valid;
    }

    function resetPage() {
        document.getElementById('loader').style.display = 'block';
        document.getElementById('analysisTable').style.display = 'none';
        document.getElementById('parameters').style.display = 'none';
        setFooter();
    }
    window.onload = check();
    window.onunload = function () {
    };
</script>
</body>
</html>