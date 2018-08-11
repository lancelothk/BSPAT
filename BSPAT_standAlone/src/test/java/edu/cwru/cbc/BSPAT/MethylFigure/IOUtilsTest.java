package edu.cwru.cbc.BSPAT.MethylFigure;

import edu.cwru.cbc.BSPAT.commons.PatternResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.*;

public class IOUtilsTest {
	private String testResourcePath;

	@BeforeMethod
	public void setUp() {
		String root = this.getClass().getClassLoader().getResource("").getFile();
		assertNotNull(root);
		testResourcePath = root + "integration/";
	}

	@Test
	public void testReadASMPatterns() throws IOException {
		String ASMPatternFileName = testResourcePath + "/demoExpected/demoRef-10-96-test-plus_bismark.analysis_ASM.txt";
		List<PatternResult> patternResultList = IOUtils.readASMPatterns(ASMPatternFileName);
		assertNotNull(patternResultList.get(0).getSnp(), "First ASM pattern should be the one with allele");
		assertNull(patternResultList.get(1).getSnp(), "Second ASM pattern should be the one without allele");
	}
}