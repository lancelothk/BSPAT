package BSPAT;

import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;
import graph.ReadAnalysisResult;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import DataType.Coordinate;
import DataType.CpGSite;
import DataType.CpGStatistics;
import DataType.PatternResult;
import DataType.SNP;

public class DrawPattern {
	private final int WIDTH = 20;
	private final int STARTX = 120;
	private final int STARTY = 20;
	private final int LEFTSTART = 10;
	private final int BARHEIGHT = 28;
	private final int HEIGHTINTERVAL = 26;
	private int height = STARTY;
	private final int RADIUS = 20;
	private final double RGBinterval = 255 / 50.0;
	private int styleChoice = 0;
	private int commonSize = 24;
	private int celllineSize = 30;
	private int smallPercentSize = 16;
	

	public void drawMethylPattern(String region, String patternResultPath, String sampleName, String frState, ReportSummary reportSummary,
			HashMap<String, Coordinate> coordinates) throws IOException {

		System.out.println("readCoordinates -- DrawSingleFigure");
		System.out.println("ReadAnalysisResult -- DrawSingleFigure");
		ReadAnalysisResult data = new ReadAnalysisResult(patternResultPath, sampleName, region,
				frState, coordinates.get(region));
		System.out.println("DrawSingleGraph() -- DrawSingleFigure");

		File folder = new File(patternResultPath + "pics/");
		if (!folder.exists()) {
			folder.mkdirs();
		}

		String cellLine = data.getCellLine();
		ArrayList<Integer> refCpGs = data.getRefCpGs();
		ArrayList<PatternResult> patternResultLists = data.getPatternResultLists();
		ArrayList<CpGStatistics> statList = data.getStatList();
		int refLength = data.getRefLength();
		String beginCoor = data.getBeginCoor();
		String endCoor = data.getEndCoor();
		
		String GBLinkFileName = patternResultPath + "pics/" + region + frState + ".bed";
		FileWriter fileWriter = new FileWriter(GBLinkFileName);
		reportSummary.setGBLink(GBLinkFileName);
		BufferedWriter bedWriter = new BufferedWriter(fileWriter);

		String fontChoice = "Courier New";

		try {
			String fileName = patternResultPath + "pics/" + region + frState;
			reportSummary.setFigure(fileName);
			FileOutputStream out = new FileOutputStream(fileName + ".eps");// "/home/ke/test.eps");
			File outputPNG = new File(fileName + ".png");

			int imageWidth = refLength * WIDTH + STARTX + 210;
			int imageHeight = STARTY + 180 + patternResultLists.size() * HEIGHTINTERVAL;

			BufferedImage off_image = new BufferedImage(imageWidth, imageHeight,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D gImage = off_image.createGraphics();
			Graphics2D g2 = new EpsGraphics("title", out, 0, 0, imageWidth, imageHeight,
					ColorMode.COLOR_RGB);

			// 1. add coordinates
			// gImage.setColor(Color.WHITE);
			gImage.setBackground(Color.WHITE);
			gImage.clearRect(0, 0, imageWidth, imageHeight);
			// gImage.setBackground(Color.WHITE);
			gImage.setPaint(Color.BLACK);
			g2.setFont(new Font(fontChoice, styleChoice, commonSize));
			gImage.setFont(new Font(fontChoice, styleChoice, commonSize));
			g2.drawString(beginCoor, STARTX + WIDTH, height);
			gImage.drawString(beginCoor, STARTX + WIDTH, height);
			g2.drawString(endCoor, STARTX + refLength * WIDTH - WIDTH, height);
			gImage.drawString(endCoor, STARTX + refLength * WIDTH - WIDTH, height);

			// 2. add reference bar
			g2.setStroke(new BasicStroke(2.0f));
			height += HEIGHTINTERVAL;
			g2.drawLine(STARTX, height, STARTX + refLength * WIDTH, height);
			g2.setStroke(new BasicStroke());
			gImage.drawLine(STARTX, height, STARTX + refLength * WIDTH, height);
			gImage.setStroke(new BasicStroke());

			// 3. add refCpGSites
			for (int i = 0; i < refCpGs.size(); i++) {
				g2.drawLine(STARTX + refCpGs.get(i) * WIDTH, height - BARHEIGHT / 2, STARTX
						+ refCpGs.get(i) * WIDTH, height + BARHEIGHT / 2);
				gImage.drawLine(STARTX + refCpGs.get(i) * WIDTH, height - BARHEIGHT / 2, STARTX
						+ refCpGs.get(i) * WIDTH, height + BARHEIGHT / 2);
			}

			// 4. add CpG sites
			DecimalFormat percent = new DecimalFormat("##.00%");
			height += HEIGHTINTERVAL;
			g2.drawString("Read Count(%)", (refLength * WIDTH) + WIDTH + STARTX, height
					+ HEIGHTINTERVAL);
			gImage.drawString("Read Count(%)", (refLength * WIDTH) + WIDTH + STARTX, height
					+ HEIGHTINTERVAL);
			height += HEIGHTINTERVAL;
			g2.setFont(new Font(fontChoice, styleChoice, celllineSize));
			g2.drawString(cellLine, LEFTSTART, height);
			g2.setFont(new Font(fontChoice, styleChoice, commonSize));
			gImage.setFont(new Font(fontChoice, styleChoice, celllineSize));
			gImage.drawString(cellLine, LEFTSTART, height);
			gImage.setFont(new Font(fontChoice, styleChoice, commonSize));
			
			String chr = beginCoor.split(":")[0];
			String startPos = beginCoor.split(":")[1];
			for(int i=0;i<patternResultLists.size();i++){
				PatternResult patternResult = patternResultLists.get(i);
				bedWriter.write("browser position " + beginCoor + "-" + endCoor
						+ "\nbrowser hide all\ntrack name=\"Pattern" + i + "\" description=\"" + sampleName
						+ "-" + region + "\" visibility=1 itemRgb=\"On\"\n");
				bedWriter.write(chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos
						+ "\t" + startPos + "\t0,0,0\n");
				for (CpGSite cpg : patternResult.getCpGList()) {
					int cgPos = Integer.parseInt(startPos) + cpg.getPosition();
					// genome browser automatically add 1 to start, no change to end.So we substract 1 from start and add 1 to the end.
					bedWriter.write(chr + "\t" + (cgPos-1) + "\t" + (cgPos + 1) + "\tCG-Pattern" + i + "\t"
							+ cpg.getMethylCount() + "\t+\t" + (cgPos-1) + "\t" + (cgPos + 1) + "\t");
					if (cpg.getMethylLabel() == true) {
						// fill black circle
						g2.fill(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH
								/ 2, height, RADIUS, RADIUS));
						gImage.fill(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH)
								- WIDTH / 2, height, RADIUS, RADIUS));
						bedWriter.write("0,0,0\n");
					} else {
						// draw empty circle
						g2.setStroke(new BasicStroke(0.05f));
						g2.draw(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH
								/ 2, height, RADIUS, RADIUS));
						g2.setStroke(new BasicStroke());
						gImage.setStroke(new BasicStroke(0.05f));
						gImage.draw(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH)
								- WIDTH / 2, height, RADIUS, RADIUS));
						gImage.setStroke(new BasicStroke());
						bedWriter.write("254,254,254\n");
					}
				}
				g2.drawString(
						patternResult.getCount() + "(" + percent.format(patternResult.getPercent())
								+ ")", (refLength * WIDTH) + WIDTH + STARTX, height
								+ HEIGHTINTERVAL);
				gImage.drawString(
						patternResult.getCount() + "(" + percent.format(patternResult.getPercent())
								+ ")", (refLength * WIDTH) + WIDTH + STARTX, height
								+ HEIGHTINTERVAL);
				height += HEIGHTINTERVAL;
			}

			// 5. add average
			DecimalFormat percentSmall = new DecimalFormat("##%");
			height += HEIGHTINTERVAL;
			// g2.drawString("Average", (refLength * WIDTH) + WIDTH + STARTX,
			// height + HEIGHTINTERVAL);
			bedWriter.write("browser position " + beginCoor + "-" + endCoor
					+ "\nbrowser hide all\ntrack name=\"Average" + "\" description=\"" + sampleName
					+ "-" + region + "\" visibility=1 itemRgb=\"On\"\n");
			bedWriter.write(chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos
					+ "\t" + startPos + "\t0,0,0\n");
			for (CpGStatistics cpgStat : statList) {
				int R = 0, G = 0, B = 0;
				if (cpgStat.getMethylationRate() > 0.5) {
					G = 255 - (int) ((cpgStat.getMethylationRate() - 0.5) * 100 * RGBinterval);
					R = 255;
				}
				if (cpgStat.getMethylationRate() < 0.5) {
					G = 255;
					R = (int) (cpgStat.getMethylationRate() * 100 * RGBinterval);
				}
				if (cpgStat.getMethylationRate() == 0.5) {
					R = 255;
					G = 255;
				}
				g2.setPaint(new Color(R, G, B));
				g2.fill(new Ellipse2D.Double(STARTX + (cpgStat.getPosition() * WIDTH) - WIDTH / 2,
						height, RADIUS, RADIUS));
				g2.setPaint(Color.BLACK);
				gImage.setPaint(new Color(R, G, B));
				gImage.fill(new Ellipse2D.Double(STARTX + (cpgStat.getPosition() * WIDTH) - WIDTH
						/ 2, height, RADIUS, RADIUS));
				gImage.setPaint(Color.BLACK);
				// move percentage a little left and shink the font size
				g2.setFont(new Font(fontChoice, styleChoice, smallPercentSize));
				g2.drawString(percentSmall.format(cpgStat.getMethylationRate()).toString(), STARTX
						+ (cpgStat.getPosition() * WIDTH) - WIDTH / 2, height + HEIGHTINTERVAL * 2);
				gImage.setFont(new Font(fontChoice, styleChoice, smallPercentSize));
				gImage.drawString(percentSmall.format(cpgStat.getMethylationRate()).toString(),
						STARTX + (cpgStat.getPosition() * WIDTH) - WIDTH / 2, height
								+ HEIGHTINTERVAL * 2);
				int cgPos = Integer.parseInt(startPos) + cpgStat.getPosition();
				// genome browser automatically add 1 to start, no change to end.So we substract 1 from start and add 1 to the end.
				bedWriter.write(chr + "\t" + (cgPos-1) + "\t" + (cgPos + 1) + "\tCG-Pattern-Average"+ "\t"
						+ cpgStat.getCountOfmethylatedSites() + "\t+\t" + (cgPos-1) + "\t" + (cgPos + 1) + "\t" + R + ","
						+ G + "," + B + "\n");
			}

			// Get the EPS output.
			String output = g2.toString();
			System.out.println(output);
			out.close();

			// png output
			ImageIO.write(off_image, "png", outputPNG);

			g2.dispose();
			gImage.dispose();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		bedWriter.close();
	}

	public void drawMethylPatternWithAllele(String region, String patternResultPath, String sampleName, String frState, ReportSummary reportSummary,
			HashMap<String, Coordinate> coordinates) throws IOException {
		System.out.println("readCoordinates -- DrawSingleFigure");
		System.out.println("ReadAnalysisResult -- DrawSingleFigure");
		ReadAnalysisResult data = new ReadAnalysisResult(patternResultPath, sampleName, region,
				frState, coordinates.get(region));
		System.out.println("DrawSingleGraph() -- DrawSingleFigure");

		File folder = new File(patternResultPath + "pics/");
		if (!folder.exists()) {
			folder.mkdirs();
		}

		String ASMGBLinkFileName = patternResultPath + "pics/" + region + frState + "_ASM.bed";
		FileWriter fileWriter = new FileWriter(ASMGBLinkFileName);
		reportSummary.setASMGBLink(ASMGBLinkFileName);
		BufferedWriter bedWriter = new BufferedWriter(fileWriter);

		String cellLine = data.getCellLine();
		ArrayList<Integer> refCpGs = data.getRefCpGs();

		ArrayList<PatternResult> patternResultLists = data.getPatternResultLists();
		ArrayList<PatternResult> allelePatternResultsLists = new ArrayList<PatternResult>();
		for (int i = 0; i < patternResultLists.size(); i++) {
			if (patternResultLists.get(i).hasAllele()) {
				allelePatternResultsLists.add(patternResultLists.get(i));
				patternResultLists.remove(i);
				i--;
			}
		}
		if (allelePatternResultsLists.size() == 0 || patternResultLists.size() == 0) {
			bedWriter.close();
			return;
		}
		PatternResult patternWithAllele = new PatternResult(allelePatternResultsLists.get(0));
		PatternResult patternWithoutAllele = new PatternResult(patternResultLists.get(0));
		for (PatternResult patternResult : allelePatternResultsLists) {
			for (int i = 0; i < patternResult.getCpGList().size(); i++) {
				if (patternResult.getCpGList().get(i).getMethylLabel()) {
					patternWithAllele.getCpGList().get(i).methylCountPlus(patternResult.getCount());
				}
				patternWithAllele.getCpGList().get(i).totalCountPlus(patternResult.getCount());
			}
		}

		for (PatternResult patternResult : patternResultLists) {
			for (int i = 0; i < patternResult.getCpGList().size(); i++) {
				if (patternResult.getCpGList().get(i).getMethylLabel()) {
					patternWithoutAllele.getCpGList().get(i)
							.methylCountPlus(patternResult.getCount());
				}
				patternWithoutAllele.getCpGList().get(i).totalCountPlus(patternResult.getCount());
			}
		}

		ArrayList<CpGStatistics> statList = data.getStatList();
		int refLength = data.getRefLength();
		String beginCoor = data.getBeginCoor();
		String endCoor = data.getEndCoor();
		String fontChoice = "Courier New";

		if (!hasASM(patternWithAllele, patternWithoutAllele)) {
			reportSummary.setHasASM(false);
			bedWriter.close();
			return;
		} else {
			reportSummary.setHasASM(true);
		}

		try {
			String fileName = patternResultPath + "pics/" + region + frState + "_ASM";
			reportSummary.setASMFigure(fileName);
			FileOutputStream out = new FileOutputStream(fileName + ".eps");// "/home/ke/test.eps");
			File outputPNG = new File(fileName + ".png");

			int imageWidth = refLength * WIDTH + STARTX + 210;
			int imageHeight = STARTY + 180 + 10 * HEIGHTINTERVAL;

			BufferedImage off_image = new BufferedImage(imageWidth, imageHeight,
					BufferedImage.TYPE_INT_RGB);
			Graphics2D gImage = off_image.createGraphics();
			Graphics2D g2 = new EpsGraphics("title", out, 0, 0, imageWidth, imageHeight,
					ColorMode.COLOR_RGB);

			// 1. add coordinates
			// gImage.setColor(Color.WHITE);
			gImage.setBackground(Color.WHITE);
			gImage.clearRect(0, 0, imageWidth, imageHeight);
			// gImage.setBackground(Color.WHITE);
			gImage.setPaint(Color.BLACK);
			g2.setFont(new Font(fontChoice, styleChoice, commonSize));
			gImage.setFont(new Font(fontChoice, styleChoice, commonSize));
			g2.drawString(beginCoor, STARTX + WIDTH, height);
			gImage.drawString(beginCoor, STARTX + WIDTH, height);
			g2.drawString(endCoor, STARTX + refLength * WIDTH - WIDTH, height);
			gImage.drawString(endCoor, STARTX + refLength * WIDTH - WIDTH, height);

			// 2. add reference bar
			g2.setStroke(new BasicStroke(2.0f));
			gImage.setStroke(new BasicStroke(2.0f));
			height += HEIGHTINTERVAL;
			g2.drawLine(STARTX, height, STARTX + refLength * WIDTH, height);
			g2.setStroke(new BasicStroke());
			gImage.drawLine(STARTX, height, STARTX + refLength * WIDTH, height);
			gImage.setStroke(new BasicStroke());

			// 3. add refCpGSites
			for (int i = 0; i < refCpGs.size(); i++) {
				g2.drawLine(STARTX + refCpGs.get(i) * WIDTH, height - BARHEIGHT / 2, STARTX
						+ refCpGs.get(i) * WIDTH, height + BARHEIGHT / 2);
				gImage.drawLine(STARTX + refCpGs.get(i) * WIDTH, height - BARHEIGHT / 2, STARTX
						+ refCpGs.get(i) * WIDTH, height + BARHEIGHT / 2);
			}

			String chr = beginCoor.split(":")[0];
			String startPos = beginCoor.split(":")[1];

			// add average for pattern with allele
			height += HEIGHTINTERVAL;
			bedWriter.write("browser position " + beginCoor + "-" + endCoor
					+ "\nbrowser hide all\nbrowser full snp130\ntrack name=\"PatternA\" description=\"" + sampleName
					+ "-" + region + "-ASM\" visibility=1 itemRgb=\"On\"\n");
			bedWriter.write(chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos
					+ "\t" + startPos + "\t0,0,0\n");
			addAverage(gImage, g2, fontChoice, patternWithAllele, chr, startPos, "PatternA",
					bedWriter);
			// set snp info
			if (patternWithAllele.hasAllele()) {
				ArrayList<SNP> snpList = retreiveSNP(chr.replace("chr", ""), coordinates
						.get(region).getStarthg19() + patternWithAllele.getAlleleLocus(), "1");
				if (snpList != null && snpList.size() > 0) {
					reportSummary.setSNP(snpList.get(0));
				}
			}
			height += HEIGHTINTERVAL;

			// add average for pattern without allele
			height += HEIGHTINTERVAL;
			bedWriter.write("track name=\"PatternB\" description=\"" + sampleName
					+ "-" + region + "-ASM\" visibility=1 itemRgb=\"On\"\n");
			bedWriter.write(chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos
					+ "\t" + startPos + "\t0,0,0\n");
			addAverage(gImage, g2, fontChoice, patternWithoutAllele, chr, startPos, "PatternB",
					bedWriter);
			height += HEIGHTINTERVAL;

			// Get the EPS output.
			String output = g2.toString();
			System.out.println(output);
			out.close();

			// png output
			ImageIO.write(off_image, "png", outputPNG);

			g2.dispose();
			gImage.dispose();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		bedWriter.close();
	}

	private void addAverage(Graphics2D gImage, Graphics2D g2, String fontChoice,
			PatternResult patternResult, String chr, String startPos, String patternName,
			BufferedWriter bedWriter) throws IOException {
		// 5. add average
		DecimalFormat percentSmall = new DecimalFormat("##%");
		height += HEIGHTINTERVAL;
		// g2.drawString("Average", (refLength * WIDTH) + WIDTH + STARTX, height
		// + HEIGHTINTERVAL);
		if (patternResult.hasAllele()) {
			gImage.setPaint(Color.BLUE);
			g2.setPaint(Color.BLUE);
			g2.fill(new Rectangle2D.Double(STARTX + (patternResult.getAlleleLocus() * WIDTH)
					- WIDTH / 2, height - HEIGHTINTERVAL / 2, RADIUS / 2, RADIUS * 2));
			gImage.fill(new Rectangle2D.Double(STARTX + (patternResult.getAlleleLocus() * WIDTH)
					- WIDTH / 2, height - HEIGHTINTERVAL / 2, RADIUS / 2, RADIUS * 2));
			int allelePos = Integer.parseInt(startPos) + patternResult.getAlleleLocus();
			bedWriter.write(chr + "\t" + (allelePos-1) + "\t" + allelePos + "\tSNP-"
					+ patternName + "\t" + 1000 + "\t+\t" + (allelePos-1) + "\t" + allelePos
					+ "\t0,0,255\n");
		}
		for (CpGSite cpg : patternResult.getCpGList()) {
			int R = 0, G = 0, B = 0;
			if (cpg.getMethylLevel() > 0.5) {
				G = 255 - (int) ((cpg.getMethylLevel() - 0.5) * 100 * RGBinterval);
				R = 255;
			}
			if (cpg.getMethylLevel() < 0.5) {
				G = 255;
				R = (int) (cpg.getMethylLevel() * 100 * RGBinterval);
			}
			if (cpg.getMethylLevel() == 0.5) {
				R = 255;
				G = 255;
			}
			g2.setPaint(new Color(R, G, B));
			g2.fill(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height,
					RADIUS, RADIUS));
			g2.setPaint(Color.BLACK);
			gImage.setPaint(new Color(R, G, B));
			gImage.fill(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2,
					height, RADIUS, RADIUS));
			gImage.setPaint(Color.BLACK);
			int cgPos = Integer.parseInt(startPos) + cpg.getPosition();
			// genome browser automatically add 1 to start, no change to end.So we substract 1 from start and add 1 to the end.
			bedWriter.write(chr + "\t" + (cgPos-1) + "\t" + (cgPos + 1) + "\tCG-" + patternName + "\t"
					+ cpg.getMethylCount() + "\t+\t" + (cgPos-1) + "\t" + (cgPos + 1) + "\t" + R + ","
					+ G + "," + B + "\n");
			// move percentage a little left and shink the font size
			g2.setFont(new Font(fontChoice, styleChoice, smallPercentSize));
			g2.drawString(percentSmall.format(cpg.getMethylLevel()).toString(),
					STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height + HEIGHTINTERVAL * 2);
			gImage.setFont(new Font(fontChoice, styleChoice, smallPercentSize));
			gImage.drawString(percentSmall.format(cpg.getMethylLevel()).toString(),
					STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height + HEIGHTINTERVAL * 2);
		}
	}

	private boolean hasASM(PatternResult patternWithAllele, PatternResult patternWithoutAllele) {
		ArrayList<CpGSite> cglistWithAllele = patternWithAllele.getCpGList();
		ArrayList<CpGSite> cglistWithoutAllele = patternWithoutAllele.getCpGList();
		for (int i = 0; i < cglistWithAllele.size(); i++) {
			if (cglistWithAllele.get(i).getMethylType() != cglistWithoutAllele.get(i)
					.getMethylType()) {
				return true;
			}
		}
		return false;
	}

	private ArrayList<SNP> retreiveSNP(String chr, long pos, String maxRet) {
		String fetchIds = "";
		// 1. search region
		try {
			EUtilsServiceStub service = new EUtilsServiceStub();
			// call NCBI ESearch utility
			// NOTE: search term should be URL encoded
			EUtilsServiceStub.ESearchRequest req = new EUtilsServiceStub.ESearchRequest();
			req.setDb("snp");
			String term = chr + "[CHR]+AND+\"Homo sapiens\"[Organism]+AND+" + pos + "[CHRPOS]";
			req.setTerm(term);
			req.setRetMax(maxRet);
			EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);
			// results output
			System.out.println("SNP range:");
			System.out.println("Found ids: " + res.getCount());
			System.out.print("First " + res.getRetMax() + " ids: ");

			int N = res.getIdList().getId().length;
			for (int i = 0; i < N; i++) {
				if (i > 0)
					fetchIds += ",";
				fetchIds += res.getIdList().getId()[i];
			}
			System.out.println();
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		// 3. fetch SNP
		ArrayList<SNP> snps = new ArrayList<SNP>();
		try {
			EFetchSnpServiceStub service = new EFetchSnpServiceStub();
			// call NCBI EFetch utility
			EFetchSnpServiceStub.EFetchRequest req = new EFetchSnpServiceStub.EFetchRequest();
			req.setId(fetchIds);
			EFetchSnpServiceStub.EFetchResult res = service.run_eFetch(req);
			// results output
			for (int i = 0; i < res.getExchangeSet().getRs().length; i++) {
				EFetchSnpServiceStub.Rs_type0 obj = res.getExchangeSet().getRs()[i];
				SNP snp = new SNP(obj.getRsId(), obj.getSnpType(), obj.getMolType(),
						obj.getPhenotype(), obj.getAssembly(), obj.getFrequency());
				snp.print();
				snps.add(snp);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return snps;
	}
}
