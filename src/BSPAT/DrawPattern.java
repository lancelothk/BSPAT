package BSPAT;

import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import DataType.Constant;
import DataType.Coordinate;
import DataType.CpGSite;
import DataType.CpGStatistics;
import DataType.PatternResult;
import DataType.PatternLink;
import DataType.SNP;

public class DrawPattern {
	private final int WIDTH = 20;
	private final int STARTX = 120;
	private final int STARTY = 20;
	private final int LEFTSTART = 10;
	private final int BARHEIGHT = 28;
	private final int HEIGHTINTERVAL = 26;
	private final int RADIUS = 20;
	private final double RGBinterval = 255 / 50.0;
	private int styleChoice = 0;
	private int commonSize = 24;
	private int celllineSize = 30;
	private int smallPercentSize = 16;
	private String figureFormat;
	private String refVersion;
	private String toolsPath;

	public DrawPattern(String figureFormat, String refVersion, String toolsPath) {
		super();
		this.figureFormat = figureFormat;
		this.refVersion = refVersion;
		this.toolsPath = toolsPath;
	}

	public void drawMethylPattern(String region, String patternResultPath, PatternLink patternLink, String sampleName, String frState,
			ReportSummary reportSummary, HashMap<String, Coordinate> coordinates) throws IOException {
		int height = STARTY;
		System.out.println("readCoordinates -- DrawSingleFigure");
		ReadAnalysisResult data = new ReadAnalysisResult(patternResultPath, patternLink.getPatternType(), sampleName, region, frState,
				coordinates.get(region));
		System.out.println("start drawMethylPattern");
		// set pattern result picture folder
		File folder = new File(patternResultPath + "pics/");
		if (!folder.exists()) {
			folder.mkdirs();
		}

		// variable initialization
		String cellLine = data.getCellLine();
		ArrayList<Integer> refCpGs = data.getRefCpGs();
		ArrayList<PatternResult> patternResultLists = data.getPatternResultLists();
		ArrayList<CpGStatistics> statList = data.getStatList();
		int refLength = data.getRefLength();
		String beginCoor = data.getBeginCoor();
		String endCoor = data.getEndCoor();
		String fontChoice = "Courier New";

		// set GB bed file link
		String GBLinkFileName = patternResultPath + "pics/" + region + frState + "-" + patternLink.getPatternType() + ".bed";
		FileWriter fileWriter = new FileWriter(GBLinkFileName);
		patternLink.setGBResultLink(GBLinkFileName);
		BufferedWriter bedWriter = new BufferedWriter(fileWriter);

		try {
			int imageWidth = refLength * WIDTH + STARTX + 210;
			int imageHeight = STARTY + 180 + patternResultLists.size() * HEIGHTINTERVAL;
			String fileName = patternResultPath + "pics/" + region + frState + "-" + patternLink.getPatternType();
			Graphics2D graphWriter = null;
			BufferedImage pngImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
			FileOutputStream epsImage = null;
			if (figureFormat.equals(Constant.PNG)) {
				graphWriter = pngImage.createGraphics();// png writer
			} else if (figureFormat.equals(Constant.EPS)) {
				epsImage = new FileOutputStream(fileName + ".eps");
				graphWriter = new EpsGraphics("title", epsImage, 0, 0, imageWidth, imageHeight, ColorMode.COLOR_RGB); // eps
																														// writer
			}
			patternLink.setFigureResultLink(fileName);

			// 1. add coordinates
			graphWriter.setBackground(Color.WHITE);
			graphWriter.clearRect(0, 0, imageWidth, imageHeight);
			graphWriter.setPaint(Color.BLACK);
			graphWriter.setFont(new Font(fontChoice, styleChoice, commonSize));
			graphWriter.drawString("chr" + beginCoor, STARTX + WIDTH, height);
			graphWriter.drawString(endCoor, STARTX + refLength * WIDTH - WIDTH, height);

			// 2. add reference bar
			graphWriter.setStroke(new BasicStroke(2.0f));
			height += HEIGHTINTERVAL;
			graphWriter.drawLine(STARTX, height, STARTX + refLength * WIDTH, height);
			graphWriter.setStroke(new BasicStroke());

			// 3. add refCpGSites
			for (int i = 0; i < refCpGs.size(); i++) {
				graphWriter
						.drawLine(STARTX + refCpGs.get(i) * WIDTH, height - BARHEIGHT / 2, STARTX + refCpGs.get(i) * WIDTH, height + BARHEIGHT / 2);
			}

			// 4. add CpG sites
			DecimalFormat percent = new DecimalFormat("##.00%");
			height += HEIGHTINTERVAL;
			graphWriter.drawString("Read Count(%)", (refLength * WIDTH) + WIDTH + STARTX, height + HEIGHTINTERVAL);
			height += HEIGHTINTERVAL;
			graphWriter.setFont(new Font(fontChoice, styleChoice, celllineSize));
			graphWriter.drawString(cellLine, LEFTSTART, height);
			graphWriter.setFont(new Font(fontChoice, styleChoice, commonSize));

			String chr = beginCoor.split(":")[0];
			String startPos = beginCoor.split(":")[1];
			for (int i = 0; i < patternResultLists.size(); i++) {
				PatternResult patternResult = patternResultLists.get(i);
				if (patternResult.hasAllele() == true) {
					ArrayList<Integer> alleleList = patternResult.getAlleleList();
					graphWriter.setPaint(Color.BLUE);
					for (int j = 0; j < alleleList.size(); j++) {
						graphWriter.fill(new Rectangle2D.Double(STARTX + (alleleList.get(j) * WIDTH) - WIDTH / 2, height - HEIGHTINTERVAL / 2,
								RADIUS / 2, RADIUS * 2));
						int allelePos = Integer.parseInt(startPos) + patternResult.getAlleleList().get(0);
						bedWriter.write("chr" + chr + "\t" + (allelePos - 1) + "\t" + allelePos + "\tSNP-" + j + "\t" + 1000 + "\t+\t"
								+ (allelePos - 1) + "\t"

								+ allelePos + "\t0,0,255\n");
					}
					graphWriter.setPaint(Color.BLACK);
				}
				bedWriter.write("browser position " + "chr" + beginCoor + "-" + endCoor + "\nbrowser hide all\ntrack name=\"Pattern" + i
						+ "\" description=\"" + sampleName + "-" + region + "\" visibility=1 itemRgb=\"On\"\n");
				bedWriter.write("chr" + chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos + "\t" + startPos + "\t0,0,0\n");
				for (CpGSite cpg : patternResult.getCpGList()) {
					int cgPos = Integer.parseInt(startPos) + cpg.getPosition();
					// genome browser automatically add 1 to start, no change to
					// end.So we substract 1 from start and add 1 to the end.
					bedWriter.write("chr" + chr + "\t" + (cgPos - 1) + "\t" + (cgPos + 1) + "\tCG-Pattern" + i + "\t" + cpg.getMethylCount()
							+ "\t+\t" + (cgPos - 1) + "\t" + (cgPos + 1) + "\t");
					if (cpg.getMethylLabel() == true) {
						// fill black circle
						graphWriter.fill(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height, RADIUS, RADIUS));
						bedWriter.write("0,0,0\n");
					} else {
						// draw empty circle
						graphWriter.setStroke(new BasicStroke(0.05f));
						graphWriter.draw(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height, RADIUS, RADIUS));
						graphWriter.setStroke(new BasicStroke());
						bedWriter.write("254,254,254\n");
					}
				}
				graphWriter.drawString(patternResult.getCount() + "(" + percent.format(patternResult.getPercent()) + ")", (refLength * WIDTH) + WIDTH
						+ STARTX, height + HEIGHTINTERVAL);
				height += HEIGHTINTERVAL;
			}

