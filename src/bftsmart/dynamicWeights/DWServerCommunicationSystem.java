package bftsmart.dynamicWeights;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.sound.midi.ControllerEventListener;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.communication.SystemMessage;
import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.Logger;

public class DWServerCommunicationSystem extends ServerCommunicationSystem {
	private LatencyMonitorPiggybackServer lmps;
	private DynamicWeightController dwc;

	public DWServerCommunicationSystem(ServerViewController controller, ServiceReplica replica,
			LatencyMonitorPiggybackServer lmps, DynamicWeightController dwc) throws Exception {
		super(controller, replica);

		this.lmps = lmps;
		this.dwc = dwc;
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
					if ((sm instanceof ConsensusMessage) && ((ConsensusMessage) sm).getPaxosVerboseType() == "ACCEPT"
							&& controller.getStaticConf().measureServers()) {
						// store latency in volatile storage
						lmps.addServerLatency(sm.getSender(), ((ConsensusMessage) sm).getNumber());
						messageHandler.processData(sm);
						count++;
					} else if ((sm instanceof ConsensusMessage)
							&& (((ConsensusMessage) sm).getPaxosVerboseType() == "PROPOSE"
									|| ((ConsensusMessage) sm).getPaxosVerboseType() == "DUMMY_PROPOSE")
							&& controller.getStaticConf().measureServers()) {
						// send immediately back
						ConsensusMessage cm = new ConsensusMessage(MessageFactory.DUMMY_PROPOSE_RESPONSE,
								((ConsensusMessage) sm).getNumber(), 0, dwc.getID());
						Logger.println("--------sending----------> " + cm + " to " + sm.getSender());
						serversConn.send(new int[] { sm.getSender() }, cm, false);

						if (((ConsensusMessage) sm).getPaxosVerboseType() == "PROPOSE") {
							messageHandler.processData(sm);
							count++;
						}

					} else if ((sm instanceof ConsensusMessage)
							&& ((ConsensusMessage) sm).getPaxosVerboseType() == "DUMMY_PROPOSE_RESPONSE"
							&& controller.getStaticConf().measureServers()) {
						lmps.addServerProposeLatency(sm.getSender(), ((ConsensusMessage) sm).getNumber());
					} else {
						messageHandler.processData(sm);
						count++;
					}

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
				if (controller.getStaticConf().measureClients()) {				//TODO no internal consensus -> latency!
					((TOMMessage) sm).setDynamicWeightTimestamp(lmps.getClientTimestamp(targets[i]));
					((TOMMessage) sm).setConsensusID(dwc.getInExec());
					// remove from tmp storage to prevent overflow
					lmps.clearClientTimestamp(targets[i], dwc.getInExec());
				}

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
					if (controller.getStaticConf().measureServers()) {
						lmps.createServerLatency(targets[i], ((ConsensusMessage) sm).getNumber());
					}
					Logger.println("--------sending----------> " + sm + " to " + Arrays.toString(targets));
					serversConn.send(target, sm, true);
				}
			} else {
				Logger.println("--------sending----------> " + sm + " to " + Arrays.toString(targets));
				serversConn.send(targets, sm, true);
			}

		}
	}
}
