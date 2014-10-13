package edu.cwru.cbc.BSPAT.DataType;

import java.io.*;
import java.util.List;

public class Constant implements Serializable {
    public static final String JOB_FOLDER_PREFIX = "Job";
    private static final long serialVersionUID = 1L;
    public static final long SPACETHRESHOLD = 100000;// maximum allow 100000MB space (100GB)
    public static final int REFEXTENSIONLENGTH = 2;
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
    public String targetPath;
    public String demoPath;
    public String testPath;
    public String logPath;

    // parameters
    public String jobID;
    public String host;
    public boolean coorReady = false;
    public String coorFileName;
    public String targetFileName;
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
    public SeqCountSummary seqCountSummary;

    /**
     * write constant object to disk
     */
    public void writeConstant() throws IOException {
        FileOutputStream constantOutputStream = new FileOutputStream(
                DISKROOTPATH + "/" + JOB_FOLDER_PREFIX + this.jobID + "/" + this.jobID + ".data");
        ObjectOutputStream constantObjectStream = new ObjectOutputStream(constantOutputStream);
        constantObjectStream.writeObject(this);
        constantOutputStream.close();
    }

    /**
     * read constant object from disk
     *
     * @param jobID
     * @return singleton of constant
     */
    public static Constant readConstant(String jobID) throws IOException {
        ObjectInputStream constantObjectStream = new ObjectInputStream(
                new FileInputStream(DISKROOTPATH + "/" + JOB_FOLDER_PREFIX + jobID + "/" + jobID + ".data"));
        Object obj = null;
        try {
            obj = constantObjectStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Serialization error!", e);
        }
        Constant constant = (Constant) obj;
        constantObjectStream.close();
        return constant;
    }

    public String getAbsolutePathCoorFile() {
        return (coorFilePath + coorFileName).replace(Constant.DISKROOTPATH, this.webRootPath);
    }

    public String getWebRootPath() {
        return webRootPath;
    }

    public void setWebRootPath(String webRootPath) {
        this.webRootPath = webRootPath;
    }

    public String getMappingResultPath() {
        return mappingResultPath;
    }

    public void setMappingResultPath(String mappingResultPath) {
        this.mappingResultPath = mappingResultPath;
    }

    public String getPatternResultPath() {
        return patternResultPath;
    }

    public void setPatternResultPath(String patternResultPath) {
        this.patternResultPath = patternResultPath;
    }

    public String getCoorFilePath() {
        return coorFilePath;
    }

    public void setCoorFilePath(String coorFilePath) {
        this.coorFilePath = coorFilePath;
    }

    public String getModifiedRefPath() {
        return modifiedRefPath;
    }

    public void setModifiedRefPath(String modifiedRefPath) {
        this.modifiedRefPath = modifiedRefPath;
    }

    public String getOriginalRefPath() {
        return originalRefPath;
    }

    public void setOriginalRefPath(String originalRefPath) {
        this.originalRefPath = originalRefPath;
    }

    public String getSeqsPath() {
        return seqsPath;
    }

    public void setSeqsPath(String seqsPath) {
        this.seqsPath = seqsPath;
    }

    public String getToolsPath() {
        return toolsPath;
    }

    public void setToolsPath(String toolsPath) {
        this.toolsPath = toolsPath;
    }

    public String getJobID() {
        return jobID;
    }

    public void setJobID(String jobID) {
        this.jobID = jobID;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isCoorReady() {
        return coorReady;
    }

    public void setCoorReady(boolean coorReady) {
        this.coorReady = coorReady;
    }

    public String getCoorFileName() {
        return coorFileName;
    }

    public void setCoorFileName(String coorFileName) {
        this.coorFileName = coorFileName;
    }

    public String getRefVersion() {
        return refVersion;
    }

    public void setRefVersion(String refVersion) {
        this.refVersion = refVersion;
    }

    public String getRandomDir() {
        return randomDir;
    }

    public void setRandomDir(String randomDir) {
        this.randomDir = randomDir;
    }

    public double getMutationPatternThreshold() {
        return mutationPatternThreshold;
    }

    public void setMutationPatternThreshold(double mutationPatternThreshold) {
        this.mutationPatternThreshold = mutationPatternThreshold;
    }

    public double getMinP0Threshold() {
        return minP0Threshold;
    }

    public void setMinP0Threshold(double minP0Threshold) {
        this.minP0Threshold = minP0Threshold;
    }

    public double getCriticalValue() {
        return criticalValue;
    }

    public void setCriticalValue(double criticalValue) {
        this.criticalValue = criticalValue;
    }

    public double getMinMethylThreshold() {
        return minMethylThreshold;
    }

    public void setMinMethylThreshold(double minMethylThreshold) {
        this.minMethylThreshold = minMethylThreshold;
    }

    public double getConversionRateThreshold() {
        return conversionRateThreshold;
    }

    public void setConversionRateThreshold(double conversionRateThreshold) {
        this.conversionRateThreshold = conversionRateThreshold;
    }

    public double getSequenceIdentityThreshold() {
        return sequenceIdentityThreshold;
    }

    public void setSequenceIdentityThreshold(double sequenceIdentityThreshold) {
        this.sequenceIdentityThreshold = sequenceIdentityThreshold;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = experiments;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getQualsType() {
        return qualsType;
    }

    public void setQualsType(String qualsType) {
        this.qualsType = qualsType;
    }

    public int getMaxmis() {
        return maxmis;
    }

    public void setMaxmis(int maxmis) {
        this.maxmis = maxmis;
    }

    public String getFigureFormat() {
        return figureFormat;
    }

    public void setFigureFormat(String figureFormat) {
        this.figureFormat = figureFormat;
    }

    public boolean isFinishedMapping() {
        return finishedMapping;
    }

    public void setFinishedMapping(boolean finishedMapping) {
        this.finishedMapping = finishedMapping;
    }

    public boolean isFinishedAnalysis() {
        return finishedAnalysis;
    }

    public void setFinishedAnalysis(boolean finishedAnalysis) {
        this.finishedAnalysis = finishedAnalysis;
    }

    public String getMappingResultLink() {
        return mappingResultLink;
    }

    public void setMappingResultLink(String mappingResultLink) {
        this.mappingResultLink = mappingResultLink;
    }

    public String getAnalysisResultLink() {
        return analysisResultLink;
    }

    public void setAnalysisResultLink(String analysisResultLink) {
        this.analysisResultLink = analysisResultLink;
    }

    public long getMappingTime() {
        return mappingTime;
    }

    public void setMappingTime(long mappingTime) {
        this.mappingTime = mappingTime;
    }

    public long getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(long analysisTime) {
        this.analysisTime = analysisTime;
    }

    public MappingSummary getMappingSummary() {
        return mappingSummary;
    }

    public void setMappingSummary(MappingSummary mappingSummary) {
        this.mappingSummary = mappingSummary;
    }

    public SeqCountSummary getSeqCountSummary() {
        return seqCountSummary;
    }

    public void setSeqCountSummary(SeqCountSummary seqCountSummary) {
        this.seqCountSummary = seqCountSummary;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getDemoPath() {
        return demoPath;
    }

    public void setDemoPath(String demoPath) {
        this.demoPath = demoPath;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }
}
