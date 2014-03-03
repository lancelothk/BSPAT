package DataType;

import java.util.Comparator;

public class MappingResultComparator implements Comparator<String[]> {

	@Override
	public int compare(String[] l1, String[] l2) {
		// character order compare comlumn 2 and 14
		String newLine1 = l1[2] + l1[14];
		String newLine2 = l2[2] + l2[14];
		return newLine1.compareTo(newLine2);
	}

}
