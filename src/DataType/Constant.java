package DataType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Queue;

import BSPAT.ReportSummary;

public class Constant implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Constant constant = null;
	public static String WEBAPPFOLDER = null;
	public static final String PNG = "png";
	public static final String EPS = "eps";
	public static final String HG16 = "hg16";
	public static final String HG17 = "hg17";
	public static final String HG18 = "hg18";
	public static final String HG19 = "hg19";

	public String runID;
	public String diskRootPath;
	public String webRootPath;
	public String host;
	public String mappingResultPath;
	public String patternResultPath;
	public boolean coorReady = false;
	public String coorFilePath;
	public String coorFileName;
	public String refVersion;
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
	public Queue<Experiment> experiments;
	public String email;
	public String qualsType;
	public int maxmis;
	public String figureFormat;
	public boolean finishedMapping;
	public long mappingTime;
	public String mappingResultLink;
	public boolean finishedAnalysis;
	public long analysisTime;
	public String analysisResultLink;

	private Constant() {
	}

	public static Constant getInstance() {
		if (constant == null) {
			synchronized (Constant.class) {
				if (constant == null) {
					constant = new Constant();
				}
			}
		}
		return constant;
	}

	/**
	 * write constant object to disk
	 * 
	 * @param runID
	 * @param constant
	 */
	public static void writeConstant() {
		try {
			FileOutputStream constantOutputStream = new FileOutputStream(WEBAPPFOLDER + "/Run" + constant.runID + "/" + constant.runID + ".data");
			ObjectOutputStream constantObjectStream = new ObjectOutputStream(constantOutputStream);
			constantObjectStream.writeObject(constant);
			constantOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * read constant object from disk
	 * 
	 * @param runID
	 * @return
	 */
	public static Constant readConstant(String runID) {
		try {
			ObjectInputStream constantObjectStream = new ObjectInputStream(new FileInputStream(WEBAPPFOLDER + "/Run" + runID + "/" + runID + ".data"));
			Object obj = constantObjectStream.readObject();
			constant = (Constant) obj;
			constantObjectStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return constant;
	}

	public String getRunID() {
		return runID;
	}

	public String getDiskRootPath() {
		return diskRootPath;
	}

	public String getWebRootPath() {
		return webRootPath;
	}

	public String getHost() {
		return host;
	}

	public String getMappingResultPath() {
		return mappingResultPath;
	}

	public String getPatternResultPath() {
		return patternResultPath;
	}

	public boolean isCoorReady() {
		return coorReady;
	}

	public String getCoorFilePath() {
		return coorFilePath;
	}

	public String getCoorFileName() {
		return coorFileName;
	}

	public String getRefVersion() {
		return refVersion;
	}

	public String getModifiedRefPath() {
		return modifiedRefPath;
	}

	public String getOriginalRefPath() {
		return originalRefPath;
	}

	public String getSeqsPath() {
		return seqsPath;
	}

	public String getToolsPath() {
		return toolsPath;
	}

	public String getRandomDir() {
		return randomDir;
	}

	public double getMutationPatternThreshold() {
		return mutationPatternThreshold;
	}

	public double getMinP0Threshold() {
		return minP0Threshold;
	}

	public double getMinMethylThreshold() {
		return minMethylThreshold;
	}

	public double getConversionRateThreshold() {
		return conversionRateThreshold;
	}

	public double getSequenceIdentityThreshold() {
		return sequenceIdentityThreshold;
	}

	public Queue<Experiment> getExperiments() {
		return experiments;
	}

	public static String getWEBAPPFOLDER() {
		return WEBAPPFOLDER;
	}

	public String getEmail() {
		return email;
	}

	public String getQualsType() {
		return qualsType;
	}

	public int getMaxmis() {
		return maxmis;
	}

	public String getFigureFormat() {
		return figureFormat;
	}

	public long getMappingTime() {
		return mappingTime;
	}

	public String getMappingResultLink() {
		return mappingResultLink;
	}

	public long getAnalysisTime() {
		return analysisTime;
	}

	public String getAnalysisResultLink() {
		return analysisResultLink;
	}

	public boolean isFinishedMapping() {
		return finishedMapping;
	}

	public boolean isFinishedAnalysis() {
		return finishedAnalysis;
	}
}
