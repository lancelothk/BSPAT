package DataType;

import java.util.Comparator;

public class PatternByCountComparator implements Comparator<Pattern> {

    @Override
    public int compare(Pattern m1, Pattern m2) {
		return m2.getCount() - m1.getCount();
	}

}
