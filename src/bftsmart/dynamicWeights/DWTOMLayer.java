package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.commons.lang3.SerializationUtils;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Decision;
import bftsmart.consensus.Epoch;
import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.consensus.roles.Acceptor;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.RequestVerifier;
import bftsmart.tom.util.Logger;

public class DWTOMLayer extends TOMLayer {
	private DynamicWeightController dwController;
	private LatencyMonitorPiggybackServer lmps;

	public DWTOMLayer(ExecutionManager manager, ServiceReplica receiver, Recoverable recoverer, Acceptor a,
			ServerCommunicationSystem cs, ServerViewController controller, RequestVerifier verifier,
			DynamicWeightController dwController, LatencyMonitorPiggybackServer lmps) {

		super(manager, receiver, recoverer, a, cs, controller, verifier);
		this.dwController = dwController;
		this.dwController.setTOMLayer(this);
		this.lmps = lmps;
	}

	/**
	 * Sets which consensus was the last to be executed
	 *
	 * @param last
	 *            ID of the consensus which was last to be executed
	 */
	@Override
	public void setLastExec(int last) {
		dwController.receiveExec(last);
		this.lastExecuted = last;
	}

	/**
	 * This method is invoked by the communication system to deliver a request.
	 * It assumes that the communication system delivers the message in FIFO
	 * order.
	 *
	 * @param msg
	 *            The request being received
	 */
	@Override
	public void requestReceived(TOMMessage msg) {
		long currTimestamp = System.currentTimeMillis();
		if (!doWork)
			return;

		// check if this request is valid and add it to the client' pending
		// requests list
		boolean readOnly = (msg.getReqType() == TOMMessageType.UNORDERED_REQUEST
				|| msg.getReqType() == TOMMessageType.UNORDERED_HASHED_REQUEST);
		if (readOnly) {
			dt.deliverUnordered(msg, syncher.getLCManager().getLastReg());
		} else {
			if (clientsManager.requestReceived(msg, true, communication)) {
				haveMessages();
			} else {
				Logger.println("(TOMLayer.requestReceive) the received TOMMessage " + msg + " was discarded.");
			}
		}
		if (controller.getStaticConf().measureClients()) {
			if (msg.getLatencyData() != null) {
				Logger.println("received TOM with latenciesData: ");
				ArrayList<ClientLatency> cls = SerializationUtils.deserialize(msg.getLatencyData());
				lmps.storeClientLatencies(cls);
			}
			lmps.storeClientTimestamp(msg.getDynamicWeightTimestamp(), currTimestamp, msg.getSender());
		}
		if (controller.getStaticConf().measureServers()
				&& ((dwController.getLastExec() + 1)
						% this.controller.getStaticConf().getServerMeasurementInterval() == 0)
				&& (execManager.getCurrentLeader() != this.controller.getStaticConf().getProcessId())) {
			// create Dummy Propose
			System.out.println("Will send dummy request " + (dwController.getLastExec() + 1));
			lmps.createProposeLatencies(lmps.getCurrentViewOtherAcceptors(), dwController.getLastExec() + 1);
			communication.send(lmps.getCurrentViewAcceptors(), new ConsensusMessage(MessageFactory.DUMMY_PROPOSE,
					dwController.getInExec() + 1, 0, dwController.getID()));
		}

	}

	/**
	 * This is the main code for this thread. It basically waits until this
	 * replica becomes the leader, and when so, proposes a value to the other
	 * acceptors
	 */
	@Override
	public void run() {
		Logger.println("Running."); // TODO: can't this be outside of the loop?
		while (doWork) {

			// blocks until this replica learns to be the leader for the current
			// epoch of the current consensus
			leaderLock.lock();
			Logger.println("Next leader for CID=" + (getLastExec() + 1) + ": " + execManager.getCurrentLeader());

			// ******* EDUARDO BEGIN **************//
			if (execManager.getCurrentLeader() != this.controller.getStaticConf().getProcessId()) {
				iAmLeader.awaitUninterruptibly();
				// waitForPaxosToFinish();
			}
			// ******* EDUARDO END **************//
			leaderLock.unlock();

			if (!doWork)
				break;

			// blocks until the current consensus finishes
			proposeLock.lock();

			if (getInExec() != -1) { // there is some consensus running
				Logger.println("(TOMLayer.run) Waiting for consensus " + getInExec() + " termination.");
				canPropose.awaitUninterruptibly();
			}
			proposeLock.unlock();

			if (!doWork)
				break;

			Logger.println("(TOMLayer.run) I'm the leader.");

			// blocks until there are requests to be processed/ordered
			messagesLock.lock();
			if (!clientsManager.havePendingRequests()) {
				haveMessages.awaitUninterruptibly();
			}
			messagesLock.unlock();

			if (!doWork)
				break;

			Logger.println("(TOMLayer.run) There are messages to be ordered.");

			Logger.println("(TOMLayer.run) I can try to propose.");

			if ((execManager.getCurrentLeader() == this.controller.getStaticConf().getProcessId()) && // I'm
																										// the
																										// leader
					(clientsManager.havePendingRequests()) && // there are
																// messages to
																// be ordered
					(getInExec() == -1)) { // there is no consensus in execution

				// Sets the current consensus
				int execId = getLastExec() + 1;
				setInExec(execId);

				Decision dec = execManager.getConsensus(execId).getDecision();

				// Bypass protocol if service is not replicated
				if (controller.getCurrentViewN() == 1) {

					Logger.println("(TOMLayer.run) Only one replica, bypassing consensus.");

					byte[] value = createPropose(dec);

					Consensus consensus = execManager.getConsensus(dec.getConsensusId());
					Epoch epoch = consensus.getEpoch(0, controller);
					epoch.propValue = value;
					epoch.propValueHash = computeHash(value);
					epoch.getConsensus().addWritten(value);
					epoch.deserializedPropValue = checkProposedValue(value, true);
					epoch.getConsensus().getDecision().firstMessageProposed = epoch.deserializedPropValue[0];
					dec.setDecisionEpoch(epoch);

					// System.out.println("ESTOU AQUI!");
					dt.delivery(dec);
					continue;

				}
				if (controller.getStaticConf().measureServers()) {
					lmps.createProposeLatencies(controller.getCurrentViewOtherAcceptors(), getInExec());
				}
				execManager.getProposer().startConsensus(execId, createPropose(dec));
			}
		}
		java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.INFO, "TOMLayer stopped.");
	}

}
