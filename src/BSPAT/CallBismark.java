package BSPAT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CallBismark {
	private String bismarkPath;
	private String bowtiePath;
	private int maxmis;
	private File bismarkPathFile;
	private File bowtiePathFile;
	private File refPathFile;
	private String qualsTypeParameter;
	private String fastaq;

	//	public static void main(String[] args) throws IOException {
	//		String cellLine = "DU145";
	//		String seqPath = "/home/ke/bismark_result/reads/" + cellLine + "_withoutBarcode/";
	//		String bismarkResultPath = "/home/ke/bismark_result/out-B1/" + cellLine + "-SAM/";
	//		String seqFile = null;
	//		String refPath = "/home/ke/bismark_result/ref-B1/";
	//		CallBismark callBismark = new CallBismark(seqPath + seqFile, bismarkResultPath, refPath);
	//	}

	public CallBismark(String refPath, String toolsPath, String qualsType, int maxmis)
			throws IOException, InterruptedException {
		this.maxmis = maxmis;

		this.bismarkPath = toolsPath + "/bismark/";
		this.bowtiePath = toolsPath + "/bowtie/";
		this.bismarkPathFile = new File(bismarkPath);
		this.bowtiePathFile = new File(bowtiePath);
		this.refPathFile = new File(refPath);

		this.fastaq = "-q"; // default is -q/--fastq
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
		// add "--yes_to_all" to always overwrite index folder, v0.7.4 don't contain this function, (v0.5.4 has)
		// add "--no" to always skip existing reference
		String preparation = bismarkPathFile.getAbsolutePath() + "/bismark_genome_preparation  --no --path_to_bowtie "
				+ bowtiePathFile.getAbsolutePath() + " --verbose " + refPathFile.getAbsolutePath();
		System.out.println("Call preparation:");
		Utilities.callCMD(preparation, null);
	}

	public void execute(String inputPath, String outputPath) throws IOException {
		File inputFile = new File(inputPath);
		File outputFile = new File(outputPath);
		File tempDir = new File(outputPath + "tmp/");
		ArrayList<File> fileList = null;

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
				fileList = Utilities.visitFiles(seqfolder);
				//fileNames = folder.list(new ExtensionFilter(new String[] { ".txt", ".fq", "fastq", "fasta", "fa" }));
				if (fileList.size() == 0) {
					System.err.println("no sequencing data files in " + inputFile);
					return;
				} else {
					if (fileList.get(0).getName().endsWith(".fa") || fileList.get(0).getName().endsWith(".fasta")) {
						fastaq = "-f"; // for fasta file
						qualsTypeParameter = "";
					}
					String nameList = "";
					int i = 0;
					for (File file : fileList) {
						i++;
						nameList += (" " + file.getAbsoluteFile());
						if (i == 20) {
							bismark = bismarkPathFile.getAbsolutePath() + "/bismark " + fastaq
									+ " --non_directional -n " + maxmis + " " + qualsTypeParameter
									+ " --path_to_bowtie " + bowtiePathFile.getAbsolutePath() + " -o "
									+ outputFile.getAbsolutePath() + " --un --ambiguous "
									+ refPathFile.getAbsolutePath() + " --sam-no-hd --temp_dir "
									+ tempDir.getAbsoluteFile() + " " + nameList;
							System.out.println("Call bismark:");
							Utilities.callCMD(bismark, new File(inputPath));
							nameList = "";
							i = 0;
						}
					}
					if (!nameList.equals("")) {
						bismark = bismarkPathFile.getAbsolutePath() + "/bismark " + fastaq + " --non_directional -n "
								+ maxmis + " " + qualsTypeParameter + " --path_to_bowtie "
								+ bowtiePathFile.getAbsolutePath() + " -o " + outputFile.getAbsolutePath()
								+ " --un --ambiguous " + refPathFile.getAbsolutePath() + " --sam-no-hd --temp_dir "
								+ tempDir.getAbsoluteFile() + " " + nameList;
						System.out.println("Call bismark:");
						Utilities.callCMD(bismark, new File(inputPath));
					}
				}
			}

			/** 3. extract information **/
			String seqFile = null;
			if (fileList  != null) {
				seqFile = "";
				for (File f : fileList) {
					seqFile = seqFile + " " + outputFile.getAbsolutePath() + "/" + f.getName() + "_bismark.sam";
				}
			} else {
				seqFile = inputFile.getName() + "_bismark.sam";
			}
			String extractor = bismarkPath + "methylation_extractor -s --comprehensive --merge_non_CpG " + seqFile;
			System.out.println("Call extractor:");
			Utilities.callCMD(extractor, new File(outputPath));

			System.out.println("Call bismark finished!!!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
