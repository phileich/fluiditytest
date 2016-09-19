package bftsmart.dynamicWeights;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.SerializationUtils;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.dynamicWeights.LatencyMonitorPiggybackServer.LatencyPOJO;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.util.Logger;

public class DynamicWeightController implements Runnable {
	private ServerViewController svController;
	private int id;
	private TOMLayer tl;
	private LatencyMonitor latencyMonitor;
	private LatencyStorage latStorage;
	private boolean reconfigInExec = false;
	private boolean calcStarted = false;
	private ServerCommunicationSystem scs;
	private int windowSize; // use last windowSize latencies
	private int calculationInterval; // every x consensus the calculation is
										// started
	private long calcDuration;
	private int currentReceivedInternalConsensus;
	private LinkedBlockingQueue<byte[]> internalLatencies = new LinkedBlockingQueue<byte[]>();

	public DynamicWeightController(int id, ServerViewController svController) {
		this(id, svController, new DummyStorage());
	}

	public DynamicWeightController(int id, ServerViewController svController, LatencyMonitor latencyMonitor) {
		this.svController = svController;
		this.id = id;
		this.latencyMonitor = latencyMonitor;
		Thread latencyMonitorThread = new Thread(this.latencyMonitor, "LatencyMonitor");
		latencyMonitorThread.setPriority(Thread.NORM_PRIORITY - 1);
		latencyMonitorThread.start();

		this.latStorage = new LatencyStorage();
		this.windowSize = svController.getStaticConf().getUseLastMeasurements();
		this.calculationInterval = svController.getStaticConf().getCalculationInterval();

		Thread controllerThread = new Thread(this, "ControllerThread");
		controllerThread.setPriority(Thread.NORM_PRIORITY - 1);
		controllerThread.start();
	}

	public int getID() {
		return id;
	}

