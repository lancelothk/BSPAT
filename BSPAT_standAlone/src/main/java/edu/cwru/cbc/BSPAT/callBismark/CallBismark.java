package edu.cwru.cbc.BSPAT.callBismark;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.cwru.cbc.BSPAT.commons.Utils;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CallBismark {

	private final static Logger LOGGER = Logger.getLogger(CallBismark.class.getName());
	private int maxmis;
	private String qualsTypeParameter;
	private String bismarkPath;
	private String bowtiePath;
	private String refPath;

	public CallBismark(String refPath, String bismarkPath, String bowtiePath, String logPath, String qualsType,
	                   int maxmis) throws IOException, InterruptedException {
		this.maxmis = maxmis;
		this.bismarkPath = bismarkPath;
		this.bowtiePath = bowtiePath;
		this.refPath = refPath;

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

		File logpathFile = new File(logPath);
		if (!logpathFile.exists()) {
			if (!logpathFile.mkdirs()) {
				throw new RuntimeException("unable to mkdir for " + logpathFile.getAbsolutePath());
			}
		}

		// build index
		/** 1. build index **/
		// add "--yes_to_all" to always overwrite index folder
		// add "--no" to always skip existing reference
		LOGGER.info("Call preparation:");
		List<String> cmdList = Arrays.asList(bismarkPath + "bismark_genome_preparation",
				"--yes_to_all", "--path_to_bowtie", bowtiePath, refPath);
		if (Utils.callCMD(cmdList, new File(refPath), logPath + "/bismark_prep.log") > 0) {
			throw new RuntimeException(
					"bismark preparation fail! Please double check your reference file<br>bismark logs:<br>" +
							Files.toString(new File(logPath + "/bismark_prep.log"), Charsets.UTF_8));
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException, ParseException {
		Options options = new Options();
		// Require all input path to be directory. File is not allowed.
		options.addOption(Option.builder("bismark").hasArg().desc("Bismark install path").build());
		options.addOption(Option.builder("bowtie").hasArg().desc("Bowtie install path").build());
		options.addOption(Option.builder("q").hasArg().desc("Quality score type. Default is phred33").build());
		options.addOption(Option.builder("n").hasArg().desc("Allowed mismatches. Default is 2").build());
		options.addOption(Option.builder("h").desc("Help").build());

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"callBismark [options] <reference file Path or file> <sequence path> <bismark result path>",
					options);
			System.exit(1);
		}

		String referencePath, bismarkResultPath, sequencePath;
		if (cmd.getArgList().size() != 3) {
			throw new RuntimeException(
					"Incorrect number of arguments! callBismark [options] <sequence path> <reference file Path or file> <bismark result path>");
		} else {
			referencePath = new File(cmd.getArgList().get(0)).getAbsolutePath();
			sequencePath = new File(cmd.getArgList().get(1)).getAbsolutePath();
			bismarkResultPath = new File(cmd.getArgList().get(2)).getAbsolutePath();
			if (!bismarkResultPath.endsWith("/")) {
				bismarkResultPath += "/";
			}
			System.out.println("Sequence path is " + sequencePath);
			System.out.println("Reference path is " + referencePath);
			System.out.println("Bismark result path is " + bismarkResultPath);
		}
		String bismarkPath = cmd.getOptionValue("bismark", "");
		if (!bismarkPath.equals("") && !bismarkPath.endsWith("/")) {
			bismarkPath += "/";
		}
		String bowtiePath = cmd.getOptionValue("bowtie", "");
		String qualType = cmd.getOptionValue("q", "phred33");
		int maxmis = Integer.parseInt(cmd.getOptionValue("n", "2"));
		CallBismark callBismark = new CallBismark(referencePath, bismarkPath, bowtiePath, bismarkResultPath, qualType,
				maxmis);
		callBismark.execute(sequencePath, bismarkResultPath, bismarkResultPath);
	}

	public void execute(String inputPath, String outputPath, String logPath) throws IOException, InterruptedException {
		String fastaq = "-q"; // default is -q/--fastq
		File inputFile = new File(inputPath);
		File outputFile = new File(outputPath);
		File tempDir = new File(outputPath + "tmp/");
		List<File> fileList = null;
		LOGGER.info("Call bismark for:\t" + inputPath);

		// if the output path not exists, create it.
		if (!outputFile.exists()) {
			outputFile.mkdirs();
		}

		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		/** 2. run bismark **/
		// if input is a folder (should be a folder)
		if (inputFile.isDirectory()) {
			File seqfolder = new File(inputPath);
			// recursive refresh file list
			fileList = Utils.visitFiles(seqfolder);
			if (fileList.size() == 0) {
				throw new RuntimeException("no sequencing data files in " + inputFile.getAbsolutePath());
			} else {
				for (File file : fileList) {
					// if contains at least one fasta file
					if (file.getName().endsWith(".fa") || file.getName().endsWith(".fasta")) {
						fastaq = "-f"; // for fasta file
						fileList = multiLineFastaToSingleLine(fileList);
						break;
					}
				}
				List<String> cmdList;
				if (fastaq.equals("-f")) {
					cmdList = new ArrayList<>(
							Arrays.asList(bismarkPath + "bismark", fastaq, "-n", String.valueOf(maxmis), "-o",
									outputFile.getAbsolutePath(), "--non_directional", "--quiet", "--un", "--ambiguous",
									"--bam", "--temp_dir", tempDir.getAbsolutePath(),
									refPath));
					if (!bowtiePath.equals("")) {
						cmdList.add("--path_to_bowtie");
						cmdList.add(bowtiePath);
					}
				} else {
					cmdList = new ArrayList<>(
							Arrays.asList(bismarkPath + "bismark", fastaq, qualsTypeParameter,
									"-n", String.valueOf(maxmis),
									"-o", outputFile.getAbsolutePath(), "--non_directional", "--quiet", "--un",
									"--ambiguous", "--bam", "--temp_dir", tempDir.getAbsolutePath(),
									refPath));
					if (!bowtiePath.equals("")) {
						cmdList.add("--path_to_bowtie");
						cmdList.add(bowtiePath);
					}
				}
				for (File file : fileList) {
					cmdList.add(file.getAbsolutePath());
				}
				if (Utils.callCMD(cmdList, new File(outputPath), logPath + "/bismark.log") > 0) {
					throw new RuntimeException(
							"bismark failed. Please double check your reference and sequence files<br>bismark logs:<br>" +
									Files.toString(new File(logPath + "/bismark.log"), Charsets.UTF_8));
				}
			}
		}

		/** 3. extract information **/
		LOGGER.info("Call extractor for:\t" + inputPath);
		List<String> cmdList = new ArrayList<>(
				Arrays.asList(bismarkPath + "bismark_methylation_extractor", "-s", "-o",
						outputPath, "--comprehensive", "--no_header", "--merge_non_CpG"));
		for (File f : fileList) {
			cmdList.add(outputFile.getAbsolutePath() + "/" + f.getName() + "_bismark.bam");
		}
		if (Utils.callCMD(cmdList, new File(outputPath), logPath + "/bismark_methylExtractor.log") > 0) {
			throw new RuntimeException(
					"bismark methylExtractor failed. Please double check your reference and sequence files<br>bismark logs:<br>" +
							Files.toString(new File(logPath + "/bismark_methylExtractor.log"), Charsets.UTF_8));
		}

		LOGGER.info("Call bismark finished!!!");
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
