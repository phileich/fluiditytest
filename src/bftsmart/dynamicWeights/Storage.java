package bftsmart.dynamicWeights;

import java.util.List;

public interface Storage {
	public List<ClientLatency> getClientLatencies();

	public List<ServerLatency> getServerLatencies();
}
