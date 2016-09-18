package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import bftsmart.reconfiguration.ClientViewController;
import bftsmart.tom.util.Logger;

public class LatencyMonitorPiggybackClient extends LatencyMonitor{
	private List<ClientLatency> clientLatencies = Collections.synchronizedList(new ArrayList<ClientLatency>());
	private int myID;

	public LatencyMonitorPiggybackClient(int id, ClientViewController cvc) {
		this.myID = id;

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
		long ts = currTimestamp - timestamp;
		ClientLatency cl = new ClientLatency();
		cl.setValue(ts / 2); // half -> RTT
		cl.setFrom(myID);
		cl.setTo(serverID);
		cl.setConsensusID(consensusID);
		clientLatencies.add(cl);
//		Logger.println("Store Client Latency: latency:" + ts + ",id:" + serverID + ",consensusID:" + consensusID);
//		Logger.println("CLientLatencies:" + clientLatencies);

	}

	@Override
	public List<Latency> getClientLatencies() {
		ArrayList<Latency> latencies = new ArrayList<Latency>(clientLatencies);
		clientLatencies.clear();
//		Logger.println("Sending latencies to server : " + StringUtils.join(latencies, ","));
		return latencies;
	}

	@Override
	public  List<Latency[]> getServerLatencies() {
		return null;
	}

	@Override
	public List<Latency[]> getServerProposeLatencies() {
		return null;
	}

	@Override
	public void clearAll() {
		clientLatencies.clear();

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
