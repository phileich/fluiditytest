package bftsmart.fluidity.strategies;

import bftsmart.fluidity.graph.FluidityGraph;

import java.util.Map;

/**
 * Created by philipp on 06.07.17.
 */
public class StrategyRandom implements DistributionStrategy {
    private FluidityGraph fluidityGraph;
    private Map<Integer, Double> bestWeightAssignment;

    public StrategyRandom() {
    }

    @Override
    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment) {
        this.fluidityGraph = fluidityGraph;
        this.bestWeightAssignment = bestWeightAssignment;

        


        return null;
    }
}
