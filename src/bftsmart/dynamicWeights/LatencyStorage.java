package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LatencyStorage {
	private List<ServerLatency[]> serverLatencies = Collections.synchronizedList(new ArrayList<ServerLatency[]>());
	private List<ServerLatency[]> serverProposeLatencies = Collections
			.synchronizedList(new ArrayList<ServerLatency[]>());
	private List<ClientLatency[]> clientLatencies = Collections.synchronizedList(new ArrayList<ClientLatency[]>());

	public LatencyStorage() {
	}

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

	public int getServerProposeSize() {
		return serverProposeLatencies.size();
	}

	/**
	 * returns the last entries since last reconfig. Clears afterwards
	 * 
	 * @return
	 */
	public List<Latency[]> getServerLatencies() { //TODO give boolean whether to delete latencies or not
		// int start = Math.max(0, serverLatencies.size() - lastValues);
		List<Latency[]> copyList = new ArrayList<Latency[]>(serverLatencies);
		serverLatencies.clear();
		return copyList;
	}

	/**
	 * returns the last entries since last reconfig. Clears afterwards
	 * 
	 * @return
	 */
	public List<Latency[]> getServerProposeLatencies() {
		// int start = Math.max(0, serverProposeLatencies.size() - lastValues);
		List<Latency[]> copyList = new ArrayList<Latency[]>(serverProposeLatencies);
		serverProposeLatencies.clear();
		return copyList;
	}

	/**
	 * returns the last entries since last reconfig. Clears afterwards
	 * 
	 * @return
	 */
	public List<Latency[]> getClientLatencies() {
		// int start = Math.max(0, clientLatencies.size() - lastValues);
		List<Latency[]> copyList = new ArrayList<Latency[]>(clientLatencies);
		clientLatencies.clear();
		return copyList;
	}

}
