<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet" type="text/css" href="style.css"/>
    <script language="javascript" type="">
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
<%@ page import="edu.cwru.cbc.BSPAT.DataType.Constant, edu.cwru.cbc.BSPAT.DataType.Experiment" %>
<%@ page import="edu.cwru.cbc.BSPAT.DataType.PatternLink" %>
<%@ page import="edu.cwru.cbc.BSPAT.core.ReportSummary" %>
<%@ page import="edu.cwru.cbc.BSPAT.core.Utilities" %>
<body>
<div id="container">
    <%@ include file="menu.html" %>
    <div id="content">
        <div id="content_top"></div>
        <div id="content_main">
            <%
                Constant constant = null;
                String jobID = (String) request.getAttribute("jobID");
                if (jobID != null) {
                    if (jobID.toLowerCase().startsWith(Constant.JOB_FOLDER_PREFIX.toLowerCase())) {
                        jobID = jobID.substring(3);
                    }
                    Constant.DISKROOTPATH = this.getServletConfig().getServletContext().getRealPath("");
                    try {
                        constant = Constant.readConstant(jobID);
                    } catch (Exception e) {
                        Utilities.showAlertWindow(response, "can not find Result ID:\t" + jobID);
                        return;
                    }
                }
                if (constant == null) {
                    System.err.println("Can not load Result ID:\t" + jobID);
                    Utilities.showAlertWindow(response, "Can not load Result ID:\t" + jobID);
                    return;
                }
            %>
            <table id="resultSummary">
                <tr>
                    <td>Analysis Result Summary<sup><a href="manualResult.jsp#resultSummary">?</a></sup></td>
                </tr>
                <tr>
                    <td>Result ID:</td>
                    <td><%=constant.jobID %>
                    </td>
                </tr>
                <tr>
                    <td>Experiment(<%=constant.experiments.size()%>):
                    </td>
                    <td>
                        <%
                            for (Experiment experiment : constant.experiments) {
                        %>
                        <%=experiment.getName()%>&nbsp;&nbsp;
                        <%
                            }
                        %>
                    </td>
                </tr>
                <tr>
                    <td><%=ReportSummary.targetBoundedText%></td>
                    <td><%=constant.seqCountSummary.getSeqTargetBounded()%>
                    </td>
                </tr>
                <tr>
                    <td><%=ReportSummary.targetAfterFilterText%></td>
                    <td><%=constant.seqCountSummary.getSeqTargetAfterFilter()%>
                    </td>
                </tr>
                <tr>
                    <td><%=ReportSummary.cpgBoundedText%></td>
                    <td><%=constant.seqCountSummary.getSeqCpGBounded()%>
                    </td>
                </tr>
                <tr>
                    <td><%=ReportSummary.cpgAfterFilterText%></td>
                    <td><%=constant.seqCountSummary.getSeqCpGAfterFilter()%>
                    </td>
                </tr>
                <tr>
                    <td><%=ReportSummary.othersText%></td>
                    <td><%=constant.seqCountSummary.getSeqOthers()%>
                    </td>
                </tr>
                <tr>
                    <td>Analysis running time:</td>
                    <td><%=constant.analysisTime%>s</td>
                </tr>
                <tr>
                    <td>Zipped analysis result:</td>
                    <td><a href=<%=constant.analysisResultLink%>>analysisResult.zip</a></td>
                </tr>

                <tr>
                    <td><p class="dottedline"></p></td>
                    <td><p class="dottedline"></p></td>
                </tr>
                <tr>
                    <td>
                        Thresholds used in analysis:
                    </td>
                </tr>
                <tr>
                    <td>Bisulfite conversion rate:</td>
                    <td><%=constant.getConversionRateThreshold()%>
                    </td>
                </tr>
                <tr>
                    <td>Sequence identity:</td>
                    <td><%=constant.sequenceIdentityThreshold%>
                    </td>
                </tr>
                <%
                    if (constant.minP0Threshold >= 0) {
                %>
                <tr>
                    <td>&alpha; threshold:</td>
                    <td><%=constant.minP0Threshold%>
                    </td>
                </tr>
                <tr>
                    <td>Critical Value:</td>
                    <td><%=constant.criticalValue%>
                    </td>
                </tr>
                <%
                    }
                %>
                <%
                    if (constant.minMethylThreshold >= 0) {
                %>
                <tr>
                    <td>Methylation pattern threshold:</td>
                    <td><%=constant.minMethylThreshold%>
                    </td>
                </tr>
                <%
                    }
                %>
                <tr>
                    <td>Mutation threshold:</td>
                    <td><%=constant.mutationPatternThreshold%>
                    </td>
                </tr>
            </table>
            <p class="dottedline"></p>
            <%
                for (Experiment experiment : constant.experiments) {
            %>
            <table id="analysisResult">
                <tr>
                    <td>Experiment:<sup><a href="manualResult.jsp#experimentResult">?</a></sup></td>
                    <td><%=experiment.getName()%>
                    </td>
                </tr>
                <%
                    for (ReportSummary reportSummary : experiment.getReportSummaries()) {
                %>
                <tr>
                    <td>Region:</td>
                    <td><%=reportSummary.getId()%>
                    </td>
                </tr>
                <% for (PatternLink patternLink : reportSummary.getPatternLinks()) {
                %>
                <tr>
                    <td><%=patternLink.getPatternType()%>
                    </td>
                    <td><a
                            href=<%=patternLink.getTextResultLink()%>>text</a></td>
                    <%
                        if (constant.coorReady) {
                    %>
                    <td><a
                            href="<%=patternLink.getFigureResultLink()%>.<%=constant.figureFormat%>"><%=constant.figureFormat%>
                    </a>
                    </td>
                    <td><a
                            href="
                            http://genome.ucsc.edu/cgi-bin/hgTracks?db=<%=constant.getRefVersion()%>&hgt.customText=http://<%=patternLink.getGBResultLink()%>">GenomeBrowser</a>
                    </td>
                    <%
                        }
                    %>
                </tr>
                <%
                    }
                %>
                <tr>
                    <%
                        if (constant.coorReady && reportSummary.hasASM()) {
                    %>
                    <td>ASM pattern</td>
                    <%
                        if (reportSummary.getASMsnp() != null) {
                    %>
                    <td><a
                            href="http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs=<%=reportSummary.getASMsnp().getrsID()%>">SNP</a>
                    </td>
                    <%
                        }
                    %>
                    <td><a
                            href="<%=reportSummary.getASMFigureLink()%>.<%=constant.figureFormat%>"><%=constant.figureFormat%>
                    </a>
                    </td>
                    <td><a
                            href="http://genome.ucsc.edu/cgi-bin/hgTracks?db=<%=constant.getRefVersion()%>&hgt.customText=http://<%=reportSummary.getASMGBLink()%>">GenomeBrowser</a>
                    </td>
                    <%
                        }
                    %>
                </tr>
                <tr>
                    <td>Other Info</td>
                    <td><a
                            href=<%=reportSummary.getStatTextLink()%>>text</a></td>
                </tr>
                <%
                    }
                %>
            </table>
            <%
                }
            %>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
</body>
</html>