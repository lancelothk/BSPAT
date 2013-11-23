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
				<p class="phead">
					2. Analysis phase:</br>
				</p>
				<p class="ptext">This page shows mapping summary and analysis
					parameter options</p>
				</br>
				<p class="dottedline"></p>
				</br>
				<h4>Mapping summary section:</h4>
				</br> <img src="images/mappingSummary_sample.png" border="1" /></br> </br>
				<p class="ptext">First line shows how many experiments user
					uploaded and their names. Second line shows how long it cost for
					mapping in seconds. Third line shows execution ID, which can be
					used to track and retrieve result later. The last line is mapping
					result download link, which is in zip format.</p>
				</br>
				<p class="dottedline"></p>
				</br>
				<h4>Analysis option section:</h4>
				</br> <img src="images/analysisOption_sample.png" border="1" /></br> </br>
				<p class="ptext">
					In this section, user can assign quality control, result filtering
					parameters and upload coordinates file. All of the parameters have
					defaut values.</br>
					<table>
						<td>Corresponding Coordinates File:</td>
						<td>File contains amplicon ID and corresponding genome
							position information in each line, which is used for generating
							figure. If do not upload this file, the result will not include
							figure. Format e.g: DU145-4(tab)chr5:32748306-32748417.</td>
					</table>
					Meaning of each parameter:</br> (a) Conversion rate threshold: For each
					read, bisulfite conversion rate is calculated. The read with a rate
					below this threshold will be filtered. </br> (b) Sequence identity
					threshold: Sequence identity is calculated for each read. Read with
					sequence identity lower than threshold will be filtered. </br> (c) Alpha
					threshold: It is a parameter used in significance calculation. The
					formula is shown in figure below. This parameter is the default
					pattern filter parameter, which is mutually exclusive with
					Methylation pattern threshold. If under user assigned minimum p0,
					there is no result to show, the program will use methylation
					pattern threshold instead to generate result. </br> <img
						src="images/formula.png" border="1" width="500" /></br> (d)
					Methylation pattern threshold: This threshold is the percentage of
					reads covered in methylation pattern. User can define a value to
					select significant patterns. Patterns with a higher value than this
					threshold will show in result. </br> (e) Mutation pattern threshold:
					This threshold is the percentage of reads covered in mutation
					pattern. User can define a value to select significant patterns.
					Patterns with a higher value than this threshold will show in
					result.</br>
				</p>
				
				</br><a href="manual.jsp">Back</a>

			</div>
			<div id="content_bottom"></div>
		</div>
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
