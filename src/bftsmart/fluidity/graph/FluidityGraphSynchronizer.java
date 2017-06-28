package bftsmart.fluidity.graph;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.dynamicWeights.InternalServiceProxy;
import bftsmart.dynamicWeights.Latency;
import bftsmart.tom.util.Logger;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            DataOutputStream dos = new DataOutputStream(out);

            ArrayList<FluidityGraphNode> nodes = fluidityGraph.getNodes();
            ArrayList<FluidityGraphEdge> edges = fluidityGraph.getEdges();

            byte[] serializeFluidityNodes = SerializationUtils.serialize(nodes);
            byte[] serializeFluidityEdges = SerializationUtils.serialize(edges);

            dos.writeInt(serializeFluidityNodes.length);
            dos.write(serializeFluidityNodes);

            dos.writeInt(serializeFluidityEdges.length);
            dos.write(serializeFluidityEdges);


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
