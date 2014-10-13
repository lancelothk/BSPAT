package edu.cwru.cbc.BSPAT_analysis.tools;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kehu on 9/2/14.
 *
 */
public class CorrectHeader {

    public static void main(String[] args) throws IOException {
        int duplicateCount = 0, diffSeq = 0;
        Map<String, String> seqMap = new HashMap<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(
                "/home/kehu/experiments/BSPAT/Bismark_insertion/supplementary_data/plate1_454_format.fasta"));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
                "/home/kehu/experiments/BSPAT/Bismark_insertion/supplementary_data/plate1_454_format.corrected.fasta"));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith(">")) {
                String header = line.split("~")[0];
                String seq = bufferedReader.readLine();
                if (seqMap.containsKey(header)) {
                    if (seqMap.get(header).equals(seq)) {
                        duplicateCount++;
                    } else {
                        diffSeq++;
                    }
                } else {
                    seqMap.put(header, seq);
                    bufferedWriter.write(header + "\n");
                    bufferedWriter.write(seq + "\n");
                }
            } else {
                throw new RuntimeException("invalid sequence line!" + line);
            }
        }

        System.out.println("duplicated reads:\t" + duplicateCount);
        System.out.println("reads with same header but diff seq:\t" + diffSeq);
        bufferedReader.close();
        bufferedWriter.close();
    }
}
