package edu.cwru.cbc.BSPAT.core;

import edu.cwru.cbc.BSPAT.DataType.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadAnalysisResult {
    private List<CpGStatistics> statList = new ArrayList<>();
    private int refLength = 0;
    private String inputFolder;
    private Coordinate coordinate;
    private String beginCoor;
    private String endCoor;
    private String cellLine;
    private String refSeq;

    public ReadAnalysisResult(String inputFolder, String cellLine, String region, Coordinate coordinate,
                              String refSeq) throws IOException {
        this.inputFolder = inputFolder;
        this.coordinate = coordinate;
        this.cellLine = cellLine;
        this.refSeq = refSeq;
        readStatFile(region);
        setCoordinate();
    }

    private void readStatFile(String ID) throws IOException {
        try (BufferedReader statBuffReader = new BufferedReader(
                new FileReader(inputFolder + ID + "_bismark.analysis_report.txt"))) {
            String line;
            String[] items;
            // read ref length
            line = statBuffReader.readLine();
            if (line == null) {
                throw new RuntimeException("analysis report is empty!");
            }
            items = line.split("\t");
            refLength = Integer.valueOf(items[1]);
            // skip 6 lines
            for (int i = 0; i < 6; i++) {
                statBuffReader.readLine();
            }
            // get start position
            line = statBuffReader.readLine();
            while (line != null && !line.startsWith("mutation")) {
                items = line.split("\t");
                if (!Character.isDigit(items[0].charAt(0))) {
                    break;
                }
                CpGStatistics cpgStat = null;
                int pos = Integer.valueOf(items[0]);
                cpgStat = new CpGStatistics(pos);
                cpgStat.setMethylLevel(Double.valueOf(items[1]));
                statList.add(cpgStat);

                line = statBuffReader.readLine();
            }
            Collections.sort(statList, new CpGStatComparator());
        }
    }

    public List<PatternResult> readPatternFile(String region, String patternType) throws IOException {
        List<PatternResult> patternResultLists = new ArrayList<>();
        try (BufferedReader patternBuffReader = new BufferedReader(
                new FileReader(inputFolder + region + "_bismark.analysis_" + patternType + ".txt"))) {
            // skip column names
            patternBuffReader.readLine();
            // reference line
            String line = patternBuffReader.readLine();
            refLength = line.split("\t")[0].length();
            if (patternType.equals(PatternLink.METHYLATION)) {
                beginCoor = String.format("%s:%s", coordinate.getChr(), coordinate.getStart() + refSeq.indexOf("CG"));
                endCoor = String.valueOf(coordinate.getStart() + refSeq.lastIndexOf("CG") + 1);
            } else {
                setCoordinate();
            }

            // start to read content
            line = patternBuffReader.readLine();
            String[] items;
            PatternResult patternResult;

            while (line != null) {
                items = line.split("\t");
                patternResult = new PatternResult();
                for (int i = 0; i < refLength; i++) {
                    CpGSitePattern cpg;
                    if (items[0].charAt(i) == '*') {
                        cpg = new CpGSitePattern(i, false);
                        patternResult.addCpG(cpg);
                        if (i + 1 < refLength && items[0].charAt(i + 1) == '*') {
                            i++;
                        }
                    } else if (items[0].charAt(i) == '@') {
                        cpg = new CpGSitePattern(i, true);
                        patternResult.addCpG(cpg);
                        if (i + 1 < refLength && items[0].charAt(i + 1) == '@') {
                            i++;
                        }
                    } else if (items[0].charAt(i) == 'A' || items[0].charAt(i) == 'C' || items[0].charAt(i) == 'G' ||
                            items[0].charAt(i) == 'T') {
                        patternResult.addAllele(i);
                    }
                }
                patternResultLists.add(patternResult);
                patternResult.setCount(Integer.valueOf(items[1]));
                patternResult.setPercent(Double.valueOf(items[2]));
                line = patternBuffReader.readLine();
            }
        }
        return patternResultLists;
    }

    private void setCoordinate() {
        beginCoor = coordinate.getChr() + ":" + coordinate.getStart();
        endCoor = String.valueOf(coordinate.getStart() + refLength - 1);
    }

    public String getBeginCoor() {
        return beginCoor;
    }

    public String getEndCoor() {
        return endCoor;
    }

    public int getRefLength() {
        return refLength;
    }

    public List<CpGStatistics> getStatList() {
        return statList;
    }

    public String getCellLine() {
        return cellLine;
    }

}