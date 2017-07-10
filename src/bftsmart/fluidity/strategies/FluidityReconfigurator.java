package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.Latency;
import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.FluidityController;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.reconfiguration.ServerViewController;

import java.util.List;

/**
 * Created by philipp on 06.07.17.
 */
public class FluidityReconfigurator implements Runnable {
    private DistributionStrategy strategy;
    private ServerViewController serverViewController;
    private FluidityController fluidityController;

    public FluidityReconfigurator(DistributionStrategy strategy, ServerViewController svController,
                                  FluidityController fluidityController) {
        this.strategy = strategy;
        this.serverViewController = svController;
        this.fluidityController = fluidityController;
    }

    @Override
    public void run() {
        LatencyStorage latencyStorage = fluidityController.getDwc().getLatStorage();
        fillGraphWithLatency(latencyStorage.getServerLatencies());

        // TODO Call strategy
    }



    private FluidityGraph fillGraphWithLatency(List<Latency[]> serverLatencies) {
        FluidityGraph returnGraph = serverViewController.getCurrentView().getFluidityGraph();

        // This class first completes the latency information of the graph with the one from the latency
        // storage and then calls the strategy
        // TODO The above

        return returnGraph;
    }
}
