package graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Graph testing main class
 * @author Ke
 *
 */
public class Gmain {

	public static void main(String[] args) throws IOException {
//		int[] IDs = {4, 6, 7, 10, 11, 12, 13, 14, 16, 18, 19, 20, 21, 22, 26, 27, 29, 31, 33, 35, 37, 38, 39, 40 };
//		String[] cellLines = { "LNCaP_withoutBarcode", "DU145_withoutBarcode", "PrEC_trim_adaptor" };
//		String inputFolder = "/home/ke/Dropbox/Lab/Pattern_Result/";
//		ArrayList<String> coordinates = new ArrayList<String>();
//		ArrayList<ReadData> readDataList;
//
//		readCoordinates(inputFolder, coordinates);
//
//		//Analysis.main.main(null);
//		
//		for (int i = 0; i < IDs.length; i++) {
//			readDataList = new ArrayList<ReadData>();
//			for (String cellLine : cellLines) {
//				if (cellLine.equals("PrEC_trim_adaptor")) {
//					ReadData readData = new ReadData(inputFolder, cellLine, String.valueOf(IDs[i]), coordinates.get(i), readDataList.get(0).getBeginPos(), readDataList.get(0).getRefLength());
//					readDataList.add(readData);
//				}else {
//					ReadData readData = new ReadData(inputFolder, cellLine, String.valueOf(IDs[i]), coordinates.get(i), 0, 0);
//					readDataList.add(readData);
//				}
//			}
//			DrawSingleGraph draw = new DrawSingleGraph(readDataList);
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			draw.getWindow().print();
//			draw.getWindow().saveAsPng(new File("/home/ke/Dropbox/Lab/Pattern_Result/pics/" + IDs[i] + "-R.png"));
//			draw.dispose();
//		}
	}

	// read coordinates
	static private void readCoordinates(String inputFolder, ArrayList<String> coordinates) throws IOException {
		FileReader coordinatesReader = new FileReader(inputFolder + "coordinates.txt");
		BufferedReader coordinatesBuffReader = new BufferedReader(coordinatesReader);
		String line = coordinatesBuffReader.readLine();
		String[] items;
		while (line != null) {
			items = line.split("-");
			coordinates.add(items[0]);
			line = coordinatesBuffReader.readLine();
		}
		coordinatesBuffReader.close();
	}
}