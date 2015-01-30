package edu.cwru.cbc.BSPAT.DataType;

import java.util.HashMap;
import java.util.Map;

public class Pattern {
    public enum PatternType {
        METHYLATION, MUTATION, MEMU, ALLELE, NONALLELE
    }

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

    public void assignPatternID() {
        this.patternID = patternCount++;
    }

    public static void resetPatternCount() {
        patternCount = 1;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }

    public int getPatternID() {
        return patternID;
    }

    public int getCGcount() {
        if (patternType == PatternType.METHYLATION || patternType == PatternType.MEMU) {
            // since patterns is grouped by hashmap, every pattern should contain at least one sequence.
            if (sequenceMap.size() == 0) {
                throw new RuntimeException("pattern sequence list is empty!");
            }
            // all seqs in list share same pattern
            return this.sequenceMap.values().iterator().next().getCpGSites().size();
        } else {
            throw new RuntimeException("getCGcount only available for methylation type");
        }
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
}
