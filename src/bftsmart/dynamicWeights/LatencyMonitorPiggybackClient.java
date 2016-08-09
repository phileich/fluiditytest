package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import bftsmart.reconfiguration.ClientViewController;
import bftsmart.tom.util.Logger;

public class LatencyMonitorPiggybackClient implements Storage {
	// key is the round
	private LinkedHashMap<Integer, ClientLatency> clientLatencies = new LinkedHashMap<Integer, ClientLatency>();
	// TODO Backup?
	private LinkedHashMap<Integer, ClientLatency> clientLatenciesBackup = new LinkedHashMap<Integer, ClientLatency>();
	private int myID;
	private ClientViewController cvc = null;// needed for current N of the
											// system

	public LatencyMonitorPiggybackClient(int id, ClientViewController cvc) {
		this.myID = id;
		this.cvc = cvc;
	}

	/**
	 * Stores the Latencies
	 * 
	 * @param serverID
	 *            the ID of the entity which receives the latency request
	 * @param round
	 *            the round defined by the replicas
	 */
	public synchronized void addClientLatency(long timestamp, int serverID, long consensusID) {
		long currTimestamp = System.currentTimeMillis();
		// TODO check if consensusID = sequenzID

		int key = createHash(serverID, consensusID);
		long ts = currTimestamp - timestamp;
		ClientLatency cl = new ClientLatency();
		cl.setValue(ts);
		cl.setFrom(myID);
		cl.setTo(serverID);
		cl.setConsensusID(consensusID);
		clientLatencies.put(key, cl);
		Logger.println("Store Client Latency: latency:" + ts + ",id:" + serverID + ",consensusID:" + consensusID);
		// print
		for (Integer name : clientLatencies.keySet()) {
			String printKey = name.toString();
			ClientLatency value = clientLatencies.get(name);
			System.out.print("(" + printKey + ":" + value + "); ");

		}
		System.out.println("");

	}

	public ClientLatency getClientLatency(int serverID, long consensusID) {
		int key = createHash(serverID, consensusID);
		return clientLatencies.get(key);
	}

	private int createHash(int id, long consensusID) {
		String key = "id:" + id + ",consensusID:" + consensusID;
		return key.hashCode();
	}

	@Override
	public List<ClientLatency> getClientLatencies() {
		ArrayList<ClientLatency> latencies = new ArrayList<ClientLatency>();
		for (Integer key : clientLatencies.keySet()) {
			ClientLatency value = clientLatencies.get(key);
			latencies.add(value);
			clientLatenciesBackup.put(key, value);
		}

		clientLatencies.clear();
		Logger.println("Sending latencies to server : " + StringUtils.join(latencies, ","));
		return latencies;
	}

	@Override
	public List<ServerLatency> getServerLatencies() {		
		return null;
	}

}
