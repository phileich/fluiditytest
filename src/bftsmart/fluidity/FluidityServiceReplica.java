package bftsmart.fluidity;

import bftsmart.consensus.messages.MessageFactory;
import bftsmart.consensus.roles.Acceptor;
import bftsmart.consensus.roles.Proposer;
import bftsmart.dynamicWeights.DWServerCommunicationSystem;
import bftsmart.dynamicWeights.DWTOMLayer;
import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.LatencyMonitorPiggybackServer;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphBuilder;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.server.*;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import bftsmart.tom.util.ShutdownHookThread;
import bftsmart.tom.util.TOMUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by philipp on 19.06.17.
 */
public class FluidityServiceReplica extends ServiceReplica {

    class MessageContextPair {

        TOMMessage message;
        MessageContext msgCtx;

        MessageContextPair(TOMMessage message, MessageContext msgCtx) {
            this.message = message;
            this.msgCtx = msgCtx;
        }
    }

    private LatencyMonitorPiggybackServer lmps = null;
    private DynamicWeightController dwc = null;
    private FluidityController fc = null;
    private FluidityGraphBuilder fluidityGraphBuilder;
    private FluidityGraph fluidityGraph;

    /**
     * Constructor
     *
     * @param id        Replica ID
     * @param executor  Executor
     * @param recoverer Recoverer
     */
    public FluidityServiceReplica(int id, Executable executor, Recoverable recoverer) {
        this(id, "", executor, recoverer, null, new DefaultReplier());
    }

    /**
     * Constructor
     *
     * @param id        Replica ID
     * @param executor  Executor
     * @param recoverer Recoverer
     * @param verifier  Requests verifier
     */
    public FluidityServiceReplica(int id, Executable executor, Recoverable recoverer, RequestVerifier verifier) {
        this(id, "", executor, recoverer, verifier, new DefaultReplier());
    }

    /**
     * Constructor
     *
     * @param id        Replica ID
     * @param executor  Executor
     * @param recoverer Recoverer
     * @param verifier  Requests verifier
     * @param replier   Replier
     */
    public FluidityServiceReplica(int id, Executable executor, Recoverable recoverer, RequestVerifier verifier,
                                  Replier replier) {
        this(id, "", executor, recoverer, verifier, replier);
    }

    /**
     * Constructor
     *
     * @param id         Process ID
     * @param configHome Configuration directory for JBP
     * @param executor   Executor
     * @param recoverer  Recoverer
     * @param verifier   Requests verifier
     * @param replier    Replier
     */
    public FluidityServiceReplica(int id, String configHome, Executable executor, Recoverable recoverer,
                                  RequestVerifier verifier, Replier replier) {
        super();
        this.id = id;
        this.SVController = new ServerViewController(id, configHome);
        this.lmps = new LatencyMonitorPiggybackServer(this.SVController, this.id);
        this.dwc = new DynamicWeightController(this.id, this.SVController, this.lmps);
        this.executor = executor;
        this.recoverer = recoverer;
        this.replier = replier;
        this.verifier = verifier;

        this.fluidityGraphBuilder = new FluidityGraphBuilder();
        this.fluidityGraphBuilder.generateGraphFromXML(SVController.getXMLGraphPath);

        this.init();
        this.recoverer.setReplicaContext(replicaCtx);
        this.replier.setReplicaContext(replicaCtx);

        //TODO Initialize fluidity graph

    }

    // this method initializes the object

