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
					1. Mapping phase:</br>
				</p>
				<p class="ptext">It requires user to upload necessary files and
					provide paramaters for sequence mapping.</p>
				</br>
				<p class="dottedline"></p>
				</br>
				<h4>Reference and Parameter section:</h4>
				</br> <img src="images/ref&par_sample.png" border="1" /></br> </br>
				<p class="ptext">
					<table>
						<tr>
							<td>Email address:</td>
							<td>The email address is used to inform user when the
								mapping or analysis procedure has finished. A unique execution ID is sent along
								with the email for retrieving the result.</td>
						</tr>
						<tr>
							<td>Reference Sequence File:</td>
							<td>File contains designed amplicon references. It should be
								in fasta format.(.fa .fasta). The label of each reference sequence should not contain any space.</td>
						</tr>
						<tr>
							<td>Quality Score type:</td>
							<td>Bowtie parameter. User need to specify what kind of quality type used in the sequence file. 
							Only available for fastq input. Moreetails can be found in <a
								href="http://bowtie-bio.sourceforge.net/manual.shtml">bowtie
									manual</a>.
							</td>
						</tr>
						<tr>
							<td>Maximum Permitted Mismatches:</td>
							<td>Bowtie parameter. Maximum number of mismatches permitted in the "seed",
								i.e. the first L base pairs of the read (where L is set with
								-l/--seedlen). This may be 0, 1, 2 or 3 and the default is 2.
								This option is mutually exclusive with the -v option. More
								details can be found in <a
								href="http://bowtie-bio.sourceforge.net/manual.shtml">bowtie
									manual</a>.
							</td>
						</tr>
					</table>
				</p>
				</br>
				<p class="dottedline"></p>
				</br>
				<h4>Experiment section:</h4>
				</br> <img src="images/experiment_sample.png" border="1" /></br> </br>
				<p class="ptext">
					In this section, user need to assign an experiment name in textbox
					and select one or multiple fastq or fasta files to upload for each
					experiment. The extension of fastq and fasta files can be .fq, .fastq, .fa, .fasta
					.txt(.txt input is by default treated as fastq file). </br> </br>Supported
					data settings are:</br> (a) separate amplicon sequence file without barcode (default);</br> (b) multiplex sequence file with barcode
					(need to upload barcode file);</br> </br> <img
						src="images/experimentWithOptions_sample.png" border="1" /></br> </br>If
					user use the second setting data, it reqiures to check
					multiplex checkbox, which will display further options. For
					setting(b), user need to check Multiplex checkbox and upload
					barcode file. One example content of barcode file is
					"ProstateBSF4F(tab)GTTTTTTAGGAGGAGGGTTTGTATA", first column is
					amplicon ID, second column is barcode. Two columns seperated by tab.
				</p>
				</br>
				<h4>Button section:</h4>
				</br> <img src="images/mappingButton_sample.png" border="1" /></br> </br> In the
				bottom of the page, "add experiment" and " delete experiment"
				buttons are used to add and delete experiment above. After finished
				set all parameters, user need to click "Execute" button to submit
				this page. </br> <a href="manual.jsp">Back</a>

			</div>
			<div id="content_bottom"></div>
		</div>
		<%@ include file="footer.html"%>
	</div>
</body>
</html>
