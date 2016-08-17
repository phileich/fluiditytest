package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LatencyStorage {
	private List<ServerLatency[]> serverLatencies = Collections.synchronizedList(new ArrayList<ServerLatency[]>());
	private List<ServerLatency[]> serverProposeLatencies = Collections.synchronizedList(new ArrayList<ServerLatency[]>());
	private List<ClientLatency[]> clientLatencies = Collections.synchronizedList(new ArrayList<ClientLatency[]>());

	public void addClientLatencies(ClientLatency[] cl) {
		this.clientLatencies.add(cl);
	}

	public void addServerLatencies(ServerLatency[] sl) {
		this.serverLatencies.add(sl);
	}
	
	public void addServerProposeLatencies(ServerLatency[] spl) {
		this.serverProposeLatencies.add(spl);
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
	
	public List<Latency[]> getServerProposeLatencies() {
		List<Latency[]> copyList = new ArrayList<Latency[]>(serverProposeLatencies);		
		return copyList;
	}

	public List<Latency[]> getClientLatencies() {
		List<Latency[]> copyList = new ArrayList<Latency[]>(clientLatencies);
		return copyList;
	}
}
