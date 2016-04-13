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

            <h3>Reference and Parameter Section:</h3>
            <table>
                <tr>
                    <td><img src="images/RefParSection.png" border="1" alt=""/></td>
                </tr>
                <tr id="email">
                    <td>
                        <h4>Email Address(optional):</h4>

                        <p class="ptext">Email address here is used to notify user
                            when the mapping or analysis procedure has finished. A unique
                            job ID send in the email is used for retrieving the
                            result.</p>
                    </td>
                </tr>
                <tr id="refFile">
                    <td>
                        <h4>Reference Sequence File:</h4>

                        <p class="ptext">The file contains reference sequences.
                            It should be in fasta format with extension ".fasta",
                            ".fa". Multiple reference sequences in one file is supported. Identifier
                            of reference sequence should be unique in the file and only be consist of
                            character and digits. To make the mapping work correctly, the length of reference sequence
                            should be longer or equal to the length of reads.</p>
                        <label>
                            <textarea readonly rows="5" cols="">Example:&#13;&#10;>region1&#13;&#10;GTCTTCCAGGAGGAGGGTTTGCACACGTCCATCTACAGTTTCGACGAGACCAAAGAC&#13;&#10;>region2&#13;&#10;AGGCAAGACAGCAGGGCTGGGGGCTTCGGACTGCGGGCGGGCGGGCCGCTGTCGCCGCTTGACGCCCCTCCGGGG</textarea>
                        </label>
                    </td>
                </tr>
                <tr id="refVersion">
                    <td>
                        <h4>Reference Genome Version:</h4>

                        <p class="ptext">It specify the reference genome version used in
                            following analysis. The coordinates in result visualization is also based on
                            this selection. Currently support hg38, hg19 and hg18 .</p>
                    </td>
                </tr>

                <tr id="quality">
                    <td>
                        <h4>Quality Score Type:</h4>

                        <p class="ptext">
                            Bismark parameter. User need to specify what kind of quality
                            type used in the sequence file. Only used for fastq input.
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
                        <h4>Experiment Section:</h4>
                    </td>
                </tr>
                <tr>
                    <td><img src="images/ExperimentSection.png" border="1" alt=""/></td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">
                            In this section, for each experiment, user need to enter an
                            experiment name(without space character, no duplicated names) in textbox and select one or
                            multiple fastq/fasta
                            files to upload. The extension of fastq/fasta files
                            can be .fq, .fastq, .fa, .fasta .txt(.txt input is by default
                            treated as fastq file). The reads in sequence files will be
                            automatically mapped to corresponding reference region. <br/>
                            Notice: <br/> All input sequence file should be single-end. For paired-end data
                            with overlapping, user can use some tools like <a
                                href="http://sco.h-its.org/exelixis/web/software/pear/">PEAR</a>
                            to merge paired-end reads into single-end reads.<br/>
                        </p>
                    </td>
                </tr>
            </table>
            <p class="dottedline"></p>
            <table>
                <tr>
                    <td>
                        <h4>Bottom Section:</h4>
                    </td>
                </tr>
                <tr>
                    <td><img src="images/mappingButton_sample.png" border="1" alt=""/></td>
                </tr>
                <tr>
                    <td>
                        <p class="ptext">In the bottom of the page, "add experiment"
                            and "delete experiment" buttons are used to add and delete
                            experiment above. After set all parameters, user can
                            to click "Execute" button to start the mapping.</p>
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
