package DataType;

import java.util.ArrayList;
import java.util.List;

public class Pattern {
    public enum PatternType {
        METHYLATION, MUTATION
    }

    private static int patternCount = 0;
    private String patternString;
    private List<Sequence> sequenceList;
    private int patternID;
    private int parrentPatternID;
    private List<Pattern> childPatternsList;
    private PatternType patternType;

    public Pattern(String patternString, PatternType patternType) {
        this.patternString = patternString;
        this.patternID = patternCount++;
        this.patternType = patternType;
        sequenceList = new ArrayList<>();
        childPatternsList = new ArrayList<>();
    }

    public int getPatternID() {
        return patternID;
    }

    public int getCGcount() {
        if (patternType != PatternType.METHYLATION) {
            throw new RuntimeException("getCGcount only available for methylation type");
        }
        // since patterns is grouped by hashmap, every pattern should contain at least one sequence.
        if (sequenceList.size() == 0) {
            throw new RuntimeException("pattern sequence list is empty!");
        }
        // all seqs in list share same pattern
        return this.sequenceList.get(0).getCpGSites().size();
    }

    public void addSequence(Sequence seq) {
        sequenceList.add(seq);
    }

    public PatternType getPatternType() {
        return patternType;
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
