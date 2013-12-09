package BSPAT;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.tools.zip.ZipFile;

import DataType.Coordinate;
import DataType.ExtensionFilter;

public class Utilities {

	public static void createFolder(String path) {
		File folder = new File(path);
		if (!folder.isDirectory()) {
			folder.mkdir();
		}
	}

	public static void convertPSLtoCoorPair(String path) throws IOException {
		File folder = new File(path);
		String[] files = folder.list(new ExtensionFilter(".psl"));
		BufferedWriter writer = new BufferedWriter(new FileWriter(path + "coordinates"));
		HashMap<String, Coordinate> coorHashMap = new HashMap<>();
		for (String name : files) {
			BufferedReader reader = new BufferedReader(new FileReader(path + name));
			for (int i = 0; i < 3; i++) {
				reader.readLine();
			}
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] items = line.split("\\s+");
				// items[0] -- query id, items[1] -- score, items[4] -- qsize,
				// items[6] -- chrom, items[7] -- strand, items[8] -- start,
				// items[9] -- end
				if (!coorHashMap.containsKey(items[0]) && items[1].equals(items[4])) {
					// first query match, score equals query size
					coorHashMap.put(items[0], new Coordinate(Long.valueOf(items[8]), Long.valueOf(items[9]), items[6], items[7]));
				}
			}
			reader.close();
		}
		for (String key : coorHashMap.keySet()) {
			Coordinate coor = coorHashMap.get(key);
			writer.write(String.format("%s\t%s\t%s\t%s\t%s\n", key, coor.getChr(), coor.getStrand(), coor.getStart(), coor.getEnd()));
		}
		writer.close();
	}

	public static boolean saveFileToDisk(Part part, String path, String fileName) throws IOException {
		if (fileName != null && !fileName.isEmpty()) {
			part.write(path + "/" + fileName);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * get field content start with given parameter 'field'
	 * 
	 * @param part
	 * @param field
	 * @return
	 */
	public static String getField(Part part, String field) {
		String contentDispositionHeader = part.getHeader("content-disposition");
		String[] elements = contentDispositionHeader.split(";");
		for (String element : elements) {
			if (element.trim().startsWith(field)) {
				return element.substring(element.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}

	public static void showAlertWindow(HttpServletResponse response, String errorMessage) {
		PrintWriter out;
		try {
			response.setContentType("text/html");
			out = response.getWriter();
			out.println("<script type=\"text/javascript\">");
			out.println("alert('" + errorMessage + "');window.history.go(-1);");
			out.println("</script>");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void callCMD(String cmd, File directory) throws IOException, InterruptedException {
		String progOutput = null;
		final Process process = Runtime.getRuntime().exec(cmd, null, directory);

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

		BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		// read the error from the command
		while ((progOutput = stdError.readLine()) != null) {
			System.out.println(progOutput);
		}

		// read the output from the command
		while ((progOutput = stdInput.readLine()) != null) {
			System.out.println(progOutput);
		}

		process.waitFor();
	}

	public static void zipFolder(String folder, String zipFileName) throws IOException {
		OutputStream os = new FileOutputStream(zipFileName);
		BufferedOutputStream bs = new BufferedOutputStream(os);
		ZipOutputStream zo = new ZipOutputStream(bs);

		zip(folder, new File(folder), zo, true, true);
		zo.closeEntry();
		zo.close();
	}

	private static void zip(String path, File basePath, ZipOutputStream zo, boolean isRecursive, boolean isOutBlankDir) throws IOException {

		File inFile = new File(path);

		File[] files = new File[0];
		if (inFile.isDirectory()) {
			files = inFile.listFiles();
		} else if (inFile.isFile()) {
			files = new File[1];
			files[0] = inFile;
		}
		byte[] buf = new byte[1024];
		int len;
		// System.out.println("baseFile: "+baseFile.getPath());
		for (int i = 0; i < files.length; i++) {
			String pathName = "";
			if (basePath != null) {
				if (basePath.isDirectory()) {
					pathName = files[i].getPath().substring(basePath.getPath().length() + 1);
				} else {// 文件
					pathName = files[i].getPath().substring(basePath.getParent().length() + 1);
				}
			} else {
				pathName = files[i].getName();
			}
			System.out.println(pathName);
			if (files[i].isDirectory()) {
				if (isOutBlankDir && basePath != null) {
					zo.putNextEntry(new ZipEntry(pathName + "/"));
				}
				if (isRecursive) {
					zip(files[i].getPath(), basePath, zo, isRecursive, isOutBlankDir);
				}
			} else {
				FileInputStream fin = new FileInputStream(files[i]);
				zo.putNextEntry(new ZipEntry(pathName));
				while ((len = fin.read(buf)) > 0) {
					zo.write(buf, 0, len);
				}
				fin.close();
			}
		}
	}

	public static void unZip(File zipfile, String destDir) {

		destDir = destDir.endsWith("//") ? destDir : destDir + "//";
		byte b[] = new byte[1024];
		int length;

		ZipFile zipFile;
		try {
			zipFile = new ZipFile(zipfile);
			Enumeration enumeration = zipFile.getEntries();
			org.apache.tools.zip.ZipEntry zipEntry = null;

			while (enumeration.hasMoreElements()) {
				zipEntry = (org.apache.tools.zip.ZipEntry) enumeration.nextElement();
				File loadFile = new File(destDir + zipEntry.getName());

				if (zipEntry.isDirectory()) {
					// loadFile.mkdirs();
				} else {
					if (!loadFile.getParentFile().exists()) {
						loadFile.getParentFile().mkdirs();
					}

					OutputStream outputStream = new FileOutputStream(loadFile);
					InputStream inputStream = zipFile.getInputStream(zipEntry);

					while ((length = inputStream.read(b)) > 0) {
						outputStream.write(b, 0, length);
					}

					inputStream.close();
					outputStream.close();
				}
			}
			System.out.println(" File unzipped succesfully ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	// read coordinates
	public static HashMap<String, Coordinate> readCoordinates(String coorPath, String coorFileName) throws IOException {
		HashMap<String, Coordinate> coordinateHash = new HashMap<String, Coordinate>();
		File coorFolder = new File(coorPath);
		FileReader coordinatesReader = new FileReader(coorFolder.getAbsolutePath() + "/" + coorFileName);
		BufferedReader coordinatesBuffReader = new BufferedReader(coordinatesReader);
		String line = coordinatesBuffReader.readLine();
		String[] items;
		while (line != null) {

			items = line.split("\t");
			String[] lines = items[1].split("-");
			coordinateHash.put(items[0], new Coordinate(Long.valueOf(lines[0].split(":")[1]), Long.valueOf(lines[1]), lines[0].split(":")[0]));
			line = coordinatesBuffReader.readLine();
		}
		coordinatesBuffReader.close();
		return coordinateHash;
	}

	public static void sendEmail(String toAddress, String runID, String text) {
		if (toAddress == null || toAddress.equals("") || runID.equals("") || runID == null) {
			return;
		}
		String smtpServer = "IMAP.gmail.com";
		String username = "lancelothk";
		String password = "~~Q1w2e3r4";
		String toMailAddress = toAddress;
		String fromMailAddress = "lancelothk@gmail.com";

		try {
			Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.host", smtpServer);
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.port", "587");
			Session session = Session.getDefaultInstance(props, new SmtpAuthenticator(username, password));
			/** *************************************************** */
			MimeMessage mimeMessage = new MimeMessage(session);
			mimeMessage.setFrom(new InternetAddress(fromMailAddress));
			mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toMailAddress));
			mimeMessage.setSubject("BS-PAT");
			mimeMessage.setText(text);
			Transport.send(mimeMessage);
			System.out.println("Sent message successfully....");
		} catch (MessagingException mex) {
			mex.printStackTrace();
		}
	}

}

class SmtpAuthenticator extends Authenticator {
	String username = null;
	String password = null;

	public SmtpAuthenticator(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(this.username, this.password);
	}

}
