package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.reconfiguration.ServerViewController;

import java.util.Map;

/**
 * Created by philipp on 06.07.17.
 */
public interface DistributionStrategy {

    //calculate best distribution
    FluidityGraph calculateNewConfiguration(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment,
                                            LatencyStorage latencyStorage, int numberOfReplicasToMove,
                                            ServerViewController serverViewController);
}
