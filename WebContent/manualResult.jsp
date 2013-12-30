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
				<h2>Result</h2>
				<p class="dottedline"></p>
				<br />
				<h3 id="resultSummary">Result summary section:</h3>
				<table>
					<tr>
						<td><img src="images/resultSummarySection.png" border="1" /></td>
					</tr>
					<tr>
						<td><p class="ptext">This section shows parameters used
								in analysis, experiment number and name, result ID, analysis
								running time.</p></td>
					</tr>
				</table>
				<p class="dottedline"></p>
				<h3 id="experimentResult">Experiment result section:</h3>
				<table>
					<tr>
						<td><img src="images/experimentResultSection.png" border="1" /></td>
					</tr>
					<tr>
						<td><p class="ptext">The result of each experiment and
								each region is listed one by one. Each region contains 6 types
								of result. The result is also shown in different forms, such as
								text, figure and genome browser link. A link to download all
								result files is provided in the bottom of the page.</p></td>
					</tr>
					<tr>
						<td><b>Methylation Pattern:</b>grouped reads with same
							methylation status.</td>
					</tr>
					<tr>
						<td><b>MethylationWithMutation pattern:</b>For each
							methylation pattern, child patterns can then be grouped again by
							mutation patterns</td>
					</tr>
					<tr>
						<td><b>Mutation Pattern:</b> grouped reads with same mutation
							status.</td>
					</tr>
					<tr>
						<td><b>MutationWithMethylation pattern:</b>For each mutation
							pattern, child patterns can be grouped again by methylation
							patterns</td>
					</tr>
					<tr>
						<td><b>ASM pattern:</b> If two methylation patterns have at
							least one differential methylated CpG site, we call it potential
							allele-specific methylation pattern.</td>
					</tr>
					<tr>
						<td><b>Other information:</b> contains summary information
							such as methylation rate in each CpG site, number of reads
							filtered by threshold, mismatch count in each base-pair.</td>
					</tr>
				</table>
				<p class="dottedline"></p>

				<table>
					<tr>
						<td><h3>Result types:</h3></td>
					</tr>
					<tr>
						<td><h4>Text:</h4></td>
					</tr>
					<tr>
						<td><p class="ptext">In the text representation of
								pattern, "@@" means one methylated CpG site, "**" means
								unmethylated CpG site. "-" means non-CpG site nucleotide,
								Mismatch comparing to reference is represented by real character
								of that position. Read count and percentage of each pattern is
								shown at the end. Here the percentages for Methylation and
								mutation pattern are calculated as read count of the pattern
								divided by total count of reads. Percentages of
								MethylationWithMutation and MutationWithMethylation are
								calculated as the read count of pattern divided by read count of
								parent pattern. Last column is pattern ID or parent pattern ID.</p></td>
					</tr>
					<tr>
						<td>Example:<br />
						<a href="images/text_example.png" target="_blank"><img
								src="images/text_example.png" border="1" width="620" /></a></td>
					</tr>
					<tr>
						<td><h4>Figure:</h4></td>
					</tr>
					<tr>
						<td><p class="ptext">Currently support PNG and EPS format
								figure. For patterns, black circle refers to methylated CpG
								site, white one refers to non-methylated CpG site. The meaning
								of percentage of pattern is same to text result. The last
								colorful row is average methylation status of each CpG site. The
								methylation rate is calculated from all reads in this region.
								The color scale is from Red(100%) to Green (0%). Mutation(SNP)
								here is represented by a blue bar.</p>
							<p class="ptext">For ASM pattern, there are only two colorful
								patterns. Each of the pattern represent an allele methylation
								pattern.</p></td>
					</tr>
					<tr>
						<td>Example:<br />
						<a href="images/figure_example.png" target="_blank"><img
								src="images/figure_example.png" border="1" width="620" /></a></td>
					</tr>
					<tr>
						<td><h4>Genome Browser Link:</h4></td>
					</tr>
					<tr>
						<td><p class="ptext">Those links point to UCSC genome
								browser with loading custom track. Target region is covered with
								black bar. The layout and color scale is same to figure result.
								dbSNP track is loaded by default. More help information can be
								found in UCSC genome browser website.</p></td>
					</tr>
					<tr>
						<td>Example:<br />
						<a href="images/gb_example.png" target="_blank"><img
								src="images/gb_example.png" border="1" width="620" /></a></td>
					</tr>
					<tr>
						<td><h4>dbSNP link:</h4></td>
					</tr>
					<tr>
						<td><p class="ptext">If the SNP associated with
								allele-specific methylation pattern is contained in dbSNP
								database, a link to dbSNP page of that SNP will be shown in the
								result page.</p></td>
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