    protected void init() {
        try {
            cs = new DWServerCommunicationSystem(this.SVController, this, lmps, dwc);
            dwc.setServerCommunicationSystem(cs);
        } catch (Exception ex) {
            Logger.getLogger(ServiceReplica.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Unable to build a communication system.");
        }

        if (this.SVController.isInCurrentView()) {
            System.out.println("In current view: " + this.SVController.getCurrentView());
            initTOMLayer(); // initiaze the TOM layer
        } else {
            System.out.println("Not in current view: " + this.SVController.getCurrentView());

            // Not in the initial view, just waiting for the view where the join
            // has been executed
            System.out.println("Waiting for the TTP: " + this.SVController.getCurrentView());
            waitTTPJoinMsgLock.lock();
            try {
                canProceed.awaitUninterruptibly();
            } finally {
                waitTTPJoinMsgLock.unlock();
            }

        }
        initReplica();
    }

    public void receiveMessages(int consId[], int regencies[], int leaders[], CertifiedDecision[] cDecs,
                                TOMMessage[][] requests) {
        int numRequests = 0;
        int consensusCount = 0;
        List<TOMMessage> toBatch = new ArrayList<>();
        List<MessageContext> msgCtxts = new ArrayList<>();
        boolean noop = true;

        for (TOMMessage[] requestsFromConsensus : requests) {

            TOMMessage firstRequest = requestsFromConsensus[0];
            int requestCount = 0;
            noop = true;
            for (TOMMessage request : requestsFromConsensus) {

                if (request.getViewID() == SVController.getCurrentViewId()) {

                    if (request.getReqType() == TOMMessageType.ORDERED_REQUEST) {

                        noop = false;

                        numRequests++;
                        MessageContext msgCtx = new MessageContext(request.getSender(), request.getViewID(),
                                request.getReqType(), request.getSession(), request.getSequence(),
                                request.getOperationId(), request.getReplyServer(), request.serializedMessageSignature,
                                firstRequest.timestamp, request.numOfNonces, request.seed, regencies[consensusCount],
                                leaders[consensusCount], consId[consensusCount],
                                cDecs[consensusCount].getConsMessages(), firstRequest, false);

                        if (requestCount + 1 == requestsFromConsensus.length) {

                            msgCtx.setLastInBatch();
                        }
                        request.deliveryTime = System.nanoTime();
                        if (executor instanceof BatchExecutable) {

                            // This is used to deliver the content decided by a
                            // consensus instance directly to
                            // a Recoverable object. It is useful to allow the
                            // application to create a log and
                            // store the proof associated with decisions (which
                            // are needed by replicas
                            // that are asking for a state transfer).
                            if (this.recoverer != null)
                                this.recoverer.Op(msgCtx.getConsensusId(), request.getContent(), msgCtx);

                            // deliver requests and contexts to the executor
                            // later
                            msgCtxts.add(msgCtx);
                            toBatch.add(request);
                        } else if (executor instanceof FIFOExecutable) {

                            // This is used to deliver the content decided by a
                            // consensus instance directly to
                            // a Recoverable object. It is useful to allow the
                            // application to create a log and
                            // store the proof associated with decisions (which
                            // are needed by replicas
                            // that are asking for a state transfer).
                            if (this.recoverer != null)
                                this.recoverer.Op(msgCtx.getConsensusId(), request.getContent(), msgCtx);

                            // This is used to deliver the requests to the
                            // application and obtain a reply to deliver
                            // to the clients. The raw decision is passed to the
                            // application in the line above.
                            byte[] response = ((FIFOExecutable) executor).executeOrderedFIFO(request.getContent(),
                                    msgCtx, request.getSender(), request.getOperationId());

                            // Generate the messages to send back to the clients
                            request.reply = new TOMMessage(id, request.getSession(), request.getSequence(), response,
                                    SVController.getCurrentViewId());
                            bftsmart.tom.util.Logger.println(
                                    "(ServiceReplica.receiveMessages) sending reply to " + request.getSender());
                            replier.manageReply(request, msgCtx);
                        } else if (executor instanceof SingleExecutable) {

                            // This is used to deliver the content decided by a
                            // consensus instance directly to
                            // a Recoverable object. It is useful to allow the
                            // application to create a log and
                            // store the proof associated with decisions (which
                            // are needed by replicas
                            // that are asking for a state transfer).
                            if (this.recoverer != null)
                                this.recoverer.Op(msgCtx.getConsensusId(), request.getContent(), msgCtx);

                            // This is used to deliver the requests to the
                            // application and obtain a reply to deliver
                            // to the clients. The raw decision is passed to the
                            // application in the line above.
                            byte[] response = ((SingleExecutable) executor).executeOrdered(request.getContent(),
                                    msgCtx);

                            // Generate the messages to send back to the clients
                            request.reply = new TOMMessage(id, request.getSession(), request.getSequence(), response,
                                    SVController.getCurrentViewId());
                            bftsmart.tom.util.Logger.println(
                                    "(ServiceReplica.receiveMessages) sending reply to " + request.getSender());
                            replier.manageReply(request, msgCtx);
                        } else {
                            throw new UnsupportedOperationException("Interface not existent");
                        }
                    } else if (request.getReqType() == TOMMessageType.RECONFIG) {
                        SVController.enqueueUpdate(request);

                    } else if (request.getReqType() == TOMMessageType.INTERNAL_CONSENSUS) {
                        noop = false;

                        MessageContext msgCtx = new MessageContext(request.getSender(), request.getViewID(),
                                request.getReqType(), request.getSession(), request.getSequence(),
                                request.getOperationId(), request.getReplyServer(), request.serializedMessageSignature,
                                firstRequest.timestamp, request.numOfNonces, request.seed, regencies[consensusCount],
                                leaders[consensusCount], consId[consensusCount],
                                cDecs[consensusCount].getConsMessages(), firstRequest, false);

                        if (requestCount + 1 == requestsFromConsensus.length) {

                            msgCtx.setLastInBatch();
                        }

                        dwc.addInternalConsensusDataToStorage(request.getContent());

                        // Send the replies back to the client
                        byte[] replies = (new String("ConsensusStored")).getBytes();

                        request.reply = new TOMMessage(id, request.getSession(), request.getSequence(), replies,
                                SVController.getCurrentViewId(), TOMMessageType.INTERNAL_CONSENSUS);
                        replier.manageReply(request, msgCtx);
                    } else {
                        throw new RuntimeException("Should never reach here!");
                    }
                } else if (request.getViewID() < SVController.getCurrentViewId()) { // message
                    // sender
                    // had
                    // an
                    // old
                    // view,
                    // resend
                    // the
                    // message
                    // to
                    // him
                    // (but
                    // only
                    // if
                    // it
                    // came
                    // from
                    // consensus
                    // an
                    // not
                    // state
                    // transfer)

                    tomLayer.getCommunication().send(new int[]{request.getSender()},
                            new TOMMessage(SVController.getStaticConf().getProcessId(), request.getSession(),
                                    request.getSequence(), TOMUtil.getBytes(SVController.getCurrentView()),
                                    SVController.getCurrentViewId()));
                }
                requestCount++;
            }

            // This happens when a consensus finishes but there are no requests
            // to deliver
            // to the application. This can happen if a reconfiguration is
            // issued and is the only
            // operation contained in the batch. The recoverer must be notified
            // about this,
            // hence the invocation of "noop"
            if (noop && this.recoverer != null) {
                System.out.println(
                        " --- A consensus instance finished, but there were no commands to deliver to the application.");
                System.out.println(" --- Notifying recoverable about a blank consensus.");

                byte[][] batch = null;
                MessageContext[] msgCtx = null;
                if (requestsFromConsensus.length > 0) {
                    // Make new batch to deliver
                    batch = new byte[requestsFromConsensus.length][];
                    msgCtx = new MessageContext[requestsFromConsensus.length];

                    // Put messages in the batch
                    int line = 0;
                    for (TOMMessage m : requestsFromConsensus) {
                        batch[line] = m.getContent();

                        msgCtx[line] = new MessageContext(m.getSender(), m.getViewID(), m.getReqType(), m.getSession(),
                                m.getSequence(), m.getOperationId(), m.getReplyServer(), m.serializedMessageSignature,
                                firstRequest.timestamp, m.numOfNonces, m.seed, regencies[consensusCount],
                                leaders[consensusCount], consId[consensusCount],
                                cDecs[consensusCount].getConsMessages(), firstRequest, true);
                        msgCtx[line].setLastInBatch();

                        line++;
                    }
                }

                this.recoverer.noOp(consId[consensusCount], batch, msgCtx);

                // MessageContext msgCtx = new MessageContext(-1, -1, null, -1,
                // -1, -1, -1, null, // Since it is a noop, there is no need to
                // pass info about the client...
                // -1, 0, 0, regencies[consensusCount], leaders[consensusCount],
                // consId[consensusCount],
                // cDecs[consensusCount].getConsMessages(), //... but there is
                // still need to pass info about the consensus
                // null, true); // there is no command that is the first of the
                // batch, since it is a noop
                // msgCtx.setLastInBatch();

                // this.recoverer.noOp(msgCtx.getConsensusId(), msgCtx);
            }

            consensusCount++;
        }

        if (executor instanceof BatchExecutable && numRequests > 0) {
            // Make new batch to deliver
            byte[][] batch = new byte[numRequests][];

            // Put messages in the batch
            int line = 0;
            for (TOMMessage m : toBatch) {
                batch[line] = m.getContent();
                line++;
            }

            MessageContext[] msgContexts = new MessageContext[msgCtxts.size()];
            msgContexts = msgCtxts.toArray(msgContexts);

            // Deliver the batch and wait for replies
            byte[][] replies = ((BatchExecutable) executor).executeBatch(batch, msgContexts);

            // Send the replies back to the client
            for (int index = 0; index < toBatch.size(); index++) {
                TOMMessage request = toBatch.get(index);
                request.reply = new TOMMessage(id, request.getSession(), request.getSequence(), replies[index],
                        SVController.getCurrentViewId());

                if (SVController.getStaticConf().getNumRepliers() > 0) {
                    bftsmart.tom.util.Logger
                            .println("(ServiceReplica.receiveMessages) sending reply to " + request.getSender()
                                    + " with sequence number " + request.getSequence() + " via ReplyManager");
                    repMan.send(request);
                } else {
                    bftsmart.tom.util.Logger.println("(ServiceReplica.receiveMessages) sending reply to "
                            + request.getSender() + " with sequence number " + request.getSequence());
                    cs.send(new int[]{request.getSender()}, request.reply);
                }
            }
            // DEBUG
            bftsmart.tom.util.Logger.println("BATCHEXECUTOR END");
        }
    }

    /**
     * This method initializes the object
     *
     * @param cs   Server side communication System
     * @param conf Total order messaging configuration
     */
    protected void initTOMLayer() {
        if (tomStackCreated) { // if this object was already initialized, don't
            // do it again
            return;
        }

        if (!SVController.isInCurrentView()) {
            throw new RuntimeException("I'm not an acceptor!");
        }

        // Assemble the total order messaging layer
        MessageFactory messageFactory = new MessageFactory(id);

        Acceptor acceptor = new Acceptor(cs, messageFactory, SVController);
        cs.setAcceptor(acceptor);

        Proposer proposer = new Proposer(cs, messageFactory, SVController);

        ExecutionManager executionManager = new ExecutionManager(SVController, acceptor, proposer, id);

        acceptor.setExecutionManager(executionManager);

        //TODO Do ew need own TOMLayer?
        tomLayer = new DWTOMLayer(executionManager, this, recoverer, acceptor, cs, SVController, verifier, dwc, lmps);

        executionManager.setTOMLayer(tomLayer);

        SVController.setTomLayer(tomLayer);

        cs.setTOMLayer(tomLayer);
        cs.setRequestReceiver(tomLayer);

        acceptor.setTOMLayer(tomLayer);

        if (SVController.getStaticConf().isShutdownHookEnabled()) {
            Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(tomLayer));
        }
        tomLayer.start(); // start the layer execution
        tomStackCreated = true;

        replicaCtx = new ReplicaContext(cs, SVController);
    }
}


