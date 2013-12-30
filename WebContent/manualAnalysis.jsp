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
				<h2>Analysis</h2>
				<p class="dottedline"></p>
				<br />
				<h3 id="mappingSummary">Mapping summary section:</h3>
				<table>
					<tr>
						<td><img src="images/mappingSummary_sample.png" border="1" />
						</td>
					</tr>
					<tr>
						<td>
							<p class="ptext">First row shows how many experiments user
								uploaded and their names. Second row shows how long it cost for
								mapping in seconds. Third row shows execution ID, which can be
								used to track and retrieve result later. The last row is mapping
								result download link, which is in zip format.</p>
						</td>
					</tr>
				</table>
				<p class="dottedline"></p>
				<h3>Analysis option section:</h3>
				<table>
					<tr>
						<td><img src="images/AnalysisOptionsSection.png" border="1" />
						</td>
					</tr>
					<tr id="conversionRate">
						<td>
							<h4>Bisulfite Conversion rate</h4>
						</td>
					</tr>
					<tr>
						<td>
							<p class="ptext">Bisulfite conversion rate is defined as the proportion of
								converted Cytosine among all Cytosine in non-CpG context. Reads
								with a rate below this threshold will be filtered.</p>
						</td>
					</tr>
					<tr id="seqIdentity">
						<td>
							<h4>Sequence identity</h4>
						</td>
					</tr>
					<tr>
						<td>
							<p class="ptext">Sequence identity is defined as the proportion of matched
								nucleotides of each read. Reads with sequence identity lower
								than threshold will be filtered.</p>
						</td>
					</tr>
					<tr id="alphaThreshold">
						<td>
							<h4>&alpha; threshold & Critical Value</h4>
						</td>
					</tr>
					<tr>
						<td>
							<p class="ptext">These two parameters are used in significance calculation. The
								formula is shown in figure below. They are default
								pattern filter parameters, which are mutually exclusive with
								Methylation pattern threshold. If there is no result to show
								under user assigned parameter, the program will use methylation
								pattern threshold instead to generate result.</p> <img
							src="images/formula.png" border="1" width="500" />
						</td>
					</tr>
					<tr id="methylPatternThreshold">
						<td>
							<h4>Methylation pattern threshold</h4>
						</td>
					</tr>
					<tr>
						<td>
							<p class="ptext">This threshold is the percentage of reads covered in
								methylation pattern comparing to total number of reads. User can define a value to select
								significant patterns. Patterns with a higher portion than this
								threshold will be shown in result.</p>
						</td>
					</tr>
					<tr id="mutationPatternThreshold">
						<td>
							<h4>Mutation pattern threshold</h4>
						</td>
					</tr>
					<tr>
						<td>
							<p class="ptext">This threshold is the percentage of reads covered in
								mutation pattern comparing to total number of reads. User can define a value to select significant
								patterns. Patterns with a higher portion than this threshold will be
								shown in result.</p>
						</td>
					</tr>
					<tr id="figureFormat">
						<td>
							<h4>Figure Format</h4>
						</td>
					</tr>
					<tr>
						<td>
							<p class="ptext">User can select the format of pattern figure. Currently
								support PNG and EPS.</p>
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
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
