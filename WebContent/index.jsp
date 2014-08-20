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

            <form action="mapping" onsubmit="return validateInput()" enctype="multipart/form-data" method="post">
                <div id="tables">
                    <table id="refcoor">
                        <tr>
                            <td>Email address(Optional):<sup><a href="manualMapping.jsp#email">?</a></sup></td>
                            <td><label>
                                <input type="text" name="email" id="email"/>
                            </label></td>
                        </tr>
                        <tr>
                            <td>Reference Sequence File:<sup><a href="manualMapping.jsp#refFile">?</a></sup></td>
                            <td><input type="file" id="ref" name="ref" multiple/></td>
                        </tr>
                        <tr>
                            <td>Reference Genome Version:<sup><a href="manualMapping.jsp#refVersion">?</a></sup></td>
                            <td><label>
                                <select name="refVersion">
                                    <option value="hg38">hg38</option>
                                    <option value="hg19">hg19</option>
                                    <option value="hg18" selected="selected">hg18</option>
                                </select>
                            </label></td>
                        </tr>
                        <tr>
                            <td>Quality Score type:<sup><a href="manualMapping.jsp#quality">?</a></sup></td>
                            <td><label>
                                <select name="qualsType">
                                    <option value="phred33" selected="selected">phred33</option>
                                    <option value="phred64">phred64</option>
                                    <option value="solexa">solexa</option>
                                    <option value="solexa-1.3">solexa-1.3</option>
                                </select>
                            </label></td>
                        </tr>
                        <tr>
                            <td>Maximum Permitted Mismatches:<sup><a href="manualMapping.jsp#maxMis">?</a></sup></td>
                            <td><label>
                                <select name="maxmis">
                                    <option value="0">0</option>
                                    <option value="1">1</option>
                                    <option value="2" selected="selected">2</option>
                                    <option value="3">3</option>
                                </select>
                            </label></td>
                        </tr>
                    </table>
                    <p class="dottedline"></p>
                    <table id="experiment">
                        <tr>
                            <td>Experiment</td>
                            <td>Reads File<sup><a href="manualMapping.jsp#experiment">?</a></sup></td>
                        </tr>
                    </table>
                    <p class="dottedline"></p>
                    <table id="buttons">
                        <tr>
                            <td><input name="add" type="button" value="add experiment"
                                       onclick="addExperiment()"/></td>
                            <td><input name="delete" type="button"
                                       value="delete experiment" onclick="deleteExperiment()"/></td>
                            <td><input type="submit" value="Execute"/>
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
    var elementCount = 0;

    function addColumnSeq(TR) {
        var index = document.createTextNode(elementCount + '  ');
        var cellLineText = document.createElement("td");
        var fileText = document.createElement("td");
        var textInput = document.createElement("input");
        var fileInput = document.createElement("input");
        textInput.setAttribute("name", "experiment" + elementCount);
        textInput.setAttribute("id", "experiment" + elementCount);
        textInput.setAttribute("type", "text");
        textInput.setAttribute("multiple", "multiple");

        // if access in JS, need use "ID" in stead of "name"
        fileInput.setAttribute("name", "seqFile" + elementCount);
        fileInput.setAttribute("id", "seqFile" + elementCount);
        fileInput.setAttribute("type", "file");
        fileInput.setAttribute("multiple", "multiple");
        cellLineText.appendChild(index);
        cellLineText.appendChild(textInput);
        fileText.appendChild(fileInput);
        fileText.setAttribute("id", "seqFileUploadTD" + elementCount);
        TR.appendChild(cellLineText);
        TR.appendChild(fileText);
    }

    function addExperiment() {
        elementCount = elementCount + 1;

        // get table
        var table = document.getElementById("experiment");
        // create rows
        var seqRow = document.createElement("tr");
        var checkBoxRow = document.createElement("tr");

        // assign name, type and value of elements
        seqRow.setAttribute("id", "trseq" + elementCount);
        seqRow.setAttribute("name", "trseq");
        checkBoxRow.setAttribute("id", "trcheckbox" + elementCount);
        checkBoxRow.setAttribute("name", "trcheckbox");

        // add element in TR
        addColumnSeq(seqRow);
        //addColumnCheckBox(checkBoxRow);
        table.appendChild(seqRow);
        table.appendChild(checkBoxRow);
        setFooter();
    }

    function deleteExperiment() {
        // get table
        var table = document.getElementById("experiment");

        if (elementCount >= 2) {
            var seqRow = document.getElementById("trseq" + elementCount);
            var checkBoxRow = document.getElementById("trcheckbox"
                    + elementCount);
            table.removeChild(seqRow);
            table.removeChild(checkBoxRow);
            elementCount = elementCount - 1;
        }
        setFooter();
    }

    function isEmptyValue(id, msg) {
        if (document.getElementById(id).value == "") {
            window.alert(msg);
            return true;
        } else {
            return false;
        }
    }

    function validateInput() {
        if (!checkEmailAddress()) {
            return false;
        }
        if (isEmptyValue("ref", "No reference file!")) {
            return false;
        }
        for (var i = 1; i <= elementCount; i++) {
            if (isEmptyValue("experiment" + i, "Experiment " + i + " name is empty!")
                    || isEmptyValue("seqFile" + i, "Experiment " + i + " has no sequence file selected!")) {
                return false;
            }
            if (containBlank("experiment" + i)) {
                return false;
            }
        }
        if (!checkRfFile()) {
            return false;
        }
        resetPage();
        return true;
    }

    function resetPage() {
        document.getElementById('loader').style.display = 'block';
        document.getElementById('tables').style.display = 'none';
        setFooter();
    }

    function checkEmailAddress() {
        var emailAddress = document.getElementById("email").value;
        if (emailAddress != "" && !validateEmail(emailAddress)) {
            window.alert("Error: Invalid email address!");
            return false;
        }
        return true;
    }

    function validateEmail(email) {
        var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(email);
    }

    function containBlank(id) {
        var experimentName = document.getElementById(id).value;
        if (experimentName.search(" ") == -1) {  //don't have blank in name
            return false;
        }
        window.alert("Error: " + id + " has blank in its name! Please remove the blank");
        return true;        //contain blank
    }

    function checkRfFile() {
        var fileName = document.getElementById("ref").value;
        var ext = fileName.split(".");
        if (ext[1] != "fa" && ext[1] != "fasta") {
            window.alert("Error: Invalid reference file! Only .fa and .fasta files are allowed.");
            return false;
        }
        return true;
    }

    window.onload = addExperiment();
</script>
</html>
