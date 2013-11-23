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
				<b>BS-PAT</b>: A methylation and mutation pattern analysis tool for amplicon bisulfite sequencing data.</br>
				</br>
				<p class="ptext">
				It has features below: (click to see large picture)</br>
				1. Call bisulfite sequence mapping tools (Bismark) for sequence mapping and execute methylation state calling;</br>
				2. Deal with multiplex inputs with and without barcodes;</br>
				3. Users can get result notice by email and retrieve results by IDs;</br></br>
				<a href="images/analysisPage_sample.png" target="_blank"> <img src="images/analysisPage_sample.png" width="300"  /></a></br></br>
				4. Provide multiple parameters for quality control and result filtering;</br></br>
				<a href="images/mappingResult_sample.png" target="_blank"><img src="images/mappingResult_sample.png" width="300" /></a></br></br>
				5. Show grouped methylation patterns of reads and methylation patterns with mutation information;</br></br>
				<a href="images/methylPattern_sample.png" target="_blank"><img src="images/methylPattern_sample.png" width="620" /></a></br>
				<a href="images/methylPatternWithMutation_sample.png" target="_blank"><img src="images/methylPatternWithMutation_sample.png" width="620"/></a></br></br>
				6. Show grouped mutation patterns of reads and mutation patterns with methylation information;</br></br>
				<a href="images/mutationPattern_sample.png" target="_blank"><img src="images/mutationPattern_sample.png" width="620" /></a></br>
				<a href="images/mutationPatternWithMethyl_sample.png" target="_blank"><img src="images/mutationPatternWithMethyl_sample.png" width="620" /></a></br></br>
				7. Show grouped methylation patterns of reads and the average methylation pattern for designated reference positions in figures;</br>
				<a href="images/figure_sample.png" target="_blank"><img src="images/figure_sample.png" width="630"  /></a></br>
				</p>
			</div>
			<div id="content_bottom"></div>
		</div>
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
