package graph;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;

import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GScene;
import DataType.CpGSite;
import DataType.CpGStatistics;
import DataType.PatternResult;

public class DrawSingleGraph extends DrawGraph {

	public DrawSingleGraph(ArrayList<ReadAnalysisResult> dataList) {
		super(dataList);
	}

	protected void addItems(GScene scene, ReadAnalysisResult data) {
		System.out.println("0 -- DrawSingleGraph");
		String cellLine = data.getCellLine();
		// set parameters
		this.refCpGs = data.getRefCpGs();
		this.patternResultLists = data.getPatternResultLists();
		this.statList = data.getStatList();
		this.refLength = data.getRefLength();
		this.beginCoor = data.getBeginCoor();
		this.endCoor = data.getEndCoor();

		totalSize += (patternResultLists.size());

		System.out.println("1 -- DrawSingleGraph");
		// Create the graphics object and add to the scene
		if (firstCellLine == true) {
			// 0. add coordinates
			GMyText beginCoorText = new GMyText(startX, topStart, beginCoor, 30);
			scene.add(beginCoorText);
			GMyText endCoorText = new GMyText(startX + (refLength * WIDTH), topStart, endCoor, 30);
			scene.add(endCoorText);

			// 1. add reference bar

			GObject refBar = new GReferenceBar(startX, height, startX + refLength * WIDTH, height,LINEWIDTH);
			scene.add(refBar);

			// 2. add refCpGSites
			for (int i = 0; i < refCpGs.size(); i++) {
				GObject refCpG = new GRefCpGSite(startX + (refCpGs.get(i) * WIDTH), height,BARHEIGHT);
				scene.add(refCpG);
			}
			firstCellLine = false;
		}
		
		System.out.println("2 -- DrawSingleGraph");

		// 3. add CpGSites
		DecimalFormat percent = new DecimalFormat("##.00%");
		GMyText cellLineText = new GMyText(leftStart, height + intervalHeight, cellLine, 50);
		scene.add(cellLineText);
		// GMyText IDText = new GMyText(leftStart, height + intervalHeight * 2,
		// ID, 50);
		// scene.add(IDText);
		GMyText descriptionLineText = new GMyText((refLength + 10) * WIDTH + startX, height, "Read Count(%)", 30);
		scene.add(descriptionLineText);
		for (PatternResult patternResult : patternResultLists) {
			height += intervalHeight;
			for (CpGSite cpg : patternResult.getCpGList()) {
				Color color;
				if (cpg.getMethylLabel() == true) {
					color = Color.BLACK;
				} else {
					color = Color.WHITE;
				}
				GObject gCpG = new GCpGSite(startX + (cpg.getPosition() * WIDTH), height, color, RADIUS);
				scene.add(gCpG);
			}
			GMyText statText = new GMyText((refLength + 10) * WIDTH + startX, height, patternResult.getCount() + "("
					+ percent.format(patternResult.getPercent()) + ")", 35);
			scene.add(statText);
		}

		System.out.println("3 -- DrawSingleGraph");
		// 4. add average
		height += intervalHeight;
		for (CpGStatistics cpgStat : statList) {
			if (cpgStat.getPosition() != 0) {
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
				GObject gCpG = new GCpGSite(startX + (cpgStat.getPosition() * WIDTH), height, new Color(R, G, B), RADIUS);
				scene.add(gCpG);
				GMyText statText = new GMyText(startX + (cpgStat.getPosition() * WIDTH), height + intervalHeight,
						percent.format(cpgStat.getMethylationRate()).toString(), 14);
				scene.add(statText);
			}
		}
		
		System.out.println("4 -- DrawSingleGraph");
		GMyText statText = new GMyText((refLength + 10) * WIDTH + startX, height, "Average", 25);
		scene.add(statText);

		maxwidth = (refLength + 10) * WIDTH + startX * 2;
		maxheight = height + intervalHeight * 3;

		height += (intervalHeight * 2);
	}
}