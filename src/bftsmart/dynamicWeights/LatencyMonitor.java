package bftsmart.dynamicWeights;

import java.util.List;

public abstract class LatencyMonitor implements Runnable{
	public abstract List<Latency> getClientLatencies();

	public abstract List<Latency[]> getServerLatencies();

	public abstract List<Latency[]> getServerProposeLatencies();

	public abstract void clearAll();
}
