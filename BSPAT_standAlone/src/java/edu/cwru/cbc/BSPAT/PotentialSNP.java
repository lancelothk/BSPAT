package edu.cwru.cbc.BSPAT;

/**
 * Created by lancelothk on 4/30/15.
 * Potential SNP
 */
public class PotentialSNP {
	private int position; // start from target seq position.
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

	@Override
	public String toString() {
		return position + ":" + nucleotide;
	}
}
