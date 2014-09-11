package tools;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kehu on 8/28/14.
 *
 */
public class AttachPrimer {

    public static void main(String[] args) throws IOException {
        Map<String, String> primerMap = readPrimerMap(
                "/home/kehu/experiments/BSPAT/BisulfiteAmplicon/PrEC_remainPrimer/barcodesFWD_REV2.txt");
        for (String key : primerMap.keySet()) {
            attachPrimer("/home/kehu/experiments/BSPAT/BisulfiteAmplicon/PrEC_remainPrimer/PrEC_trim_adaptor",
                         "/home/kehu/experiments/BSPAT/BisulfiteAmplicon/PrEC_remainPrimer/result", key,
                         primerMap.get(key));
        }
    }

    public static Map<String, String> readPrimerMap(String fileName) throws IOException {
        Map<String, String> primerMap = new HashMap<>();
        CsvListReader csvListReader = new CsvListReader(new BufferedReader(new FileReader(fileName)),
                                                        CsvPreference.TAB_PREFERENCE);
        List<String> columns;
        while ((columns = csvListReader.read()) != null) {
            if (columns.size() == 2) {
                if (!primerMap.containsKey(columns.get(0))) {
                    primerMap.put(columns.get(0), columns.get(1));
                } else {
                    throw new RuntimeException("duplicate key in line:" + csvListReader.getLineNumber());
                }
            } else {
                throw new RuntimeException("column number incorrect in line:" + csvListReader.getLineNumber());
            }
        }
        csvListReader.close();
        return primerMap;
    }

    public static void attachPrimer(String inputPath, String outputPath, String key, String primer) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(inputPath + "/" + key + ".txt"));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath + "/" + key + ".txt"));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            // first line
            bufferedWriter.write(line + "\n");
            // second line
            line = bufferedReader.readLine();
            bufferedWriter.write(primer + line + "\n");
            // third line
            line = bufferedReader.readLine();
            bufferedWriter.write(line + "\n");
            // fourth line
            line = bufferedReader.readLine();
            bufferedWriter.write(StringUtils.leftPad(line, primer.length() + line.length(), 'f') + "\n");
        }
        bufferedReader.close();
        bufferedWriter.close();
    }
}
