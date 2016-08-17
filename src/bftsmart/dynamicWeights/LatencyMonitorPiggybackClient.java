package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import bftsmart.reconfiguration.ClientViewController;
import bftsmart.tom.util.Logger;

public class LatencyMonitorPiggybackClient implements Storage {
	// TODO Backup?
	private List<ClientLatency> clientLatencies = Collections.synchronizedList(new ArrayList<ClientLatency>());
	private List<ClientLatency> clientLatenciesBackup = Collections.synchronizedList(new ArrayList<ClientLatency>());
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
		long ts = currTimestamp - timestamp;
		ClientLatency cl = new ClientLatency();
		cl.setValue(ts / 2); // half -> RTT
		cl.setFrom(myID);
		cl.setTo(serverID);
		cl.setConsensusID(consensusID);
		clientLatencies.add(cl);
		Logger.println("Store Client Latency: latency:" + ts + ",id:" + serverID + ",consensusID:" + consensusID);
		// print
		Logger.println("CLientLatencies:" + clientLatencies);
		Logger.println("CLientLatenciesBackup:" + clientLatenciesBackup);

	}

	@Override
	public synchronized List<Latency> getClientLatencies() {
		ArrayList<Latency> latencies = new ArrayList<Latency>(clientLatencies);
		clientLatenciesBackup.addAll(clientLatencies);

		clientLatencies.clear();
		Logger.println("Sending latencies to server : " + StringUtils.join(latencies, ","));
		return latencies;
	}

	@Override
	public synchronized List<Latency[]> getServerLatencies() {
		return null;
	}

	@Override
	public List<Latency[]> getServerProposeLatencies() {
		return null;
	}

}
