package bftsmart.fluidity.strategies;

import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;

import java.util.ArrayList;
import java.util.Collections;
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
        ArrayList<FluidityGraphNode> newNodes;
        ArrayList<Integer> newReplicas;

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                newlyMutedReplicas.add(processId);
            }
        }

        // Set processId replica passive and randomly create new replicas all within the graph



        // Distribute new Replicas
        newNodes = getNodesForNewReplica(newlyMutedReplicas.size());

        // Small optimization to let old and new replicas running
        for (int replicaId : newlyMutedReplicas) {
            int nodeId = fluidityGraph.getNodeIdFromReplicaId(replicaId);
            FluidityGraphNode node = fluidityGraph.getNodeById(nodeId);
                if (newNodes.contains(node)) {
                    newNodes.remove(node); // TODO Make sure that only one muted replica per node
                    newlyMutedReplicas.remove(replicaId);
                }
        }

        newReplicas = generateNewReplicas(newlyMutedReplicas.size());

        // Add new replicas to the graph
        for (int proId : newReplicas) {
            FluidityGraphNode graphNode = newNodes.get(0); //TODO Check if remove really shifts
            fluidityGraph.addReplicaToNode(graphNode, proId);
            newNodes.remove(0);
        }

        // Delete the old replicas from the graph
        for (int repId : newlyMutedReplicas) {
            fluidityGraph.removeReplicaFromNode(repId);
        }

    }

    private ArrayList<FluidityGraphNode> getNodesByReplicaId(ArrayList<Integer> replicas) {
        ArrayList<FluidityGraphNode> oldNodes = new ArrayList<>();

        for (int replicaId : replicas) {
            int nodeId = fluidityGraph.getNodeIdFromReplicaId(replicaId);
            oldNodes.add(fluidityGraph.getNodeById(nodeId));
        }

        return oldNodes;
    }

    private ArrayList<Integer> generateNewReplicas(int size) {
        ArrayList<Integer> newReplicas = new ArrayList<>();

        int maxProcessId = Collections.max(bestWeightAssignment.keySet());

        for (int i = 0; i < size; i++) {
            newReplicas.add(maxProcessId + 1);
            maxProcessId++;
        }

        return newReplicas;
    }

    private ArrayList<FluidityGraphNode> getNodesForNewReplica(int numOfReplicas) {
        ArrayList<FluidityGraphNode> nodeOfGraph = fluidityGraph.getNodes();
        ArrayList<FluidityGraphNode> returnNodes = new ArrayList<>();

        for (int i = 0; i < numOfReplicas; i++) {
            boolean foundNode = false;
            while (!foundNode) {
                int nodeNr = getRandomNumberForNode(nodeOfGraph.size());
                FluidityGraphNode tempNode = nodeOfGraph.get(nodeNr);
                // TODO If numOfReplicas is higher than the number of available data centers, then we are in a loop


                if (fluidityGraph.checkForCapacity(tempNode)) {
                    if (!fluidityGraph.hasAlreadyActiveReplica(tempNode)) {
                        returnNodes.add(tempNode);
                        foundNode = true;
                    }
                }
            }
        }

        return returnNodes;
    }

    private int getRandomNumberForNode(int range) {
        //TODO Check if outcome is always the same for all replicas
        Random randomGenerator = new Random(1234);
        return randomGenerator.nextInt(range);
    }
}
