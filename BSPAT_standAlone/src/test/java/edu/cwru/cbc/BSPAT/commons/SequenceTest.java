package edu.cwru.cbc.BSPAT.commons;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SequenceTest {
	@Test
	public void testReverse() throws Exception {
		int refLength = 100;
		// length is 10, contains half cpg
		String seqStringTop = "AACGCTTGAC";
		String seqStringBottom = "GTCAAGCGTT";

		Sequence seqTop = new Sequence("id", "TOP", "test", 20, seqStringTop);
		seqTop.addCpG(new CpGSite(22, false));
		seqTop.addCpG(new CpGSite(29, true));

		Sequence seqBottom = new Sequence("id", "BOTTOM", "test", 70, seqStringBottom);
		seqBottom.addCpG(new CpGSite(69, true));
		seqBottom.addCpG(new CpGSite(76, false));

		Sequence seqTopLeftEdge = new Sequence("id", "TOP", "test", 0, seqStringTop);
		seqTopLeftEdge.addCpG(new CpGSite(2, false));
		seqTopLeftEdge.addCpG(new CpGSite(9, true));

		Sequence seqBottomLeftEdge = new Sequence("id", "BOTTOM", "test", 90, seqStringBottom);
		seqBottomLeftEdge.addCpG(new CpGSite(89, true));
		seqBottomLeftEdge.addCpG(new CpGSite(96, false));

		Sequence seqTopRightEdge = new Sequence("id", "TOP", "test", 90, seqStringTop);
		seqTopRightEdge.addCpG(new CpGSite(92, false));
		seqTopRightEdge.addCpG(new CpGSite(99, true));

		Sequence seqBottomRightEdge = new Sequence("id", "BOTTOM", "test", 0, seqStringBottom);
		seqBottomRightEdge.addCpG(new CpGSite(-1, true));
		seqBottomRightEdge.addCpG(new CpGSite(6, false));

		// check if double reverses go back to original state
		testDoubleReverses(refLength, seqTop);
		testDoubleReverses(refLength, seqTopLeftEdge);
		testDoubleReverses(refLength, seqTopRightEdge);
		testDoubleReverses(refLength, seqBottom);
		testDoubleReverses(refLength, seqBottomLeftEdge);
		testDoubleReverses(refLength, seqBottomRightEdge);

		// test reverse by comparing same-content TOP/BOTTOM sequence
		testReverseTOP_Bottom(refLength, seqTop, seqBottom);
		testReverseTOP_Bottom(refLength, seqBottom, seqTop);
		testReverseTOP_Bottom(refLength, seqTopLeftEdge, seqBottomLeftEdge);
		testReverseTOP_Bottom(refLength, seqBottomLeftEdge, seqTopLeftEdge);
		testReverseTOP_Bottom(refLength, seqTopRightEdge, seqBottomRightEdge);
		testReverseTOP_Bottom(refLength, seqBottomRightEdge, seqTopRightEdge);
	}

	private void testReverseTOP_Bottom(int refLength, Sequence seqTop, Sequence seqBottom) {
		Sequence test = copySequence(seqTop);
		test.reverse(refLength);
		assertEqualSequence(test, seqBottom);
	}

	private void testDoubleReverses(int refLength, Sequence seqTop) {
		Sequence test = copySequence(seqTop);
		test.reverse(refLength);
		test.reverse(refLength);
		assertEqualSequence(test,seqTop);
	}

	private void assertEqualSequence(Sequence actual, Sequence expected) {
		assertEquals(actual.getStartPos(), expected.getStartPos());
		assertEquals(actual.getStartPos(), expected.getStartPos());
		assertEquals(actual.getCpGSites().size(), expected.getCpGSites().size(),
				"CpG sites number don't match between actual and expected sequences");
		for (int i = 0; i < actual.getCpGSites().size(); i++) {
			assertEquals(actual.getCpGSites().get(i).getPosition(), expected.getCpGSites().get(i).getPosition());
			assertEquals(actual.getCpGSites().get(i).isMethylated(), expected.getCpGSites().get(i).isMethylated());
		}
	}

	private Sequence copySequence(Sequence oldSeq){
		Sequence newSeq = new Sequence(oldSeq.getId(), oldSeq.getStrand(), oldSeq.getRegion(), oldSeq.getStartPos(), oldSeq.getOriginalSeq());
		for (CpGSite cpGSite : oldSeq.getCpGSites()) {
			newSeq.addCpG(new CpGSite(cpGSite.getPosition(), cpGSite.isMethylated()));
		}
		return newSeq;
	}

	@Test
	public void testProcessSequence() throws Exception {
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

		assertEquals(seq1.getSequenceIdentity(), 0.978, 0.001, "sequence identity not equal for seq 1!");
		assertEquals(seq2.getSequenceIdentity(), 0.989, 0.001, "sequence identity not equal for seq 2!");
		assertEquals(seq3.getSequenceIdentity(), 0.989, 0.001, "sequence identity not equal for seq 3!");

		assertEquals(seq1.getBisulConversionRate(), 0.947, 0.001, "bisulfite conversion rate not equal for seq 1!");
		assertEquals(seq2.getBisulConversionRate(), 0.947, 0.001, "bisulfite conversion rate not equal for seq 2!");
		assertEquals(seq3.getBisulConversionRate(), 0.965, 0.001, "bisulfite conversion rate not equal for seq 3!");

		assertEquals(seq1.getMethylationString(),
				"--------------------------**---------------------------------------------------@@-------**--------");
		assertEquals(seq2.getMethylationString(),
				"--------------------------**---------------------------------------------------@@-------@@--------");
		assertEquals(seq3.getMethylationString(),
				"--------------------------@@---------------------------------------------------**-------@@--------");

		// test partial CpG
		String beginningPartialCpGRef = "CGAAG";
		Sequence seqBPTop = new Sequence("1", "TOP", "test", 1, "GAAG");
		Sequence seqBPBottom = new Sequence("1", "BOTTOM", "test", 1, "GAAG");
		seqBPTop.addCpG(new CpGSite(0, true));
		seqBPBottom.addCpG(new CpGSite(0, false));
		seqBPTop.processSequence(beginningPartialCpGRef);
		seqBPBottom.processSequence(beginningPartialCpGRef);
		assertEquals(seqBPTop.getMethylationString(), "@---");
		assertEquals(seqBPBottom.getMethylationString(), "*---");

		String endPartialCpGRef = "GGAACG";
		Sequence seqEPTop = new Sequence("1", "TOP", "test", 0, "GGAAC");
		Sequence seqEPBottom = new Sequence("1", "BOTTOM", "test", 0, "AAAAC");
		seqEPTop.addCpG(new CpGSite(4, true));
		seqEPBottom.addCpG(new CpGSite(4, false));
		seqEPTop.processSequence(endPartialCpGRef);
		seqEPBottom.processSequence(endPartialCpGRef);
		assertEquals(seqEPTop.getMethylationString(), "----@");
		assertEquals(seqEPBottom.getMethylationString(), "----*");
	}
}