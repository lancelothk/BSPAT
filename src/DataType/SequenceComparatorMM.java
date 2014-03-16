package DataType;

import java.util.Comparator;

public class SequenceComparatorMM implements Comparator<Sequence> {

    @Override
    public int compare(Sequence s1, Sequence s2) {
        return s1.getMethylationStringWithMutations().compareTo(s2.getMethylationStringWithMutations());
    }

}