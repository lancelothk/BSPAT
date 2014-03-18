package BSPAT;

import DataType.CpGSite;
import DataType.ExtensionFilter;
import DataType.Sequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * Import reference and Bismark result to specific data structure
 *
 * @author Ke
 */
public class ImportBismarkResult {

    private String inputFolder;
    private String refPath;
    private Hashtable<String, String> referenceSeqs = new Hashtable<String, String>();
    private Hashtable<String, Sequence> sequencesHashtable = new Hashtable<String, Sequence>();

    public ImportBismarkResult(String refPath, String inputFolder) throws IOException {
        this.inputFolder = inputFolder;
        this.refPath = refPath;
        readReference();
        readBismarkAlignmentResult();
        readBismarkCpGResult();
    }

    public Hashtable<String, String> getReferenceSeqs() {
        return referenceSeqs;
    }

    public List<Sequence> getSequencesList() {
        List<Sequence> sequencesList = new ArrayList<Sequence>();
        for (Sequence seq : sequencesHashtable.values()) {
            sequencesList.add(seq);
        }
        return sequencesList;
    }

    /**
     * readReference
     *
     * @throws IOException
     */
    private void readReference() throws IOException {
        File refPathFile = new File(refPath);
        String[] fileNames = null;
        fileNames = refPathFile.list(new ExtensionFilter(new String[]{".txt", "fasta", "fa"}));
        for (String str : fileNames) {
            FileReader fileReader = new FileReader(refPathFile + "/" + str);
            BufferedReader buffReader = new BufferedReader(fileReader);
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
            buffReader.close();
        }
    }

    /**
     * readBismarkAlignmentResult
     *
     * @throws IOException
     */
    private void readBismarkAlignmentResult() throws IOException {
        File inputFile = new File(inputFolder);
        String[] names = inputFile.list(new ExtensionFilter(new String[]{"_bismark.sam"}));
        Arrays.sort(names);

        for (String name : names) {
            FileReader fileReader = new FileReader(inputFolder + name);
            BufferedReader buffReader = new BufferedReader(fileReader);
            String line = buffReader.readLine();
            String[] items = null;
            while (line != null) {
                items = line.split("\t");
                // substract two bps from the start position to match original
                // reference
                Sequence seq = new Sequence(items[0], items[1], items[2], Integer.valueOf(items[3]) - 2, items[9],
                                            items[10], cutTag(items[11]), cutTag(items[12]), cutTag(items[13]),
                                            cutTag(items[14])
                );
                if (cutTag(items[14]).equals("CT")) {
                    seq.setFRstate("F");
                } else {
                    seq.setFRstate("R");
                }
                sequencesHashtable.put(seq.getId(), seq);
                line = buffReader.readLine();
            }
            buffReader.close();
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
     * @throws IOException
     */
    private void readBismarkCpGResult() throws IOException {
        File inputFile = new File(inputFolder);
        String[] names = inputFile.list(new ExtensionFilter(new String[]{"_bismark.txt"}));
        Arrays.sort(names);

        for (String name : names) {
            // only read CpG context result
            if (name.startsWith("CpG_context")) {
                FileReader fileReader = new FileReader(inputFolder + name);
                BufferedReader buffReader = new BufferedReader(fileReader);
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
                    seq = sequencesHashtable.get(items[0]);
                    if (seq != null) {
                        // substract two bps from the start position to match
                        // original reference
                        CpGSite cpg = new CpGSite(Integer.parseInt(items[3]) - 2, methylLabel);
                        seq.addCpG(cpg);
                    }
                    line = buffReader.readLine();
                }
                buffReader.close();
            }
        }
    }

    // private String removeMisChr(){
    // Hashtable<String , Sequence> tempSeqTable = new Hashtable<String,
    // Sequence>();
    // // sum count of each mapped region
    // Hashtable< String, Integer> majorHash = new Hashtable<String, Integer>();
    // for (Sequence seq : sequencesHashtable.values()) {
    // if (!majorHash.containsKey(seq.getRegion())){
    // majorHash.put(seq.getRegion(), 1);
    // }else {
    // majorHash.put(seq.getRegion(), majorHash.get(seq.getRegion())+1);
    // }
    // }
    // // get major region
    // String major = null;
    // int num = 0;
    // for (String key : majorHash.keySet()) {
    // if (majorHash.get(key) > num){
    // major = key;
    // num = majorHash.get(key);
    // }
    // }
    // //exclude non-major ones.
    // for (Sequence seq : sequencesHashtable.values()) {
    // if (seq.getRegion().equals(major)){
    // tempSeqTable.put(seq.getId(), seq);
    // }
    // }
    // sequencesHashtable = tempSeqTable;
    // return major;
    // }

}
