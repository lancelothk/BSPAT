package edu.cwru.cbc.BSPAT.MethylFigure;

import edu.cwru.cbc.BSPAT.commons.CpG;
import edu.cwru.cbc.BSPAT.commons.CpGStatistics;
import edu.cwru.cbc.BSPAT.commons.PatternResult;
import edu.cwru.cbc.BSPAT.commons.PotentialSNP;
import org.apache.commons.cli.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kehu on 3/25/16.
 * Command line interface for MethylFigure.
 */
public class MethylFigurePgm {
	private static final int CELLLINE_FONT_SIZE = 32;
	private static final int CELLLINE_CHAR_LENGTH = CELLLINE_FONT_SIZE;
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

	public static void main(String[] args) throws ParseException, IOException {
		Options options = new Options();
		// Require all input path to be directory. File is not allowed.
		options.addOption(
				Option.builder("t").hasArg().desc("Figure format. Support eps and png. Default is png").build());
		options.addOption(Option.builder("f").hasArg().desc("Text font used in figure. Default is Arial").build());
		options.addOption(
				Option.builder("a").desc("Draw ASM pattern. In this case, only pattern result is required.").build());
		options.addOption(Option.builder("h").desc("Help").build());

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("MethylFigure [options] <pattern file> {<report file> | -a }", options);
			System.exit(1);
		}

		String figureFont = cmd.getOptionValue("f", "Arial");
		String figureFormat = cmd.getOptionValue("t", "png");

