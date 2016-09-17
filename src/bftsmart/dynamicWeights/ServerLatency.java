package bftsmart.dynamicWeights;

public class ServerLatency extends Latency {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2009961313741786805L;
	private Long ts_sent;
	private Long ts_received;

	public ServerLatency() {

	}

	public ServerLatency(Long ts_sent, Long ts_received) {
		this.ts_received = ts_received;
		this.ts_sent = ts_sent;
		calcValue();
	}

	public ServerLatency(Long ts_sent, Long ts_received, int from, int to) {
		this.ts_received = ts_received;
		this.ts_sent = ts_sent;
		super.setFrom(from);
		super.setTo(to);
		calcValue();
	}

	public ServerLatency(Long ts_sent) {
		this.ts_sent = ts_sent;
	}

	public void setReceived(Long timestamp) {
		this.ts_received = timestamp;
		calcValue();
	}

	public void setSent(Long timestamp) {
		this.ts_sent = timestamp;
		calcValue();
	}

	private void calcValue() {
		if (ts_sent != null && ts_received != null) {
			setValue(ts_received - ts_sent);
		}
	}

}
