package BSPAT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.Part;

import DataType.Coordinate;
import DataType.ExtensionFilter;

public class IO {
	/**
	 * read coordinates file
	 * @param coorPath
	 * @param coorFileName
	 * @return coordinateHash
	 * @throws IOException
	 */
	public static HashMap<String, Coordinate> readCoordinates(String coorPath, String coorFileName) throws IOException {
		/**
		 * coordinates file format:
		 * <1:region ID - String> <2:chrom - String> <3:strand - String> <4:start position - long> <5:end position - long>
		 * E.g: 19	2	+	38155208	38155307
		 */
		HashMap<String, Coordinate> coordinateHash = new HashMap<String, Coordinate>();
		File coorFolder = new File(coorPath);
		FileReader coordinatesReader = new FileReader(coorFolder.getAbsolutePath() + "/" + coorFileName);
		BufferedReader coordinatesBuffReader = new BufferedReader(coordinatesReader);
		String line = coordinatesBuffReader.readLine();
		String[] items;
		while (line != null) {
			items = line.split("\t");
			coordinateHash.put(items[0], new Coordinate(items[0], items[1], items[2], Long.valueOf(items[3]), Long.valueOf(items[4])));
			line = coordinatesBuffReader.readLine();
		}
		coordinatesBuffReader.close();
		return coordinateHash;
	}
	
	public static void createFolder(String path) {
		File folder = new File(path);
		if (!folder.isDirectory()) {
			folder.mkdir();
		}
	}

	public static void deleteFiles(String path, String[] extensions) {
		File folder = new File(path);
		File[] files = folder.listFiles(new ExtensionFilter(extensions));
		for (File file : files) {
			file.delete();
		}
	}
	
	public static boolean saveFileToDisk(Part part, String path, String fileName) throws IOException {
		if (fileName != null && !fileName.isEmpty()) {
			part.write(path + "/" + fileName);
			return true;
		} else {
			return false;
		}
	}
	
	// only add allowed files.
	public static ArrayList<File> visitFiles(File f) {
		ArrayList<File> list = new ArrayList<File>();
		File[] files = f.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				list.addAll(visitFiles(file));
			} else {
				if (file.getName().endsWith(".txt") || file.getName().endsWith(".fq") || file.getName().endsWith(".fastq")
						|| file.getName().endsWith(".fa") || file.getName().endsWith(".fasta")) {
					list.add(file);
				}
			}
		}
		return list;
	}
}
