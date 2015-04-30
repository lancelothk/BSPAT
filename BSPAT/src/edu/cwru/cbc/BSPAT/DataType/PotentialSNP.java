package edu.cwru.cbc.BSPAT.DataType;

/**
 * Created by lancelothk on 4/30/15.
 * Potential SNP
 */
public class PotentialSNP {
    private int position;
    private char nucleotide;

    public PotentialSNP(int position, char nucleotide) {
        this.position = position;
        this.nucleotide = nucleotide;
    }

    public int getPosition() {
        return position;
    }

    public char getNucleotide() {
        return nucleotide;
    }
}
