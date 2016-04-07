package edu.cwru.cbc.BSPAT.commons;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kehu on 2/29/16.
 */
public class Utils {

	public static Map<String, List<BedInterval>> readBedFile(String bedFile) throws IOException {
		return Files.asCharSource(new File(bedFile), Charsets.UTF_8)
				.readLines(new LineProcessor<Map<String, List<BedInterval>>>() {
					private final Splitter tabSplitter = Splitter.on("\t");
					private Map<String, List<BedInterval>> bedIntervalMap = new HashMap<>();

					@Override
					public boolean processLine(String line) throws IOException {
						List<String> itemList = tabSplitter.splitToList(line);
						if (itemList.size() == 5) {
							boolean isPlusStrand;
							switch (itemList.get(4)) {
								case "+":
									isPlusStrand = true;
									break;
								case "-":
									isPlusStrand = false;
									break;
								default:
									throw new RuntimeException(
											"invalid strand symbol in target region file: " + itemList.get(4));
							}
							//  require bed file position 0-based.
							BedInterval bedInterval = new BedInterval(itemList.get(0),
									Integer.parseInt(itemList.get(1)), Integer.parseInt(itemList.get(2)),
									itemList.get(3), isPlusStrand);
							List<BedInterval> bedIntervalList = bedIntervalMap.get(itemList.get(0));
							if (bedIntervalList == null) {
								bedIntervalList = new ArrayList<>();
								bedIntervalList.add(bedInterval);
								bedIntervalMap.put(itemList.get(0), bedIntervalList);
							} else {
								bedIntervalList.add(bedInterval);
							}
							return true;
						} else {
							throw new RuntimeException(
									"in valid target region file! Should contain 5 columns: <Ref_name> <start_position>   <end _position>    <region_name>   <stand(+/-)>");
						}
					}

					@Override
					public Map<String, List<BedInterval>> getResult() {
						return bedIntervalMap;
					}
				});
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
