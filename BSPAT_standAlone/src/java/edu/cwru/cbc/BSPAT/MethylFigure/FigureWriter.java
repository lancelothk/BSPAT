package edu.cwru.cbc.BSPAT.MethylFigure;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by kehu on 8/20/14.
 * Actually graph / bed writer wrapper.
 */
public class FigureWriter {
	public static final String PNG = "png";
	public static final String EPS = "eps";
	private String figureName;
	private String figureFormat;
	private BufferedWriter bedWriter;
	private FileOutputStream epsImage;
	private BufferedImage pngImage;
	private Graphics2D graphWriter;

	public FigureWriter(String figureFileName, String figureFormat, int imageWidth, int imageHeight) throws
			IOException {
		this.figureFormat = figureFormat;
		figureName = figureFileName;

		// choose figure writer
		pngImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		switch (figureFormat) {
			case PNG:
				graphWriter = pngImage.createGraphics();// png writer
				break;
			case EPS:
				epsImage = new FileOutputStream(figureName.endsWith(".eps") ? figureName : figureName + "." + EPS);
				// eps writer
				graphWriter = new EpsGraphics("title", epsImage, 0, 0, imageWidth, imageHeight, ColorMode.COLOR_RGB);
				break;
			default:
				throw new RuntimeException("invalid figure format!");
		}
	}

	public void close() throws IOException {
		if (figureFormat.equals(PNG)) {
			// png output
			File outputPNG = new File(figureName.endsWith(".png") ? figureName : figureName + "." + PNG);
			ImageIO.write(pngImage, PNG, outputPNG);
		} else if (figureFormat.equals(EPS)) {
			((EpsGraphics) graphWriter).close();
			epsImage.close();
		}
		graphWriter.dispose();
		bedWriter.close();
	}

	public String getFigureName() {
		return figureName;
	}

	public Graphics2D getGraphWriter() {
		return graphWriter;
	}

	public BufferedWriter getBedWriter() {
		return bedWriter;
	}
}