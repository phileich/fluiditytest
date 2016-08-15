package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LatencyStorage {
	private List<ServerLatency[]> serverLatencies = Collections.synchronizedList(new ArrayList<ServerLatency[]>());
	private List<ClientLatency[]> clientLatencies = Collections.synchronizedList(new ArrayList<ClientLatency[]>());

	public void addClientLatencies(ClientLatency[] cl) {
		this.clientLatencies.add(cl);
	}

	public void addServerLatencies(ServerLatency[] sl) {
		this.serverLatencies.add(sl);
	}

	public int getClientSize() {
		return clientLatencies.size();
	}

	public int getServerSize() {
		return serverLatencies.size();
	}

	public List<Latency[]> getServerLatencies() {
		List<Latency[]> copyList = new ArrayList<Latency[]>(serverLatencies);		
		return copyList;
	}

	public List<Latency[]> getClientLatencies() {
		List<Latency[]> copyList = new ArrayList<Latency[]>(clientLatencies);
		return copyList;
	}
}
