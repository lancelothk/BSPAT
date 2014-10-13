package edu.cwru.cbc.BSPAT.DataType;

/**
 * Created by lancelothk on 3/19/14.
 */
public class MeMuPatternComparator implements java.util.Comparator<Pattern> {
    @Override
    public int compare(Pattern o1, Pattern o2) {
        if (o1.getMethylationParentID() == o2.getMethylationParentID()) {
			return o2.getCount() - o1.getCount();
		} else {
            return o1.getMethylationParentID() - o2.getMethylationParentID();
        }
    }
}
