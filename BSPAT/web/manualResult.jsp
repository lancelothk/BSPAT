<%@ page import="edu.cwru.cbc.BSPAT.core.ReportSummary" %>
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
            <h2>Result</h2>

            <p class="dottedline"></p>
            <br/>

            <h3 id="resultSummary">Result summary section:</h3>
            <table>
                <tr>
                    <td><img src="images/analysisResultSummarySection.png" border="1" alt=""/></td>
                </tr>
                <tr>
                    <td>
                        <h4>Result ID</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">A unique ID is used to track and retrieve result.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Experiment</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">List experiments executed in the mapping phase.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4><%=ReportSummary.targetBoundedText%>
                        </h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Number of sequences cover whole target region. Used only for generating
                            methylation patterns.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4><%=ReportSummary.targetAfterFilterText%>
                        </h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Number of sequences filtered by bisulfite conversion rate and sequence
                            identity.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4><%=ReportSummary.cpgBoundedText%>
                        </h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Number of sequences cover all CpGs in target region. Used for generating
                            methylation pattern.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4><%=ReportSummary.cpgAfterFilterText%>
                        </h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Number of sequences filtered by bisulfite conversion rate and sequence
                            identity.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4><%=ReportSummary.othersText%>
                        </h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Number of sequences cover neither whole target region nor all CpGs in target
                            region.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Analysis phase running time</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Time cost of analysis in current execution.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Zipped analysis result</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Compressed analysis results, including all results shown in result page, such
                            as
                            text pattern, figure and GenomeBrowser bed file.</p>
                    </td>
                </tr>
            </table>

            <p class="dottedline"></p>
            <table>
                <tr>
                    <td><h3>Thresholds Section:</h3></td>
                </tr>
                <tr>
                    <td><img src="images/analysisUsedThresholdSection.png" border="1" alt=""/></td>
                </tr>

                <tr>
                    <td>
                        <h4>Bisulfite conversion rate</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">
                            <a href="manualAnalysis.jsp#conversionRate">Bisulfite conversion rate</a> threshold used in
                            current execution.
                        </p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Sequence identity</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">
                            <a href="manualAnalysis.jsp#seqIdentity">Sequence identity</a> threshold used in current
                            execution.
                        </p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Critical value, Methylation and SNP threshold</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Thresholds used in current execution to filter patterns and alleles.</p>
                    </td>
                </tr>
            </table>
            <p class="dottedline"></p>

            <table>
                <tr>
                    <td><h3 id="experimentResult">Experiment result section:</h3></td>
                </tr>
                <tr>
                    <td><img src="images/experimentResultSection.png" border="1" alt=""/></td>
                </tr>
                <tr>
                    <td><p class="ptext">The result of each experiment and each region are listed one by one. A link to
                        download all
                        result files is provided in the bottom of the page.</p>
                    </td>
                </tr>
                <tr>
                <tr>
                    <td>
                        <h4>Methylation Pattern</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">For each read, the methylation status at all CpG
                            sites covered by the read is regarded as its methylation signature or a
                            pattern. All reads have the same signature/pattern will be grouped together
                            and the number of all such reads is the support of the pattern.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>MethylationWithSNP pattern</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Combination pattern of methylation and SNP. Sorted by
                            Methylation status first.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>ASM pattern</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">When an allele exists, BSPAT groups all reads according to their alleles at the
                            variant
                            locus and according to methylation status of CpG sites. BSPAT separates all CpG sites
                            into three categories according to the proportion of their methylated reads:
                            low methylation level (<=20% reads are methylated), high methylation level
                            (>= 80% reads are methylated), and intermediate level (otherwise). When
                            there is at least one CpG site that the levels of methylation are different for
                            different alleles, BSPAT regards the region as an allele specific methylation region.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Other information</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Contains summary information such as methylation rate in each CpG site,
                            number of reads filtered by threshold.</p>
                    </td>
                </tr>
            </table>
            <p class="dottedline"></p>

            <table>
                <tr>
                    <td><h3>Result types:</h3></td>
                </tr>
                <tr>
                    <td><h4>Text</h4></td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">In the text representation of pattern, "@@" means one methylated CpG site, "**"
                            means unmethylated CpG site. "-" means non-CpG site nucleotide. Mismatch comparing to
                            reference is represented by actual character of that position. Read count and percentage of
                            each pattern is shown at the end. Here the percentage of pattern is calculated as read count
                            of the pattern divided by total count of reads. Last one or two columns are pattern IDs. In
                            Methylation pattern files, the ID refers to current pattern. In combination pattern files,
                            the ID means the parent pattern ID which current pattern derives from.</p>
                    </td>
                </tr>
                <tr>
                    <td>Example:<br/> <a href="images/text_example.png"
                                         target="_blank"><img src="images/text_example.png" border="1"
                                                              width="620" alt=""/></a></td>
                </tr>
                <tr>
                    <td><h4>Figure</h4></td>
                </tr>
                <tr>
                    <td><p class="ptext">Currently support PNG and EPS format figure. For patterns, black circle refers
                        to
                        methylated CpG site, white one refers to non-methylated CpG site. The meaning of percentage of
                        pattern is same to text result. The last colorful row is average methylation status of each CpG
                        site. The methylation rate is calculated from all reads in this region. The color scale is from
                        Red(100%) to Green (0%). SNP here is represented by a blue bar.</p>

                        <p class="ptext">For ASM pattern, there are only two colorful patterns. Each of the pattern
                            represents an allele methylation pattern.</p></td>
                </tr>
                <tr>
                    <td>Example:<br/> <a href="images/figure_example.png"
                                         target="_blank"><img src="images/figure_example.png"
                                                              border="1" width="620" alt=""/></a></td>
                </tr>
                <tr>
                    <td><h4>Genome Browser Link</h4></td>
                </tr>
                <tr>
                    <td><p class="ptext">Those links point to UCSC genome browser with loaded custom track. Target
                        region
                        is covered with black bar. The layout and color scale is same to figure result. dbSNP track is
                        loaded by default. More help information can be found in UCSC genome browser website. To add or
                        remove
                        custom track,
                        please refer to UCSC genome browser manual. </p></td>
                </tr>
                <tr>
                    <td>Example:<br/> <a href="images/gb_example.png"
                                         target="_blank"><img src="images/gb_example.png" border="1"
                                                              width="620" alt=""/></a></td>
                </tr>
                <tr>
                    <td><h4>dbSNP link</h4></td>
                </tr>
                <tr>
                    <td><p class="ptext">If the SNP associated with allele-specific methylation pattern is contained in
                        dbSNP database, a link to dbSNP page of that SNP will be shown in the result page.</p></td>
                </tr>
            </table>
            <p class="dottedline"></p>

            <p style="text-align: center">
                <a href="manual.jsp">Back</a>
            </p>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
</body>
</html>
