package BSPAT;

import DataType.*;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawPattern {
    private final String DEFAULTFONT = "Arial";
    private final int CELLLINEFONTSIZE = 32;
    private final int WIDTH = 20;
    private final int STARTX = 180;
    private final int STARTY = 20;
    private final int LEFTSTART = 10;
    private final int BARHEIGHT = 28;
    private final int HEIGHTINTERVAL = 26;
    private final int RADIUS = 20;
    private final double RGBinterval = 255 / 50.0;
    private final int STYLECHOICE = 0;
    private final int COMMONSIZE = 28;
    private final int SMALLPERCENTSIZE = 20;

    private String figureFormat;
    private String refVersion;
    private String toolsPath;
    private String region;
    private String patternResultPath;
    private String sampleName;
    private Map<String, Coordinate> coordinateMap;
    private ReadAnalysisResult data;
    private String cellLine;
    private List<Integer> refCpGs;
    private List<CpGStatistics> statList;
    private int refLength;
    private String beginCoor;
    private String endCoor;

    public DrawPattern(String figureFormat, String refVersion, String toolsPath, String region,
                       String patternResultPath, String sampleName,
                       Map<String, Coordinate> coordinateMap) throws IOException {
        super();
        this.figureFormat = figureFormat;
        this.refVersion = refVersion;
        this.toolsPath = toolsPath;
        this.region = region;
        this.patternResultPath = patternResultPath;
        this.sampleName = sampleName;
        this.coordinateMap = coordinateMap;
        this.data = new ReadAnalysisResult(patternResultPath, sampleName, region, coordinateMap.get(region));
        this.cellLine = data.getCellLine();
        this.refCpGs = data.getRefCpGs();
        this.statList = data.getStatList();
        this.refLength = data.getRefLength();
        this.beginCoor = data.getBeginCoor();
        this.endCoor = data.getEndCoor();
    }

    private void buildFigureFrame(Graphics2D graphWriter, int imageHeight, int imageWidth, int height) {
        // 1. add coordinates
        graphWriter.setBackground(Color.WHITE);
        graphWriter.clearRect(0, 0, imageWidth, imageHeight);
        graphWriter.setPaint(Color.BLACK);
        graphWriter.setFont(new Font(DEFAULTFONT, STYLECHOICE, COMMONSIZE));
        graphWriter.drawString("chr" + beginCoor, STARTX + WIDTH, height);
        graphWriter.drawString(endCoor, STARTX + refLength * WIDTH - WIDTH, height);

        // 2. add reference bar
        graphWriter.setStroke(new BasicStroke(2.0f));
        height += HEIGHTINTERVAL;
        graphWriter.drawLine(STARTX, height, STARTX + refLength * WIDTH, height);
        graphWriter.setStroke(new BasicStroke());

        // 3. add refCpGSites
        for (Integer cpgPos : refCpGs) {
            graphWriter.drawLine(STARTX + cpgPos * WIDTH, height - BARHEIGHT / 2, STARTX + cpgPos * WIDTH,
                                 height + BARHEIGHT / 2);
        }
    }

    public void drawMethylPattern(PatternLink patternLink) throws IOException {
        List<PatternResult> patternResultLists = data.readPatternFile(region, patternLink.getPatternType());
        int imageWidth = refLength * WIDTH + STARTX + 230;
        int imageHeight = STARTY + 180 + patternResultLists.size() * HEIGHTINTERVAL;

        FigureWriter methylWriter = new FigureWriter(patternResultPath, figureFormat, region,
                                                     patternLink.getPatternType(), imageWidth, imageHeight);

        patternLink.setGBResultLink(methylWriter.getGBLinkFileName());
        patternLink.setFigureResultLink(methylWriter.getFigureName());

        int height = STARTY;
        buildFigureFrame(methylWriter.getGraphWriter(), imageHeight, imageWidth, height);

        // 4. add CpG sites
        DecimalFormat percent = new DecimalFormat("##.00%");
        height += HEIGHTINTERVAL;
        methylWriter.getGraphWriter().drawString("Read Count(%)", (refLength * WIDTH) + WIDTH + STARTX,
                                                 height + HEIGHTINTERVAL);
        height += HEIGHTINTERVAL;
        methylWriter.getGraphWriter().setFont(new Font(DEFAULTFONT, STYLECHOICE, CELLLINEFONTSIZE));
        methylWriter.getGraphWriter().drawString(cellLine, LEFTSTART, height);
        methylWriter.getGraphWriter().setFont(new Font(DEFAULTFONT, STYLECHOICE, COMMONSIZE));

        String chr = beginCoor.split(":")[0];
        String startPos = beginCoor.split(":")[1];
        methylWriter.getBedWriter().write(
                String.format("browser position chr%s-%d\nbrowser hide all\n", beginCoor, Long.parseLong(endCoor) + 1));
        for (int i = 0; i < patternResultLists.size(); i++) {
            PatternResult patternResult = patternResultLists.get(i);
            methylWriter.getBedWriter().write(
                    String.format("track name=\"Pattern%d\" description=\"%s-%s\" visibility=1 itemRgb=\"On\"\n", i,
                                  sampleName, region));
            methylWriter.getBedWriter().write(
                    String.format("chr%s\t%s\t%s\trefbar\t0\t+\t%s\t%s\t0,0,0\n", chr, startPos, endCoor, startPos,
                                  startPos));
            for (CpGSite cpg : patternResult.getCpGList()) {
                int cgPos = Integer.parseInt(startPos) + cpg.getPosition();
                // genome browser automatically add 1 to start, no change to
                // end.So we substract 1 from start and add 1 to the end.
                methylWriter.getBedWriter().write(
                        String.format("chr%s\t%d\t%d\tCG-Pattern%d\t%d\t+\t%d\t%d\t", chr, cgPos - 1, cgPos + 1, i,
                                      cpg.getMethylCount(), cgPos - 1, cgPos + 1));
                if (cpg.isMethylated()) {
                    // fill black circle
                    methylWriter.getGraphWriter().fill(
                            new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height, RADIUS,
                                                 RADIUS));
                    methylWriter.getBedWriter().write("0,0,0\n");
                } else {
                    // draw empty circle
                    methylWriter.getGraphWriter().setStroke(new BasicStroke(0.05f));
                    methylWriter.getGraphWriter().draw(
                            new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height, RADIUS,
                                                 RADIUS));
                    methylWriter.getGraphWriter().setStroke(new BasicStroke());
                    methylWriter.getBedWriter().write("224,224,224\n");
                }
            }
            methylWriter.getGraphWriter().drawString(
                    patternResult.getCount() + "(" + percent.format(patternResult.getPercent()) + ")",
                    (refLength * WIDTH) + WIDTH + STARTX, height + HEIGHTINTERVAL);
            addAllele(patternResult, methylWriter.getGraphWriter(), methylWriter.getBedWriter(), chr, startPos, height);
            height += HEIGHTINTERVAL;
        }

        methylWriter.getBedWriter().write(String.format(
                "browser position chr%s-%d\nbrowser hide all\ntrack name=\"Average\" description=\"%s-%s\" visibility=1 itemRgb=\"On\"\n",
                beginCoor, Long.parseLong(endCoor) + 1, sampleName, region));
        methylWriter.getBedWriter().write(
                String.format("chr%s\t%s\t%s\trefbar\t0\t+\t%s\t%s\t0,0,0\n", chr, startPos, endCoor, startPos,
                              startPos));

        addAverage(methylWriter.getGraphWriter(), DEFAULTFONT, statList, chr, startPos, "Pattern-Average",
                   methylWriter.getBedWriter(), height);
        methylWriter.close();
    }

    public void drawASMPattern(ReportSummary reportSummary, Pattern allelePattern, Pattern nonAllelePattern,
                               int totalCount) throws IOException, InterruptedException {
        int imageWidth = refLength * WIDTH + STARTX + 210;
        int imageHeight = STARTY + 180 + 10 * HEIGHTINTERVAL;

        if (allelePattern == null || allelePattern.sequenceList().size() == 0 ||
                nonAllelePattern.sequenceList().size() == 0) {
            return;
        }

        PatternResult patternWithAllele = patternToPatternResult(allelePattern, refCpGs, totalCount);
        PatternResult patternWithoutAllele = patternToPatternResult(nonAllelePattern, refCpGs, totalCount);

        if (!hasASM(patternWithAllele, patternWithoutAllele)) {
            reportSummary.setHasASM(false);
            return;
        } else {
            reportSummary.setHasASM(true);
        }

        FigureWriter ASMWriter = new FigureWriter(patternResultPath, figureFormat, region, "ASM", imageWidth,
                                                  imageHeight);
        reportSummary.setASMGBLink(ASMWriter.getGBLinkFileName());
        reportSummary.setASMFigureLink(ASMWriter.getFigureName());

        int height = STARTY;
        buildFigureFrame(ASMWriter.getGraphWriter(), imageHeight, imageWidth, height);

        String chr = beginCoor.split(":")[0];
        String startPos = beginCoor.split(":")[1];
        DecimalFormat percent = new DecimalFormat("##.00%");


        ASMWriter.getBedWriter().write(
                String.format("browser position chr%s-%d\nbrowser hide all\n", beginCoor, Long.parseLong(endCoor) + 1));
        height += 2.5 * HEIGHTINTERVAL;
        ASMWriter.getGraphWriter().drawString("Read Count(%)", (refLength * WIDTH) + WIDTH + STARTX, height);

        ASMWriter.getGraphWriter().setFont(new Font(DEFAULTFONT, STYLECHOICE, CELLLINEFONTSIZE));
        ASMWriter.getGraphWriter().drawString(cellLine, LEFTSTART, height);
        ASMWriter.getGraphWriter().setFont(new Font(DEFAULTFONT, STYLECHOICE, COMMONSIZE));

        // add average for pattern without allele
        ASMWriter.getBedWriter().write(
                String.format("track name=\"PatternA\" description=\"%s-%s-ASM\" visibility=1 itemRgb=\"On\"\n",
                              sampleName, region));
        ASMWriter.getBedWriter().write(
                String.format("chr%s\t%s\t%s\trefbar\t0\t+\t%s\t%s\t0,0,0\n", chr, startPos, endCoor, startPos,
                              startPos));
        height += HEIGHTINTERVAL;
        addAllele(patternWithoutAllele, ASMWriter.getGraphWriter(), ASMWriter.getBedWriter(), chr, startPos,
                  height + HEIGHTINTERVAL);
        addAverage(ASMWriter.getGraphWriter(), DEFAULTFONT, patternWithoutAllele.getCpGList(), chr, startPos,
                   "PatternA", ASMWriter.getBedWriter(), height);
        height += HEIGHTINTERVAL;
        ASMWriter.getGraphWriter().drawString(
                patternWithoutAllele.getCount() + "(" + percent.format(patternWithoutAllele.getPercent()) + ")",
                (refLength * WIDTH) + WIDTH + STARTX, height);

        // add average for pattern with allele
        ASMWriter.getBedWriter().write(
                String.format("track name=\"PatternB\" description=\"%s-%s-ASM\" visibility=1 itemRgb=\"On\"\n",
                              sampleName, region));
        ASMWriter.getBedWriter().write(
                String.format("chr%s\t%s\t%s\trefbar\t0\t+\t%s\t%s\t0,0,0\n", chr, startPos, endCoor, startPos,
                              startPos));
        height += 2 * HEIGHTINTERVAL;
        addAverage(ASMWriter.getGraphWriter(), DEFAULTFONT, patternWithAllele.getCpGList(), chr, startPos, "PatternB",
                   ASMWriter.getBedWriter(), height);
        addAllele(patternWithAllele, ASMWriter.getGraphWriter(), ASMWriter.getBedWriter(), chr, startPos,
                  height + HEIGHTINTERVAL);
        // set snp info
        if (patternWithAllele.hasAllele()) {
            List<SNP> snpList;
            try {
                snpList = retreiveSNP(chr, convertCoordinates(chr, coordinateMap.get(region).getStart(), "hg38",
                                                              patternResultPath) +
                        patternWithAllele.getAlleleList().get(0), "1");
            } catch (RemoteException e) {
                e.printStackTrace();
                snpList = null;
            }
            if (snpList != null && snpList.size() > 0) {
                reportSummary.setASMsnp(snpList.get(0));
            }
        }
        height += HEIGHTINTERVAL;
        ASMWriter.getGraphWriter().drawString(
                patternWithAllele.getCount() + "(" + percent.format(patternWithAllele.getPercent()) + ")",
                (refLength * WIDTH) + WIDTH + STARTX, height);

        ASMWriter.close();
    }

    private PatternResult patternToPatternResult(Pattern pattern, List<Integer> refCpGs, int totalCount) {
        PatternResult patternResult = new PatternResult();
        Map<Integer, CpGSite> cpGSiteMap = new HashMap<>();
        for (Integer pos : refCpGs) {
            if (cpGSiteMap.containsKey(pos)) {
                throw new RuntimeException("refCpG has duplicated CpGsites!");
            }
            cpGSiteMap.put(pos, new CpGSite(pos, false));
        }
        for (Sequence sequence : pattern.sequenceList()) {
            for (CpGSite cpGSite : sequence.getCpGSites()) {
                int pos = cpGSite.getPosition();
                if (cpGSiteMap.containsKey(pos)) {
                    if (cpGSite.isMethylated()) {
                        cpGSiteMap.get(pos).addMethylCount(1);
                    } else {
                        cpGSiteMap.get(pos).addNonMethylCount(1);
                    }
                } else {
                    throw new RuntimeException("sequence contains cpgsite not in ref");
                }
            }
        }
        patternResult.setCpGList(new ArrayList<>(cpGSiteMap.values()));
        patternResult.setCount(pattern.sequenceList().size());
        patternResult.setPercent(pattern.sequenceList().size() / (double) totalCount);
        if (pattern.getPatternType() == Pattern.PatternType.ALLELE) {
            patternResult.addAllele(Integer.parseInt(pattern.getPatternString().split("-")[0]));
        } else if (pattern.getPatternType() != Pattern.PatternType.NONALLELE) {
            throw new RuntimeException("only support convert allele and non-allele Pattern to PatternResult");
        }
        return patternResult;
    }

    private void addAllele(PatternResult patternResult, Graphics2D graphWriter, BufferedWriter bedWriter, String chr,
                           String startPos, int height) throws IOException {
        if (patternResult.hasAllele()) {
            List<Integer> alleleList = patternResult.getAlleleList();
            graphWriter.setPaint(Color.BLUE);
            for (int j = 0; j < alleleList.size(); j++) {
                graphWriter.fill(
                        new Rectangle2D.Double(STARTX + ((alleleList.get(j) - 1) * WIDTH), height, RADIUS / 2, RADIUS));
                int allelePos = Integer.parseInt(startPos) + patternResult.getAlleleList().get(0);
                bedWriter.write(
                        "chr" + chr + "\t" + (allelePos - 1) + "\t" + allelePos + "\tSNP-Pattern" + j + "\t" + 1000 +
                                "\t+\t" + (allelePos - 1) + "\t"

                                + allelePos + "\t0,0,255\n");
            }
            graphWriter.setPaint(Color.BLACK);
        }
    }

    private void addAverage(Graphics2D graphWriter, String fontChoice, List<? extends CpG> cpgList, String chr,
                            String startPos, String patternName, BufferedWriter bedWriter,
                            int height) throws IOException {
        // 5. add average
        DecimalFormat percentSmall = new DecimalFormat("##%");
        height += HEIGHTINTERVAL;
        for (CpG cpg : cpgList) {
            int R = 0, G = 0, B = 0;
            double methylLevel = cpg.getMethylLevel();
            if (methylLevel > 0.5) {
                G = (int) (255 - ((methylLevel - 0.5) * 100 * RGBinterval));
                R = 255;
            }
            if (methylLevel < 0.5) {
                G = 255;
                R = (int) (methylLevel * 100 * RGBinterval);
            }
            if (methylLevel == 0.5) {
                R = 255;
                G = 255;
            }
            graphWriter.setPaint(new Color(R, G, B));
            graphWriter.fill(
                    new Ellipse2D.Double(STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height, RADIUS, RADIUS));
            graphWriter.setPaint(Color.BLACK);
            // move percentage a little left and shink the font size
            graphWriter.setFont(new Font(fontChoice, STYLECHOICE, SMALLPERCENTSIZE));
            graphWriter.drawString(percentSmall.format(cpg.getMethylLevel()),
                                   STARTX + (cpg.getPosition() * WIDTH) - WIDTH / 2, height + HEIGHTINTERVAL * 2);

            int cgPos = Integer.parseInt(startPos) + cpg.getPosition();
            // genome browser automatically add 1 to start, no change to end.So
            // we substract 1 from start and add 1 to the end.
            bedWriter.write("chr" + chr + "\t" + (cgPos - 1) + "\t" + (cgPos + 1) + "\tCG-" + patternName + "\t" +
                                    cpg.getMethylCount() + "\t+\t" + (cgPos - 1) + "\t" + (cgPos + 1) + "\t" + R + "," +
                                    G + "," + B + "\n");

        }
    }

    private boolean hasASM(PatternResult patternWithAllele, PatternResult patternWithoutAllele) {
        // use 0.2 as threshold to filter out unequal patterns. ASM pattern should be roughly equal.
        if (patternWithAllele.getPercent() < 0.2 || patternWithoutAllele.getPercent() < 0.2) {
            return false;
        }
        List<CpGSite> cglistWithAllele = patternWithAllele.getCpGList();
        List<CpGSite> cglistWithoutAllele = patternWithoutAllele.getCpGList();
        // if there is at least one cpg site with different methyl type and the different bigger than 0.2, it is ASM
        for (int i = 0; i < cglistWithAllele.size(); i++) {
            if (cglistWithAllele.get(i).getMethylType() != cglistWithoutAllele.get(i).getMethylType() &&
                    Math.abs(cglistWithAllele.get(i).getMethylLevel() - cglistWithoutAllele.get(i).getMethylLevel()) >=
                            0.2) {
                return true;
            }
        }
        return false;
    }

    private List<SNP> retreiveSNP(String chr, long pos, String maxRet) throws RemoteException {
        List<SNP> snps = new ArrayList<>();
        String fetchIds = "";
        // 1. search region
        EUtilsServiceStub service = new EUtilsServiceStub();
        // call NCBI ESearch utility
        // NOTE: search term should be URL encoded
        EUtilsServiceStub.ESearchRequest req = new EUtilsServiceStub.ESearchRequest();
        req.setDb("snp");
        String term = chr + "[CHR]+AND+\"Homo sapiens\"[Organism]+AND+" + pos + "[CHRPOS]";
        System.out.println(term);
        req.setTerm(term);
        req.setRetMax(maxRet);
        EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);
        // results output
        System.out.println("SNP range:");
        System.out.println("Found ids: " + res.getCount());
        System.out.print("First " + res.getRetMax() + " ids: ");

        if (res.getCount().equals("0")) {
            return snps;
        }

        int N = res.getIdList().getId().length;
        for (int i = 0; i < N; i++) {
            if (i > 0) fetchIds += ",";
            fetchIds += res.getIdList().getId()[i];
        }
        System.out.println();

        // 3. fetch SNP
        EFetchSnpServiceStub serviceF = new EFetchSnpServiceStub();
        // call NCBI EFetch utility
        EFetchSnpServiceStub.EFetchRequest reqF = new EFetchSnpServiceStub.EFetchRequest();
        reqF.setId(fetchIds);
        EFetchSnpServiceStub.EFetchResult resF = serviceF.run_eFetch(reqF);
        if (resF.getExchangeSet() == null || resF.getExchangeSet().getRs() == null) {
            return null;
        }
        // results output
        for (int i = 0; i < resF.getExchangeSet().getRs().length; i++) {
            EFetchSnpServiceStub.Rs_type0 obj = resF.getExchangeSet().getRs()[i];
            SNP snp = new SNP(obj.getRsId(), obj.getSnpType(), obj.getMolType(), obj.getPhenotype(), obj.getAssembly(),
                              obj.getFrequency());
            snp.print();
            snps.add(snp);
        }
        return snps;
    }

    private long convertCoordinates(String chr, long pos, String targetRefVersion,
                                    String patternResultPath) throws IOException, InterruptedException {
        if (refVersion.equals(targetRefVersion)) {
            return pos;
        }
        String liftOverPath = toolsPath + "/liftover/";
        String originPosFileName = "tmpCoordinate." + refVersion;
        String targetPosFileName = "tmpCoordinate." + targetRefVersion;
        String chain = refVersion + "ToHg" + targetRefVersion.replace("hg", "") + ".over.chain.gz";
        // write pos into file
        try (BufferedWriter coorWriter = new BufferedWriter(new FileWriter(patternResultPath + originPosFileName))) {
            coorWriter.write("chr" + chr + ":" + pos + "-" + pos + "\n");
        }

        // call liftover
        File liftOverPathFile = new File(liftOverPath);
        String callLiftOver = liftOverPathFile.getAbsolutePath() + "/liftOver -positions " + originPosFileName + " " +
                liftOverPathFile.getAbsolutePath() + "/" + chain + " " + targetPosFileName + " /dev/null";
        System.out.println("Call liftOver:");
        Utilities.callCMD(callLiftOver, new File(patternResultPath), null);

        // read result
        String[] items = null;
        try (BufferedReader coorReader = new BufferedReader(new FileReader(patternResultPath + targetPosFileName))) {
            items = coorReader.readLine().split(":");
        }
        File originPosFile = new File(patternResultPath + originPosFileName);
        if (!originPosFile.delete()) {
            throw new RuntimeException("Failed to delete: " + originPosFile.getAbsolutePath());
        }
        File targetPosFile = new File(patternResultPath + targetPosFileName);
        if (!targetPosFile.delete()) {
            throw new RuntimeException("Failed to delete: " + originPosFile.getAbsolutePath());
        }
        IO.deleteFiles(patternResultPath, new String[]{".bed", ".bedmapped", ".bedunmapped"});
        return Long.valueOf(items[1].split("-")[0]);
    }

}
