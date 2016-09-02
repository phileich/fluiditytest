package bftsmart.dynamicWeights;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SerializationUtils;

import bftsmart.reconfiguration.ReconfigureReply;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.Extractor;
import bftsmart.tom.util.Logger;
import bftsmart.tom.util.TOMUtil;

public class DWServiceProxyLatencyMonitorPiggyBack extends DWServiceProxy {
	private LatencyMonitorPiggybackClient lmpc = null;
	private boolean measureClients;

	public DWServiceProxyLatencyMonitorPiggyBack(int processId) {
		this(processId, null, null, null);
	}

	public DWServiceProxyLatencyMonitorPiggyBack(int processId, String configHome) {
		this(processId, configHome, null, null);
	}

	public DWServiceProxyLatencyMonitorPiggyBack(int processId, String configHome, Comparator<byte[]> replyComparator,
			Extractor replyExtractor) {
		super(processId);
		lmpc = new LatencyMonitorPiggybackClient(processId, getViewManager());
		measureClients = getViewManager().getStaticConf().measureClients();
	}

	@Override
	public byte[] invokeOrdered(byte[] request) {
		return invoke(request, TOMMessageType.ORDERED_REQUEST);
	}

	@Override
	public byte[] invokeUnordered(byte[] request) {
		return invoke(request, TOMMessageType.UNORDERED_REQUEST);
	}

	@Override
	public byte[] invokeUnorderedHashed(byte[] request) {
		return invoke(request, TOMMessageType.UNORDERED_HASHED_REQUEST);
	}

	/**
	 * This method sends a request to the replicas, and returns the related
	 * reply. If the servers take more than invokeTimeout seconds the method
	 * returns null. This method is thread-safe.
	 *
	 * @param request
	 *            Request to be sent
	 * @param reqType
	 *            TOM_NORMAL_REQUESTS for service requests, and other for
	 *            reconfig requests.
	 * @return The reply from the replicas related to request
	 */
	@Override
	public byte[] invoke(byte[] request, TOMMessageType reqType) {
		canSendLock.lock();

		// Clean all statefull data to prepare for receiving next replies
		Arrays.fill(replies, null);
		receivedReplies = 0;
		response = null;
		replyQuorum = getReplyQuorum();

		// Send the request to the replicas, and get its ID
		reqId = generateRequestId(reqType);
		operationId = generateOperationId();
		requestType = reqType;

		replyServer = -1;
		hashResponseController = null;

		if (requestType == TOMMessageType.UNORDERED_HASHED_REQUEST) {

			replyServer = getRandomlyServerId();
			Logger.println("[" + this.getClass().getName() + "] replyServerId(" + replyServer + ") " + "pos("
					+ getViewManager().getCurrentViewPos(replyServer) + ")");

			hashResponseController = new HashResponseController(getViewManager().getCurrentViewPos(replyServer),
					getViewManager().getCurrentViewProcesses().length);

			TOMMessage sm = new TOMMessage(getProcessId(), getSession(), reqId, operationId, request,
					getViewManager().getCurrentViewId(), requestType);
			sm.setReplyServer(replyServer);
			// add all collected DWLatencies
			if (measureClients) {
				sm.setDynamicWeightTimestamp(System.currentTimeMillis());
				sm.setLatencyData(SerializationUtils.serialize((Serializable) lmpc.getClientLatencies()));
			}
			TOMulticast(sm);
		} else {
			if (measureClients) {
				TOMulticast(request, reqId, operationId, reqType,
						SerializationUtils.serialize((Serializable) lmpc.getClientLatencies()));
			} else {
				TOMulticast(request, reqId, operationId, reqType);
			}
		}

		Logger.println("Sending request (" + reqType + ") with reqId=" + reqId);
		Logger.println("Expected number of matching replies: " + replyQuorum);

		// This instruction blocks the thread, until a response is obtained.
		// The thread will be unblocked when the method replyReceived is invoked
		// by the client side communication system
		try {
			if (reqType == TOMMessageType.UNORDERED_HASHED_REQUEST) {
				if (!this.sm.tryAcquire(invokeUnorderedHashedTimeout, TimeUnit.SECONDS)) {
					System.out.println("######## UNORDERED HASHED REQUEST TIMOUT ########");
					canSendLock.unlock();
					return invoke(request, TOMMessageType.ORDERED_REQUEST);
				}
			} else {
				if (!this.sm.tryAcquire(invokeTimeout, TimeUnit.SECONDS)) {
					Logger.println("###################TIMEOUT#######################");
					Logger.println("Reply timeout for reqId=" + reqId);
					System.out.print(getProcessId() + " // " + reqId + " // TIMEOUT // ");
					System.out.println("Replies received: " + receivedReplies);
					canSendLock.unlock();

					return null;
				}
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		Logger.println("Response extracted = " + response);

		byte[] ret = null;

		if (response == null) {
			// the response can be null if n-f replies are received but there
			// isn't
			// a replyQuorum of matching replies
			Logger.println("Received n-f replies and no response could be extracted.");

			canSendLock.unlock();
			if (reqType == TOMMessageType.UNORDERED_REQUEST || reqType == TOMMessageType.UNORDERED_HASHED_REQUEST) {
				// invoke the operation again, whitout the read-only flag
				Logger.println("###################RETRY#######################");
				return invokeOrdered(request);
			} else {
				throw new RuntimeException("Received n-f replies without f+1 of them matching.");
			}
		} else {
			// normal operation
			// ******* EDUARDO BEGIN **************//
			if (reqType == TOMMessageType.ORDERED_REQUEST) {
				// Reply to a normal request!
				if (response.getViewID() == getViewManager().getCurrentViewId()) {
					ret = response.getContent(); // return the response
				} else {// if(response.getViewID() >
						// getViewManager().getCurrentViewId())
					// updated view received
					reconfigureTo((View) TOMUtil.getObject(response.getContent()));

					canSendLock.unlock();
					return invoke(request, reqType);
				}
			} else if (reqType == TOMMessageType.UNORDERED_REQUEST
					|| reqType == TOMMessageType.UNORDERED_HASHED_REQUEST) {
				if (response.getViewID() == getViewManager().getCurrentViewId()) {
					ret = response.getContent(); // return the response
				} else {
					canSendLock.unlock();
					return invoke(request, TOMMessageType.ORDERED_REQUEST);
				}
			} else {
				if (response.getViewID() > getViewManager().getCurrentViewId()) {
					// Reply to a reconfigure request!
					Logger.println("Reconfiguration request' reply received!");
					Object r = TOMUtil.getObject(response.getContent());
					if (r instanceof View) { // did not executed the request
												// because it is using an
												// outdated view
						reconfigureTo((View) r);

						canSendLock.unlock();
						return invoke(request, reqType);
					} else if (r instanceof ReconfigureReply) { // reconfiguration
																// executed!
						reconfigureTo(((ReconfigureReply) r).getView());
						ret = response.getContent();
					} else {
						Logger.println("Unknown response type");
					}
				} else {
					Logger.println("Unexpected execution flow");
				}
			}
		}
		// ******* EDUARDO END **************//

		canSendLock.unlock();
		return ret;
	}

	/**
	 * This is the method invoked by the client side communication system.
	 *
	 * @param reply
	 *            The reply delivered by the client side communication system
	 */
	@Override
	public void replyReceived(TOMMessage reply) {
		if (measureClients) {
			lmpc.addClientLatency(reply.getDynamicWeightTimestamp(), reply.getSender(), reply.getConsensusID());
		}
		super.replyReceived(reply);
	}

}
