package bftsmart.dynamicWeights;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.util.Logger;

public class Synchronizer implements Runnable {
	private LatencyReducer latReducer;
	private int id;
	private int n;
	private ServerCommunicationSystem scs;
	private Storage latencyMonitor;
	private TOMConfiguration conf;

	public Synchronizer(Storage latencyMonitor, int id, int n, ServerCommunicationSystem scs, TOMConfiguration conf) {
		this.latReducer = new MedianReducer();
		this.id = id;
		this.scs = scs;
		this.n = n;
		this.latencyMonitor = latencyMonitor;
		this.conf = conf;
	}

	@Override
	public void run() {
		InternalServiceProxy internalClient = new InternalServiceProxy(id + 100);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			DataOutputStream dos = new DataOutputStream(out);

			if (conf.measureClients()) {
				// get latencies since last reconfig
				List<Latency> clientLatencies = latencyMonitor.getClientLatencies();
				// reduce
				System.out.println("Sync - pre Reduce Client: " + clientLatencies);
				Latency[] reducedClientLat = latReducer.reduce(clientLatencies, n);
				System.out.println("Sync - post Reduce Client: " + Arrays.deepToString(reducedClientLat));
				// send
				byte[] serializeClientLat = SerializationUtils.serialize(reducedClientLat);
				dos.writeInt(serializeClientLat.length);
				dos.write(serializeClientLat);
				Logger.println("Sending client latencies to internal consensus: " + Arrays.toString(reducedClientLat));
			}
			if (conf.measureServers()) {
				// get latencies since last reconfig
				List<Latency[]> serverLatencies = latencyMonitor.getServerLatencies();
				List<Latency[]> serverProposeLatencies = latencyMonitor.getServerProposeLatencies();
				// reduce
				System.out.println("Sync - pre Reduce Server: " + Arrays.deepToString(serverLatencies.toArray()));
				Latency[] reducedServerLat = latReducer.reduce2d(serverLatencies, n);
				System.out.println("Sync - post Reduce Server: " + Arrays.deepToString(reducedServerLat));

				System.out
						.println("Sync - pre Reduce Propose: " + Arrays.deepToString(serverProposeLatencies.toArray()));
				Latency[] reducedServerProposeLat = latReducer.reduce2d(serverProposeLatencies, n);
				System.out.println("Sync - post Reduce Propose: " + Arrays.deepToString(reducedServerProposeLat));
				// send
				byte[] serializeServerLat = SerializationUtils.serialize(reducedServerLat);
				byte[] serializeServerProposeLat = SerializationUtils.serialize(reducedServerProposeLat);
				dos.writeInt(serializeServerLat.length);
				dos.write(serializeServerLat);
				dos.writeInt(serializeServerProposeLat.length);
				dos.write(serializeServerProposeLat);
				Logger.println("Sending server latencies to internal consensus: " + Arrays.toString(reducedServerLat));
				Logger.println("Sending server propose latencies to internal consensus: "
						+ Arrays.toString(reducedServerProposeLat));
			}
			// clear to prevent overflow
			latencyMonitor.clearAll();

			byte[] reply = internalClient.invokeInternal(out.toByteArray());
			if (reply != null) {
				Logger.println("Received Internal Consensus: " + new String(reply));
			} else {
				Logger.println("Received Internal Consensus: NULL");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			internalClient.close();
		}
	}

}
