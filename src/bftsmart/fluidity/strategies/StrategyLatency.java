package bftsmart.fluidity.strategies;

import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by philipp on 06.07.17.
 */
public class StrategyLatency implements DistributionStrategy {
    private FluidityGraph fluidityGraph;
    private Map<Integer, Double> bestWeightAssignment;

    /*
    0 = nodes that contain no replicas
    1 = nodes containing unmuted replicas, which were also unmuted before the bestweightcalculation
    2 = nodes containing unmuted replicas, which were muted before the bestweightcalculation
    3 = nodes containing muted replicas, which were also muted before the bestweightcalculation
    4 = nodes containing muted replicas, which were unmuted before the bestweightcalculation
     */
    private ArrayList<FluidityGraphNode>[] nodeCategory = new ArrayList[5];

    @Override
    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment) {
        this.fluidityGraph = fluidityGraph;
        this.bestWeightAssignment = bestWeightAssignment;

        latencyDistribution();

        return this.fluidityGraph;
    }

    private void latencyDistribution() {
        ArrayList<Integer> newlyMutedReplicas = new ArrayList<>();
        ArrayList<FluidityGraphNode> newNodes;
        ArrayList<Integer> newReplicas;

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                newlyMutedReplicas.add(processId);
            }
        }

        // Small optimization to let old and new replicas running
        newNodes = getNodesForNewReplica(newlyMutedReplicas.size());
        for (int replicaId : newlyMutedReplicas) {
            int nodeId = fluidityGraph.getNodeIdFromReplicaId(replicaId);
            FluidityGraphNode node = fluidityGraph.getNodeById(nodeId);
            if (newNodes.contains(node)) {
                newNodes.remove(node);
                newlyMutedReplicas.remove(replicaId);
            }
        }

        // Delete the old replicas from the graph
        for (int repId : newlyMutedReplicas) {
            fluidityGraph.removeReplicaFromNode(repId);
        }

        // Distribute new Replicas

        newReplicas = generateNewReplicas(newlyMutedReplicas.size());

        // Add new replicas to the graph
        for (int proId : newReplicas) {
            FluidityGraphNode graphNode = newNodes.get(0); //TODO Check if remove really shifts
            fluidityGraph.addReplicaToNode(graphNode, proId);
            newNodes.remove(0);
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
        ArrayList<FluidityGraphNode> nodesOfGraph = fluidityGraph.getNodes();
        ArrayList<FluidityGraphNode> returnNodes = new ArrayList<>();

        for (int i = 0; i < numOfReplicas; i++) {
            boolean foundNode = false;
            while (!foundNode) {
                int nodeNr = getPossibleNodeForGraph(nodesOfGraph.size());
                FluidityGraphNode tempNode = nodesOfGraph.get(nodeNr);
                if (fluidityGraph.checkForCapacity(tempNode)) {
                    if (!fluidityGraph.hasAlreadyUnmutedReplica(tempNode)) {
                        returnNodes.add(tempNode);
                        foundNode = true;
                    }
                }
            }
        }

        return returnNodes;
    }

//    private int getRandomNumberForNode(int range) {
//        //TODO Check if outcome is always the same for all replicas
//        Random randomGenerator = new Random(1234);
//        return randomGenerator.nextInt(range);
//    }

    private int getPossibleNodeForGraph() {
        
    }

    private void categorizeNodes() {
        ArrayList<FluidityGraphNode> nodesOfGraph = fluidityGraph.getNodes();
        for (ArrayList<FluidityGraphNode> nodeList : nodeCategory) {
            nodeList = new ArrayList<>();
        }

        for (FluidityGraphNode node : nodesOfGraph) {
            ArrayList<Integer> nodeReplicas = node.getReplicas();

            if (nodeReplicas.size() == 0) {
                nodeCategory[0].add(node);
            } else {
                double[] weightsOfReplicas = fluidityGraph.getWeightsOfReplicas(nodeReplicas);
                for (int i = 0; i < weightsOfReplicas.length; i++) {
                    if (weightsOfReplicas[i] == 0.0d) {
                        nodeReplicas.get(i); //Davon die neuen gewichte abfragen und anschließend in kategorie einteilen
                    }
                }
            }
        }



    }
}
