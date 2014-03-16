package DataType;

import java.util.ArrayList;
import java.util.List;

public class Pattern {
    private String patternString;
    private List<Sequence> sequenceList;
    private int parrentPatternID;
    private List<Pattern> childPatternsList;
    private int CGcount;

    public Pattern(String patternString) {
        this.patternString = patternString;
        sequenceList = new ArrayList<Sequence>();
        childPatternsList = new ArrayList<Pattern>();
    }

    public void setCGcount(int cGcount) {
        CGcount = cGcount;
    }

    public int getCGcount() {
        return CGcount;
    }

    public void addSequence(Sequence seq) {
        sequenceList.add(seq);
    }

    public void addChildPattern(Pattern childPattern) {
        childPatternsList.add(childPattern);
    }

    public void setParrentPatternID(int parrentPatternID) {
        this.parrentPatternID = parrentPatternID;
    }

    public List<Pattern> getChildPatternsList() {
        return childPatternsList;
    }

    public int getParrentPatternID() {
        return parrentPatternID;
    }

    public int getCount() {
        return sequenceList.size();
    }

    public String getPatternString() {
        return patternString;
    }

    public List<Sequence> sequenceList() {
        return sequenceList;
    }
}
