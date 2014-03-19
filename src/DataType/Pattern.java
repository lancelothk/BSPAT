package DataType;

import java.util.ArrayList;
import java.util.List;

public class Pattern {
    public enum PatternType {
        METHYLATION, MUTATION, MEMU
    }

    private static int patternCount = 1;
    private String patternString;
    private List<Sequence> sequenceList;
    private int patternID;
    private int methylationParentID;
    private int mutationParentID;
    private PatternType patternType;

    public Pattern(String patternString, PatternType patternType) {
        this.patternString = patternString;
        this.patternType = patternType;
        sequenceList = new ArrayList<>();
    }

    public void assignPatternID() {
        this.patternID = patternCount++;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
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

    public int getCount() {
        return sequenceList.size();
    }

    public String getPatternString() {
        return patternString;
    }

    public List<Sequence> sequenceList() {
        return sequenceList;
    }

    public int getMethylationParentID() {
        return methylationParentID;
    }

    public void setMethylationParentID(int methylationParentID) {
        this.methylationParentID = methylationParentID;
    }

    public int getMutationParentID() {
        return mutationParentID;
    }

    public void setMutationParentID(int mutationParentID) {
        this.mutationParentID = mutationParentID;
    }
}
