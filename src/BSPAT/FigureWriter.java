package BSPAT;

import DataType.Constant;
import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by kehu on 8/20/14.
 * Actually graph / bed writer wrapper.
 */
public class FigureWriter {
    private String figureName;
    private String figureFormat;
    private String GBLinkFileName;
    private BufferedWriter bedWriter;
    private FileOutputStream epsImage;
    private BufferedImage pngImage;
    private Graphics2D graphWriter;

    public FigureWriter(String figureFolder, String figureFormat, String region, String type, int imageWidth,
                        int imageHeight) throws IOException {
        this.figureFormat = figureFormat;
        GBLinkFileName = figureFolder + "pics/" + region + "_" + type + ".bed";
        figureName = figureFolder + "pics/" + region + "_" + type;

        // set pattern result picture folder
        File folder = new File(figureFolder + "pics/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        bedWriter = new BufferedWriter(new FileWriter(GBLinkFileName));

        // choose figure writer
        pngImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        switch (figureFormat) {
            case Constant.PNG:
                graphWriter = pngImage.createGraphics();// png writer

                break;
            case Constant.EPS:
                epsImage = new FileOutputStream(figureName + ".eps");
                graphWriter = new EpsGraphics("title", epsImage, 0, 0, imageWidth, imageHeight,
                                              ColorMode.COLOR_RGB); // eps writer
                break;
            default:
                throw new RuntimeException("invalid figure format!");
        }
    }

    public void close() throws IOException {
        if (figureFormat.equals(Constant.PNG)) {
            // png output
            File outputPNG = new File(figureName + ".png");
            ImageIO.write(pngImage, "png", outputPNG);
        } else if (figureFormat.equals(Constant.EPS)) {
            ((EpsGraphics) graphWriter).close();
            epsImage.close();
        }
        graphWriter.dispose();
        bedWriter.close();
    }

    public String getGBLinkFileName() {
        return GBLinkFileName;
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