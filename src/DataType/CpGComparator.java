package DataType;

import java.util.Comparator;

public class CpGComparator implements Comparator<CpGSite> {

	@Override
	public int compare(CpGSite c1, CpGSite c2) {
		// TODO Auto-generated method stub
		if (c1.getPosition() > c2.getPosition()) {
			return 1;
		} else if (c1.getPosition() < c2.getPosition()) {
			return -1;
		} else {
			return 0;
		}
	}

}
