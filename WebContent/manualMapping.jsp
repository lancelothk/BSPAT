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
            <h2>Mapping</h2>

            <p class="dottedline"></p>
            <br/>

            <h3>Reference and Parameter section:</h3>
            <table>
                <tr>
                    <td><img src="images/RefParSection.png" border="1" alt=""/></td>
                </tr>
                <tr id="email">
                    <td>
                        <h4>Email address(optional):</h4>

                        <p class="ptext">The email address is used to notice user
                            when the mapping or analysis procedure has finished. A unique
                            execution ID is sent along with the email for retrieving the
                            result.</p>
                    </td>
                </tr>
                <tr id="refFile">
                    <td>
                        <h4>Target Sequence File:</h4>

                        <p class="ptext">File contains sequences which exactly covered target genomic region. Those
                            target
                            sequences will be
                            queried by <a href="http://genome.ucsc.edu/cgi-bin/hgBlat">Blat</a> for genomic coordinates.
                            Only top result with
                            equal 'score' and 'qsize' will be picked and used in following steps. In the mapping
                            process, the target region will be extended 1000bp in both end as reference of mapping.
                            The file should be in fasta format with extension ".txt", "fasta",
                            "fa". Multiple target sequences in one file is supported. Name
                            of target sequence should be a unique identifier in the
                            target sequence file. The name should only be consist of character and
                            digits.</p>
                        <label>
                            <textarea readonly rows="5" cols="">Example:&#13;&#10;>region1&#13;&#10;GTCTTCCAGGAGGAGGGTTTGCACACGTCCATCTACAGTTTCGACGAGACCAAAGAC&#13;&#10;>region2&#13;&#10;AGGCAAGACAGCAGGGCTGGGGGCTTCGGACTGCGGGCGGGCGGGCCGCTGTCGCCGCTTGACGCCCCTCCGGGG</textarea>
                        </label>
                    </td>
                </tr>
                <tr id="refVersion">
                    <td>
                        <h4>Reference Genome Version:</h4>

                        <p class="ptext">The reference genome version is used in
                            following analysis. The result visualization is also based on
                            this selection. Currently support hg19, hg18, hg17 and hg16.</p>
                    </td>
                </tr>

                <tr id="quality">
                    <td>
                        <h4>Quality Score type:</h4>

                        <p class="ptext">
                            Bismark parameter. User need to specify what kind of quality
                            type used in the sequence file. Only available for fastq input.
                            Currently support phred33, phred64, solexa and solexa1.3. More
                            details can be found in <a
                                href="http://www.bioinformatics.babraham.ac.uk/projects/bismark/Bismark_User_Guide.pdf">Bismark
                            user guide</a> and <a
                                href="http://bowtie-bio.sourceforge.net/manual.shtml">bowtie
                            manual</a>.
                        </p>
                    </td>
                </tr>
                <tr id="maxMis">
                    <td>
                        <h4>Maximum Permitted Mismatches:</h4>

                        <p class="ptext">
                            Bismark parameter. Maximum number of mismatches permitted in the
                            "seed", i.e. the first L base pairs of the read (where L is set
                            with -l/--seedlen). Currently support 0,1,2 and 3. More details
                            can be found in <a
                                href="http://www.bioinformatics.babraham.ac.uk/projects/bismark/Bismark_User_Guide.pdf">Bismark
                            user guide</a> and <a
                                href="http://bowtie-bio.sourceforge.net/manual.shtml">bowtie
                            manual</a>.
                        </p>
                    </td>
                </tr>
            </table>
            <p class="dottedline"></p>
            <table>
                <tr id="experiment">
                    <td>
                        <h4>Experiment section:</h4>
                    </td>
                </tr>
                <tr>
                    <td><img src="images/ExperimentSection.png" border="1" alt=""/></td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">
                            In this section,for each experiment, user need to enter an
                            experiment name in textbox and select one or multiple fastq or
                            fasta files to upload. The extension of fastq and fasta files
                            can be .fq, .fastq, .fa, .fasta .txt(.txt input is by default
                            treated as fastq file). The reads in sequence files will be
                            automatically mapped to corresponding target region. <br/>
                            Notices: <br/> (a) All input sequence file should be single-end. For paired-end data
                            with overlapping, user can use some tools like <a
                                href="http://sco.h-its.org/exelixis/web/software/pear/">PEAR</a>
                            to merge paired-end reads into single-end reads.<br/>(b) In the following analysis, only
                            sequences fully covering target region will be included. E.g:
                            <label>
                                <textarea readonly rows="5" cols="">target:CAACAACGTCTAGGG&#13;&#10;&nbsp;ACGCAACAACGTCTAGGGACT&nbsp;(included)&#13;&#10;&nbsp;ACGCAACAACGTC&nbsp;(excluded)&#13;&#10;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ACAACGTCTAG&nbsp;(excluded)&#13;&#10;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;TAGGGACTGATC&nbsp;(excluded)</textarea>
                            </label>

                        </p>
                    </td>
                </tr>
            </table>
            <p class="dottedline"></p>
            <table>
                <tr>
                    <td>
                        <h4>Button section:</h4>
                    </td>
                </tr>
                <tr>
                    <td><img src="images/mappingButton_sample.png" border="1" alt=""/></td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">In the bottom of the page, "add experiment"
                            and " delete experiment" buttons are used to add and delete
                            experiment above. After finished set all parameters, user need
                            to click "Execute" button to submit this page.</p>
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
