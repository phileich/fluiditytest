package bftsmart.dynamicWeights;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.TOMLayer;

public class DynamicWeightController implements Runnable {
	private ServerViewController svController;
	private int id;
	private TOMLayer tl;
	private Storage latencyMonitor;
	private LatencyStorage latStorage;
	private boolean reconfigInExec = false;
	private boolean calcStarted = false;
	private ServerCommunicationSystem scs;

	public DynamicWeightController(int id, ServerViewController svController) {
		this(id, svController, new DummyStorage());
	}

	public DynamicWeightController(int id, ServerViewController svController, Storage latencyMonitor) {
		this.svController = svController;
		this.id = id;
		this.latencyMonitor = latencyMonitor;
		this.latStorage = new LatencyStorage();		
	}

	public int getID() {
		return id;
	}

	// TODO erweitern von Klassen
	@Override
	public void run() {
		// Start calculation of reconfiguration
		System.out.println("start reconfig calculation");
		// synchronize Data
		Thread syncThread = new Thread(new Synchronizer(latencyMonitor, id, svController.getCurrentViewN(), scs),
				"SynchronizationThread");
		syncThread.start();
	}

	public void setTOMLayer(TOMLayer tl) {
		this.tl = tl;
	}

	public int getInExec() {
		int id = tl.getInExec();
		if (id != -1) {
			return id;
		} else {
			return tl.getLastExec();
		}
	}

	public synchronized void receiveExec(int exec) {
		// trigger sync every 100 consensus
		if (exec % 100 == 0 && exec != 0) {
			if (!reconfigInExec) {
				reconfigInExec = true;
//				new Thread(this, "ControllerThread").start();
			}
		}

	}

	public ServerCommunicationSystem getServerCommunicationSystem() {
		return scs;
	}

	public void setServerCommunicationSystem(ServerCommunicationSystem scs) {
		this.scs = scs;
	}

	public void addInternalConsensusDataToStorage(byte[] data) {
		try {
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

			int clientLength = dis.readInt();
			byte[] serializedClientLat = new byte[clientLength];
			dis.readFully(serializedClientLat);
			ClientLatency[] clientLatencies = SerializationUtils.deserialize(serializedClientLat);

			int serverLength = dis.readInt();
			byte[] serializedServerLat = new byte[serverLength];
			dis.readFully(serializedServerLat);
			ServerLatency[] serverLatencies = SerializationUtils.deserialize(serializedServerLat);

			int serverProposeLength = dis.readInt();
			byte[] serializedServerProposeLat = new byte[serverProposeLength];
			dis.readFully(serializedServerProposeLat);
			ServerLatency[] serverProposeLatencies = SerializationUtils.deserialize(serializedServerProposeLat);

			System.out.println("received Client Latencies from internal conensus: " + Arrays.toString(clientLatencies));
			System.out.println("received Server Latencies from internal conensus: " + Arrays.toString(serverLatencies));
			System.out.println("received Server Propose Latencies from internal conensus: "
					+ Arrays.toString(serverProposeLatencies));

			latStorage.addClientLatencies(clientLatencies);
			latStorage.addServerLatencies(serverLatencies);
			latStorage.addServerProposeLatencies(serverProposeLatencies);

			// if n -f entries -> trigger calculation
			if (latStorage.getClientSize() >= (svController.getCurrentViewN() - svController.getCurrentViewF())
					&& latStorage.getServerSize() >= (svController.getCurrentViewN() - svController.getCurrentViewF())
					&& !calcStarted) {
				// wait a bit??
				calcStarted = true;
				try {
					Thread.sleep(5000);
					Thread reconfigThread = new Thread(new Reconfigurator(latStorage, svController),
							"ReconfigurationThread");
					reconfigThread.start();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
