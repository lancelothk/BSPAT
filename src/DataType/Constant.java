package DataType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Constant implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Constant constant = null;
	public static final String PNG = "png";
	public static final String EPS = "eps";
	public static final String HG18 = "hg18";
	public static final String HG19 = "hg19";
	
	public String runID;
	public String diskRootPath;
	public String webRootPath;
	public String host;
	public String mappingResultPath;
	public String patternResultPath;
	public String coorFilePath;
	public String coorFileName;
	public String modifiedRefPath;
	public String originalRefPath;
	public String seqsPath;
	public String toolsPath;
	public String randomDir;
	public double mutationPatternThreshold;
	public double minP0Threshold;
	public double minMethylThreshold;
	public double conversionRateThreshold;
	public double sequenceIdentityThreshold;
	public ArrayList<Experiment> experiments;
	public static String WEBAPPFOLDER = null;
	public String email;
	public boolean coorReady;
	public String qualsType;
	public int maxmis;
	public String refVersion;
	public String figureFormat;
	
	private Constant(){
	}
	
	public static Constant getInstance(){
		if (constant == null){
			synchronized (Constant.class) {
				if (constant == null){
					constant = new Constant();
				}
			}
		}
		return constant;
	}
	
	/**
	 * solve singleton with serialization
	 * @return
	 */
	private Object readResolve()  {
	    return constant;
	}
	
	/**
	 * write constant object to disk
	 * @param runID
	 * @param constant
	 */
	public static void writeConstant(String runID, Constant constant) {
		try {
			FileOutputStream constantOutputStream = new FileOutputStream(WEBAPPFOLDER + "/Run" +  runID + "/" + runID + ".data");
			ObjectOutputStream constantObjectStream = new ObjectOutputStream(constantOutputStream);
			constantObjectStream.writeObject(constant);
			constantObjectStream.flush();
			constantOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * read constant object from disk
	 * @param runID
	 * @return
	 */
	public static Constant readConstant(String runID) {
		Constant constant = null;
		try {
			FileInputStream constantInputStream = new FileInputStream(WEBAPPFOLDER + "/Run" +  runID + "/" + runID + ".data");
			ObjectInputStream constantObjectStream = new ObjectInputStream(constantInputStream);
			constant = (Constant) constantObjectStream.readObject();
			constantInputStream.close();
			constantObjectStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return constant;
	}

}
