package DataType;

import java.util.Comparator;


public class CpGStatComparator implements Comparator<CpGStatistics> {

    @Override
    public int compare(CpGStatistics c1, CpGStatistics c2) {
        if (c1.getPosition() > c2.getPosition()) {
            return 1;
        } else if (c1.getPosition() < c2.getPosition()) {
            return -1;
        } else {
            return 0;
        }
    }

}
