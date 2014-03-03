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
				<h2>Preprocessing:</h2>
				split multiplex file by uploaded barcodes
				<p class="dottedline"></p>
				<form action="preprocessing" enctype="multipart/form-data" method="post">
					<table id="files">
							<tr>
								<td>Multiplex File:</td>
								<td><input type="file" name="multiplex" multiple="multiple" /></td>
							</tr>
							<tr>
								<td>Barcode File:</td>
								<td><input type="file" name="barcodes" multiple="multiple" /></td>
							</tr>
							<tr>
								<td><input type="submit" value="Execute" /></td>
							</tr>
					</table>
				</form>
			</div>
			<div id="content_bottom"></div>
		</div>
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
