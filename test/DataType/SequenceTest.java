package DataType;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SequenceTest {

    @Test
    public void testProcessSequence() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String ref = "AAACATCTCTAATGAGGGAGGAGGCCCGAGGATGGCTGGGTTTGATTTATGACTGGAGGAGAAGGTCCACTTCCCACTGCGAAGCAGGCGACCTGCTC";
        List<Sequence> seqList = new ArrayList<>();
        seqList.add(new Sequence("1", "", "test", 0,
                                 "AAATATTTTTAATGAGGGAGGAGGTTTGAGGATGGTTGGGTTTGATTTATGATTGGAGGAGAAGGCTTATTTTTTATTGCGAAGTAGGCGATTTGTTC",
                                 "", "", "", "", ""));
        seqList.add(new Sequence("2", "", "test", 0,
                                 "AAATATTTTTAATGAGGGAGGAGGTTTGAGGATGGTTGGGTTTGATTTATGATTGGAGGAGAAGGTCTATTTTTTATTGTGAAGTAGGTAATTTGTTT",
                                 "", "", "", "", ""));
        seqList.add(new Sequence("3", "", "test", 0,
                                 "AAATATTTTTAATGAGGGAGGAGGTTTGAGGATGGTTGGGTTTGATTTATGATTGGAGGAGAAGGTTTATTTTTTATTGCGAAGTAGGGTATTTGTTC",
                                 "", "", "", "", ""));

        for (Sequence sequence : seqList) {
            sequence.addCpG(new CpGSite(27, true));
            sequence.addCpG(new CpGSite(80, true));
            sequence.addCpG(new CpGSite(89, true));
        }
        Sequence.processSequence(ref, seqList);

        assertEquals("sequence identity not equal for seq 1!", 0.989, seqList.get(0).getSequenceIdentity(), 0.001);
        assertEquals("sequence identity not equal for seq 2!", 0.989, seqList.get(1).getSequenceIdentity(), 0.001);
        assertEquals("sequence identity not equal for seq 3!", 0.979, seqList.get(2).getSequenceIdentity(), 0.001);

        assertEquals("bisulfite conversion rate not equal for seq 1!", 0.947, seqList.get(0).getBisulConversionRate(),
                     0.001);
        assertEquals("bisulfite conversion rate not equal for seq 2!", 0.947, seqList.get(1).getBisulConversionRate(),
                     0.001);
        assertEquals("bisulfite conversion rate not equal for seq 3!", 0.947, seqList.get(2).getBisulConversionRate(),
                     0.001);
    }
}