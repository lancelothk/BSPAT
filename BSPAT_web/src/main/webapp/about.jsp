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
            <p class="phead">
                Version: 2.4.2-beta<br/>
            </p>

            <p class="phead">
                Developed and Maintained By: <br/>
            </p>

            <p class="ptext">
                Ke Hu &nbsp&nbsp Ph.D student
                <br/>Advisor: &nbsp&nbsp <a href="http://cbc.case.edu">Prof. Jing Li </a>
                <br/>Computational Biology Lab @ EECS department
                <br/>Case Western Reserve University
                <br/>If you have any questions or suggestions, feel free to send an email to:
                <img src="images/emailadd.png" alt=""/>

            <p class="phead"> Source code hosted in <a href="https://github.com/lancelothk/BSPAT">GitHub</a></p>

            <p class="phead"> Citation:</p>
            <a href="http://bmcbioinformatics.biomedcentral.com/articles/10.1186/s12859-015-0649-2"> Hu, K., Ting, A.H.
                and Li, J., 2015. BSPAT: a fast online tool for DNA methylation co-occurrence pattern analysis based on
                high-throughput bisulfite sequencing data. BMC bioinformatics, 16(1), p.1.</a>

            <p class="phead">License:</p>
            Copyright 2014 © Computational Biology lab @ Case Western Reserve University.
            <br/>Under GPL v3.
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
</body>
</html>
