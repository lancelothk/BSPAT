import DataType.ExtensionFilter;

import java.io.*;

/**
 * Created by kehu on 9/4/14.
 * For amplify BSPAT dataset
 */
public class AmplifyDataset {
    public static void main(String[] args) throws IOException {
        String celllineName = "PrEC";
        amplify("/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer/", "/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer_2X/", 2);
        amplify("/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer/", "/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer_5X/", 5);
        amplify("/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer/", "/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer_10X/", 10);
        amplify("/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer/", "/home/kehu/experiments/BSPAT/Amplification/" + celllineName + "_remainPrimer_70X/", 70);
    }

    public static void amplify(String inputPath, String outputPath, int amplifier) throws IOException {
        File inputDir = new File(inputPath);
        File[] inputFiles = inputDir.listFiles(new ExtensionFilter(".txt"));
        File outputDir = new File(outputPath);
        if (!outputDir.exists()){
            outputDir.mkdirs();
        }
        for (File inputFile : inputFiles) {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath + "/" + insertBeforeExtensiton(inputFile.getName(), amplifier + "X")));
            String header;
            while ((header = bufferedReader.readLine()) != null){
                String seq = bufferedReader.readLine();
                String qualHeader = bufferedReader.readLine();
                String qualSeq = bufferedReader.readLine();
                for (int i = 1; i <= amplifier; i++) {
                    bufferedWriter.write(header + "_" + i + "X\n");
                    bufferedWriter.write(seq + "\n");
                    bufferedWriter.write(qualHeader + "_" + i + "X\n");
                    bufferedWriter.write(qualSeq + "\n");
                }
            }
            bufferedReader.close();
            bufferedWriter.close();
        }
    }

    public static String insertBeforeExtensiton(String name, String insertion){
        String[] items = name.split("\\.");
        if (items.length == 2){
            return String.format("%s_%s.%s", items[0], insertion, items[1]);
        }else {
            throw new RuntimeException("filename is not standard!");
        }
    }
}
