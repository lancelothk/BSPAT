package DataType;

import java.util.Comparator;

public class PatternByCountComparator implements Comparator<Pattern> {

    @Override
    public int compare(Pattern m1, Pattern m2) {
        if (m1.getCount() > m2.getCount()) {
            return -1;
        } else if (m1.getCount() < m2.getCount()) {
            return 1;
        } else {
            return 0;
        }
    }

}
