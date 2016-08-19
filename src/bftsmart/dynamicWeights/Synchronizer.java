package bftsmart.dynamicWeights;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class Synchronizer implements Runnable {
	private LatencyReducer latReducer;
	private int id;
	private int n;
	private ServerCommunicationSystem scs;
	private Storage latencyMonitor;

	public Synchronizer(Storage latencyMonitor, int id, int n, ServerCommunicationSystem scs) {
		this.latReducer = new MedianReducer();
		this.id = id;
		this.scs = scs;
		this.n = n;
		this.latencyMonitor = latencyMonitor;

	}

	@Override
	public void run() {
		InternalServiceProxy internalClient = new InternalServiceProxy(id + 100);
		try {
			// get latencies
			List<Latency> clientLatencies = latencyMonitor.getClientLatencies();
			List<Latency[]> serverLatencies = latencyMonitor.getServerLatencies();
			List<Latency[]> serverProposeLatencies = latencyMonitor.getServerProposeLatencies();

			// reduce clientLatency
			Latency[] reducedClientLat = latReducer.reduce(clientLatencies, n);

			Latency[] reducedServerLat = latReducer.reduce2d(serverLatencies, n);

			Latency[] reducedServerProposeLat = latReducer.reduce2d(serverProposeLatencies, n);

			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			DataOutputStream dos = new DataOutputStream(out);

			byte[] serializeClientLat = SerializationUtils.serialize(reducedClientLat);
			byte[] serializeServerLat = SerializationUtils.serialize(reducedServerLat);
			byte[] serializeServerProposeLat = SerializationUtils.serialize(reducedServerProposeLat);

			dos.writeInt(serializeClientLat.length);
			dos.write(serializeClientLat);
			dos.writeInt(serializeServerLat.length);
			dos.write(serializeServerLat);
			dos.writeInt(serializeServerProposeLat.length);
			dos.write(serializeServerProposeLat);

			Logger.println("Sending client latencies to internal consensus: " + Arrays.toString(reducedClientLat));
			Logger.println("Sending server latencies to internal consensus: " + Arrays.toString(reducedServerLat));
			Logger.println("Sending server propose latencies to internal consensus: "
					+ Arrays.toString(reducedServerProposeLat));

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
