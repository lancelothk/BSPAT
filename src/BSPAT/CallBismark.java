package BSPAT;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CallBismark {
    private String bismarkPath;
    private String bowtiePath;
    private int maxmis;
    private File bismarkPathFile;
    private File bowtiePathFile;
    private File refPathFile;
    private String qualsTypeParameter;


    //	public static void main(String[] args) throws IOException {
    //		String cellLine = "DU145";
    //		String seqPath = "/home/ke/bismark_result/reads/" + cellLine + "_withoutBarcode/";
    //		String bismarkResultPath = "/home/ke/bismark_result/out-B1/" + cellLine + "-SAM/";
    //		String seqFile = null;
    //		String refPath = "/home/ke/bismark_result/ref-B1/";
    //		CallBismark callBismark = new CallBismark(seqPath + seqFile, bismarkResultPath, refPath);
    //	}

    public CallBismark(String refPath, String toolsPath, String qualsType,
                       int maxmis) throws IOException, InterruptedException {
        this.maxmis = maxmis;
        this.bismarkPath = toolsPath + "/bismark/";
        this.bowtiePath = toolsPath + "/bowtie/";
        this.bismarkPathFile = new File(bismarkPath);
        this.bowtiePathFile = new File(bowtiePath);
        this.refPathFile = new File(refPath);

        this.qualsTypeParameter = "";
        if (qualsType.equals("phred33")) {
            this.qualsTypeParameter = "--phred33-quals";
        } else if (qualsType.equals("phred64")) {
            this.qualsTypeParameter = "--phred64-quals";
        } else if (qualsType.equals("solexa")) {
            this.qualsTypeParameter = "--solexa-quals";
        } else if (qualsType.equals("solexa1.3")) {
            this.qualsTypeParameter = "--solexa1.3-quals";
        }

        // build index
        /** 1. build index **/
        // add "--yes_to_all" to always overwrite index folder
        // add "--no" to always skip existing reference
        String preparation = String.format("%s/bismark_genome_preparation --yes_to_all --path_to_bowtie %s %s",
                bismarkPathFile.getAbsolutePath(), bowtiePathFile.getAbsolutePath(), refPathFile.getAbsolutePath());
        System.out.println("Call preparation:");
        Utilities.callCMD(preparation, new File(refPathFile.getAbsolutePath()),
                          refPathFile.getAbsolutePath() + "bismark_prep.log");
    }

    public void execute(String inputPath, String outputPath) throws IOException {
        String fastaq = "-q"; // default is -q/--fastq
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        File tempDir = new File(outputPath + "tmp/");
        List<File> fileList = null;
        System.out.println("Call bismark for:\t" + inputPath);

        // if the output path not exists, create it.
        if (!outputFile.exists()) {
            outputFile.mkdirs();
        }

        try {
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            /** 2. run bismark **/
            String bismark = "";

            // if input is a folder (should be a folder)
            if (inputFile.isDirectory()) {
                File seqfolder = new File(inputPath);
                File[] seqfiles = seqfolder.listFiles();
                // if zip file, unzip
                for (File file : seqfiles) {
                    if (file.getName().endsWith(".zip")) {
                        Utilities.unZip(file, inputPath);
                        file.delete();
                    }
                }
                // recursive refresh file list
                fileList = IO.visitFiles(seqfolder);
                //fileNames = folder.list(new ExtensionFilter(new String[] { ".txt", ".fq", "fastq", "fasta", "fa" }));
                if (fileList.size() == 0) {
                    System.err.println("no sequencing data files in " + inputFile);
                    return;
                } else {
                    for (File file : fileList) {
                        // if contains at least one fasta file
                        if (file.getName().endsWith(".fa") || file.getName().endsWith(".fasta") ||
                                file.getName().endsWith(".fna") || file.getName().endsWith(".ffn") ||
                                file.getName().endsWith(".fas") || file.getName().endsWith(".faa") ||
                                file.getName().endsWith(".frn")) {
                            fastaq = "-f"; // for fasta file
                            fileList = multilineFastaToSingleLine(fileList);
                            qualsTypeParameter = "";
                            break;
                        }
                    }
                    String nameList = "";
                    int i = 0;
                    for (File file : fileList) {
                        i++;
                        nameList += (" " + file.getAbsoluteFile());
                        if (i == 20) {
                            bismark = String.format(
                                    "%s/bismark %s %s --path_to_bowtie %s -n %d -o %s --non_directional --quiet --un --ambiguous --sam-no-hd --temp_dir %s %s %s",
                                    bismarkPathFile.getAbsolutePath(), fastaq, qualsTypeParameter,
                                    bowtiePathFile.getAbsolutePath(), maxmis, outputFile.getAbsolutePath(),
                                    tempDir.getAbsoluteFile(), refPathFile.getAbsolutePath(), nameList
                                                   );
                            Utilities.callCMD(bismark, new File(outputPath), outputPath + "bismark_" + i + ".log");
                            nameList = "";
                            i = 0;
                        }
                    }
                    if (!nameList.equals("")) {
                        bismark = String.format(
                                "%s/bismark %s %s --path_to_bowtie %s -n %d -o %s --non_directional --quiet --un --ambiguous --sam-no-hd --temp_dir %s %s %s",
                                bismarkPathFile.getAbsolutePath(), fastaq, qualsTypeParameter,
                                bowtiePathFile.getAbsolutePath(), maxmis, outputFile.getAbsolutePath(),
                                tempDir.getAbsoluteFile(), refPathFile.getAbsolutePath(), nameList
                                               );
                        Utilities.callCMD(bismark, new File(outputPath), outputPath + "bismark_" + i + ".log");
                    }
                }
            }

            /** 3. extract information **/
            System.out.println("Call extractor for:\t" + inputPath);
            String seqFile = null;
            if (fileList != null) {
                seqFile = "";
                for (File f : fileList) {
                    seqFile = seqFile + " " + outputFile.getAbsolutePath() + "/" + f.getName() + "_bismark.sam";
                }
            } else {
                seqFile = inputFile.getName() + "_bismark.sam";
            }
            String extractor = String.format(
                    "%sbismark_methylation_extractor -s -o %s --comprehensive --no_header --merge_non_CpG %s",
                    bismarkPath, outputPath, seqFile
                                            );
            Utilities.callCMD(extractor, new File(outputPath), outputPath + "bismark_methylExtractor.log");

            System.out.println("Call bismark finished!!!");
            //clean tmp files
            tempDir.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert multiline fasta file to single line fasta file. delete original files.
     *
     * @param fileList original file list
     * @return converted file list
     * @throws IOException
     */
    private List<File> multilineFastaToSingleLine(List<File> fileList) throws IOException {
        List<File> newFileList = new ArrayList<>();
        for (File file : fileList) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            File newFile = new File(file.getAbsoluteFile() + "_processed.fa");
            BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
            String line;
            StringBuilder seq = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (seq.length() > 0) {
                        writer.write(seq.toString() + "\n");
                        seq = new StringBuilder();
                    }
                    writer.write(line + "\n");
                } else {
                    seq.append(line);
                }
            }
            if (seq.length() > 0) {
                writer.write(seq.toString());
            }
            reader.close();
            writer.close();
            file.delete();
            newFileList.add(newFile);
        }
        return newFileList;
    }
}
