package graph;

import java.awt.Color;

import no.geosoft.cc.geometry.Geometry;
import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GSegment;
import no.geosoft.cc.graphics.GStyle;

/**
 * draw CpG sites
 */
public class GCpGSite extends GObject {
	private GSegment CpG;
	private int x, y;
	private int radius; 

	public GCpGSite(int x, int y, Color color,int radius) {
		this.x = x;
		this.y = y;
		this.radius = radius;
		GStyle style = new GStyle();
		style.setBackgroundColor(color);
		setStyle(style);

		CpG = new GSegment();
		addSegment(CpG);

	}

	public void draw() {
		CpG.setGeometry(Geometry.createCircle(x, y, radius));
	}
}