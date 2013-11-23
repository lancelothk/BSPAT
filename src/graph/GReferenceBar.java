package graph;

import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GSegment;
import no.geosoft.cc.graphics.GStyle;

public class GReferenceBar extends GObject {
	private GSegment bar;
	int xBegin, yBegin, xEnd, yEnd;

	public GReferenceBar(int xBegin, int yBegin, int xEnd, int yEnd, int lineWidth) {
		this.xBegin = xBegin;
		this.xEnd = xEnd;
		this.yBegin = yBegin;
		this.yEnd = yEnd;
		
		GStyle style = new GStyle();
		style.setLineStyle(GStyle.LINESTYLE_SOLID );
		style.setLineWidth(lineWidth);
		setStyle(style);
		
		bar = new GSegment();
		addSegment(bar);
	}

	public void draw() {
		bar.setGeometry(xBegin, yBegin, xEnd, yEnd);
	}
}
