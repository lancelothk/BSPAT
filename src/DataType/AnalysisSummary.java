package DataType;

import java.io.Serializable;

public class AnalysisSummary implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4282040809641125013L;
	private long seqBeforeFilter;
	private long seqAfterFilter;

	public long getSeqBeforeFilter() {
		return seqBeforeFilter;
	}

	public void addSeqBeforeFilter(long seqBeforeFilter) {
		this.seqBeforeFilter += seqBeforeFilter;
	}

	public long getSeqAfterFilter() {
		return seqAfterFilter;
	}

	public void addSeqAfterFilter(long seqAfterFilter) {
		this.seqAfterFilter += seqAfterFilter;
	}

	@Override
	public String toString() {
		return String.format("Sequences before filtering:\t%d\nSequences after filtering:\t%d\n", seqBeforeFilter, seqAfterFilter);
	}
}
