package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class LatencyMonitorPiggybackServer implements Storage {
	// key is the round
	private LinkedHashMap<Long, ServerLatency[]> serverLatencies = new LinkedHashMap<Long, ServerLatency[]>();
	private LinkedHashMap<Integer, ServerLatency> tmpServerLatencies = new LinkedHashMap<Integer, ServerLatency>();
	private LinkedHashMap<Integer, Long[]> tmpClientTimestamps = new LinkedHashMap<Integer, Long[]>();
	private ArrayList<ClientLatency> clientLatencies = new ArrayList<ClientLatency>();
	private int myID;
	private ServerViewController svc = null;// needed for current N of the
											// system

	public LatencyMonitorPiggybackServer(ServerViewController svc, int id) {
		this.svc = svc;
		this.myID = id;
	}

	/**
	 * Stores the Latencies
	 * 
	 * @param serverID
	 *            the ID of the entity which receives the latency request
	 * @param consensusID
	 *            the consensusID defined by the replicas
	 */
	public synchronized void addServerLatency(int serverID, long consensusID) {
		long latReceived = System.currentTimeMillis();
		// get Latency
		int key = createHash(serverID, consensusID);
		ServerLatency storedLatency = tmpServerLatencies.get(key);
		if (storedLatency == null) {
			// error not created yet
		} else {
			storedLatency.setReceived(latReceived);
			Logger.println("Store Server Latency: latency:" + storedLatency.getValue() + ",id:" + serverID
					+ ",consensusID:" + consensusID);

			ServerLatency[] latencyOfRound = serverLatencies.get(consensusID);

			if (latencyOfRound == null) {
				// this does not exist yet -> create
				int n = svc.getCurrentViewN();
				latencyOfRound = new ServerLatency[n];
				latencyOfRound[serverID] = storedLatency;
				// my own latency with lat = 0
				ServerLatency myLat = new ServerLatency(new Long(0), new Long(0));
				latencyOfRound[myID] = myLat;

			} else {
				// check if correct id
				if (serverID < latencyOfRound.length) {
					latencyOfRound[serverID] = storedLatency;
				} else {
					// ERROR!
				}
			}
			serverLatencies.put(consensusID, latencyOfRound);

			// remove tmpLatency, no longer needed to store
			tmpServerLatencies.remove(key);

			// print
			for (Long name : serverLatencies.keySet()) {
				String printKey = name.toString();
				String value = Arrays.deepToString(serverLatencies.get(name));
				System.out.print("(" + printKey + ":" + value + "); ");

			}
			System.out.println("");
		}

	}

	public void createLatency(int to, long consensusID) {
		Logger.println("Created Latency for " + to + " in consensusID " + consensusID);
		ServerLatency latency = new ServerLatency(System.currentTimeMillis());
		latency.setFrom(myID);
		latency.setTo(to);
		latency.setConsensusID(consensusID);
		int key = createHash(to, consensusID);
		tmpServerLatencies.put(key, latency);
	}

	public ArrayList<Long> getServerLatency() {

		return null;
	}

	private int createHash(int id, long consensusID) {
		String key = "id:" + id + ",consensusID:" + consensusID;
		return key.hashCode();
	}

	public void storeClientTimestamp(long sent_timestamp, long recv_timestamp, int clientID) {
		Long[] tmpTimestamp = { sent_timestamp, recv_timestamp };
		Logger.println("Store Client Timestamp: " + sent_timestamp + " for client " + clientID);
		tmpClientTimestamps.put(clientID, tmpTimestamp);

	}

	public Long getClientTimestamp(int clientID) {
		Long[] timestampArr = tmpClientTimestamps.get(clientID);
		if (timestampArr != null) {
			long timstamp = timestampArr[0] + (System.currentTimeMillis() - timestampArr[1]);
			return timstamp;
		}
		// error
		return new Long(-1);

	}

	public void clearClientTimestamp(int clientID, long consensusID) {
		int key = createHash(clientID, consensusID);
		tmpClientTimestamps.remove(key);
	}

	public void storeClientLatency(ClientLatency cl) {
		clientLatencies.add(cl);
	}

	public void storeClientLatencies(ArrayList<ClientLatency> cls) {
		clientLatencies.addAll(cls);
		Logger.println("Store client latencies: " + StringUtils.join(cls, ","));
	}

	@Override
	public List<ServerLatency> getServerLatencies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ClientLatency> getClientLatencies() {
		//return a copy of the latencies
		 List<ClientLatency> latencies = (List<ClientLatency>) clientLatencies.clone();
		
		
		return latencies;

	}
}
