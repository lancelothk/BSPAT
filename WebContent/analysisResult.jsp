<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet" type="text/css" href="style.css"/>
    <script language="javascript">
        function check() {
            if (document.getElementById('minp0').checked) {
                document.getElementById('minp0text').disabled = false;
                document.getElementById('minmethyltext').disabled = true;
            } else if (document.getElementById('minmethyl').checked) {
                document.getElementById('minp0text').disabled = true;
                document.getElementById('minmethyltext').disabled = false;
            }
        }
    </script>
    <title>BSPAT</title>
</head>

<body>
<div id="container">
    <%@ include file="menu.html" %>
    <div id="content">
        <div id="content_top"></div>
        <div id="content_main">
            <table id="resultSummary">
                <tr>
                    <td>Analysis Result Summary<sup><a href="manualResult.jsp#resultSummary">?</a></sup></td>
                </tr>
                <tr>
                    <td>Result ID:</td>
                    <td><c:out value="${constant.runID}"/></td>
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
                    <td>Bisulfite conversion rate:</td>
                    <td><c:out value="${constant.getConversionRateThreshold()}"/></td>
                </tr>
                <tr>
                    <td>Sequence identity:</td>
                    <td><c:out value="${constant.sequenceIdentityThreshold}"/></td>
                </tr>
                <tr>
                    <td>Sequence number before filtering:</td>
                    <td><c:out value="${constant.seqCountSummary.getSeqBeforeFilter()}"/></td>
                </tr>
                <tr>
                    <td>Sequence number after filtering:</td>
                    <td><c:out value="${constant.seqCountSummary.getSeqAfterFilter()}"/></td>
                </tr>
                <c:choose>
                    <c:when test="${constant.minP0Threshold != -1}">
                        <tr>
                            <td>&alpha; threshold:</td>
                            <td><c:out value="${constant.minP0Threshold}"/></td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:when test="${constant.minMethylThreshold != -1}">
                            <tr>
                                <td>methylation pattern threshold:</td>
                                <td><c:out value="${constant.minMethylThreshold}"/></td>
                            </tr>
                        </c:when>
                    </c:otherwise>
                </c:choose>
                <tr>
                    <td>Mutation pattern threshold:</td>
                    <td><c:out value="${constant.mutationPatternThreshold}"/></td>
                </tr>
                <tr>
                    <td>Analysis running time:</td>
                    <td><c:out value="${constant.analysisTime}"/>s</td>
                </tr>
                <tr>
                    <td>Zipped analysis result:</td>
                    <td><a href=<c:out value="${constant.analysisResultLink}"/>>analysisResult.zip</a></td>
                </tr>
            </table>
            <p class="dottedline"></p>
            <c:forEach items="${constant.experiments}" var="experiment">
                <table id="analysisResult">
                    <tr>
                        <td>Experiment:<sup><a href="manualResult.jsp#experimentResult">?</a></sup></td>
                        <td><c:out value="${experiment.getName()}"/></td>
                    </tr>
                    <c:forEach items="${experiment.getReportSummaries()}"
                               var="reportSummary">
                        <tr>
                            <td>Region:</td>
                            <td><c:out value="${reportSummary.getId()}"/></td>
                        </tr>
                        <c:forEach items="${reportSummary.getPatternLinks()}"
                                   var="patternLink">
                            <tr>
                                <td><c:out value="${patternLink.getPatternType()}"/></td>
                                <td><a
                                        href=<c:out value="${patternLink.getTextResultLink()}"/>>text</a></td>
                                <c:choose>
                                    <c:when test="${constant.coorReady == true}">
                                        <td><a
                                                href="<c:out value="${patternLink.getFigureResultLink()}"/>.<c:out value="${constant.figureFormat}" />"><c:out
                                                value="${constant.figureFormat}"/></a></td>
                                        <td><a
                                                href="http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg18&position=chr7&hgt.customText=http://<c:out value="${patternLink.getGBResultLink()}"/>">GenomeBrowser</a>
                                        </td>
                                    </c:when>
                                </c:choose>
                            </tr>
                        </c:forEach>
                        <tr>
                            <c:choose>
                                <c:when
                                        test="${constant.coorReady == true && reportSummary.hasASM == true}">
                                    <td>ASM pattern</td>
                                    <c:choose>
                                        <c:when test="${reportSummary.getASMsnp() != null}">
                                            <td><a
                                                    href="http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs=<c:out value="${reportSummary.getASMsnp().getrsID()}"/>">SNP</a>
                                            </td>
                                        </c:when>
                                    </c:choose>
                                    <td><a
                                            href="<c:out value="${reportSummary.getASMFigureLink()}"/>.<c:out value="${constant.figureFormat}"/>"><c:out
                                            value="${constant.figureFormat}"/></a></td>
                                    <td><a
                                            href="http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg18&position=chr7&hgt.customText=http://<c:out value="${reportSummary.getASMGBLink()}"/>">GenomeBrowser</a>
                                    </td>
                                </c:when>
                            </c:choose>
                        </tr>
                        <tr>
                            <td>Other Info</td>
                            <td><a
                                    href=<c:out value="${reportSummary.getStatTextLink()}"/>>text</a></td>
                        </tr>
                    </c:forEach>
                </table>
            </c:forEach>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="../footer.html" %>
</div>
</body>
</html>