package edu.cwru.cbc.BSPAT;


import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Import reference and Bismark result to specific data structure
 *
 * @author Ke
 */
public class ImportBismarkResult {
	private Map<String, String> referenceSeqs = new Hashtable<>();
	// use hashMap to remove duplicated seqs and match methyl calling result
	private Map<String, Sequence> sequencesHashMap = new Hashtable<>();

	public ImportBismarkResult(String refPath, String inputFolder) throws IOException {
		readReference(refPath);
		readBismarkAlignmentResult(inputFolder);
		readBismarkCpGResult(inputFolder);
	}

	public Map<String, String> getReferenceSeqs() {
		return referenceSeqs;
	}

	public List<Sequence> getSequencesList() {
		List<Sequence> sequencesList = new ArrayList<>();
		sequencesList.addAll(sequencesHashMap.values());
		return sequencesList;
	}

	private void readReference(String refPath) throws IOException {
		File refPathFile = new File(refPath);
		String[] fileNames = refPathFile.list(new ExtensionFilter(new String[]{".txt", "fasta", "fa", "fna"}));
		for (String str : fileNames) {
			try (BufferedReader buffReader = new BufferedReader(new FileReader(refPathFile + "/" + str))) {
				String line, name = null;
				StringBuilder ref = new StringBuilder();
				while ((line = buffReader.readLine()) != null) {
					if (line.length() != 0 && line.charAt(0) == '>') {
						if (ref.length() > 0) {
							referenceSeqs.put(name, ref.toString().toUpperCase());
							ref = new StringBuilder();
						}
						name = line.substring(1, line.length());
					} else {
						ref.append(line);
					}
				}
				if (ref.length() > 0) {
					referenceSeqs.put(name, ref.toString().toUpperCase());
				}
			}
		}
	}

	private void readBismarkAlignmentResult(String inputFolder) throws IOException {
		File inputFile = new File(inputFolder);
		File[] inputFiles = inputFile.listFiles(new ExtensionFilter(new String[]{"_bismark.sam", "_bismark.bam"}));

		for (File file : inputFiles) {
			final SamReader reader = SamReaderFactory.makeDefault().open(file);
			for (final SAMRecord samRecord : reader) {
				Sequence seq = new Sequence(samRecord.getReadName(),
						(samRecord.getFlags() & 0x10) == 0x10 ? "BOTTOM" : "TOP", samRecord.getReferenceName(),
						samRecord.getStart() - 1, samRecord.getReadString()); // 0-based start position
				sequencesHashMap.put(seq.getId(), seq);
			}


			try (BufferedReader buffReader = new BufferedReader(new FileReader(inputFolder + file.getName()))) {
				String line = buffReader.readLine();
				String[] items;
				while (line != null && !line.startsWith("@")) {
					items = line.split("\t");
					// substract two bps from the start position to match original reference
					// Since bismark use 1-based position, substract one more bp to convert to 0-based position.

					line = buffReader.readLine();
				}
			}
		}
	}

	private void readBismarkCpGResult(String inputFolder) throws IOException {
		File inputFile = new File(inputFolder);
		String[] names = inputFile.list(new ExtensionFilter(new String[]{"_bismark.txt"}));
		Arrays.sort(names);

		for (String name : names) {
			// only read CpG context result
			if (name.startsWith("CpG_context")) {
				try (BufferedReader buffReader = new BufferedReader(new FileReader(inputFolder + name))) {
					String line = buffReader.readLine();
					String[] items;
					Sequence seq;

					while (line != null) {
						items = line.split("\t");
						seq = sequencesHashMap.get(items[0]);
						if (seq != null) {
							// subtract two bps from the start position to match original reference
							// Since bismark use 1-based position, substract one more bp to convert to 0-based position.
							int cpgPos = Integer.parseInt(items[3]) - 1;
							if (seq.getStrand().equals("BOTTOM")) {
								cpgPos--;
							}
							CpGSite cpg = new CpGSite(cpgPos, items[1].equals("+"));
							seq.addCpG(cpg);
						}
						line = buffReader.readLine();
					}
				}
			}
		}
	}

}