	@Override
	public void run() {
		currentReceivedInternalConsensus = 0;
		while (true) {
			byte[] data = internalLatencies.poll();
			if (data != null) {
				addToLatStorage(data);
				currentReceivedInternalConsensus++;
				// if n-f entries -> trigger calculation
				if (!calcStarted && currentReceivedInternalConsensus >= (svController.getCurrentViewN()
						- svController.getCurrentViewF())) {

					// wait for slower data
					calcStarted = true;
					System.out.println("n-f internal received- waiting for slower");
					try {
						Thread.sleep(5000);
						// check if slower data has arrived -> add
						if (internalLatencies.peek() != null) {
							while (internalLatencies.peek() != null) {
								data = internalLatencies.poll();
								addToLatStorage(data);
							}
						}
						Thread reconfigThread = new Thread(new Reconfigurator(latStorage, svController, this),
								"ReconfigurationThread");
						reconfigThread.setPriority(Thread.NORM_PRIORITY - 1);
						reconfigThread.start();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		}
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

	public int getLastExec() {
		return tl.getLastExec();
	}

	public synchronized void receiveExec(int exec) {
		// System.out.println("EXEC " + exec);
		if (exec % calculationInterval == 0 && exec != 0) {
			if (!reconfigInExec) {
				reconfigInExec = true;
				calcDuration = System.currentTimeMillis();
				System.out.println("---------------- Calculation started ----------------");
				Thread syncThread = new Thread(new Synchronizer(latencyMonitor, id, svController.getCurrentViewN(), scs,
						svController.getStaticConf()), "SynchronizationThread");
				syncThread.setPriority(Thread.NORM_PRIORITY - 1);
				syncThread.start();
			}
		}

	}

	public ServerCommunicationSystem getServerCommunicationSystem() {
		return scs;
	}

	public void setServerCommunicationSystem(ServerCommunicationSystem scs) {
		this.scs = scs;
	}

	public synchronized void addInternalConsensusDataToStorage(byte[] data) {
		internalLatencies.add(data);
		// try {
		// DataInputStream dis = new DataInputStream(new
		// ByteArrayInputStream(data));
		// if (svController.getStaticConf().measureClients()) {
		// int clientLength = dis.readInt();
		// byte[] serializedClientLat = new byte[clientLength];
		// dis.readFully(serializedClientLat);
		// ClientLatency[] clientLatencies =
		// SerializationUtils.deserialize(serializedClientLat);
		// Logger.println("received Client Latencies from internal conensus: " +
		// Arrays.toString(clientLatencies));
		//
		// latStorage.addClientLatencies(clientLatencies);
		// }
		//
		// if (svController.getStaticConf().measureServers()) {
		// int serverLength = dis.readInt();
		// byte[] serializedServerLat = new byte[serverLength];
		// dis.readFully(serializedServerLat);
		// ServerLatency[] serverLatencies =
		// SerializationUtils.deserialize(serializedServerLat);
		//
		// int serverProposeLength = dis.readInt();
		// byte[] serializedServerProposeLat = new byte[serverProposeLength];
		// dis.readFully(serializedServerProposeLat);
		// ServerLatency[] serverProposeLatencies =
		// SerializationUtils.deserialize(serializedServerProposeLat);
		// Logger.println("received Server Latencies from internal conensus: " +
		// Arrays.toString(serverLatencies));
		// Logger.println("received Server Propose Latencies from internal
		// conensus: "
		// + Arrays.toString(serverProposeLatencies));
		//
		// latStorage.addServerLatencies(serverLatencies);
		// latStorage.addServerProposeLatencies(serverProposeLatencies);
		// }
		//
		// // if n-f entries -> trigger calculation
		// if ((latStorage.getClientSize() >= (svController.getCurrentViewN() -
		// svController.getCurrentViewF())
		// || !svController.getStaticConf().measureClients())
		// && (latStorage.getServerSize() >= (svController.getCurrentViewN() -
		// svController.getCurrentViewF())
		// || !svController.getStaticConf().measureServers())
		// && (latStorage
		// .getServerProposeSize() >= (svController.getCurrentViewN() -
		// svController.getCurrentViewF())
		// || !svController.getStaticConf().measureServers())
		// && !calcStarted) {
		// // wait a bit??
		// calcStarted = true;
		// try {
		// Thread.sleep(5000);
		// Thread reconfigThread = new Thread(new Reconfigurator(latStorage,
		// svController, this),
		// "ReconfigurationThread");
		// reconfigThread.start();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// }
		//
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

	}

	public void notifyReconfigFinished() {
		// restart and clear everything for new Calc
		System.out.println("---------------- Calculation finished (duration: "
				+ (System.currentTimeMillis() - calcDuration) + "ms) ----------------");
		this.reconfigInExec = false;
		this.calcStarted = false;
		this.currentReceivedInternalConsensus = 0;

	}

	public int getWindowSize() {
		return windowSize;
	}

	private void addToLatStorage(byte[] data) {
		try {
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
			if (svController.getStaticConf().measureClients()) {
				int clientLength = dis.readInt();
				byte[] serializedClientLat = new byte[clientLength];
				dis.readFully(serializedClientLat);
				ClientLatency[] clientLatencies = SerializationUtils.deserialize(serializedClientLat);
				Logger.println("received Client Latencies from internal conensus:" + Arrays.toString(clientLatencies));

				latStorage.addClientLatencies(clientLatencies);
			}

			if (svController.getStaticConf().measureServers()) {
				int serverLength = dis.readInt();
				byte[] serializedServerLat = new byte[serverLength];
				dis.readFully(serializedServerLat);
				ServerLatency[] serverLatencies = SerializationUtils.deserialize(serializedServerLat);

				int serverProposeLength = dis.readInt();
				byte[] serializedServerProposeLat = new byte[serverProposeLength];
				dis.readFully(serializedServerProposeLat);
				ServerLatency[] serverProposeLatencies = SerializationUtils.deserialize(serializedServerProposeLat);
				Logger.println("received Server Latencies from internal conensus:" + Arrays.toString(serverLatencies));
				Logger.println("received Server Propose Latencies from internal conensus: "
						+ Arrays.toString(serverProposeLatencies));

				latStorage.addServerLatencies(serverLatencies);
				latStorage.addServerProposeLatencies(serverProposeLatencies);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
