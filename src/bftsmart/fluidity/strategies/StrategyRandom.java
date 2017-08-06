package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.reconfiguration.ServerViewController;

import java.util.*;

/**
 * Created by philipp on 06.07.17.
 */
public class StrategyRandom implements DistributionStrategy {
    private FluidityGraph fluidityGraph;
    private Map<Integer, Double> bestWeightAssignment;
    private Random randomGenerator = new Random(1234);
    private ServerViewController svController;
    private int numOfReplicasToMove;

    /*
    0 = nodes that contain no replicas
    1 = nodes containing unmuted replicas, which were also unmuted before the bestweightcalculation
    2 = nodes containing unmuted replicas, which were muted before the bestweightcalculation
    3 = nodes containing muted replicas, which were also muted before the bestweightcalculation
    4 = nodes containing muted replicas, which were unmuted before the bestweightcalculation
     */
    private ArrayList<FluidityGraphNode>[] nodeCategory;

    public StrategyRandom() {
    }

    @Override
    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment,
                                          LatencyStorage latencyStorage, int numberOfReplicasToMove, ServerViewController serverViewController) {
        this.fluidityGraph = fluidityGraph;
        this.bestWeightAssignment = bestWeightAssignment;
        this.svController = serverViewController;
        this.numOfReplicasToMove = numberOfReplicasToMove;
        this.nodeCategory = new ArrayList[5];

        randomDistribution();

        return this.fluidityGraph;
    }

    private void randomDistribution() {
        ArrayList<Integer> newlyMutedReplicas = new ArrayList<>();
        ArrayList<Integer> replicasToRemove = new ArrayList<>();
        ArrayList<FluidityGraphNode> newNodes;
        ArrayList<Integer> newReplicas;

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                newlyMutedReplicas.add(processId);
            }
        }

        categorizeNodes();
        replicasToRemove = getReplicasToRemove(newlyMutedReplicas);

        // Small optimization to let old and new replicas running
        newNodes = getNodesForNewReplica(numOfReplicasToMove);
        for (int replicaId : replicasToRemove) {
            int nodeId = fluidityGraph.getNodeIdFromReplicaId(replicaId);
            FluidityGraphNode node = fluidityGraph.getNodeById(nodeId);
                if (newNodes.contains(node)) {
                    newNodes.remove(node);
                    replicasToRemove.remove(replicaId);
                }
        }

        // Delete the old replicas from the graph
        for (int repId : replicasToRemove) {
            fluidityGraph.removeReplicaFromNode(repId);
        }

        // Distribute new Replicas
        newReplicas = generateNewReplicas(replicasToRemove.size());

        // Add new replicas to the graph
        for (int proId : newReplicas) {
            FluidityGraphNode graphNode = newNodes.get(0);
            fluidityGraph.addReplicaToNode(graphNode, proId);
            newNodes.remove(0);
        }
    }

    private ArrayList<Integer> getReplicasToRemove(ArrayList<Integer> newlyMutedReplicas) {
        ArrayList<Integer> selectedReplicas = new ArrayList<>();

        for (int i = 0; i < numOfReplicasToMove; i++) {
            boolean nodeFound = false;

            while (!nodeFound) {
                int index = getRandomNumberForNode(newlyMutedReplicas.size());
                if (!selectedReplicas.contains(newlyMutedReplicas.get(index))) {
                    selectedReplicas.add(newlyMutedReplicas.get(index));
                    nodeFound = true;
                }
            }
        }

        return selectedReplicas;
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
        Set<FluidityGraphNode> tempPossibleNodes = new HashSet<>(nodeCategory[0]);
        tempPossibleNodes.addAll(nodeCategory[3]);
        tempPossibleNodes.addAll(nodeCategory[4]);
        ArrayList<FluidityGraphNode> possibleNodes = new ArrayList<>();
        possibleNodes.addAll(tempPossibleNodes);
        ArrayList<FluidityGraphNode> returnNodes = new ArrayList<>();

        for (int i = 0; i < numOfReplicas; i++) {
            boolean foundNode = false;
            while (!foundNode) {
                int nodeNr = getRandomNumberForNode(possibleNodes.size());
                FluidityGraphNode tempNode = possibleNodes.get(nodeNr);
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

    private int getRandomNumberForNode(int range) {
        return randomGenerator.nextInt(range);
    }

    private void categorizeNodes() {
        ArrayList<FluidityGraphNode> nodesOfGraph = fluidityGraph.getNodes();
        for (int i = 0; i < nodeCategory.length; i++) {
            nodeCategory[i] = new ArrayList<>();
        }

        for (FluidityGraphNode node : nodesOfGraph) {
            ArrayList<Integer> nodeReplicas = node.getReplicas();

            if (nodeReplicas.size() == 0) {
                nodeCategory[0].add(node);
            } else {
                double[] weightsOfReplicas = fluidityGraph.getWeightsOfReplicas(nodeReplicas);
                for (int i = 0; i < weightsOfReplicas.length; i++) {
                    if (weightsOfReplicas[i] == 0.0d) {
                        int rep = nodeReplicas.get(i);
                        double newWeight = bestWeightAssignment.get(rep);

                        if (newWeight == 0.0d) {
                            nodeCategory[3].add(node);
                            //i = weightsOfReplicas.length; //optimization
                        } else {
                            nodeCategory[2].add(node);
                            //i = weightsOfReplicas.length; //optimization
                        }

                    } else {
                        int rep = nodeReplicas.get(i);
                        double newWeight = bestWeightAssignment.get(rep);

                        if (newWeight == 0.0d) {
                            nodeCategory[4].add(node);
                        } else {
                            nodeCategory[1].add(node);
                        }
                    }
                }
            }
        }
    }
}
