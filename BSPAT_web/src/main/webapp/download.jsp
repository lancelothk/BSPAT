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
            <h2>Downloads:</h2>

            <p class="dottedline"></p>
            <table id="downloads">
                <tr>
                    <td>1. Extract samples from multiplex file</td>
                </tr>
                <tr>
                    <td>Usage: perl BarcodesExtractor.pl &lt;barcode file&gt; &lt;multiplex file&gt; &lt;output
                        path&gt;</td>
                </tr>
                <tr>
                    <td><a href="download/BarcodesExtractor.pl"> BarcodesExtractor.pl </a></td>
                </tr>
                <tr>
                    <td><p class="dottedline"></p></td>
                </tr>
                <tr>
                    <td>2. demo data</td>
                </tr>
                <tr>
                    <td>Including reference file and demo sequence files</td>
                </tr>
                <tr>
                    <td><a href="download/demo_data.zip"> demo_data.zip </a></td>
                </tr>
                <tr>
                    <td><p class="dottedline"></p></td>
                </tr>
            </table>
        </div>
        <div id="content_bottom"></div>
    </div>
    <%@ include file="footer.html" %>
</div>
</body>
</html>
