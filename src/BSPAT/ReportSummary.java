package BSPAT;

import DataType.SNP;

public class ReportSummary {
	private String stat;
	private String methylation;
	private String mutation;
	private String methylationWithMutation;
	private String mutationWithMethylation;
	private String figure;
	private String ASMFigure;
	private String ASMGBLink;
	private String GBLink;
	private boolean hasASM;
	private SNP snp;

	public void replacePath(String outputPath, String replace, boolean hasFigure, String host) {
		stat = stat.replace(outputPath, replace);
		methylation = methylation.replace(outputPath, replace);
		mutation = mutation.replace(outputPath, replace);
		methylationWithMutation = methylationWithMutation.replace(outputPath, replace);
		mutationWithMethylation = mutationWithMethylation.replace(outputPath, replace);
		if (hasFigure == true) {
			figure = figure.replace(outputPath, replace);
			GBLink = GBLink.replace(outputPath, replace);
			GBLink = host + GBLink;
			if (hasASM == true) {
				ASMFigure = ASMFigure.replace(outputPath, replace);
				ASMGBLink = ASMGBLink.replace(outputPath, replace);
				ASMGBLink = host + ASMGBLink;
			}
		}
	}

	public String generateHTML(String region, boolean coorReady, String frState) {
		String html = "";
		html += "<tr><td>" + region + frState + ": </td>" + "<td><p id=\"resultParagraph\">"
				+ "<a href=" + methylation + ">Pattern</a>\n";
		if (coorReady == true) {
			html += "(<a href=" + figure + ".png" + ">PNG</a>)\n";
			html += "(<a href=" + figure + ".eps" + ">EPS</a>)\n";
			html += "(<a href=http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg18&position=chr7&hgt.customText=http://"
					+ GBLink + ">GenomeBroswer</a>)\n";
		}
		html += "<a href=" + methylationWithMutation + ">PatternWithMutation</a>\n" + "<a href="
				+ mutation + ">Mutation</a>\n" + "<a href=" + mutationWithMethylation
				+ ">MutationWithPattern</a>\n<a href=" + stat + ">Stat</a>\n</p>";
		if (coorReady == true && hasASM) {
			html += "(<a href=" + ASMFigure + ".png" + ">ASM_PNG</a>)\n";
			if (this.snp != null) {
				html += "<a href=http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?searchType=adhoc_search&type=rs&rs="
						+ snp.getrsID() + ">SNP</a>\n";
			}
			html += "(<a href=http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg18&position=chr7&hgt.customText=http://"
					+ ASMGBLink + ">ASM_GenomeBroswer</a>)\n" + "</td></tr>";
		} else {
			html += "</td></tr>";
		}
		return html;
	}

	public void setStat(String stat) {
		this.stat = stat;
	}

	public void setMethylation(String methylation) {
		this.methylation = methylation;
	}

	public void setMethylationWithMutation(String methylationWithMutation) {
		this.methylationWithMutation = methylationWithMutation;
	}

	public void setMutation(String mutation) {
		this.mutation = mutation;
	}

	public void setMutationWithMethylation(String mutationWithMethylation) {
		this.mutationWithMethylation = mutationWithMethylation;
	}

	public void setFigure(String figure) {
		this.figure = figure;
	}

	public void setASMFigure(String figure) {
		this.ASMFigure = figure;
	}

	public void setGBLink(String link) {
		this.GBLink = link;
	}
	
	public void setASMGBLink(String link) {
		this.ASMGBLink = link;
	}

	public boolean hasASM() {
		return hasASM;
	}

	public void setHasASM(boolean hasASM) {
		this.hasASM = hasASM;
	}

	public void setSNP(SNP snp) {
		this.snp = snp;
	}

}
