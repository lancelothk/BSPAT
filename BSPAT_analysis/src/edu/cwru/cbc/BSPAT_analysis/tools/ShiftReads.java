package edu.cwru.cbc.BSPAT_analysis.tools;

import java.io.*;
import java.util.Random;

/**
 * Created by kehu on 1/30/15.
 * Cut reads' start and end with random length.
 */
public class ShiftReads {

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("/home/kehu/IdeaProjects/BSPAT/BSPAT/web/demo/demoSequence.fastq"));
        BufferedWriter writer = new BufferedWriter(new FileWriter("/home/kehu/IdeaProjects/BSPAT/BSPAT/web/demo/demoSequence_shift.fastq"));

        Random rand = new Random();

        String line;
        while ((line = reader.readLine()) != null){
            int shiftLeft = rand.nextInt(5);
            int shiftRight = rand.nextInt(5);
            writer.write(line + "\n");
            line = reader.readLine();
            writer.write(line.substring(shiftLeft, line.length() - shiftRight) + "\n");
            line = reader.readLine();
            writer.write(line + "\n");
            line = reader.readLine();
            writer.write(line.substring(shiftLeft, line.length() - shiftRight) + "\n");
        }

        reader.close();
        writer.close();
    }
}
