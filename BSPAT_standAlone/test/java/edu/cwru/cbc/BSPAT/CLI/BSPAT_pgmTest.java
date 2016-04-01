package edu.cwru.cbc.BSPAT.CLI;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.testng.FileAssert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * Created by kehu on 4/1/16.
 */
public class BSPAT_pgmTest {
	private String[] resultFiles = {"LOC440034-10-96-test_bismark.analysis.txt", "LOC440034-10-96-test_bismark.analysis_ASM.txt", "LOC440034-10-96-test_bismark.analysis_Methylation.txt", "LOC440034-10-96-test_bismark.analysis_MethylationWithSNP.txt", "LOC440034-10-96-test_bismark.analysis_report.txt"};
	private String testResourcePath = "BSPAT_standAlone/test/resources/integration/";

	@Test
	public void testIntegration() throws Exception {
		// with default parameter
		String inputFile = testResourcePath + "demoSequence.fastq_bismark.bam";
		String referenceFile = testResourcePath + "demoReference.fasta";
		String targetRegionFile = testResourcePath + "target.bed";
		String[] args = {referenceFile, inputFile, targetRegionFile};
		System.out.println(new File("").getAbsolutePath());
		BSPAT_pgm.main(args);
		for (String resultFile : resultFiles) {
			FileAssert.assertFile(new File(testResourcePath + resultFile),
					"Output" + resultFile + " doesn't exist!");
			assertEqualFiles(testResourcePath + resultFile, testResourcePath + "expected/" + resultFile,
					resultFile);
		}

	}

	private void assertEqualFiles(String actualFile, String expectedFile, String fileName) throws IOException {
		String actualContent = Files.toString(new File(actualFile), Charsets.UTF_8);
		String expectedContent = Files.toString(new File(expectedFile), Charsets.UTF_8);
		assertEquals(actualContent, expectedContent, fileName + " content doesn't equal to expected content!");
	}

	@AfterMethod
	public void tearDown() throws Exception {
		for (String resultFile : resultFiles) {
			if (!new File(testResourcePath + resultFile).delete()) {
				throw new IOException(testResourcePath + resultFile + " doesn't delete successfully!");
			}
		}

	}
}