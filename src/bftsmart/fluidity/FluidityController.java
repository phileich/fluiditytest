package bftsmart.fluidity;

import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.LatencyMonitor;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphBuilder;
import bftsmart.reconfiguration.ServerViewController;

/**
 * This class is the main controller for the fluidity approach
 */
public class FluidityController implements Runnable {
    private DynamicWeightController dwc;
    private int replicaId;
    private ServerViewController svController;
    private LatencyMonitor latencyMonitor;
    private FluidityGraph fluidityGraph;

    public FluidityController(int id, ServerViewController svController, LatencyMonitor latencyMonitor,
                              DynamicWeightController dynamicWeightController,
                              FluidityGraph fluidityGraph) {
        this.replicaId = id;
        this.svController = svController;
        this.latencyMonitor = latencyMonitor;
        this.dwc = dynamicWeightController;
        this.fluidityGraph = fluidityGraph;


    }

    @Override
    public void run() {

    }


}
