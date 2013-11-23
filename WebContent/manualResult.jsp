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
					3. Result phase:</br>
				</p>
				</br>
				<p class="dottedline"></p>
				</br>
				<h4>Result summary section:</h4>
				</br> <img src="images/resultSummary_sample.png" border="1" /></br> </br>
				<p class="ptext">This section contains parameters used in this
					analysis run and analysis running time.</p>
				</br>
				<p class="dottedline"></p>
				</br>
				<h4>Experiment result section:</h4>
				</br> <img src="images/ResultExperiment_sample.png" border="1" /></br> </br>
				<p class="ptext">
					In this section, the result of each experiment is shown separately.
					Each amplicon region occupy one row. It contains methylation
					pattern, corresponding figure of methylation pattern, methylation
					pattern with mutation information, mutation pattern, mutation
					pattern with methylation information, and some other information in
					Stat link.</br> In the text representation of pattern, "@@" means one
					methylated CpG site, "**" means unmethylated CpG site. "-" means
					non-CpG site nucleotide, "|" means mismatch comparing with
					reference.
				</p>
				</br>
				<h5>Methylation Pattern:</h5>
				<p class="ptext">
					Methylation Pattern is obtained by grouping reads with same
					methylation status on all CpG sites. In the text result, each line
					contains a methylation pattern. First column is text represented
					pattern, second column is the number of reads which share this
					pattern. Third column is the percentage of number of reads of this
					pattern in the total number of reads. The last column is the ID of
					this pattern, which can be used to find corresponding children
					pattern with mutation information. (Click to see large figure) </br> <a
						href="images/methylPattern_sample.png" target="_blank"> <img
						src="images/methylPattern_sample.png" border="1" width="620" /></a></br>

					Figure representation of methylation patterns is also provided
					here. In the figure, it shows experiment name, corresponding genome
					position, read number and percentage. The last line display average
					methylation rate by color. </br> <a href="images/figure_sample.png"
						target="_blank"> <img src="images/figure_sample.png"
						border="1" width="620" /></a></br>
				</p>
				</br>
				<h5>Methylation Pattern with mutation information:</h5>
				<p class="ptext">
					Methylation Pattern with mutation information is obtained from
					corresponding methylation pattern. Each pattern is derived from
					corresponding methylation pattern, with considering mutation
					information. Last column is corresponding methylation pattern ID.
					(Click to see large figure) </br> <a
						href="images/methylPatternWithMutation_sample.png" target="_blank">
						<img src="images/methylPatternWithMutation_sample.png" border="1"
						width="620" />
					</a></br>
				</p>
				</br>
				<h5>Mutation Pattern:</h5>
				<p class="ptext">
					The difference between mutation pattern and methylation pattern
					mentioned above is that methylation pattern consider only
					methylation information to group reads, but mutation pattern use
					mismatches to group reads. It may contains potential SNP
					information. (Click to see large figure) </br> <a
						href="images/methylPattern_sample.png" target="_blank"> <img
						src="images/mutationPattern_sample.png" border="1" width="620" /></a></br>
				</p>
				</br>
				<h5>Mutation Pattern with methylation information:</h5>
				<p class="ptext">
					Mutation pattern with metyhylation information consider mismatches
					first. Then for each group, it group reads by methylation pattern.
					(Click to see large figure) </br> <a
						href="images/methylPatternWithMutation_sample.png" target="_blank">
						<img src="images/mutationPatternWithMethyl_sample.png" border="1"
						width="620" />
					</a></br>
				</p>

				<h5>Other information:</h5>
				<p class="ptext">
					In the stat file, it first contains methylation rate for each CpG
					site. First column is position in reference, then methylation rate.
					Besides, bisulfite conversion rate threshold and sequence identity
					threshold used in analysis process is listed. The read counts before and after filtering are shown below those thresholds. 
					Mismatches count in each position is shown in the end of this file. (Click to see large
					figure) </br> <a href="images/Stat_sample.png" target="_blank"> <img
						src="images/Stat_sample.png" border="1" width="509" />
					</a></br>
				</p>
				
				</br><a href="manual.jsp">Back</a>

			</div>
			<div id="content_bottom"></div>
		</div>
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
