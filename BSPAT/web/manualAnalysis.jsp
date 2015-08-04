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
            <h2>Analysis</h2>

            <p class="dottedline"></p>
            <br/>

            <h3 id="mappingSummary">Mapping Summary Section:</h3>
            <table>
                <tr>
                    <td><img src="images/mappingSummary_sample.png" border="1" alt=""/>
                    </td>
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
                        <h4>Sequences analysed in total</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">The total number of sequences uploaded and analysed.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Sequences with a unique best hit alignment</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Number of alignments with a unique best hit from different alignments. More
                            details see <a
                                    href="http://www.bioinformatics.babraham.ac.uk/projects/bismark/Bismark_User_Guide.pdf">Bismark
                                user guide</a>.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Sequences without any alignment</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Number of sequence didn't have alignments under any conditions. More details
                            see <a
                                    href="http://www.bioinformatics.babraham.ac.uk/projects/bismark/Bismark_User_Guide.pdf">Bismark
                                user guide</a>.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Sequences did not map uniquely</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">More details see <a
                                href="http://www.bioinformatics.babraham.ac.uk/projects/bismark/Bismark_User_Guide.pdf">Bismark
                            user guide</a>.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Invalid sequences</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Sequences discarded because genomic sequence could not be extracted. More
                            details see <a
                                    href="http://www.bioinformatics.babraham.ac.uk/projects/bismark/Bismark_User_Guide.pdf">Bismark
                                user guide</a>.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Mapping efficiency</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">The percentage of aligned sequences.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Mapping phase running time</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Time cost of mapping and blat query in current execution.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Reference coordinates:</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">The genomic coordinates of uploaded reference sequences. Queried by using <a
                                href="http://genome.ucsc.edu/cgi-bin/hgBlat">Blat</a>. Only top results with equal
                            'score' and
                            'qsize' will be picked and used in following steps.</p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <h4>Zipped mapping result</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Compressed mapping results generated by Bismark, including mapped sequences in
                            sam format,
                            called methylation status, mapping report. More details see <a
                                    href="http://www.bioinformatics.babraham.ac.uk/projects/bismark/Bismark_User_Guide.pdf">Bismark
                                user guide</a>.</p>
                    </td>
                </tr>
            </table>
            <p class="dottedline"></p>

            <h3>Analysis option section:</h3>
            <table>
                <tr>
                    <td><img src="images/AnalysisOptionsSection.png" border="1" alt=""/>
                    </td>
                </tr>
                <tr id="targetCoordinates">
                    <td>
                        <h4>Target Coordinates</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">User should upload a file contains the coordinates which define the target
                            region of analysis. The file format should be same to reference coordinates file. It will be
                            easier to create one from reference coordinates file and upload it here. </p>

                        <p class="ptext">If no target coordinate file uploaded, by default BSPAT will use first mapped
                            reads as target region.</p>

                        <p class="ptext">
                            Notice: In the analysis procedure, only sequences fully covering target region will be
                            included for MethylationWithSNP pattern
                            pattern analysis. For methylation pattern analysis, all sequences fully covering all CpG
                            sites in target
                            region will be included.
                            E.g:
                            <img src="images/target_example.png" border="1" alt=""/>
                        </p>
                    </td>
                </tr>
                <tr id="conversionRate">
                    <td>
                        <h4>Bisulfite Conversion rate</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Bisulfite conversion rate is defined as the proportion of
                            converted Cytosine among all Cytosine in non-CpG context. Reads
                            with a rate below this threshold will be filtered.</p>
                    </td>
                </tr>
                <tr id="seqIdentity">
                    <td>
                        <h4>Sequence identity</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">Sequence identity is defined as the proportion of matched
                            nucleotides of each read. Reads with sequence identity lower
                            than threshold will be filtered.</p>
                    </td>
                </tr>
                <tr id="criticalValue">
                    <td>
                        <h4>Critical Value</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">This critical value is used in a simple Z-score like statistic to measure the
                            significance of a pattern. The formula is shown in figure below. p0 here is 1/#observed
                            patterns. If the p-value corresponding to the Z-score is smaller than given critical value
                            threshold, the co-occurrence pattern is treated as significant. If the CpG site number
                            smaller than 3, the program will use the below methylation pattern threshold instead. If
                            there is no result to show under assigned parameter, please try to use methylation pattern
                            threshold instead to generate result.</p> <img
                            src="images/formula.png" border="1" width="300" alt=""/></td>
                </tr>
                <tr id="methylPatternThreshold">
                    <td>
                        <h4>Methylation pattern threshold</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">This threshold is the percentage of reads covered in methylation pattern
                            comparing to total number of reads. Patterns with a percentage higher than this threshold
                            will be shown in result.</p>
                    </td>
                </tr>
                <tr id="SNPThreshold">
                    <td>
                        <h4>SNP threshold</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">This threshold is used to declare SNP.<br/>
                            Mismatches with proportion larger than this threshold will be treated as a SNP.</p>
                    </td>
                </tr>
                <tr id="figureFormat">
                    <td>
                        <h4>Figure Format</h4>
                    </td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">User can select the output format of pattern figure. Currently
                            support PNG and EPS.</p>
                    </td>
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
