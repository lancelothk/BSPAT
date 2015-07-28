package edu.cwru.cbc.BSPAT.DataType;

import java.lang.reflect.InvocationTargetException;

import static org.testng.AssertJUnit.assertEquals;

public class SequenceTest {

	@org.junit.Test
	public void testProcessSequence() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		String ref = "AAACATCTCTAATGAGGGAGGAGGCCCGAGGATGGCTGGGTTTGATTTATGACTGGAGGAGAAGGTCCACTTCCCACTGCGAAGCAGGCGACCTGCTC";
		Sequence seq1 = new Sequence("1", "TOP", "test", 0,
				"AAATATTTTTAATGAGGGAGGAGGTTTGAGGATGGTTGGGTTTGATTTATGATTGGAGGAGAAGGCTTATTTTTTATTGAGAAGTAGGCGATTTGTTC");
		Sequence seq2 = new Sequence("2", "TOP", "test", 0,
				"AAATATTTTTAATGAGGGAGGAGGTTTGAGGATGGTTGGGTTTGATTTATGATTGGAGGAGAAGGTCTATTTTTTATTGTGAAGTAGGTAATTTGTTT");
		Sequence seq3 = new Sequence("3", "BOTTOM", "test", 0,
				"AAACATCTCTAATAAAAAAAAAAACCCGAAAATAACTAAATTTAATTTATAACTAAAAAAACAAATCCACTTCCCACTACGAAACAAACGACCTGCTC");

		seq1.addCpG(new CpGSite(26, false));
		seq1.addCpG(new CpGSite(79, true));
		seq1.addCpG(new CpGSite(88, false));
		seq1.processSequence(ref);

		seq2.addCpG(new CpGSite(26, false));
		seq2.addCpG(new CpGSite(79, true));
		seq2.addCpG(new CpGSite(88, true));
		seq2.processSequence(ref);

		seq3.addCpG(new CpGSite(26, true));
		seq3.addCpG(new CpGSite(79, false));
		seq3.addCpG(new CpGSite(88, true));
		seq3.processSequence(ref);


		assertEquals("sequence identity not equal for seq 1!", 0.978, seq1.getSequenceIdentity(), 0.001);
		assertEquals("sequence identity not equal for seq 2!", 0.989, seq2.getSequenceIdentity(), 0.001);
		assertEquals("sequence identity not equal for seq 3!", 0.989, seq3.getSequenceIdentity(), 0.001);

		assertEquals("bisulfite conversion rate not equal for seq 1!", 0.947, seq1.getBisulConversionRate(),
				0.001);
		assertEquals("bisulfite conversion rate not equal for seq 2!", 0.947, seq2.getBisulConversionRate(),
				0.001);
		assertEquals("bisulfite conversion rate not equal for seq 3!", 0.965, seq3.getBisulConversionRate(),
				0.001);

		assertEquals(
				"--------------------------**---------------------------------------------------@@-------**--------",
				seq1.getMethylationString());
		assertEquals(
				"--------------------------**---------------------------------------------------@@-------@@--------",
				seq2.getMethylationString());
		assertEquals(
				"--------------------------@@---------------------------------------------------**-------@@--------",
				seq3.getMethylationString());


		String beginningPartialCpGRef = "CGAAG";
		Sequence seqBPTop = new Sequence("1", "TOP", "test", 1, "GAAG");
		Sequence seqBPBottom = new Sequence("1", "BOTTOM", "test", 1, "GAAG");
		seqBPTop.addCpG(new CpGSite(0, true));
		seqBPBottom.addCpG(new CpGSite(0, false));
		seqBPTop.processSequence(beginningPartialCpGRef);
		seqBPBottom.processSequence(beginningPartialCpGRef);
		assertEquals("@---", seqBPTop.getMethylationString());
		assertEquals("*---", seqBPBottom.getMethylationString());

		String endPartialCpGRef = "GGAACG";
		Sequence seqEPTop = new Sequence("1", "TOP", "test", 0, "GGAAC");
		Sequence seqEPBottom = new Sequence("1", "BOTTOM", "test", 0, "AAAAC");
		seqEPTop.addCpG(new CpGSite(4, true));
		seqEPBottom.addCpG(new CpGSite(4, false));
		seqEPTop.processSequence(endPartialCpGRef);
		seqEPBottom.processSequence(endPartialCpGRef);
		assertEquals("----@", seqEPTop.getMethylationString());
		assertEquals("----*", seqEPBottom.getMethylationString());
	}
}