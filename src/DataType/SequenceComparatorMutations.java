package DataType;

import java.util.Comparator;

public class SequenceComparatorMutations implements Comparator<Sequence> {

	@Override
	public int compare(Sequence s1, Sequence s2) {
		// TODO Auto-generated method stub
		return s1.getMutationString().compareTo(s2.getMutationString());
	}

}
