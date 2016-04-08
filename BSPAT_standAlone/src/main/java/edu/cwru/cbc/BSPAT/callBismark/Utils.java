package edu.cwru.cbc.BSPAT.callBismark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kehu on 2/29/16.
 */
public class Utils {

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
	 * cmd program caller wrapper.
	 *
	 * @return exit value
	 */
	public static int callCMD(List<String> cmds, File directory,
	                          String fileName) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder(cmds).directory(directory);
		if (fileName != null) {
			processBuilder.redirectOutput(new File(fileName));
			processBuilder.redirectError(new File(fileName));
		}
		Process process = processBuilder.start(); // throws IOException
		process.waitFor();
		return process.exitValue();
	}

}
