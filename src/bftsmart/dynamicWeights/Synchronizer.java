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
	private ServerViewController svController;
	private ServerCommunicationSystem scs;
	private Storage latencyMonitor;

	public Synchronizer(Storage latencyMonitor, int id, ServerViewController svController,
			ServerCommunicationSystem scs) {
		this.latReducer = new MedianReducer();
		this.id = id;
		this.scs = scs;
		this.svController = svController;
		this.latencyMonitor = latencyMonitor;

	}

	@Override
	public void run() {
		InternalServiceProxy internalClient = new InternalServiceProxy(id + 100);
		try {
			int currentN = svController.getCurrentViewN();
			// get latencies
			List<Latency> clientLatencies = latencyMonitor.getClientLatencies();
			List<Latency[]> serverLatencies = latencyMonitor.getServerLatencies();

			// reduce clientLatency
			Latency[] reducedClientLat = latReducer.reduce(clientLatencies, currentN);

			Latency[] reducedServerLat = latReducer.reduce2d(serverLatencies, currentN);

			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			DataOutputStream dos = new DataOutputStream(out);

			byte[] serializeClientLat = SerializationUtils.serialize(reducedClientLat);
			byte[] serializeServerLat = SerializationUtils.serialize(reducedServerLat);

			dos.writeInt(serializeClientLat.length);
			dos.write(serializeClientLat);
			dos.writeInt(serializeServerLat.length);
			dos.write(serializeServerLat);

			Logger.println("Sending client latencies to internal consensus: " + Arrays.toString(reducedClientLat));
			Logger.println("Sending server latencies to internal consensus: " + Arrays.toString(reducedServerLat));
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
