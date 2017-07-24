package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.graph.FluidityGraph;

import java.util.Map;

/**
 * Created by philipp on 06.07.17.
 */
public interface DistributionStrategy {

    FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment,
                                          LatencyStorage latencyStorage);
}
