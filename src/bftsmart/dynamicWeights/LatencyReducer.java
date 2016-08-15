package bftsmart.dynamicWeights;

import java.util.List;

public interface LatencyReducer {
	public Latency[] reduce(List<Latency> latencies, int currentN);

	public Latency[] reduce2d(List<Latency[]> latencies, int currentN);
}
