package bftsmart.dynamicWeights;

import java.util.List;

public class Synchronizer {
	private LatencyReducer latReducer;

	public Synchronizer() {
		this.latReducer = new Kalman();
	}

	public void synchronize(Storage latencyMonitor, int currentN) {
		// get latencies
		List<ClientLatency> clientLatencies = latencyMonitor.getClientLatencies();

		// reduce clientLatency
		latReducer.reduce(clientLatencies, currentN);
	}

}
