package DataType;

import java.util.Comparator;

public class CpGComparator implements Comparator<CpGSitePattern> {

    @Override
    public int compare(CpGSitePattern c1, CpGSitePattern c2) {
        if (c1.getPosition() > c2.getPosition()) {
            return 1;
        } else if (c1.getPosition() < c2.getPosition()) {
            return -1;
        } else {
            return 0;
        }
    }

}
