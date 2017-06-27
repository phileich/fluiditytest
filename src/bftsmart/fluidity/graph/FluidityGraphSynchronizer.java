package bftsmart.fluidity.graph;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.dynamicWeights.InternalServiceProxy;

/**
 * Created by philipp on 26.06.17.
 */
public class FluidityGraphSynchronizer implements Runnable {
    private int replicaId;
    private FluidityGraph fluidityGraph;
    private ServerCommunicationSystem scs;

    public FluidityGraphSynchronizer(int replicaId, FluidityGraph fluidityGraph,
                                     ServerCommunicationSystem serverCommunicationSystem) {
        this.replicaId = replicaId;
        this.fluidityGraph = fluidityGraph;
        this.scs = serverCommunicationSystem;
    }

    @Override
    public void run() {
        InternalServiceProxy internalClient = new InternalServiceProxy(replicaId + 200);

    }
}
