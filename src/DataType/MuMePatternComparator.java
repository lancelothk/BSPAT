package DataType;

/**
 * Created by lancelothk on 3/19/14.
 */
public class MuMePatternComparator implements java.util.Comparator<Pattern> {
    @Override
    public int compare(Pattern o1, Pattern o2) {
        if (o1.getMutationParentID() == o2.getMutationParentID()) {
			return o2.getCount() - o1.getCount();
		} else {
            return o1.getMutationParentID() - o2.getMutationParentID();
        }
    }
}
