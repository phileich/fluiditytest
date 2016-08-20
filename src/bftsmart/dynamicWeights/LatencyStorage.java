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

	/**
	 * returns the last 'lastValues' entries
	 * 
	 * @return
	 */
	public List<Latency[]> getServerLatencies(int lastValues) {
		int start = Math.max(0, serverLatencies.size() - lastValues);
		List<Latency[]> copyList = new ArrayList<Latency[]>(serverLatencies.subList(start, serverLatencies.size()));

		return copyList;
	}

	/**
	 * returns the last 'lastValues' entries
	 * 
	 * @return
	 */
	public List<Latency[]> getServerProposeLatencies(int lastValues) {
		int start = Math.max(0, serverProposeLatencies.size() - lastValues);
		List<Latency[]> copyList = new ArrayList<Latency[]>(
				serverProposeLatencies.subList(start, serverProposeLatencies.size()));
		return copyList;
	}

	/**
	 * returns the last 'lastValues' entries
	 * 
	 * @return
	 */
	public List<Latency[]> getClientLatencies(int lastValues) {
		int start = Math.max(0, clientLatencies.size() - lastValues);
		List<Latency[]> copyList = new ArrayList<Latency[]>(clientLatencies.subList(start, clientLatencies.size()));
		return copyList;
	}

}
