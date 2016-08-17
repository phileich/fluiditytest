package bftsmart.dynamicWeights;

import java.util.List;

public interface Storage {
	public List<Latency> getClientLatencies();

	public List<Latency[]> getServerLatencies();
	
	public List<Latency[]> getServerProposeLatencies();
}
