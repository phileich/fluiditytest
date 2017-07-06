package bftsmart.fluidity.strategies;

import bftsmart.fluidity.graph.FluidityGraph;

import java.util.Map;

/**
 * Created by philipp on 06.07.17.
 */
public interface DistributionStrategy {

    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment);
}
