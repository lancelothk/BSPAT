<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<link rel="stylesheet" type="text/css" href="style.css" />
<title>BS-PAT</title>
</head>

<body>
	<div id="container">
		<%@ include file="menu.html"%>
		<div id="content">
			<div id="content_top"></div>
			<div id="content_main">
			<h2>Preprocessing scripts:</h2>
			<table id="downloads">
			<tr><td>Extract samples from multiplex file. </td></tr>
			<tr><td>Usage: perl BarcodesExtractor.pl &lt;barcode file&gt; &lt;multiplex file&gt; &lt;output path&gt;</td></tr>
			<tr><td><a href="BarcodesExtractor.pl"> BarcodesExtractor.pl </a></td></tr>
			
			<tr><td>Example input data </td></tr>
			<tr><td>Including reference file and several sequence files</td></tr>
			<tr><td><a href="example.zip"> example.zip </a></td></tr>
			</table>
			</div>
			<div id="content_bottom"></div>
		</div>
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
