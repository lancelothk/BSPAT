package DataType;

import java.util.Comparator;

public class SequenceComparatorRegion implements Comparator<Sequence> {

	@Override
	public int compare(Sequence s1, Sequence s2) {
		// TODO Auto-generated method stub
		return s1.getRegion().compareTo(s2.getRegion());
//		int sn1, sn2;
//		char sc1=' ', sc2= ' ';
//		if (s1.getRegion().contains("F")) {
//			sn1 = Integer.valueOf(s1.getRegion().replace("F", ""));
//			sc1 = 'F';
//		} else if (s1.getRegion().contains("R")) {
//			sn1 = Integer.valueOf(s1.getRegion().replace("R", ""));
//			sc1 = 'R';
//		} else {
//			sn1 = Integer.valueOf(s1.getRegion());
//		}
//		if (s2.getRegion().contains("F")) {
//			sn2 = Integer.valueOf(s2.getRegion().replace("F", ""));
//			sc2 = 'F';
//		} else if (s2.getRegion().contains("R")) {
//			sn2 = Integer.valueOf(s2.getRegion().replace("R", ""));
//			sc2 = 'R';
//		} else {
//			sn2 = Integer.valueOf(s2.getRegion());
//		}
//		if (sn1 > sn2){
//			return 1;
//		}else if (sn1 < sn2){
//			return -1;
//		}else {
//			if (sc1 == 'F'){
//				return 1;
//			}
//			if (sc2 == 'F'){
//				return -1;
//			}
//			return 0;
//		}
	}

}
