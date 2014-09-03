package BSPAT;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
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

    public CallBismark(String refPath, String toolsPath, String logPath, String qualsType,
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
        System.out.println("Call preparation:");
        System.out.println(refPathFile.getAbsolutePath());
        List<String> cmdList = Arrays.asList(bismarkPathFile.getAbsolutePath() + "/bismark_genome_preparation",
                                             "--yes_to_all", "--path_to_bowtie", bowtiePathFile.getAbsolutePath(),
                                             refPathFile.getAbsolutePath());
        Utilities.callCMD(cmdList, new File(refPathFile.getAbsolutePath()), logPath + "/bismark_prep.log");
        System.out.println(refPathFile.getAbsolutePath());
    }

    public void execute(String inputPath, String outputPath, String logPath) throws IOException, InterruptedException {
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
            if (fileList.size() == 0) {
                throw new RuntimeException("no sequencing data files in " + inputFile.getAbsolutePath());
            } else {
                for (File file : fileList) {
                    // if contains at least one fasta file
                    if (file.getName().endsWith(".fa") || file.getName().endsWith(".fasta")) {
                        fastaq = "-f"; // for fasta file
                        fileList = multiLineFastaToSingleLine(fileList);
                        qualsTypeParameter = "";
                        break;
                    }
                }
                List<String> cmdList = new ArrayList<>(
                        Arrays.asList(bismarkPathFile.getAbsolutePath() + "/bismark", fastaq, qualsTypeParameter,
                                      "--path_to_bowtie", bowtiePathFile.getAbsolutePath(), "-n",
                                      String.valueOf(maxmis), "-o", outputFile.getAbsolutePath(), "--non_directional",
                                      "--quiet", "--un", "--ambiguous", "--sam-no-hd", "--temp_dir",
                                      tempDir.getAbsolutePath(), refPathFile.getAbsolutePath()));
                for (File file : fileList) {
                    cmdList.add(file.getAbsolutePath());
                }
                Utilities.callCMD(cmdList, new File(outputPath), logPath + "/bismark_.log");
            }
        }

        /** 3. extract information **/
        System.out.println("Call extractor for:\t" + inputPath);
        List<String> cmdList = new ArrayList<>(
                Arrays.asList(bismarkPath + "/bismark_methylation_extractor", "-s", "-o", outputPath, "--comprehensive",
                              "--no_header", "--merge_non_CpG"));
        for (File f : fileList) {
            cmdList.add(outputFile.getAbsolutePath() + "/" + f.getName() + "_bismark.sam");
        }
        Utilities.callCMD(cmdList, new File(outputPath), logPath + "/bismark_methylExtractor.log");

        System.out.println("Call bismark finished!!!");
        //clean tmp files
        tempDir.delete();
    }

    /**
     * Convert multi-line fasta file to single line fasta file. delete original files.
     *
     * @param fileList original file list
     * @return converted file list
     * @throws IOException
     */
    private List<File> multiLineFastaToSingleLine(List<File> fileList) {
        List<File> newFileList = new ArrayList<>();
        for (File file : fileList) {
            File newFile = new File(file.getAbsoluteFile() + "_processed.fa");
            try (BufferedReader reader = new BufferedReader(new FileReader(file));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(newFile))) {
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
            } catch (IOException e) {
                throw new RuntimeException("IO error in converting multi-line fasta file to single line fasta file", e);
            }
            if (!file.delete()) {
                throw new RuntimeException(
                        "failed to delete old fasta file when merge multiple lines to single line: " + file.getPath());
            }
            newFileList.add(newFile);
        }
        return newFileList;
    }
}
