package graph;

import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GSegment;
import no.geosoft.cc.graphics.GStyle;

public class GRefCpGSite extends GObject {
	private GSegment refCpG;
	private int x, y;
	private int barheight;
	

	/**
	 * 
	 * @param x
	 * @param y
	 * @param height
	 */
	public GRefCpGSite(int x, int y,int barheight) {
		this.x = x;
		this.y = y;
		this.barheight = barheight;
		refCpG = new GSegment();
		GStyle style = new GStyle();
		style.setLineStyle(GStyle.LINESTYLE_SOLID );
		style.setLineWidth(2);
		setStyle(style);
		addSegment(refCpG);
	}

	public void draw() {
		refCpG.setGeometry(x, y - barheight/2, x, y + barheight/2);
	}
}
