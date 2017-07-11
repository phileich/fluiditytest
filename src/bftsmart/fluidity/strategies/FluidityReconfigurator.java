package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.DynamicWeightController;
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
    private DynamicWeightController dynamicWeightController;

    private FluidityGraph newFluidityGraph;


    public FluidityReconfigurator(DistributionStrategy strategy, ServerViewController svController,
                                  FluidityController fluidityController) {
        this.strategy = strategy;
        this.serverViewController = svController;
        this.fluidityController = fluidityController;
        this.dynamicWeightController = this.fluidityController.getDwc();
    }

    @Override
    public void run() {
        LatencyStorage latencyStorage = fluidityController.getDwc().getLatStorage();
        FluidityGraph filledFluidityGraph = fillGraphWithLatency(latencyStorage.getServerLatencies());

        newFluidityGraph = strategy.getReconfigGraph(filledFluidityGraph, dynamicWeightController.getBestWeightAssignment());

        fluidityController.notifyNewFluidityGraph(newFluidityGraph);
    }



    private FluidityGraph fillGraphWithLatency(List<Latency[]> serverLatencies) {
        FluidityGraph returnGraph = serverViewController.getCurrentView().getFluidityGraph();

        // This class first completes the latency information of the graph with the one from the latency
        // storage and then calls the strategy
        // TODO The above

        return returnGraph;
    }
}
