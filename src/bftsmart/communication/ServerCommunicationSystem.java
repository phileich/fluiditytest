/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.communication;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.communication.client.CommunicationSystemServerSideFactory;
import bftsmart.communication.client.RequestReceiver;
import bftsmart.communication.server.ServersCommunicationLayer;
import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.consensus.roles.Acceptor;
import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.LatencyMonitorPiggybackServer;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;

/**
 *
 * @author alysson
 */
public class ServerCommunicationSystem extends Thread {

	private boolean doWork = true;
	public final long MESSAGE_WAIT_TIME = 100;
	private LinkedBlockingQueue<SystemMessage> inQueue = null;
	protected MessageHandler messageHandler = new MessageHandler();
	private ServersCommunicationLayer serversConn;
	private CommunicationSystemServerSide clientsConn;
	private ServerViewController controller;
	private LatencyMonitorPiggybackServer lmps;
	private DynamicWeightController dwc;

	/**
	 * Creates a new instance of ServerCommunicationSystem
	 */
	public ServerCommunicationSystem(ServerViewController controller, ServiceReplica replica,
			LatencyMonitorPiggybackServer lmps, DynamicWeightController dwc) throws Exception {
		super("Server CS");

		this.controller = controller;
		this.lmps = lmps;
		this.dwc = dwc;

		inQueue = new LinkedBlockingQueue<SystemMessage>(controller.getStaticConf().getInQueueSize());

		// create a new conf, with updated port number for servers
		// TOMConfiguration serversConf = new
		// TOMConfiguration(conf.getProcessId(),
		// Configuration.getHomeDir(), "hosts.config");

		// serversConf.increasePortNumber();

		serversConn = new ServersCommunicationLayer(controller, inQueue, replica);

		// ******* EDUARDO BEGIN **************//
		// if (manager.isInCurrentView() || manager.isInInitView()) {
		clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
		// }
		// ******* EDUARDO END **************//
		// start();
	}

	// ******* EDUARDO BEGIN **************//
	public void joinViewReceived() {
		serversConn.joinViewReceived();
	}

	public void updateServersConnections() {
		this.serversConn.updateConnections();
		if (clientsConn == null) {
			clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
		}

	}

	// ******* EDUARDO END **************//
	public void setAcceptor(Acceptor acceptor) {
		messageHandler.setAcceptor(acceptor);
	}

	public void setTOMLayer(TOMLayer tomLayer) {
		messageHandler.setTOMLayer(tomLayer);
	}

	public void setRequestReceiver(RequestReceiver requestReceiver) {
		if (clientsConn == null) {
			clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
		}
		clientsConn.setRequestReceiver(requestReceiver);
	}

	/**
	 * Thread method responsible for receiving messages sent by other servers.
	 */
	@Override
	public void run() {

		long count = 0;
		while (doWork) {
			try {
				if (count % 1000 == 0 && count > 0) {
					Logger.println("(ServerCommunicationSystem.run) After " + count + " messages, inQueue size="
							+ inQueue.size());
				}

				SystemMessage sm = inQueue.poll(MESSAGE_WAIT_TIME, TimeUnit.MILLISECONDS);

				if (sm != null) {
					Logger.println("<-------receiving---------- " + sm);
					if ((sm instanceof ConsensusMessage) && ((ConsensusMessage) sm).getPaxosVerboseType() == "ACCEPT") {
						// store latency in volatile storage
						lmps.addServerLatency(sm.getSender(), ((ConsensusMessage) sm).getNumber());
					} else if ((sm instanceof ConsensusMessage)
							&& (((ConsensusMessage) sm).getPaxosVerboseType() == "PROPOSE"
									|| ((ConsensusMessage) sm).getPaxosVerboseType() == "DUMMY_PROPOSE")) {
						// send immediately back
						ConsensusMessage cm = new ConsensusMessage(MessageFactory.DUMMY_PROPOSE_RESPONSE,
								((ConsensusMessage) sm).getNumber(), 0, dwc.getID());
						Logger.println("--------sending----------> " + cm + " to " + sm.getSender());
						serversConn.send(new int[] { sm.getSender() }, cm, false);
					} else if ((sm instanceof ConsensusMessage)
							&& ((ConsensusMessage) sm).getPaxosVerboseType() == "DUMMY_PROPOSE_RESPONSE") {
						lmps.addServerProposeLatency(sm.getSender(), ((ConsensusMessage) sm).getNumber());
					}
					messageHandler.processData(sm);
					count++;
				} else {
					messageHandler.verifyPending();
				}
			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}
		}
		java.util.logging.Logger.getLogger(ServerCommunicationSystem.class.getName()).log(Level.INFO,
				"ServerCommunicationSystem stopped.");

	}

	/**
	 * Send a message to target processes. If the message is an instance of
	 * TOMMessage, it is sent to the clients, otherwise it is set to the
	 * servers.
	 *
	 * @param targets
	 *            the target receivers of the message
	 * @param sm
	 *            the message to be sent
	 */
	public void send(int[] targets, SystemMessage sm) {
		if (sm instanceof TOMMessage) {
			// Request Latencies from all clients for Dynamic Weight Calculation
			for (int i = 0; i < targets.length; i++) {
				// cause clientsConn.send needs an int[]
				int[] target = new int[1];
				target[0] = targets[i];
				((TOMMessage) sm).setDynamicWeightTimestamp(lmps.getClientTimestamp(targets[i]));
				((TOMMessage) sm).setConsensusID(dwc.getInExec());
				// remove from tmp storage to prevent overflow
				lmps.clearClientTimestamp(targets[i], dwc.getInExec());

				Logger.println("--------sending----------> " + sm + " to " + Arrays.toString(targets));
				clientsConn.send(target, (TOMMessage) sm, false);
			}
		} else {
			if (sm instanceof ConsensusMessage && ((ConsensusMessage) sm).getPaxosVerboseType() == "WRITE") {
				for (int i = 0; i < targets.length; i++) {
					// cause serversConn.send needs an int[]
					int[] target = new int[1];
					target[0] = targets[i];

					// ((ConsensusMessage)
					// sm).setDynamicWeightTimestamp(System.currentTimeMillis());

					// store latency in storage as sent
					lmps.createLatency(targets[i], ((ConsensusMessage) sm).getNumber());
					Logger.println("--------sending----------> " + sm + " to " + Arrays.toString(targets));
					serversConn.send(target, sm, true);
				}
			} else {
				Logger.println("--------sending----------> " + sm + " to " + Arrays.toString(targets));
				serversConn.send(targets, sm, true);
			}

		}
	}

	public ServersCommunicationLayer getServersConn() {
		return serversConn;
	}

	public CommunicationSystemServerSide getClientsConn() {
		return clientsConn;
	}

	@Override
	public String toString() {
		return serversConn.toString();
	}

	public void shutdown() {

		System.out.println("Shutting down communication layer");

		this.doWork = false;
		clientsConn.shutdown();
		serversConn.shutdown();
	}
}
