package DataType;

import java.io.*;
import java.util.List;

public class Constant implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Constant constant = null;
    public static String DISKROOTPATH = "";
    public static final String PNG = "png";
    public static final String EPS = "eps";
    public static final String HG16 = "hg16";
    public static final String HG17 = "hg17";
    public static final String HG18 = "hg18";
    public static final String HG19 = "hg19";

    // paths
    public String webRootPath;
    public String mappingResultPath;
    public String patternResultPath;
    public String coorFilePath;
    public String modifiedRefPath;
    public String originalRefPath;
    public String seqsPath;
    public String toolsPath;

    // parameters
    public String runID;
    public String host;
    public boolean coorReady = false;
    public String coorFileName;
    public String refVersion;
    public String randomDir;
    public double mutationPatternThreshold;
    public double minP0Threshold;
    public double criticalValue;
    public double minMethylThreshold;
    public double conversionRateThreshold;
    public double sequenceIdentityThreshold;
    public List<Experiment> experiments;
    public String email;
    public String qualsType;
    public int maxmis;
    public String figureFormat;

    // stage result
    public boolean finishedMapping;
    public boolean finishedAnalysis;
    public String mappingResultLink;
    public String analysisResultLink;
    public long mappingTime;
    public long analysisTime;
    public MappingSummary mappingSummary;
    public AnalysisSummary analysisSummary;

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
     */
    public static void writeConstant() {
        try {
            FileOutputStream constantOutputStream = new FileOutputStream(DISKROOTPATH + "/Run" + constant.runID + "/" + constant.runID + ".data");
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
     * @return singleton of constant
     */
    public static Constant readConstant(String runID) {
        try {
            ObjectInputStream constantObjectStream = new ObjectInputStream(new FileInputStream(DISKROOTPATH + "/Run" + runID + "/" + runID + ".data"));
            Object obj = constantObjectStream.readObject();
            constant = (Constant) obj;
            constantObjectStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return constant;
    }
}
