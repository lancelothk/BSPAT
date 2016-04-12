package edu.cwru.cbc.BSPAT.CLI;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.cli.ParseException;
import org.testng.FileAssert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class BSPAT_pgmTest {
	private String testResourcePath;

	@BeforeMethod
	public void setUp() throws Exception {
		String root = this.getClass().getClassLoader().getResource("").getFile();
		assertNotNull(root);
		testResourcePath = root + "integration/";
	}

	@Test
	public void integrationTest_demoSam() throws InterruptedException, ParseException, IOException {
		demoDataTest("demoSequence.fastq_bismark.sam");
	}

	private void demoDataTest(String bismarkInputFileName) throws IOException, InterruptedException, ParseException {
		String[] resultFilenames = {"LOC440034-10-96-test-plus_bismark.analysis.txt",
				"LOC440034-10-96-test-plus_bismark.analysis_ASM.txt",
				"LOC440034-10-96-test-plus_bismark.analysis_Methylation.txt",
				"LOC440034-10-96-test-plus_bismark.analysis_MethylationWithSNP.txt",
				"LOC440034-10-96-test-plus_bismark.analysis_report.txt"};
		// with default parameter
		String inputFile = testResourcePath + "/demoInput/" + bismarkInputFileName;
		String referenceFile = testResourcePath + "/demoInput/demoReference.fasta";
		String targetRegionFile = testResourcePath + "/demoInput/target.bed";
		String[] args = {referenceFile, inputFile, targetRegionFile, "-o", testResourcePath + "/demoActual"};
		BSPAT_pgm.main(args);
		for (String resultFilename : resultFilenames) {
			String actualResultFilename = testResourcePath + "/demoActual/" + resultFilename;
			String expectedResultFilename = testResourcePath + "/demoExpected/" + resultFilename;
			File actualResultFile = new File(actualResultFilename);
			FileAssert.assertFile(actualResultFile, "Output " + resultFilename + " doesn't exist!");
			assertEqualFiles(actualResultFile, new File(expectedResultFilename),
					resultFilename);
			if (!actualResultFile.delete()) {
				throw new IOException(actualResultFilename + " doesn't delete successfully!");
			}
		}
	}

	@Test
	public void integrationTest_halfCpG() throws InterruptedException, ParseException, IOException {
		String[] resultFilenames = {"10_minus_extend-24-80-10F-minus-minus_bismark.analysis.txt",
				"10_minus_extend-24-80-10F-minus-minus_bismark.analysis_Methylation.txt",
				"10_minus_extend-24-80-10F-minus-minus_bismark.analysis_report.txt"};
		// with default parameter
		String inputFile = testResourcePath + "/halfCpGInput/ProstateBSF10F.txt_bismark.bam";
		String referenceFile = testResourcePath + "/halfCpGInput/10_minus_extend.fa";
		String targetRegionFile = testResourcePath + "/halfCpGInput/10F_minus_target_reverse.bed";
		String[] args = {referenceFile, inputFile, targetRegionFile, "-o", testResourcePath + "/halfCpGActual"};
		BSPAT_pgm.main(args);
		for (String resultFilename : resultFilenames) {
			String actualResultFilename = testResourcePath + "/halfCpGActual/" + resultFilename;
			String expectedResultFilename = testResourcePath + "/halfCpGExpected/" + resultFilename;
			File actualResultFile = new File(actualResultFilename);
			FileAssert.assertFile(actualResultFile, "Output " + resultFilename + " doesn't exist!");
			assertEqualFiles(actualResultFile, new File(expectedResultFilename),
					resultFilename);
			if (!actualResultFile.delete()) {
				throw new IOException(actualResultFilename + " doesn't delete successfully!");
			}
		}
	}

	@Test
	public void integrationTest_minusRef() throws IOException, ParseException, InterruptedException {
		String[] resultFilenames = {"10_minus_extend-5-103-10F-minus-minus_bismark.analysis.txt",
				"10_minus_extend-5-103-10F-minus-minus_bismark.analysis_Methylation.txt",
				"10_minus_extend-5-103-10F-minus-minus_bismark.analysis_report.txt"};
		// with default parameter
		String inputFile = testResourcePath + "/minusRefInput/ProstateBSF10F.txt_bismark.bam";
		String referenceFile = testResourcePath + "/minusRefInput/10_minus_extend.fa";
		String targetRegionFile = testResourcePath + "/minusRefInput/10F_minus_target_reverse.bed";
		String[] args = {referenceFile, inputFile, targetRegionFile, "-o", testResourcePath + "/minusRefActual"};
		BSPAT_pgm.main(args);
		for (String resultFilename : resultFilenames) {
			String actualResultFilename = testResourcePath + "/minusRefActual/" + resultFilename;
			String expectedResultFilename = testResourcePath + "/minusRefExpected/" + resultFilename;
			File actualResultFile = new File(actualResultFilename);
			FileAssert.assertFile(actualResultFile, "Output " + resultFilename + " doesn't exist!");
			assertEqualFiles(actualResultFile, new File(expectedResultFilename),
					resultFilename);
			if (!actualResultFile.delete()) {
				throw new IOException(actualResultFilename + " doesn't delete successfully!");
			}
		}
	}

	@Test
	public void integrationTest_demoBam() throws InterruptedException, ParseException, IOException {
		demoDataTest("demoSequence.fastq_bismark.bam");
	}

	private void assertEqualFiles(File actualFile, File expectedFile, String fileName) throws IOException {
		String actualContent = Files.toString(actualFile, Charsets.UTF_8);
		String expectedContent = Files.toString(expectedFile, Charsets.UTF_8);
		assertEquals(actualContent, expectedContent, fileName + " content doesn't equal to expected content!");
	}

}