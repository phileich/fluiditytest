package bftsmart.fluidity.cloudconnection;

import bftsmart.dynamicWeights.Latency;
import bftsmart.dynamicWeights.MedianReducer;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.reconfiguration.ViewManager;
import bftsmart.tom.util.Logger;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CloudConnector {
    private int id;
    private FluidityGraph fluidityGraph;

    public CloudConnector(int id) {
        this.id = id;
    }

    public void start() {
    InternalServiceProxy internalClient = new InternalServiceProxy(id + 100);
    fluidityGraph = internalClient.getViewManager().getCurrentView().getFluidityGraph();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            DataOutputStream dos = new DataOutputStream(out);


            //byte[] serializedFluidityGraph = SerializationUtils.serialize(fluidityGraph);
            byte[] serializedFluidityGraph = (new String("FluidityConsensus")).getBytes();
            dos.writeInt(serializedFluidityGraph.length);
            dos.write(serializedFluidityGraph);


            byte[] reply = internalClient.invokeInternal(out.toByteArray());
            if (reply != null) {
                //Logger.println("Received Internal Consensus: " + new String(reply));
                //TODO For all replicas get enough correct graphs before proceeding
                FluidityGraph replyFluidityGraph = SerializationUtils.deserialize(reply);
                System.out.println("Oldfl: " + fluidityGraph.toString());
                System.out.println("--------------------------------");
                System.out.println("replyfl: " + replyFluidityGraph.toString());

                ViewManager.main(null);
                //TODO Extend view manager to change weights and fluidity graph

                //TODO Extend this client for giving cloud provider commands

            } else {
                Logger.println("Received Internal Consensus: NULL");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            internalClient.close();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("You have to specify an <id>");
            System.exit(-1);
        }

        CloudConnector cloudConnector = new CloudConnector(Integer.parseInt(args[0]));
        cloudConnector.start();
    }
}
