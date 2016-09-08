package bftsmart.dynamicWeights;

import java.io.Serializable;

public class Latency implements Serializable, Comparable<Latency> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6229999093145673414L;
	private Double ts_value;
	private int from;
	private int to;
	private long consensusID;

	public Latency() {

	}

	public Latency(int from, int to, double value) {
		this.from = from;
		this.to = to;
		this.ts_value = value;
	}

	public double getValue() {
		return ts_value;
	}

	public void setValue(double ts_value) {
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
			return "(" + from + "," + to + "," + Double.toString(ts_value) + ")";
		} else {
			return "null";
		}
	}

	@Override
	public int compareTo(Latency o) {
		// compares first by consensusID, then from, then to
		if (consensusID > o.consensusID) {
			return 1;
		} else if (consensusID < o.consensusID) {
			return -1;
		} else {
			if (from > o.from) {
				return 1;
			} else if (from < o.from) {
				return -1;
			} else {
				if (to > o.to) {
					return 1;
				} else if (to < o.to) {
					return -1;
				} else {
					return 0;
				}
			}
		}

	}
}
