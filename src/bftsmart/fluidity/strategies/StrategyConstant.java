package bftsmart.fluidity.strategies;

import bftsmart.fluidity.graph.FluidityGraph;

import java.util.Map;

/**
 * Created by eichhamp on 17.07.2017.
 */
public class StrategyConstant implements DistributionStrategy {
    @Override
    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment) {
        return null;
    }
}
