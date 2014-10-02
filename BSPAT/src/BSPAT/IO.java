package BSPAT;

import DataType.Coordinate;
import DataType.ExtensionFilter;
import DataType.MappingSummary;

import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IO {
    /**
     * read coordinates file
     *
     * @param coorPath
     * @param coorFileName
     * @return coordinateHash
     * @throws IOException
     */
    public static HashMap<String, Coordinate> readCoordinates(String coorPath, String coorFileName) {
        /**
         * coordinates file format:
         * <0:region ID - String> <1:chrom - String> <2:strand - String> <3:start position - long> <4:end position - long>
         * E.g: 19	2	+	38155208	38155307
         */
        HashMap<String, Coordinate> coordinateHash = new HashMap<>();
        File coorFolder = new File(coorPath);
        try (BufferedReader coordinatesBuffReader = new BufferedReader(
                new FileReader(coorFolder.getAbsolutePath() + "/" + coorFileName))) {
            String line = coordinatesBuffReader.readLine();
            String[] items;
            while (line != null) {
                if (!line.equals("")) {
                    items = line.split("\t");
                    if (items.length != 5){
                        throw new RuntimeException("invalid coordinate file! Please double check your uploaded coordinate file");
                    }
                    coordinateHash.put(items[0], new Coordinate(items[0], items[1], items[2], Long.valueOf(items[3]),
                                                                Long.valueOf(items[4])));
                }
                line = coordinatesBuffReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("error in reading coordinate file! Please double check your uploaded coordinate file");
        }
        return coordinateHash;
    }

    public static void createFolder(String path) {
        File folder = new File(path);
        if (!folder.isDirectory()) {
            folder.mkdir();
        }
    }

    public static void deleteFiles(String path, String[] extensions) {
        File folder = new File(path);
        File[] files = folder.listFiles(new ExtensionFilter(extensions));
        for (File file : files) {
            file.delete();
        }
    }

    public static void saveFileToDisk(Part part, String path, String fileName) {
        File dir = new File(path);
        if (!dir.isDirectory()) { // if path directory do not exist, make one
            dir.mkdirs();
        }
        try {
            part.write(path + "/" + fileName);
        } catch (IOException e) {
            throw new RuntimeException(fileName + " failed to transmit to server!", e);
        }
    }

    // only add allowed files.
    public static List<File> visitFiles(File f) {
        List<File> list = new ArrayList<File>();
        File[] files = f.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                list.addAll(visitFiles(file));
            } else {
                if (file.getName().endsWith(".txt") || file.getName().endsWith(".fq") ||
                        file.getName().endsWith(".fastq") ||
                        file.getName().endsWith(".fa") || file.getName().endsWith(".fasta") ||
                        file.getName().endsWith(".fna") || file.getName().endsWith(".ffn") ||
                        file.getName().endsWith(".fas") || file.getName().endsWith(".faa") ||
                        file.getName().endsWith(".frn")) {
                    list.add(file);
                }
            }
        }
        return list;
    }

    /**
     * Read mapping report and generate mapping summary
     *
     * @param mappingResultPath
     * @return
     * @throws IOException
     */
    public static MappingSummary readMappingSummary(String mappingResultPath) throws IOException {
        MappingSummary mappingSummary = new MappingSummary();
        File mappingResultPathFile = new File(mappingResultPath);
        File[] folders = mappingResultPathFile.listFiles();
        for (File folder : folders) {
            // only check folder
            if (folder.isDirectory()) {
                File[] files = folder.listFiles();
                files = folder.listFiles(new ExtensionFilter(new String[]{"_report.txt"}));
                for (File file : files) {
                    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file));) {
                        // ignore first 5 lines
                        for (int i = 0; i < 5; i++) {
                            bufferedReader.readLine();
                        }
                        // read following 6 lines
                        for (int i = 0; i < 6; i++) {
                            String line = bufferedReader.readLine();
                            String value = line.split(":\t")[1];
                            switch (i) {
                                case 0:
                                    mappingSummary.addSeqAnalysed(Long.parseLong(value));
                                    break;
                                case 1:
                                    mappingSummary.addUniqueBestHit(Long.parseLong(value));
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    mappingSummary.addNoAlignment(Long.parseLong(value));
                                    break;
                                case 4:
                                    mappingSummary.addNotUnique(Long.parseLong(value));
                                    break;
                                case 5:
                                    mappingSummary.addNotExtracted(Long.parseLong(value));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        return mappingSummary;
    }
}