package bftsmart.dynamicWeights;

import java.io.Serializable;

public class Latency implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6229999093145673414L;
	private Long ts_value;
	private int from;
	private int to;
	private long consensusID;

	public long getValue() {
		return ts_value;
	}

	public void setValue(long ts_value) {
		this.ts_value = ts_value;
	}

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public long getConsensusID() {
		return consensusID;
	}

	public void setConsensusID(long consensusID) {
		this.consensusID = consensusID;
	}

	public String toString() {
		if (ts_value != null) {
			return Long.toString(ts_value);
		} else {
			return "null";
		}
	}
}
