package edu.cwru.cbc.BSPAT.DataType;

import java.io.Serializable;

public class MappingSummary implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -4948258734655039540L;
	private long seqAnalysed = 0;
	private long uniqueBestHit = 0;
	private long noAlignment = 0;
	private long notUnique = 0;
	private long notExtracted = 0;

	public long getSeqAnalysed() {
		return seqAnalysed;
	}

	public void addSeqAnalysed(long seqAnalysed) {
		this.seqAnalysed += seqAnalysed;
	}

	public long getUniqueBestHit() {
		return uniqueBestHit;
	}

	public void addUniqueBestHit(long uniqueBestHit) {
		this.uniqueBestHit += uniqueBestHit;
	}

	public long getNoAlignment() {
		return noAlignment;
	}

	public void addNoAlignment(long noAlignment) {
		this.noAlignment += noAlignment;
	}

	public long getNotUnique() {
		return notUnique;
	}

	public void addNotUnique(long notUnique) {
		this.notUnique += notUnique;
	}

	public long getNotExtracted() {
		return notExtracted;
	}

	public void addNotExtracted(long notExtracted) {
		this.notExtracted += notExtracted;
	}

	public double getMappingEfficiency() {
		return uniqueBestHit / (double) seqAnalysed;
	}

	public String getMappingEfficiencyString() {
		return String.format("%.3f", getMappingEfficiency());
	}

	@Override
	public String toString() {
		return String
				.format("Sequences analysed in total:\t%d\nNumber of alignments with a unique best hit from the different alignments:\t%d\nMapping efficiency:\t%s\nSequences with no alignments under any condition:\t%d\nSequences did not map uniquely:\t%d\nSequences which were discarded because genomic sequence could not be extracted:\t%d\n",
						seqAnalysed, uniqueBestHit, getMappingEfficiencyString(), noAlignment, notUnique, notExtracted);
	}

}
