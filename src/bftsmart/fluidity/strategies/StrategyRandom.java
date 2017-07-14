package bftsmart.fluidity.strategies;

import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

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

        randomDistribution();


        return this.fluidityGraph;
    }

    private void randomDistribution() {
        ArrayList<Integer> newlyMutedReplicas = new ArrayList<>();

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                newlyMutedReplicas.add(processId);
            }
            //TODO Set processId replica passive and randomly create a new replica all within the graph
            getNodesForNewReplica(newlyMutedReplicas.size());
        }


    }

    private ArrayList<FluidityGraphNode> getNodesForNewReplica(int numOfReplicas) {
        ArrayList<FluidityGraphNode> nodeOfGraph = fluidityGraph.getNodes();
        ArrayList<FluidityGraphNode> returnNodes = new ArrayList<>();

        for (int i = 0; i < numOfReplicas; i++) {
            boolean notYetFound = true;
            while (notYetFound) {
                int nodeNr = getRandomNumberForNode(nodeOfGraph.size());
                FluidityGraphNode tempNode = nodeOfGraph.get(nodeNr);
                // TODO check for already active or passive replicas in the node through fluidityGraph

                if (fluidityGraph.checkForCapacity(tempNode)) {
                    returnNodes.add(tempNode);
                    notYetFound = false;
                }
            }
        }

        return returnNodes;
    }

    private int getRandomNumberForNode(int range) {
        Random randomGenerator = new Random(1234);
        return randomGenerator.nextInt(range);
    }
}