		if (cmd.hasOption("a")) {
			if (cmd.getArgList().size() != 1) {
				throw new RuntimeException(
						"Incorrect number of arguments! MethylFigure [options] <pattern file> {<report file> | -a }");
			}
			String patternFileName = cmd.getArgList().get(0);
			String regionName = obtainRegionName(patternFileName);
			drawASMFigure(regionName, patternFileName, figureFormat, figureFont);
		} else {
			if (cmd.getArgList().size() != 2) {
				throw new RuntimeException(
						"Incorrect number of arguments! MethylFigure [options] <pattern file> {<report file> | -a }");
			}
			String patternFileName = cmd.getArgList().get(0);
			String reportFileName = cmd.getArgList().get(1);
			String regionName = obtainRegionName(patternFileName);
			if (!regionName.equals(obtainRegionName(reportFileName))) {
				System.err.println("pattern file and report file don't have identical region name!");
				System.exit(1);
			}
			drawFigure(regionName, patternFileName, reportFileName, figureFormat, figureFont);
		}
	}

	private static void drawASMFigure(String regionName, String ASMPatternFileName, String figureFormat,
	                                  String figureFont) throws IOException {
		List<PatternResult> patternResultList = readASMPatterns(ASMPatternFileName);
		List<CpGStatistics> cpGStatisticsList = new ArrayList<>();
		for (PatternResult patternResult : patternResultList) {
			cpGStatisticsList.addAll(patternResult.getCpGList());
		}
		int height = FIGURE_STARTY;
		int left = FIGURE_STARTX + (int) (regionName.length() * CELLLINE_CHAR_LENGTH / 2 * 1.3);
		int imageWidth = left + PatternResult.targetRegionLength * BPWIDTH + 13 * 20;
		int imageHeight = FIGURE_STARTY + 180 + patternResultList.size() * 2 * HEIGHT_INTERVAL;

		FigureWriter methylWriter = new FigureWriter(ASMPatternFileName.replace(".txt", ""), figureFormat, imageWidth,
				imageHeight);
		buildFigureFrame(methylWriter.getGraphWriter(), imageHeight, imageWidth, height, left,
				PatternResult.targetRegionLength, cpGStatisticsList, figureFont);

		DecimalFormat percent = new DecimalFormat("##.00%");
		height += HEIGHT_INTERVAL;
		methylWriter.getGraphWriter().setFont(new Font(figureFont, Font.PLAIN, CELLLINE_FONT_SIZE));
		methylWriter.getGraphWriter().drawString(regionName, REGION_NAME_LEFTSTART, height);
		methylWriter.getGraphWriter().setFont(new Font(figureFont, Font.PLAIN, COMMON_FONT_SIZE));

		// pattern with allele
		addAverage(methylWriter.getGraphWriter(), figureFont, patternResultList.get(0).getCpGList(), height, left);
		addAllele(patternResultList.get(0), methylWriter.getGraphWriter(), height + HEIGHT_INTERVAL, left);
		height += 2 * HEIGHT_INTERVAL;
		methylWriter.getGraphWriter().drawString(
				patternResultList.get(0).getCount() + "(" + percent.format(patternResultList.get(0).getPercent()) + ")",
				(PatternResult.targetRegionLength + 2) * BPWIDTH + left, height);
		// pattern without allele
		addAverage(methylWriter.getGraphWriter(), figureFont, patternResultList.get(1).getCpGList(), height, left);
		height += 2 * HEIGHT_INTERVAL;
		methylWriter.getGraphWriter().drawString(
				patternResultList.get(1).getCount() + "(" + percent.format(patternResultList.get(1).getPercent()) + ")",
				(PatternResult.targetRegionLength + 2) * BPWIDTH + left, height);
		methylWriter.close();
	}

	private static List<PatternResult> readASMPatterns(String ASMPatternFileName) throws IOException {
		List<PatternResult> patternResultLists = new ArrayList<>();
		try (BufferedReader patternBuffReader = new BufferedReader(new FileReader(ASMPatternFileName))) {
			// skip column names
			patternBuffReader.readLine();
			// reference line
			String line = patternBuffReader.readLine();
			int regionLength = line.split("\t")[0].length() - 2;// ignore two space at two ends of reference string
			PatternResult.targetRegionLength = regionLength;
			PatternResult patternWithoutAllele = readASMPattern(patternBuffReader, regionLength);
			PatternResult patternWithAllele = readASMPattern(patternBuffReader, regionLength);

			// skip 2 lines
			patternBuffReader.readLine();
			patternBuffReader.readLine();

			for (CpGStatistics cpg : patternWithAllele.getCpGList()) {
				double methylLevel = Double.parseDouble(patternBuffReader.readLine().split("\t")[1]);
				cpg.setMethylLevel(methylLevel);
			}

			// skip 2 lines
			patternBuffReader.readLine();
			patternBuffReader.readLine();

			for (CpGStatistics cpg : patternWithoutAllele.getCpGList()) {
				double methylLevel = Double.parseDouble(patternBuffReader.readLine().split("\t")[1]);
				cpg.setMethylLevel(methylLevel);
			}

			patternResultLists.add(patternWithoutAllele);
			patternResultLists.add(patternWithAllele);
			patternBuffReader.close();
		}
		return patternResultLists;
	}

	private static PatternResult readASMPattern(BufferedReader patternBuffReader, int regionLength) throws IOException {
		String line;// start to read content
		String[] items;
		line = patternBuffReader.readLine();
		items = line.split("\t");
		String patternString = items[0].trim();
		return parsePatternString(regionLength, items, patternString);
	}

	private static PatternResult parsePatternString(int regionLength, String[] items, String patternString) {
		PatternResult patternResult = new PatternResult();
		for (int i = 0; i < regionLength; i++) {
			CpGStatistics cpg;
			if (patternString.charAt(i) == '*') {
				cpg = new CpGStatistics(i, false);
				if (i + 1 < regionLength && patternString.charAt(i + 1) == '*') {
					i++;
				} else if (i == 0) {
					cpg = new CpGStatistics(i - 1, false);
				} else if (patternString.charAt(i - 1) != '-') {
					cpg = new CpGStatistics(i - 1, false);
				}
				patternResult.addCpG(cpg);
			} else if (patternString.charAt(i) == '@') {
				cpg = new CpGStatistics(i, true);
				if (i + 1 < regionLength && patternString.charAt(i + 1) == '@') {
					i++;
				} else if (i == 0) {
					cpg = new CpGStatistics(i - 1, true);
				} else if (patternString.charAt(i - 1) != '-') {
					cpg = new CpGStatistics(i - 1, false);
				}
				patternResult.addCpG(cpg);
			} else if (patternString.charAt(i) == 'A' || patternString.charAt(i) == 'C' || patternString.charAt(
					i) == 'G' ||
					patternString.charAt(i) == 'T') {
				patternResult.setSnp(new PotentialSNP(i, patternString.charAt(i)));
			}
		}
		patternResult.setCount(Integer.parseInt(items[1]));
		patternResult.setPercent(Double.parseDouble(items[2]));
		return patternResult;
	}

	private static String obtainRegionName(String fileName) {
		return new File(fileName).getName().split("_")[0];
	}

	public static void drawFigure(String regionName, String patternFileName, String reportFileName, String figureFormat,
	                              String figureFont) throws IOException {
		List<PatternResult> patternResultList = readPatternFile(patternFileName);
		List<CpGStatistics> cpGStatisticsList = readReportFile(reportFileName);

		int height = FIGURE_STARTY;
		int left = FIGURE_STARTX + (int) (regionName.length() * CELLLINE_CHAR_LENGTH / 2 * 1.3);
		int imageWidth = left + PatternResult.targetRegionLength * BPWIDTH + 13 * 20;
		int imageHeight = FIGURE_STARTY + 180 + patternResultList.size() * HEIGHT_INTERVAL;

		FigureWriter methylWriter = new FigureWriter(patternFileName.replace(".txt", ""), figureFormat, imageWidth,
				imageHeight);
		buildFigureFrame(methylWriter.getGraphWriter(), imageHeight, imageWidth, height, left,
				PatternResult.targetRegionLength, cpGStatisticsList, figureFont);
		DecimalFormat percent = new DecimalFormat("##.00%");
		height += HEIGHT_INTERVAL * 2;
		methylWriter.getGraphWriter().setFont(new Font(figureFont, Font.PLAIN, CELLLINE_FONT_SIZE));
		methylWriter.getGraphWriter().drawString(regionName, REGION_NAME_LEFTSTART, height);
		methylWriter.getGraphWriter().setFont(new Font(figureFont, Font.PLAIN, COMMON_FONT_SIZE));

		// 4. add CpG sites
		for (PatternResult patternResult : patternResultList) {
			for (CpGStatistics cpg : patternResult.getCpGList()) {
				if (cpg.isMethylated()) {
					// fill black circle
					methylWriter.getGraphWriter().fill(
							new Ellipse2D.Double(left + cpg.getPosition() * BPWIDTH, height, CG_RADIUS, CG_RADIUS));
				} else {
					// draw empty circle
					methylWriter.getGraphWriter().draw(
							new Ellipse2D.Double(left + cpg.getPosition() * BPWIDTH, height, CG_RADIUS, CG_RADIUS));
				}
			}
			methylWriter.getGraphWriter().drawString(
					patternResult.getCount() + "(" + percent.format(patternResult.getPercent()) + ")",
					(PatternResult.targetRegionLength + 2) * BPWIDTH + left, height + HEIGHT_INTERVAL);
			addAllele(patternResult, methylWriter.getGraphWriter(), height, left);
			height += HEIGHT_INTERVAL;
		}

		addAverage(methylWriter.getGraphWriter(), figureFont, cpGStatisticsList, height, left);
		methylWriter.close();
	}

	private static void addAllele(PatternResult patternResult, Graphics2D graphWriter, int height, int left) throws
			IOException {
		if (patternResult.getSnp() != null) {
			graphWriter.setPaint(Color.BLUE);
			graphWriter.fill(new Rectangle2D.Double(left + (patternResult.getSnp().getPosition() * BPWIDTH), height,
					CG_RADIUS / 2, CG_RADIUS));
			graphWriter.setPaint(Color.BLACK);
		}
	}

	private static void addAverage(Graphics2D graphWriter, String fontChoice, List<? extends CpG> cpgList, int height,
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
			Font oldFont = graphWriter.getFont();
			graphWriter.setFont(new Font(fontChoice, Font.PLAIN, SMALL_PERCENT_FONT_SIZE));
			graphWriter.drawString(percentSmall.format(cpg.getMethylLevel()), left + cpg.getPosition() * BPWIDTH,
					height + HEIGHT_INTERVAL * 3 / 2);
			graphWriter.setFont(oldFont);
		}
	}


	private static List<CpGStatistics> readReportFile(String reportFileName) throws IOException {
		List<CpGStatistics> statList = new ArrayList<>();
		try (BufferedReader statBuffReader = new BufferedReader(new FileReader(reportFileName))) {
			String line;
			String[] items;
			// skip target start
			int targetStart = Integer.parseInt(statBuffReader.readLine().split("\t")[1]);
			// read target ref length
			line = statBuffReader.readLine();
			if (line == null) {
				throw new RuntimeException("analysis report is empty!");
			}
			// skip lines
			for (int i = 0; i < 10; i++) {
				statBuffReader.readLine();
			}
			// get start position
			line = statBuffReader.readLine();
			while (line != null && !line.startsWith("mutation")) {
				items = line.split("\t");
				if (!Character.isDigit(items[0].charAt(0))) {
					break;
				}
				// start from target start. 0-based.
				int pos = Integer.parseInt(items[0]) - targetStart;
				CpGStatistics cpgStat = new CpGStatistics(pos, false);
				cpgStat.setMethylLevel(Double.parseDouble(items[1]));
				statList.add(cpgStat);
				line = statBuffReader.readLine();
			}
			statList.sort(CpG::compareTo);
		}
		return statList;
	}

	private static List<PatternResult> readPatternFile(String patternFileName) throws IOException {
		List<PatternResult> patternResultLists = new ArrayList<>();
		try (BufferedReader patternBuffReader = new BufferedReader(new FileReader(patternFileName))) {
			// skip column names
			patternBuffReader.readLine();
			// reference line
			String line = patternBuffReader.readLine();
			int regionLength = line.split("\t")[0].length();
			PatternResult.targetRegionLength = regionLength;

			// start to read content
			line = patternBuffReader.readLine();
			String[] items;
			PatternResult patternResult;

			while (line != null) {
				items = line.split("\t");
				String patternString = items[0];
				patternResult = parsePatternString(regionLength, items, patternString);
				patternResultLists.add(patternResult);
				line = patternBuffReader.readLine();
			}
		}
		return patternResultLists;
	}

	private static void buildFigureFrame(Graphics2D graphWriter, int imageHeight, int imageWidth, int height, int left,
	                                     int refLength, List<CpGStatistics> cpGStatisticsList, String figureFont) {
		graphWriter.setBackground(Color.WHITE);
		graphWriter.clearRect(0, 0, imageWidth, imageHeight);
		graphWriter.setPaint(Color.BLACK);
		graphWriter.setFont(new Font(figureFont, Font.PLAIN, COMMON_FONT_SIZE));

		// 1. add reference bar
		graphWriter.setStroke(new BasicStroke(2.0f));
		height += HEIGHT_INTERVAL;
		graphWriter.drawLine(left, height, left + refLength * BPWIDTH, height);
		graphWriter.setStroke(new BasicStroke());

		// 2. add refCpGSites
		for (CpGStatistics cpgStat : cpGStatisticsList) {
			graphWriter.fill(
					new Rectangle2D.Double(
							left + (cpgStat.getPosition() < 0 ? 0 : cpgStat.getPosition()) * BPWIDTH,
							height - BAR_HEIGHT / 4,
							(cpgStat.getPosition() < 0 || cpgStat.getPosition() == refLength - 1) ? BPWIDTH : CG_RADIUS,
							BAR_HEIGHT / 2));
		}

		// 3. add "read count" column
		graphWriter.drawString("Read Count(%)", (PatternResult.targetRegionLength + 2) * BPWIDTH + left,
				height + HEIGHT_INTERVAL / 2);
	}
}
