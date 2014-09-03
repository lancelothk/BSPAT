/**
 * Created by kehu on 9/3/14.
 */

function isInvalidEmailAddress() {
    var emailAddress = document.getElementById("email");
    if (emailAddress.value != "" && !validateEmail(emailAddress.value)) {
        createErrorMsg(emailAddress, "Invalid email address!");
        return true;
    } else {
        return false;
    }
}

function validateEmail(email) {
    var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
}

function containBlank(id) {
    var experimentName = document.getElementById(id);
    if (experimentName.value.search(" ") == -1) {  //don't have blank in name
        return false;
    } else {
        createErrorMsg(experimentName, id + " has blank in its name! Please remove the blank");
        return true;        //contain blank
    }
}


function createErrorMsg(element, msg) {
    var errorMsg = document.createElement("div");
    errorMsg.id = "errorMsg_" + element.id;
    errorMsg.className = "errorMsg";
    errorMsg.innerText = msg;
    element.parentNode.insertBefore(errorMsg, element);
}

function isEmptyValue(id, msg) {
    var element = document.getElementById(id);
    if (element.value == "") {
        createErrorMsg(element, msg);
        return true;
    } else {
        return false;
    }
}

function removeErrorMsg() {
    var errorMsgs = document.querySelectorAll('*[id^="errorMsg"]');
    for (var index = 0; index < errorMsgs.length; ++index) {
        var element = errorMsgs[index];
        element.parentNode.removeChild(element);
    }
}


function isInvalidRefFile() {
    var refFile = document.getElementById("ref");
    var ext = refFile.value.split('.').pop();
    if (ext != "fa" && ext != "fasta") {
        createErrorMsg(refFile, "Only .fa and .fasta files are allowed");
        return true;
    } else {
        return false;
    }
}

function isInvalidSeqFile(id) {
    var seqFile = document.getElementById(id);
    var ext = seqFile.value.split('.').pop();
    if (ext != "fa" && ext != "fasta" && ext != "fq" && ext != "fastq" && ext != "txt") {
        createErrorMsg(seqFile, "Only .fa .fasta .fq .fastq .txt files are allowed");
        return true;
    } else {
        return false;
    }
}

function isInvalidValue(id, msg) {
    var valueInput = document.getElementById(id);
    if (!$.isNumeric(valueInput.value) || valueInput.value < 0 || valueInput.value > 1) {
        createErrorMsg(valueInput, msg);
        return true;
    } else {
        return false;
    }
}