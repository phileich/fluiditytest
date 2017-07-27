package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.reconfiguration.ServerViewController;

import java.util.Map;

/**
 * Created by eichhamp on 17.07.2017.
 */
public class StrategyConstant implements DistributionStrategy {


    @Override
    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment,
                                          LatencyStorage latencyStorage, int numberOfReplicasToMove, ServerViewController serverViewController) {
        return fluidityGraph;
    }
}
