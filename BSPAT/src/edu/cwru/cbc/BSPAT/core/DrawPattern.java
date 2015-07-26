package edu.cwru.cbc.BSPAT.core;

import com.google.common.io.Files;
import edu.cwru.cbc.BSPAT.DataType.*;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;
import org.apache.commons.io.Charsets;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class DrawPattern {
	private final static Logger LOGGER = Logger.getLogger(DrawPattern.class.getName());
	private static final int CELLLINE_FONT_SIZE = 32;
	public static final int CELLLINE_CHAR_LENGTH = CELLLINE_FONT_SIZE;
	private static final int BPWIDTH = 10;
	private static final int REGION_NAME_LEFTSTART = 10;
	private static final int FIGURE_STARTX = REGION_NAME_LEFTSTART + 10;
	private static final int FIGURE_STARTY = 20;
	private static final int BAR_HEIGHT = 28;
	private static final int HEIGHT_INTERVAL = 26;
	private static final int CG_RADIUS = 20;
	private static final double RGB_INTERVAL = 255 / 50.0;
	private static final int COMMON_FONT_SIZE = 28;
	private static final int SMALL_PERCENT_FONT_SIZE = 10;
	private String figure_font;
	private String figureFormat;
	private String refVersion;
	private String toolsPath;
	private String region;
	private String patternResultPath;
	private String sampleName;
	private Map<String, Coordinate> coordinateMap;
	private ReadAnalysisResult data;
	private String cellLine;
	private String strand;

	public DrawPattern(String figureFormat, String refVersion, String toolsPath, String region,
	                   String patternResultPath, String sampleName, Map<String, Coordinate> coordinateMap) throws
			IOException {
		super();
		this.figureFormat = figureFormat;
		this.refVersion = refVersion;
		this.toolsPath = toolsPath;
		this.region = region;
		this.patternResultPath = patternResultPath;
		this.sampleName = sampleName;
		this.coordinateMap = coordinateMap;
		this.data = new ReadAnalysisResult(patternResultPath, sampleName, region, coordinateMap.get(region));
		this.cellLine = data.getCellLine();
		this.strand = coordinateMap.get(region).getStrand();
		Properties properties = new Properties();
		properties.load(new FileInputStream(Constant.DISKROOTPATH + Constant.propertiesFileName));
		figure_font = properties.getProperty("figure_font");
	}

	private void buildFigureFrame(Graphics2D graphWriter, int imageHeight, int imageWidth, int height, int left,
	                              String chr, String beginCoor, String endCoor, int refLength,
	                              List<CpGStatistics> cpGStatisticsList) {
		// 1. add coordinates
		graphWriter.setBackground(Color.WHITE);
		graphWriter.clearRect(0, 0, imageWidth, imageHeight);
		graphWriter.setPaint(Color.BLACK);
		graphWriter.setFont(new Font(figure_font, Font.PLAIN, COMMON_FONT_SIZE));
		graphWriter.drawString("chr" + chr + ":" + beginCoor, left, height);
		int endCoorLeft = left + (refLength - 1) * BPWIDTH;
		int beginCoorRight = left + (beginCoor.length() + chr.length()) * 15 + BPWIDTH;
		graphWriter.drawString(endCoor, endCoorLeft > beginCoorRight ? endCoorLeft : beginCoorRight, height);

		// 2. add reference bar
		graphWriter.setStroke(new BasicStroke(2.0f));
		height += HEIGHT_INTERVAL;
		graphWriter.drawLine(left, height, left + refLength * BPWIDTH, height);
		graphWriter.setStroke(new BasicStroke());

		// 3. add refCpGSites
		for (CpGStatistics cpgStat : cpGStatisticsList) {
			graphWriter.fill(
					new Rectangle2D.Double(
							left + (cpgStat.getPosition() < 0 ? 0 : cpgStat.getPosition()) * BPWIDTH,
							height - BAR_HEIGHT / 4,
							(cpgStat.getPosition() < 0 || cpgStat.getPosition() == refLength - 1) ? BPWIDTH : CG_RADIUS,
							BAR_HEIGHT / 2));
		}
	}

	public void drawPattern(PatternLink patternLink) throws IOException {
		List<PatternResult> patternResultLists = data.readPatternFile(region, patternLink.getPatternType());
		List<CpGStatistics> statList = data.getStatList();
		int targetLength = data.getRegionLength();
		String chr = data.getCoordinate().getChr();
		String strand = data.getCoordinate().getStrand();
		int beginCoor = data.getCoordinate().getStart();
		int endCoor = data.getCoordinate().getEnd();

		if (patternLink.getPatternType().equals(PatternLink.METHYLATION)) {
			int startPos = statList.get(0).getPosition() < 0 ? 0 : statList.get(0).getPosition();
			if (strand.equals("-")) {
				endCoor = endCoor - startPos;
				beginCoor = endCoor - targetLength + 1;
			} else {
				beginCoor = beginCoor + startPos;
				endCoor = beginCoor + targetLength - 1;
			}

			List<CpGStatistics> updatedStatList = new ArrayList<>(statList.size());
			int firstCpGPos = statList.get(0).getPosition() < 0 ? 0 : statList.get(0).getPosition();
			for (CpGStatistics aStatList : statList) {
				CpGStatistics updatedCpG = new CpGStatistics(aStatList);
				updatedCpG.setPosition(aStatList.getPosition() - firstCpGPos);
				updatedStatList.add(updatedCpG);
			}
			statList = updatedStatList;
		}

		int height = FIGURE_STARTY;
		int left = FIGURE_STARTX + cellLine.length() * CELLLINE_CHAR_LENGTH;

		int imageWidth = targetLength * BPWIDTH + left + (Integer.toString(beginCoor).length() + Integer.toString(
				endCoor)
				.length() + chr.length()) * 15;
		int imageHeight = FIGURE_STARTY + 180 + patternResultLists.size() * HEIGHT_INTERVAL;

		FigureWriter methylWriter = new FigureWriter(patternResultPath, figureFormat, region,
				patternLink.getPatternType(), imageWidth, imageHeight);

		patternLink.setGBResultLink(methylWriter.getGBLinkFileName());
		patternLink.setFigureResultLink(methylWriter.getFigureName());

		buildFigureFrame(methylWriter.getGraphWriter(), imageHeight, imageWidth, height, left, chr,
				String.valueOf(beginCoor), String.valueOf(endCoor), targetLength, statList);

		// 4. add CpG sites
		DecimalFormat percent = new DecimalFormat("##.00%");
		height += HEIGHT_INTERVAL;
		methylWriter.getGraphWriter().drawString("Read Count(%)", (targetLength + 2) * BPWIDTH + left,
				height + HEIGHT_INTERVAL / 2);
		height += HEIGHT_INTERVAL;
		methylWriter.getGraphWriter().setFont(new Font(figure_font, Font.PLAIN, CELLLINE_FONT_SIZE));
		methylWriter.getGraphWriter().drawString(cellLine, REGION_NAME_LEFTSTART, height);
		methylWriter.getGraphWriter().setFont(new Font(figure_font, Font.PLAIN, COMMON_FONT_SIZE));

		methylWriter.getBedWriter().write(
				String.format("browser position chr%s:%d-%d\nbrowser hide all\n", chr, beginCoor, endCoor));
		for (int i = 0; i < patternResultLists.size(); i++) {
			PatternResult patternResult = patternResultLists.get(i);
			methylWriter.getBedWriter().write(
					String.format("track name=\"Pattern%d\" description=\"%s-%s\" visibility=1 itemRgb=\"On\"\n", i,
							sampleName, region));
			methylWriter.getBedWriter().write(
					String.format("chr%s\t%d\t%d\trefbar\t0\t%s\t%d\t%d\t0,0,0\n", chr, beginCoor - 1, endCoor, strand,
							beginCoor - 1, beginCoor - 1));
			for (CpGSitePattern cpg : patternResult.getCpGList()) {
				int cgPos = beginCoor + cpg.getPosition();
				// genome browser automatically add 1 to start, no change to
				// end.So we deduct 1 from start and add 1 to the end.
				methylWriter.getBedWriter().write(
						String.format("chr%s\t%d\t%d\tCG-Pattern%d\t%d\t%s\t%d\t%d\t", chr, cgPos - 1, cgPos + 1, i,
								cpg.getMethylCount(), strand, cgPos - 1, cgPos + 1));
				if (cpg.isMethylated()) {
					// fill black circle
					methylWriter.getGraphWriter().fill(
							new Ellipse2D.Double(left + cpg.getPosition() * BPWIDTH, height, CG_RADIUS, CG_RADIUS));
					methylWriter.getBedWriter().write("0,0,0\n");
				} else {
					// draw empty circle
					methylWriter.getGraphWriter().draw(
							new Ellipse2D.Double(left + cpg.getPosition() * BPWIDTH, height, CG_RADIUS, CG_RADIUS));
					methylWriter.getBedWriter().write("224,224,224\n");
				}
			}
			methylWriter.getGraphWriter().drawString(
					patternResult.getCount() + "(" + percent.format(patternResult.getPercent()) + ")",
					(targetLength + 2) * BPWIDTH + left, height + HEIGHT_INTERVAL);
			addAllele(patternResult, methylWriter.getGraphWriter(), methylWriter.getBedWriter(), chr, beginCoor, height,
					left);
			height += HEIGHT_INTERVAL;
		}

		methylWriter.getBedWriter().write(String.format(
				"browser position chr%s:%d-%d\nbrowser hide all\ntrack name=\"Average\" description=\"%s-%s\" visibility=1 itemRgb=\"On\"\n",
				chr, beginCoor, endCoor, sampleName, region));
		methylWriter.getBedWriter().write(
				String.format("chr%s\t%d\t%d\trefbar\t0\t%s\t%d\t%d\t0,0,0\n", chr, beginCoor - 1, endCoor, strand,
						beginCoor - 1, beginCoor - 1));

		addAverage(methylWriter.getGraphWriter(), figure_font, statList, chr, beginCoor, "Pattern-Average",
				methylWriter.getBedWriter(), height, left);
		methylWriter.close();
	}

	public void drawASMPattern(ReportSummary reportSummary, PatternResult patternWithAllele,
	                           PatternResult patternWithoutAllele,
	                           String logPath) throws IOException, InterruptedException {
		int refLength = data.getRegionLength();
		String chr = data.getCoordinate().getChr();
		String strand = data.getCoordinate().getStrand();
		int beginCoor = data.getCoordinate().getStart();
		int endCoor = data.getCoordinate().getEnd();
		List<CpGStatistics> statList = data.getStatList();

		int height = FIGURE_STARTY;
		int left = FIGURE_STARTX + cellLine.length() * CELLLINE_CHAR_LENGTH;
		int imageWidth = refLength * BPWIDTH + left + 240;
		int imageHeight = FIGURE_STARTY + 180 + 10 * HEIGHT_INTERVAL;

		FigureWriter ASMWriter = new FigureWriter(patternResultPath, figureFormat, region, "ASM", imageWidth,
				imageHeight);
		reportSummary.setASMGBLink(ASMWriter.getGBLinkFileName());
		reportSummary.setASMFigureLink(ASMWriter.getFigureName());

		buildFigureFrame(ASMWriter.getGraphWriter(), imageHeight, imageWidth, height, left, chr,
				String.valueOf(beginCoor), String.valueOf(endCoor), refLength, statList);

		DecimalFormat percent = new DecimalFormat("##.00%");

		ASMWriter.getBedWriter().write(
				String.format("browser position chr%s:%d-%d\nbrowser hide all\n", chr, beginCoor, endCoor));
		height += 2 * HEIGHT_INTERVAL;
		ASMWriter.getGraphWriter().drawString("Read Count(%)", (refLength + 2) * BPWIDTH + left,
				height - HEIGHT_INTERVAL / 2);

		ASMWriter.getGraphWriter().setFont(new Font(figure_font, Font.PLAIN, CELLLINE_FONT_SIZE));
		ASMWriter.getGraphWriter().drawString(cellLine, REGION_NAME_LEFTSTART, height);
		ASMWriter.getGraphWriter().setFont(new Font(figure_font, Font.PLAIN, COMMON_FONT_SIZE));

		// add average for pattern without allele
		ASMWriter.getBedWriter().write(
				String.format("track name=\"PatternA\" description=\"%s-%s-ASM\" visibility=1 itemRgb=\"On\"\n",
						sampleName, region));
		ASMWriter.getBedWriter().write(
				String.format("chr%s\t%d\t%d\trefbar\t0\t%s\t%d\t%d\t0,0,0\n", chr, beginCoor - 1,
						endCoor, strand, beginCoor - 1, beginCoor - 1));
		addAverage(ASMWriter.getGraphWriter(), figure_font, patternWithoutAllele.getCpGList(), chr, beginCoor,
				"PatternA", ASMWriter.getBedWriter(), height, left);
		addAllele(patternWithoutAllele, ASMWriter.getGraphWriter(), ASMWriter.getBedWriter(), chr, beginCoor,
				height + HEIGHT_INTERVAL, left);
		height += HEIGHT_INTERVAL * 1.5;
		ASMWriter.getGraphWriter().drawString(
				patternWithoutAllele.getCount() + "(" + percent.format(patternWithoutAllele.getPercent()) + ")",
				(refLength + 2) * BPWIDTH + left, height);

		// add average for pattern with allele
		ASMWriter.getBedWriter().write(
				String.format("track name=\"PatternB\" description=\"%s-%s-ASM\" visibility=1 itemRgb=\"On\"\n",
						sampleName, region));
		ASMWriter.getBedWriter().write(
				String.format("chr%s\t%d\t%d\trefbar\t0\t%s\t%d\t%d\t0,0,0\n", chr, beginCoor - 1,
						endCoor, strand, beginCoor - 1, beginCoor - 1));
		height += 2 * HEIGHT_INTERVAL;
		addAverage(ASMWriter.getGraphWriter(), figure_font, patternWithAllele.getCpGList(), chr, beginCoor, "PatternB",
				ASMWriter.getBedWriter(), height, left);
		addAllele(patternWithAllele, ASMWriter.getGraphWriter(), ASMWriter.getBedWriter(), chr, beginCoor,
				height + HEIGHT_INTERVAL, left);
		// set snp info
		if (patternWithAllele.hasAllele()) {
			List<SNP> snpList;
			snpList = retreiveSNP(chr, convertCoordinates(chr, coordinateMap.get(region).getStart(), "hg38",
					patternResultPath, logPath) +
					patternWithAllele.getAlleleList().get(0), "1");
			if (snpList != null && snpList.size() > 0) {
				reportSummary.setASMsnp(snpList.get(0));
			}
		}
		height += HEIGHT_INTERVAL * 1.5;
		ASMWriter.getGraphWriter().drawString(
				patternWithAllele.getCount() + "(" + percent.format(patternWithAllele.getPercent()) + ")",
				(refLength + 2) * BPWIDTH + left, height);
		ASMWriter.close();
	}

	private void addAllele(PatternResult patternResult, Graphics2D graphWriter, BufferedWriter bedWriter, String chr,
	                       int startPos, int height, int left) throws IOException {
		if (patternResult.hasAllele()) {
			List<Integer> alleleList = patternResult.getAlleleList();
			graphWriter.setPaint(Color.BLUE);
			for (int j = 0; j < alleleList.size(); j++) {
				graphWriter.fill(
						new Rectangle2D.Double(left + (alleleList.get(j) * BPWIDTH), height, CG_RADIUS / 2,
								CG_RADIUS));
				int allelePos = startPos + patternResult.getAlleleList().get(0);
				bedWriter.write(
						String.format("chr%s\t%d\t%d\tSNP-Pattern%d\t%d\t%s\t%d\t%d\t0,0,255\n", chr,
								allelePos - 1, allelePos, j, 1000, strand, allelePos - 1, allelePos));
			}
			graphWriter.setPaint(Color.BLACK);
		}
	}

	private void addAverage(Graphics2D graphWriter, String fontChoice, List<? extends CpG> cpgList, String chr,
	                        int startPos, String patternName, BufferedWriter bedWriter, int height,
	                        int left) throws IOException {
		// 5. add average
		DecimalFormat percentSmall = new DecimalFormat("##%");
		height += HEIGHT_INTERVAL;
		for (CpG cpg : cpgList) {
			int R = 0, G = 0, B = 0;
			double methylLevel = cpg.getMethylLevel();
			if (methylLevel > 0.5) {
				G = (int) (255 - ((methylLevel - 0.5) * 100 * RGB_INTERVAL));
				R = 255;
			}
			if (methylLevel < 0.5) {
				G = 255;
				R = (int) (methylLevel * 100 * RGB_INTERVAL);
			}
			if (methylLevel == 0.5) {
				R = 255;
				G = 255;
			}
			graphWriter.setPaint(new Color(R, G, B));
			graphWriter.fill(new Ellipse2D.Double(left + cpg.getPosition() * BPWIDTH, height, CG_RADIUS, CG_RADIUS));
			graphWriter.setPaint(Color.BLACK);
			// move percentage a little left and shink the font size
			graphWriter.setFont(new Font(fontChoice, Font.PLAIN, SMALL_PERCENT_FONT_SIZE));
			graphWriter.drawString(percentSmall.format(cpg.getMethylLevel()), left + cpg.getPosition() * BPWIDTH,
					height + HEIGHT_INTERVAL * 2);

			int absolutePos = startPos + cpg.getPosition();
			// genome browser automatically add 1 to start, no change to end.So
			// we substract 1 from start and add 1 to the end.
			bedWriter.write(
					"chr" + chr + "\t" + (absolutePos - 1) + "\t" + (absolutePos + 1) + "\tCG-" + patternName + "\t" +
							cpg.getMethylCount() + "\t" + strand + "\t" + (absolutePos - 1) + "\t" + (absolutePos + 1) + "\t" + R + "," +
							G + "," + B + "\n");

		}
	}

	private List<SNP> retreiveSNP(String chr, long pos, String maxRet) throws RemoteException {
		List<SNP> snps = new ArrayList<>();
		String fetchIds = "";
		// 1. search region
		EUtilsServiceStub service = new EUtilsServiceStub();
		// call NCBI ESearch utility
		// NOTE: search term should be URL encoded
		EUtilsServiceStub.ESearchRequest req = new EUtilsServiceStub.ESearchRequest();
		req.setDb("snp");
		String term = chr + "[CHR]+AND+\"Homo sapiens\"[Organism]+AND+" + pos + "[CHRPOS]";
		LOGGER.info(term);
		req.setTerm(term);
		req.setRetMax(maxRet);
		EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);
		// results output
		LOGGER.info("SNP range:");
		LOGGER.info("Found ids: " + res.getCount());
		LOGGER.info("First " + res.getRetMax() + " ids: ");

		if (res.getCount().equals("0")) {
			return snps;
		}

		int N = res.getIdList().getId().length;
		for (int i = 0; i < N; i++) {
			if (i > 0) fetchIds += ",";
			fetchIds += res.getIdList().getId()[i];
		}

		// 3. fetch SNP
		EFetchSnpServiceStub serviceF = new EFetchSnpServiceStub();
		// call NCBI EFetch utility
		EFetchSnpServiceStub.EFetchRequest reqF = new EFetchSnpServiceStub.EFetchRequest();
		reqF.setId(fetchIds);
		EFetchSnpServiceStub.EFetchResult resF = serviceF.run_eFetch(reqF);
		if (resF.getExchangeSet() == null || resF.getExchangeSet().getRs() == null) {
			return null;
		}
		// results output
		for (int i = 0; i < resF.getExchangeSet().getRs().length; i++) {
			EFetchSnpServiceStub.Rs_type0 obj = resF.getExchangeSet().getRs()[i];
			SNP snp = new SNP(obj.getRsId(), obj.getSnpType(), obj.getMolType(), obj.getPhenotype(), obj.getAssembly(),
					obj.getFrequency());
			snp.print();
			snps.add(snp);
		}
		return snps;
	}

	private long convertCoordinates(String chr, long pos, String targetRefVersion, String patternResultPath,
	                                String logPath) throws IOException, InterruptedException {
		if (refVersion.equals(targetRefVersion)) {
			return pos;
		}
		String liftOverPath = toolsPath + "/liftover/";
		String originPosFileName = "tmpCoordinate." + refVersion;
		String targetPosFileName = "tmpCoordinate." + targetRefVersion;
		String chain = refVersion + "ToHg" + targetRefVersion.replace("hg", "") + ".over.chain.gz";
		// write pos into file
		try (BufferedWriter coorWriter = new BufferedWriter(new FileWriter(patternResultPath + originPosFileName))) {
			coorWriter.write("chr" + chr + ":" + pos + "-" + pos + "\n");
		}

		// call liftover
		String liftOverAbsPath = new File(liftOverPath).getAbsolutePath();
		LOGGER.info("Call liftOver:");
		List<String> cmdList = Arrays.asList(liftOverAbsPath + "/liftOver", "-positions", originPosFileName,
				liftOverAbsPath + "/" + chain, targetPosFileName, "/dev/null");
		if (Utilities.callCMD(cmdList, new File(patternResultPath), logPath + "/liftover.log") > 0) {
			throw new RuntimeException("liftover failed<br>" + "logs:<br>" +
					Files.toString(new File(logPath + "/liftover.log"), Charsets.UTF_8));
		}
		LOGGER.info("liftOver finished");
		// read result
		String[] items;
		try (BufferedReader coorReader = new BufferedReader(new FileReader(patternResultPath + targetPosFileName))) {
			items = coorReader.readLine().split(":");
		}
		File originPosFile = new File(patternResultPath + originPosFileName);
		if (!originPosFile.delete()) {
			throw new RuntimeException("Failed to delete: " + originPosFile.getAbsolutePath());
		}
		File targetPosFile = new File(patternResultPath + targetPosFileName);
		if (!targetPosFile.delete()) {
			throw new RuntimeException("Failed to delete: " + originPosFile.getAbsolutePath());
		}
		IO.deleteFiles(patternResultPath, new String[]{".bed", ".bedmapped", ".bedunmapped"});
		return Long.valueOf(items[1].split("-")[0]);
	}

}
