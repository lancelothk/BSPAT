package BSPAT;

import DataType.Constant;
import DataType.Coordinate;
import DataType.ExtensionFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tools.zip.ZipFile;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utilities {

    public static void handleServletException(Exception e, Constant constant) {
        e.printStackTrace();
        if (constant != null && constant.email != null && constant.jobID != null) {
            try {
                Utilities.sendEmail(constant.email, constant.jobID, String.format("%s failed:\n%s\n", constant.jobID,
                                                                                  ExceptionUtils.getStackTrace(e)));
            } catch (MessagingException innerException) {
                innerException.printStackTrace();
                throw new RuntimeException("failed to send email!");
            }
        }
        throw new RuntimeException(ExceptionUtils.getStackTrace(e));
    }

    public static void convertPSLtoCoorPair(String path, String outFileName, String refVersion) throws IOException {
        File folder = new File(path);
        String[] files = folder.list(new ExtensionFilter(".psl"));
        HashMap<String, Coordinate> coorHashMap = new HashMap<>();

        for (String name : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path + name))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(" ") || line.startsWith("QUERY") || line.startsWith("-") ||
                            line.startsWith("BLAT")) {
                        continue;
                    }
                    String[] items = line.split("\\s+");
                    // items[0] -- query id, items[1] -- score, items[4] -- qsize,
                    // items[6] -- chrom, items[7] -- strand, items[8] -- start,
                    // items[9] -- end
                    // filter blat result by checking if score equals to qsize
                    if (!coorHashMap.containsKey(items[0]) && items[1].equals(items[4])) {
                        // first query match, score equals query size
                        coorHashMap.put(items[0], new Coordinate(items[0], items[6], items[7], Long.valueOf(items[8]),
                                                                 Long.valueOf(items[9])));
                    }
                }
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path + outFileName))) {
            for (String key : coorHashMap.keySet()) {
                Coordinate coor = coorHashMap.get(key);
                writer.write(
                        String.format("%s\t%s\t%s\t%s\t%s\n", key, coor.getChr(), coor.getStrand(), coor.getStart(),
                                      coor.getEnd()));
            }
        }
        if (coorHashMap.size() == 0) {
            throw new RuntimeException("No correct coordinate found!");
        }
    }

    // delete folder content recursively
    public static void deleteFolderContent(String folder) throws IOException {
        File folderFile = new File(folder);
        File[] contents = folderFile.listFiles();
        for (File file : contents) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                // delete directory recursively
                FileUtils.deleteDirectory(file);
            }
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

    public static void showAlertWindow(HttpServletResponse response, String errorMessage) throws IOException {
        PrintWriter out;
        response.setContentType("text/html");
        out = response.getWriter();
        out.println("<script type=\"text/javascript\">");
        out.println("alert('" + errorMessage + "');window.history.go(-1);");
        out.println("</script>");
    }

    /**
     * cmd program caller wrapper.
     *
     * @return exit value
     * @param cmd       command string.
     * @param directory execution directory
     * @param fileName  file name to write output. If leave null, write to stdout.
     * @throws IOException
     * @throws InterruptedException
     */
    public static int callCMD(String cmd, File directory, String fileName) throws IOException, InterruptedException {
        final Process process = Runtime.getRuntime().exec(cmd, null, directory);
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String progOutput;
            if (fileName != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                    // read the error from the command
                    while ((progOutput = stdError.readLine()) != null) {
                        writer.write(progOutput + "\n");
                    }

                    // read the output from the command
                    while ((progOutput = stdInput.readLine()) != null) {
                        writer.write(progOutput + "\n");
                    }
                }
            } else {
                // read the error from the command
                while ((progOutput = stdError.readLine()) != null) {
                    System.out.println(progOutput);
                }

                // read the output from the command
                while ((progOutput = stdInput.readLine()) != null) {
                    System.out.println(progOutput);
                }
            }
        }
        process.waitFor();
        return process.exitValue();
    }

    public static void zipFolder(String folder, String zipFileName) throws IOException {
        try (OutputStream os = new FileOutputStream(zipFileName);
             BufferedOutputStream bs = new BufferedOutputStream(os);
             ZipOutputStream zo = new ZipOutputStream(bs)
        ) {
            zip(folder, new File(folder), zo, true, true);
            zo.closeEntry();
        }
    }

    private static void zip(String path, File basePath, ZipOutputStream zo, boolean isRecursive,
                            boolean isOutBlankDir) throws IOException {

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
        if (files == null) {
            throw new RuntimeException("no file to be compressed in given path!");
        }
        for (File file : files) {
            String pathName;
            if (basePath != null) {
                if (basePath.isDirectory()) {
                    pathName = file.getPath().substring(basePath.getPath().length() + 1);
                } else {// file
                    pathName = file.getPath().substring(basePath.getParent().length() + 1);
                }
            } else {
                pathName = file.getName();
            }
            System.out.println(pathName);
            if (file.isDirectory()) {
                if (isOutBlankDir && basePath != null) {
                    zo.putNextEntry(new ZipEntry(pathName + "/"));
                }
                if (isRecursive) {
                    zip(file.getPath(), basePath, zo, true, isOutBlankDir);
                }
            } else {
                try (FileInputStream fin = new FileInputStream(file)) {
                    zo.putNextEntry(new ZipEntry(pathName));
                    while ((len = fin.read(buf)) > 0) {
                        zo.write(buf, 0, len);
                    }
                }
            }
        }
    }

    public static void unZip(File zipfile, String destDir) throws IOException {

        destDir = destDir.endsWith("//") ? destDir : destDir + "//";
        byte b[] = new byte[1024];
        int length;

        ZipFile zipFile;
        zipFile = new ZipFile(zipfile);
        @SuppressWarnings("rawtypes") Enumeration enumeration = zipFile.getEntries();
        org.apache.tools.zip.ZipEntry zipEntry = null;

        while (enumeration.hasMoreElements()) {
            zipEntry = (org.apache.tools.zip.ZipEntry) enumeration.nextElement();
            File loadFile = new File(destDir + zipEntry.getName());

            if (!zipEntry.isDirectory()) {
                if (!loadFile.getParentFile().exists()) {
                    loadFile.getParentFile().mkdirs();
                }

                try (OutputStream outputStream = new FileOutputStream(loadFile);
                     InputStream inputStream = zipFile.getInputStream(zipEntry)
                ) {
                    while ((length = inputStream.read(b)) > 0) {
                        outputStream.write(b, 0, length);
                    }
                }
            }
        }
        System.out.println(" File unzipped succesfully ");
    }

    public static void sendEmail(String toAddress, String jobID, String text) throws MessagingException {
        if (toAddress == null || toAddress.equals("") || jobID == null || jobID.equals("")) {
            return;
        }
        String username, password;
        try {
            File rootFolder = new File(Constant.DISKROOTPATH);
            CsvListReader csvListReader = new CsvListReader(
                    new BufferedReader(new FileReader(rootFolder.getParent() + "/emailConf.csv")),
                    CsvPreference.STANDARD_PREFERENCE);
            List<String> results = csvListReader.read();
            if (results.size() == 2) {
                username = results.get(0);
                password = results.get(1);
            } else {
                throw new RuntimeException("email conf file format error!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can not read email conf file!");
        }

        String smtpServer = "IMAP.gmail.com";
        String fromMailAddress = "bspatnotice@gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");
        Session session = Session.getDefaultInstance(props, new SmtpAuthenticator(username, password));
        /** *************************************************** */
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(fromMailAddress));
        mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
        mimeMessage.setSubject("BSPAT");
        mimeMessage.setText(text);
        Transport.send(mimeMessage);
        System.out.println("Sent message successfully....");
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
