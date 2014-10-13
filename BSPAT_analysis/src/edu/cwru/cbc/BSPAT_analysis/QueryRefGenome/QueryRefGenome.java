package edu.cwru.cbc.BSPAT_analysis.QueryRefGenome;

import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by kehu on 9/9/14.
 */
public class QueryRefGenome {
    private Map<String, List<CpGIsland>> groupedCpGIsland;
    private final int MINCPGNUM = 10;
    private int regionNumber;
    private int regionLength;

    public static void main(String[] args) throws IOException {
        QueryRefGenome queryRefGenome = new QueryRefGenome("/media/kehu/win-data/Dataset/cpgIslandExt_hg18_UCSCGB.txt",
                                                           100, 200);
        Map<String, String> resultMap = queryRefGenome.query("/media/kehu/win-data/Dataset/hg18_UCSC_ref/chromFa");
        for (String key : resultMap.keySet()) {
            System.out.println('>' + key + "\n" + resultMap.get(key).toUpperCase());
        }
    }

    public QueryRefGenome(String cpgIslandFileName, int regionNumber, int regionLength) throws IOException {
        this.regionNumber = regionNumber;
        this.regionLength = regionLength;
        List<CpGIsland> cpGIslandList = readCpGIsland(cpgIslandFileName);
        System.out.println(cpGIslandList.size());
        List<CpGIsland> randomCpGIslandList = pickRandomCpGIslands(cpgIslandFileName);
        System.out.println(randomCpGIslandList.size());
        groupedCpGIsland = groupByChrom(randomCpGIslandList);
    }

    public Map<String, String> query(String refGenomePath) throws IOException {
        Map<String, String> queriedReferenceMap = new HashMap<>();
        for (String chr : groupedCpGIsland.keySet()) {
            StringBuilder refChr = loadRefGenome(refGenomePath, chr);
            for (CpGIsland cpGIsland : groupedCpGIsland.get(chr)) {
                String ref = queryRefGenome(refChr, cpGIsland);
                queriedReferenceMap.put(String.format("%s:%d-%d", cpGIsland.getChrom(), cpGIsland.getChromStart(),
                                                      cpGIsland.getChromEnd()), ref);
            }
        }
        return queriedReferenceMap;
    }

    private StringBuilder loadRefGenome(String refGenomePath, String chr) throws IOException {
        return CharStreams.readLines(new FileReader(String.format("%s/%s.fa", refGenomePath, chr)),
                                     new LineProcessor<StringBuilder>() {
                                         StringBuilder refChrBuilder = new StringBuilder(100000000);

                                         @Override
                                         public boolean processLine(String s) throws IOException {
                                             if (!s.startsWith(">")) {
                                                 refChrBuilder.append(s);
                                             }
                                             return true;
                                         }

                                         @Override
                                         public StringBuilder getResult() {
                                             return refChrBuilder;
                                         }
                                     });
    }


    private String queryRefGenome(StringBuilder refChr, CpGIsland cpGIsland) {
        // only pick first regionLength bp
        return refChr.substring(cpGIsland.getChromStart(), cpGIsland.getChromStart() + regionLength);
    }

    private Map<String, List<CpGIsland>> groupByChrom(List<CpGIsland> cpGIslandList) {
        Map<String, List<CpGIsland>> groupedMap = new HashMap<>();
        for (CpGIsland cpGIsland : cpGIslandList) {
            if (groupedMap.containsKey(cpGIsland.getChrom())) {
                groupedMap.get(cpGIsland.getChrom()).add(cpGIsland);
            } else {
                List<CpGIsland> newList = new ArrayList<>();
                newList.add(cpGIsland);
                groupedMap.put(cpGIsland.getChrom(), newList);
            }
        }
        return groupedMap;
    }

    private List<CpGIsland> pickRandomCpGIslands(String CpGIslandFileName) throws IOException {
        List<CpGIsland> pickedList = new ArrayList<>();
        List<CpGIsland> cpgIslandList = readCpGIsland(CpGIslandFileName);
        Random rand = new Random();
        for (int i = 0; i < regionNumber; i++) {
            int r = rand.nextInt(cpgIslandList.size());
            while (cpgIslandList.get(r).getCpgNum() <= MINCPGNUM || cpgIslandList.get(r).length() <= regionLength ||
                    pickedList.contains(cpgIslandList.get(r))) {
                r = rand.nextInt(cpgIslandList.size());
            }
            pickedList.add(cpgIslandList.get(r));
        }
        return pickedList;
    }

    private List<CpGIsland> readCpGIsland(String fileName) throws IOException {
        List<CpGIsland> cpGIslandList = new ArrayList<>();
        ICsvBeanReader beanReader = new CsvBeanReader(new FileReader(fileName), CsvPreference.TAB_PREFERENCE);
        beanReader.getHeader(true);
        final String[] header = new String[]{"chrom", "chromStart", "chromEnd", "name", null, "cpgNum", null, null, null, null};
        final CellProcessor[] processors = new CellProcessor[]{null, new ParseInt(), new ParseInt(), null, null, new ParseInt(), null, null, null, null};
        CpGIsland cpgIsland;
        while ((cpgIsland = beanReader.read(CpGIsland.class, header, processors)) != null) {
            if (!cpgIsland.getChrom().contains("_") && cpgIsland.getChrom().startsWith("chr")) {
                cpGIslandList.add(cpgIsland);
            }
        }
        return cpGIslandList;
    }
}
