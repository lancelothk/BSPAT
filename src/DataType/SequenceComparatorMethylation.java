package DataType;

import java.util.Comparator;

public class SequenceComparatorMethylation implements Comparator<Sequence> {

	@Override
	public int compare(Sequence s1, Sequence s2) {
		// TODO Auto-generated method stub
		return s1.getMethylationString().compareTo(s2.getMethylationString()) ;
	}

}
