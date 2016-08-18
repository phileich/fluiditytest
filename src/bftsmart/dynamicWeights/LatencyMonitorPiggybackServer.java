package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class LatencyMonitorPiggybackServer implements Storage {
	// key is the round
	private ConcurrentHashMap<Long, ServerLatency[]> serverLatencies = new ConcurrentHashMap<Long, ServerLatency[]>();
	private ConcurrentHashMap<Integer, ServerLatency> tmpServerLatencies = new ConcurrentHashMap<Integer, ServerLatency>();
	private ConcurrentHashMap<Long, ServerLatency[]> serverProposeLatencies = new ConcurrentHashMap<Long, ServerLatency[]>();
	private ConcurrentHashMap<Integer, ServerLatency> tmpServerProposeLatencies = new ConcurrentHashMap<Integer, ServerLatency>();
	private ConcurrentHashMap<Integer, Long[]> tmpClientTimestamps = new ConcurrentHashMap<Integer, Long[]>();
	// private ArrayList<ClientLatency> clientLatencies = new
	// ArrayList<ClientLatency>();
	private List<ClientLatency> clientLatencies = Collections.synchronizedList(new ArrayList<ClientLatency>());
	private int myID;
	private ServerViewController svc = null;// needed for current N of the
											// system

	public LatencyMonitorPiggybackServer(ServerViewController svc, int id) {
		this.svc = svc;
		this.myID = id;
	}

	/**
	 * Stores the Latencies. Represented by a Write - Accept cycle
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
			System.out.print("Server Latencies: ");
			for (Long name : serverLatencies.keySet()) {
				String printKey = name.toString();
				String value = Arrays.deepToString(serverLatencies.get(name));
				System.out.print("(" + printKey + ":" + value + "); ");

			}
			System.out.println("");
		}

	}

	/**
	 * Stores the ProposeLatencies
	 * 
	 * @param serverID
	 *            the ID of the entity which receives the latency request
	 * @param consensusID
	 *            the consensusID defined by the replicas
	 */
	public synchronized void addServerProposeLatency(int serverID, long consensusID) {
		long latReceived = System.currentTimeMillis();
		// get Latency
		int key = createHash(serverID, consensusID);
		ServerLatency storedLatency = tmpServerProposeLatencies.get(key);
		if (storedLatency == null) {
			// error not created yet
		} else {
			storedLatency.setReceived(latReceived);
			storedLatency.setValue(storedLatency.getValue() / 2); // half -> RTT
			Logger.println("Store Server Propose Latency: latency:" + storedLatency.getValue() + ",id:" + serverID
					+ ",consensusID:" + consensusID);

			ServerLatency[] latencyOfRound = serverProposeLatencies.get(consensusID);

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
			serverProposeLatencies.put(consensusID, latencyOfRound);

			// remove tmpLatency, no longer needed to store
			tmpServerProposeLatencies.remove(key);

			// print
			System.out.print("Server Propose Latencies: ");
			for (Long name : serverProposeLatencies.keySet()) {
				String printKey = name.toString();
				String value = Arrays.deepToString(serverProposeLatencies.get(name));
				System.out.print("(" + printKey + ":" + value + "); ");

			}
			System.out.println("");
		}

	}

	public synchronized void createLatency(int to, long consensusID) {
		Logger.println("Created Latency for " + to + " in consensusID " + consensusID);
		ServerLatency latency = new ServerLatency(System.currentTimeMillis());
		latency.setFrom(myID);
		latency.setTo(to);
		latency.setConsensusID(consensusID);
		int key = createHash(to, consensusID);
		tmpServerLatencies.put(key, latency);
	}

	public synchronized void createProposeLatency(int to, long consensusID) {
		Logger.println("Created Propose Latency for " + to + " in consensusID " + consensusID);
		ServerLatency latency = new ServerLatency(System.currentTimeMillis());
		latency.setFrom(myID);
		latency.setTo(to);
		latency.setConsensusID(consensusID);
		int key = createHash(to, consensusID);
		tmpServerProposeLatencies.put(key, latency);
	}

	public synchronized void createProposeLatencies(int[] to, long consensusID) {
		for (int i = 0; i < to.length; i++) {
			Logger.println("Created Propose Latency for " + to[i] + " in consensusID " + consensusID);
			ServerLatency latency = new ServerLatency(System.currentTimeMillis());
			latency.setFrom(myID);
			latency.setTo(to[i]);
			latency.setConsensusID(consensusID);
			int key = createHash(to[i], consensusID);
			tmpServerProposeLatencies.put(key, latency);
		}

	}

	private synchronized int createHash(int id, long consensusID) {
		String key = "id:" + id + ",consensusID:" + consensusID;
		return key.hashCode();
	}

	public synchronized void storeClientTimestamp(long sent_timestamp, long recv_timestamp, int clientID) {
		Long[] tmpTimestamp = { sent_timestamp, recv_timestamp };
		Logger.println("Store Client Timestamp: " + sent_timestamp + " for client " + clientID);
		tmpClientTimestamps.put(clientID, tmpTimestamp);

	}

	public synchronized Long getClientTimestamp(int clientID) {
		Long[] timestampArr = tmpClientTimestamps.get(clientID);
		if (timestampArr != null) {
			long timstamp = timestampArr[0] + (System.currentTimeMillis() - timestampArr[1]);
			return timstamp;
		}
		// error
		return new Long(-1);

	}

	public synchronized void clearClientTimestamp(int clientID, long consensusID) {
		// TODO chekc batching
		int key = createHash(clientID, consensusID);
		tmpClientTimestamps.remove(key);
	}

	public synchronized void storeClientLatency(ClientLatency cl) {
		clientLatencies.add(cl);
	}

	public synchronized void storeClientLatencies(ArrayList<ClientLatency> cls) {
		clientLatencies.addAll(cls);
		Logger.println("Store client latencies: " + StringUtils.join(cls, ","));
	}

	@Override
	public synchronized List<Latency[]> getServerLatencies() {
		List<Latency[]> latencies = new ArrayList<Latency[]>(serverLatencies.values());
		return latencies;
	}

	@Override
	public synchronized List<Latency[]> getServerProposeLatencies() {
		List<Latency[]> latencies = new ArrayList<Latency[]>(serverProposeLatencies.values());
		return latencies;
	}

	@Override
	public synchronized List<Latency> getClientLatencies() {
		// return a copy of the latencies
		List<Latency> latencies = new ArrayList<Latency>(clientLatencies);
		return latencies;

	}

	public int[] getCurrentViewAcceptors() {
		return svc.getCurrentViewAcceptors();
	}

	public int[] getCurrentViewOtherAcceptors() {
		return svc.getCurrentViewOtherAcceptors();
	}
}
