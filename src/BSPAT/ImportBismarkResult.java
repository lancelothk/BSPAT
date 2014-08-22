package BSPAT;

import DataType.CpGSite;
import DataType.ExtensionFilter;
import DataType.Sequence;

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
        for (Sequence seq : sequencesHashMap.values()) {
            sequencesList.add(seq);
		}
		return sequencesList;
	}

	/**
	 * readReference
	 *
	 * @param refPath
	 * @throws IOException
	 */
	private void readReference(String refPath) throws IOException {
		File refPathFile = new File(refPath);
		String[] fileNames = null;
		fileNames = refPathFile.list(new ExtensionFilter(new String[]{".txt", "fasta", "fa", "fna"}));
		for (String str : fileNames) {
			try (BufferedReader buffReader = new BufferedReader(new FileReader(refPathFile + "/" + str))) {
				String line, name = null;
				StringBuilder ref = new StringBuilder();
				while ((line = buffReader.readLine()) != null) {
					if (line.startsWith(">")) {
						if (ref.length() > 0) {
							referenceSeqs.put(name, ref.toString().toUpperCase());
							ref = new StringBuilder();
						}
						name = line.replace(">", "");
					} else {
						ref.append(line);
					}
				}
				if (ref.length() > 0) {
					referenceSeqs.put(name, ref.toString().toUpperCase());
					ref = new StringBuilder();
				}
			}
		}
	}

	/**
	 * readBismarkAlignmentResult
	 *
	 * @param inputFolder
	 * @throws IOException
	 */
	private void readBismarkAlignmentResult(String inputFolder) throws IOException {
		File inputFile = new File(inputFolder);
		String[] names = inputFile.list(new ExtensionFilter(new String[]{"_bismark.sam"}));
		Arrays.sort(names);

		for (String name : names) {
			try (BufferedReader buffReader = new BufferedReader(new FileReader(inputFolder + name))) {
				String line = buffReader.readLine();
				String[] items = null;
				while (line != null) {
					items = line.split("\t");
                    // substract two bps from the start position to match original reference
                    // Since bismark use 1-based position, substract one more bp to convert to 0-based position.
                    Sequence seq = new Sequence(items[0], items[1], items[2], Integer.valueOf(items[3]) - 3, items[9],
                                                items[10], cutTag(items[11]), cutTag(items[12]), cutTag(items[13]),
												cutTag(items[14]));
					if (cutTag(items[14]).equals("CT")) {
						seq.setFRstate("F");
					} else {
						seq.setFRstate("R");
					}
                    sequencesHashMap.put(seq.getId(), seq);
                    line = buffReader.readLine();
				}
			}
		}
	}

	/**
	 * extract value from TAG:TYPE:VALUE, e.g. "XR:Z:GA"
	 *
	 * @param raw
	 * @return extracted value string
	 */
	private String cutTag(String raw) {
		return raw.split(":")[2];
	}

	/**
	 * readBismarkCpGResult
	 *
	 * @param inputFolder
	 * @throws IOException
	 */
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
					boolean methylLabel;
					Sequence seq;

					while (line != null) {
						items = line.split("\t");
						if (items[1].equals("+")) {
							methylLabel = true;
						} else {
							methylLabel = false;
						}
                        seq = sequencesHashMap.get(items[0]);
                        if (seq != null) {
                            // substract two bps from the start position to match original reference
                            // Since bismark use 1-based position, substract one more bp to convert to 0-based position.
                            CpGSite cpg = new CpGSite(Integer.parseInt(items[3]) - 3, methylLabel);
                            seq.addCpG(cpg);
						}
						line = buffReader.readLine();
					}
				}
			}
		}
	}

}
