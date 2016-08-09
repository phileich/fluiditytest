package bftsmart.dynamicWeights;

import java.util.List;

public interface LatencyReducer {
	public double[] reduce(List<ClientLatency> latencies, int currentN);
}
