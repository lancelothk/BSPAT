package edu.cwru.cbc.BSPAT.DataType;

import java.util.HashMap;
import java.util.Map;

public class Pattern {
    private static int patternCount = 1;
    private String patternString;
    private Map<String, Sequence> sequenceMap;
    private int patternID;
    private int methylationParentID;
    private int mutationParentID;
    private PatternType patternType;
    public Pattern(String patternString, PatternType patternType) {
        this.patternString = patternString;
        this.patternType = patternType;
        sequenceMap = new HashMap<>();
    }

    public static void resetPatternCount() {
        patternCount = 1;
    }

    public void assignPatternID() {
        this.patternID = patternCount++;
    }

    public int getPatternID() {
        return patternID;
    }

    public void addSequence(Sequence seq) {
        sequenceMap.put(seq.getId(), seq);
    }

    public PatternType getPatternType() {
        return patternType;
    }

    public int getCount() {
        return sequenceMap.size();
    }

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public Map<String, Sequence> getSequenceMap() {
        return sequenceMap;
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

    public enum PatternType {
        METHYLATION, MUTATION, MEMU, ALLELE, NONALLELE
    }
}
