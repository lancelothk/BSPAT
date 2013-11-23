package graph;

import java.awt.Font;

import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GPosition;
import no.geosoft.cc.graphics.GSegment;
import no.geosoft.cc.graphics.GStyle;
import no.geosoft.cc.graphics.GText;

public class GMyText extends GObject {
	private GSegment gstext;
	private int x, y;

	public GMyText(int x, int y, String text, int fontSize) {
		this.x = x;
		this.y = y;
		gstext = new GSegment();

		GStyle label = new GStyle();
		label.setFont(new Font("Dialog", Font.PLAIN, fontSize));
		gstext.setStyle(label);
		gstext.addText(new GText(text, GPosition.CENTER));
		addSegment(gstext);
	}

	public void draw() {
		gstext.setGeometry(x, y);
	}
}