			// 5. add average
			DecimalFormat percentSmall = new DecimalFormat("##%");
			height += HEIGHTINTERVAL;
			// g2.drawString("Average", (refLength * WIDTH) + WIDTH + STARTX,
			// height + HEIGHTINTERVAL);
			bedWriter.write("browser position " + "chr" + beginCoor + "-" + endCoor + "\nbrowser hide all\ntrack name=\"Average"
					+ "\" description=\"" + sampleName + "-" + region + "\" visibility=1 itemRgb=\"On\"\n");
			bedWriter.write("chr" + chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos + "\t" + startPos + "\t0,0,0\n");
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
				graphWriter.setPaint(new Color(R, G, B));
				graphWriter.fill(new Ellipse2D.Double(STARTX + (cpgStat.getPosition() * WIDTH) - WIDTH / 2, height, RADIUS, RADIUS));
				graphWriter.setPaint(Color.BLACK);
				// move percentage a little left and shink the font size
				graphWriter.setFont(new Font(fontChoice, styleChoice, smallPercentSize));
				graphWriter.drawString(percentSmall.format(cpgStat.getMethylationRate()).toString(), STARTX + (cpgStat.getPosition() * WIDTH) - WIDTH
						/ 2, height + HEIGHTINTERVAL * 2);
				int cgPos = Integer.parseInt(startPos) + cpgStat.getPosition();
				// genome browser automatically add 1 to start, no change to
				// end.So we substract 1 from start and add 1 to the end.
				bedWriter.write("chr" + chr + "\t" + (cgPos - 1) + "\t" + (cgPos + 1) + "\tCG-Pattern-Average" + "\t"
						+ cpgStat.getCountOfmethylatedSites() + "\t+\t" + (cgPos - 1) + "\t" + (cgPos + 1) + "\t" + R + "," + G + "," + B + "\n");
			}
			if (figureFormat.equals(Constant.PNG)) {
				// png output
				File outputPNG = new File(fileName + ".png");
				ImageIO.write(pngImage, "png", outputPNG);
			} else if (figureFormat.equals(Constant.EPS)) {
				epsImage.close();
			}
			graphWriter.dispose();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		bedWriter.close();
	}

	public void drawMethylPatternWithAllele(String region, String patternResultPath, PatternLink patternType, String sampleName, String frState,
			ReportSummary reportSummary, HashMap<String, Coordinate> coordinates) throws IOException {
		int height = STARTY;
		System.out.println("readCoordinates -- DrawSingleFigure");
		ReadAnalysisResult data = new ReadAnalysisResult(patternResultPath, patternType.getPatternType(), sampleName, region, frState,
				coordinates.get(region));
		System.out.println("start drawMethylPatternWithAllele");

		File folder = new File(patternResultPath + "pics/");
		if (!folder.exists()) {
			folder.mkdirs();
		}

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
		// set GB link
		String ASMGBLinkFileName = patternResultPath + "pics/" + region + frState + "_ASM.bed";
		FileWriter fileWriter = new FileWriter(ASMGBLinkFileName);
		reportSummary.setASMGBLink(ASMGBLinkFileName);
		BufferedWriter bedWriter = new BufferedWriter(fileWriter);
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
			patternWithAllele.countPlus(patternResult.getCount());
		}
		

		for (PatternResult patternResult : patternResultLists) {
			for (int i = 0; i < patternResult.getCpGList().size(); i++) {
				if (patternResult.getCpGList().get(i).getMethylLabel()) {
					patternWithoutAllele.getCpGList().get(i).methylCountPlus(patternResult.getCount());
				}
				patternWithoutAllele.getCpGList().get(i).totalCountPlus(patternResult.getCount());
			}
			patternWithoutAllele.countPlus(patternResult.getCount());
		}
		
		int totalCount = patternWithAllele.getCount() + patternWithoutAllele.getCount();
		patternWithAllele.setPercent(patternWithAllele.getCount() / (double) totalCount);
		patternWithoutAllele.setPercent(patternWithoutAllele.getCount() / (double) totalCount);

		int refLength = data.getRefLength();
		String beginCoor = data.getBeginCoor();
		String endCoor = data.getEndCoor();
		String fontChoice = "Courier New";

		try {
			int imageWidth = refLength * WIDTH + STARTX + 210;
			int imageHeight = STARTY + 180 + 10 * HEIGHTINTERVAL;
			// set figure link
			String fileName = patternResultPath + "pics/" + region + frState + "_ASM";
			Graphics2D graphWriter = null;
			BufferedImage pngImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
			FileOutputStream epsImage = null;
			if (figureFormat.equals(Constant.PNG)) {
				graphWriter = pngImage.createGraphics();// png writer
			} else if (figureFormat.equals(Constant.EPS)) {
				epsImage = new FileOutputStream(fileName + ".eps");
				graphWriter = new EpsGraphics("title", epsImage, 0, 0, imageWidth, imageHeight, ColorMode.COLOR_RGB); // eps
																																			// writer
			}
			reportSummary.setASMFigureLink(fileName);

			if (!hasASM(patternWithAllele, patternWithoutAllele)) {
				reportSummary.setHasASM(false);
				bedWriter.close();
				return;
			} else {
				reportSummary.setHasASM(true);
			}

			// 1. add coordinates
			// gImage.setColor(Color.WHITE);
			graphWriter.setBackground(Color.WHITE);
			graphWriter.clearRect(0, 0, imageWidth, imageHeight);
			// gImage.setBackground(Color.WHITE);
			graphWriter.setPaint(Color.BLACK);
			graphWriter.setFont(new Font(fontChoice, styleChoice, commonSize));
			graphWriter.drawString("chr" + beginCoor, STARTX + WIDTH, height);
			graphWriter.drawString(endCoor, STARTX + refLength * WIDTH - WIDTH, height);

			// 2. add reference bar
			graphWriter.setStroke(new BasicStroke(2.0f));
			height += HEIGHTINTERVAL;
			graphWriter.drawLine(STARTX, height, STARTX + refLength * WIDTH, height);
			graphWriter.setStroke(new BasicStroke());

			// 3. add refCpGSites
			for (int i = 0; i < refCpGs.size(); i++) {
				graphWriter
						.drawLine(STARTX + refCpGs.get(i) * WIDTH, height - BARHEIGHT / 2, STARTX + refCpGs.get(i) * WIDTH, height + BARHEIGHT / 2);
			}

			String chr = beginCoor.split(":")[0];
			String startPos = beginCoor.split(":")[1];

			// add average for pattern with allele
			height += HEIGHTINTERVAL;
			bedWriter.write("browser position " + "chr" + beginCoor + "-" + endCoor
					+ "\nbrowser hide all\nbrowser full snp130\ntrack name=\"PatternA\" description=\"" + sampleName + "-" + region
					+ "-ASM\" visibility=1 itemRgb=\"On\"\n");
			bedWriter.write("chr" + chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos + "\t" + startPos + "\t0,0,0\n");
			DecimalFormat percent = new DecimalFormat("##.00%");
			height += HEIGHTINTERVAL;
			graphWriter.drawString("Read Count(%)", (refLength * WIDTH) + WIDTH + STARTX, height + HEIGHTINTERVAL);
			addAverage(graphWriter, fontChoice, patternWithAllele, chr, startPos, "PatternA", bedWriter, height);
			// set snp info
			if (patternWithAllele.hasAllele()) {
				ArrayList<SNP> snpList = retreiveSNP(chr, convertCoordinates(chr, coordinates.get(region).getStart(), "hg19", patternResultPath)
						+ patternWithAllele.getAlleleList().get(0), "1");
				if (snpList != null && snpList.size() > 0) {
					reportSummary.setASMsnp(snpList.get(0));
				}
			}
			height += HEIGHTINTERVAL;
			graphWriter.drawString(patternWithAllele.getCount() + "(" + percent.format(patternWithAllele.getPercent()) + ")", (refLength * WIDTH)
					+ WIDTH + STARTX, height + HEIGHTINTERVAL);

			// add average for pattern without allele
			height += HEIGHTINTERVAL;
			bedWriter.write("track name=\"PatternB\" description=\"" + sampleName + "-" + region + "-ASM\" visibility=1 itemRgb=\"On\"\n");
			bedWriter.write("chr" + chr + "\t" + startPos + "\t" + endCoor + "\trefbar\t0\t+\t" + startPos + "\t" + startPos + "\t0,0,0\n");
			addAverage(graphWriter, fontChoice, patternWithoutAllele, chr, startPos, "PatternB", bedWriter, height);
			height += HEIGHTINTERVAL;
			graphWriter.drawString(patternWithoutAllele.getCount() + "(" + percent.format(patternWithoutAllele.getPercent()) + ")",
					(refLength * WIDTH) + WIDTH + STARTX, height + HEIGHTINTERVAL);

			if (figureFormat.equals(Constant.PNG)) {
				// png output
				File outputPNG = new File(fileName + ".png");
				ImageIO.write(pngImage, "png", outputPNG);
			} else if (figureFormat.equals(Constant.EPS)) {
				epsImage.close();
			}

			graphWriter.dispose();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		bedWriter.close();
	}

	private void addAverage(Graphics2D gImage, String fontChoice, PatternResult patternResult, String chr, String startPos, String patternName,
			BufferedWriter bedWriter, int height) throws IOException {
		// 5. add average
		DecimalFormat percentSmall = new DecimalFormat("##%");
		height += HEIGHTINTERVAL;
		// + HEIGHTINTERVAL);
		if (patternResult.hasAllele()) {
			gImage.setPaint(Color.BLUE);
			gImage.fill(new Rectangle2D.Double(STARTX + (patternResult.getAlleleList().get(0) * WIDTH) - WIDTH / 2, height - HEIGHTINTERVAL / 2,
					RADIUS / 2, RADIUS * 2));
			int allelePos = Integer.parseInt(startPos) + patternResult.getAlleleList().get(0);
			bedWriter.write("chr" + chr + "\t" + (allelePos - 1) + "\t" + allelePos + "\tSNP-" + patternName + "\t" + 1000 + "\t+\t"
					+ (allelePos - 1) + "\t" + allelePos + "\t0,0,255\n");
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
			gImage.setPaint(new Color(R, G, B));
			gImage.fill(new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height, RADIUS, RADIUS));
			gImage.setPaint(Color.BLACK);
			int cgPos = Integer.parseInt(startPos) + cpg.getPosition();
			// genome browser automatically add 1 to start, no change to end.So
			// we substract 1 from start and add 1 to the end.
			bedWriter.write("chr" + chr + "\t" + (cgPos - 1) + "\t" + (cgPos + 1) + "\tCG-" + patternName + "\t" + cpg.getMethylCount() + "\t+\t"
					+ (cgPos - 1) + "\t" + (cgPos + 1) + "\t" + R + "," + G + "," + B + "\n");
			// move percentage a little left and shink the font size
			gImage.setFont(new Font(fontChoice, styleChoice, smallPercentSize));
			gImage.drawString(percentSmall.format(cpg.getMethylLevel()).toString(), STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height
					+ HEIGHTINTERVAL * 2);
		}
	}

	private boolean hasASM(PatternResult patternWithAllele, PatternResult patternWithoutAllele) {
		ArrayList<CpGSite> cglistWithAllele = patternWithAllele.getCpGList();
		ArrayList<CpGSite> cglistWithoutAllele = patternWithoutAllele.getCpGList();
		for (int i = 0; i < cglistWithAllele.size(); i++) {
			if (cglistWithAllele.get(i).getMethylType() != cglistWithoutAllele.get(i).getMethylType()) {
				return true;
			}
		}
		return false;
	}

	private ArrayList<SNP> retreiveSNP(String chr, long pos, String maxRet) {
		ArrayList<SNP> snps = new ArrayList<SNP>();
		String fetchIds = "";
		// 1. search region
		try {
			EUtilsServiceStub service = new EUtilsServiceStub();
			// call NCBI ESearch utility
			// NOTE: search term should be URL encoded
			EUtilsServiceStub.ESearchRequest req = new EUtilsServiceStub.ESearchRequest();
			req.setDb("snp");
			String term = chr + "[CHR]+AND+\"Homo sapiens\"[Organism]+AND+" + pos + "[CHRPOS]";
			System.out.println(term);
			req.setTerm(term);
			req.setRetMax(maxRet);
			EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);
			// results output
			System.out.println("SNP range:");
			System.out.println("Found ids: " + res.getCount());
			System.out.print("First " + res.getRetMax() + " ids: ");

			if (res.getCount().equals("0")){
				return snps;
			}
			
			int N = res.getIdList().getId().length;
			for (int i = 0; i < N; i++) {
				if (i > 0)
					fetchIds += ",";
				fetchIds += res.getIdList().getId()[i];
			}
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 3. fetch SNP
		try {
			EFetchSnpServiceStub service = new EFetchSnpServiceStub();
			// call NCBI EFetch utility
			EFetchSnpServiceStub.EFetchRequest req = new EFetchSnpServiceStub.EFetchRequest();
			req.setId(fetchIds);
			EFetchSnpServiceStub.EFetchResult res = service.run_eFetch(req);
			// results output
			for (int i = 0; i < res.getExchangeSet().getRs().length; i++) {
				EFetchSnpServiceStub.Rs_type0 obj = res.getExchangeSet().getRs()[i];
				SNP snp = new SNP(obj.getRsId(), obj.getSnpType(), obj.getMolType(), obj.getPhenotype(), obj.getAssembly(), obj.getFrequency());
				snp.print();
				snps.add(snp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return snps;
	}

	private long convertCoordinates(String chr, long pos, String targetRefVersion, String patternResultPath) throws IOException, InterruptedException {
		if (refVersion.equals(targetRefVersion)) {
			return pos;
		}
		String liftOverPath = toolsPath + "/liftover/";
		String originPosFileName = "tmpCoordinate." + refVersion;
		String targetPosFileName = "tmpCoordinate." + targetRefVersion;
		String chain = refVersion + "ToHg" + targetRefVersion.replace("hg", "") + ".over.chain.gz";
		// write pos into file
		BufferedWriter coorWriter = new BufferedWriter(new FileWriter(patternResultPath + originPosFileName));
		coorWriter.write("chr" + chr + ":" + pos + "-" + pos + "\n");
		coorWriter.close();

		// call liftover
		File liftOverPathFile = new File(liftOverPath);
		String callLiftOver = liftOverPathFile.getAbsolutePath() + "/liftOver -positions " + originPosFileName + " "
				+ liftOverPathFile.getAbsolutePath() + "/" + chain + " " + targetPosFileName + " /dev/null";
		System.out.println("Call liftOver:");
		Utilities.callCMD(callLiftOver, new File(patternResultPath));

		// read result
		BufferedReader coorReader = new BufferedReader(new FileReader(patternResultPath + targetPosFileName));
		String[] items = coorReader.readLine().split(":");
		coorReader.close();
		File originPosFile = new File(patternResultPath + originPosFileName);
		originPosFile.delete();
		File targetPosFile = new File(patternResultPath + targetPosFileName);
		targetPosFile.delete();
		IO.deleteFiles(patternResultPath, new String[] { ".bed", ".bedmapped", ".bedunmapped" });
		return Long.valueOf(items[1].split("-")[0]);
	}
}
