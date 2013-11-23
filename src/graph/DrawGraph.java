package graph;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

//import Glib.GScene;
//import Glib.GWindow;
import no.geosoft.cc.graphics.*;
import DataType.CpGStatistics;
import DataType.PatternResult;

public class DrawGraph extends JFrame{
	protected GWindow window;
	protected int maxwidth;
	protected int maxheight;
	protected final int WIDTH = 20;
	protected final int leftStart = 200;
	protected final int topStart = 150;
	protected final int startX = 400;
	protected final int startY = 200;
	protected final int intervalHeight = 50;
	protected final int LINEWIDTH = 5;
	protected final int BARHEIGHT = 40;
	protected final int RADIUS = 20;
	protected final double RGBinterval = 255 / 50.0;
	protected ArrayList<Integer> refCpGs;
	protected ArrayList<PatternResult> patternResultLists;
	protected ArrayList<CpGStatistics> statList;
	protected int refLength;
	protected String beginCoor;
	protected String endCoor;
	protected int height = startY;
	protected boolean firstCellLine = true;
	protected int totalSize = 0;

	public DrawGraph(ArrayList<ReadAnalysisResult> dataList) {
		// super("Methylation Statistics");
		// setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create the graphic canvas
		window = new GWindow(new Color(255, 255, 255));
		getContentPane().add(window.getCanvas());
		//add(window.getCanvas());
		// Create scene with default viewport and world extent settings
		GScene scene = new GScene(window);

		// add items to scene
		for (ReadAnalysisResult data : dataList) {
			addItems(scene, data);
		}
//		window.getCanvas().setSize(new Dimension(maxwidth, maxheight));
//		window.redraw();
//		window.refresh();
		pack();
		setSize(new Dimension(maxwidth, maxheight));
		//this.setSize();
		// setVisible(false);
	}

	protected void addItems(GScene scene, ReadAnalysisResult readData) {
	}

	public GWindow getWindow() {
		return window;
	}
}
