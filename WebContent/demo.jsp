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
            <h2>Step 1: Mapping:</h2>

            <p class="dottedline"></p>
            <img id="loader" src="images/progress.gif" alt="progress" style="display: none;"/>

            <form action="mapping" enctype="multipart/form-data" method="post">
                <div id="tables">
                    <table id="refcoor">
                        <tr>
                            <td>Email address(Optional):<sup><a href="manualMapping.jsp#email">?</a></sup></td>
                            <td><label>
                                <input type="text" name="email"/>
                            </label></td>
                        </tr>
                        <tr>
                            <td>Reference Sequence File:<sup><a href="manualMapping.jsp#refFile">?</a></sup></td>
                            <td>demoReference.fasta</td>
                        </tr>
                        <tr>
                            <td>Reference Genome Version:<sup><a href="manualMapping.jsp#refVersion">?</a></sup></td>
                            <td><label>
                                <select name="refVersion" disabled>
                                    <option value="hg19">hg19</option>
                                    <option value="hg18" selected="selected">hg18</option>
                                    <option value="hg17">hg17</option>
                                    <option value="hg16">hg16</option>
                                </select>
                            </label></td>
                            <input type="hidden" name="refVersion" value="hg18"/>
                        </tr>
                        <tr>
                            <td>Quality Score type:<sup><a href="manualMapping.jsp#quality">?</a></sup></td>
                            <td><label>
                                <select name="qualsType" disabled>
                                    <option value="phred33" selected="selected">phred33</option>
                                    <option value="phred64">phred64</option>
                                    <option value="solexa">solexa</option>
                                    <option value="solexa-1.3">solexa-1.3</option>
                                </select>
                            </label></td>
                            <input type="hidden" name="qualsType" value="phred33"/>
                        </tr>
                        <tr>
                            <td>Maximum Permitted Mismatches:<sup><a href="manualMapping.jsp#maxMis">?</a></sup></td>
                            <td><label>
                                <select name="maxmis" disabled>
                                    <option value="0">0</option>
                                    <option value="1">1</option>
                                    <option value="2" selected="selected">2</option>
                                    <option value="3">3</option>
                                </select>
                            </label></td>
                            <input type="hidden" name="maxmis" value="2"/>
                        </tr>
                    </table>
                    <p class="dottedline"></p>
                    <table id="experiment">
                        <tr>
                            <td>Experiment</td>
                            <td>Reads File<sup><a href="manualMapping.jsp#experiment">?</a></sup></td>
                        </tr>
                        <tr>
                            <td>1 <label><input type="text" name="demoExperiment" disabled
                                                value="demoExperiment"/></label></td>
                            <td>demoSequence.fastq</td>
                        </tr>
                    </table>
                    <p class="dottedline"></p>
                    <table id="buttons">
                        <tr>
                            <input type="hidden" name="demo" value="true"/>
                            <td><input type="submit" value="Execute"
                                       onclick="document.getElementById('loader').style.display = 'block';document.getElementById('tables').style.display = 'none';setFooter();"/>
                            </td>
                        </tr>
                    </table>
                </div>
            </form>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
</body>
<script language="javascript">
    window.onload = addExperiment;
</script>
</html>
