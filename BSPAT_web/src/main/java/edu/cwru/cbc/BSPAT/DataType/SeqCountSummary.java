package edu.cwru.cbc.BSPAT.DataType;

import java.io.Serializable;

public class SeqCountSummary implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -4282040809641125013L;
	private long seqTargetBounded;
	private long seqTargetAfterFilter;
	private long seqCpGBounded;
	private long seqCpGAfterFilter;
	private long seqOthers;

	public long getSeqTargetBounded() {
		return seqTargetBounded;
	}

	public void addSeqBeforeFilter(long seqTargetBounded) {
		this.seqTargetBounded += seqTargetBounded;
	}

	public long getSeqTargetAfterFilter() {
		return seqTargetAfterFilter;
	}

	public void addSeqAfterFilter(long seqTargetAfterFilter) {
		this.seqTargetAfterFilter += seqTargetAfterFilter;
	}

	public long getSeqCpGBounded() {
		return seqCpGBounded;
	}

	public void addSeqCpGBounded(long seqCpGBounded) {
		this.seqCpGBounded += seqCpGBounded;
	}

	public long getSeqCpGAfterFilter() {
		return seqCpGAfterFilter;
	}

	public void addSeqCpGAfterFilter(long seqCpGAfterFilter) {
		this.seqCpGAfterFilter += seqCpGAfterFilter;
	}

	public long getSeqOthers() {
		return seqOthers;
	}

	public void addSeqOthers(long seqOthers) {
		this.seqOthers += seqOthers;
	}

	@Override
	public String toString() {
		return String.format(
				"Sequences cover target region:\t%d\nSequences don't cover whole target but cover all CpGs:\t%d\nSequences after filtering:\t%d\n",
				seqTargetBounded,
				seqCpGBounded, seqTargetAfterFilter);
	}
}
