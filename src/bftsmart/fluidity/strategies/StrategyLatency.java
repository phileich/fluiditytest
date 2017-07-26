package bftsmart.fluidity.strategies;

import bftsmart.demo.microbenchmarks.LatencyServer;
import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.fluidity.strategies.WeightGraph.WeightGraphReconfigurator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by philipp on 06.07.17.
 */
public class StrategyLatency implements DistributionStrategy {
    private FluidityGraph fluidityGraph;
    private Map<Integer, Double> bestWeightAssignment;
    private LatencyStorage latencyStorage;
    private int numberOfReplicasToMove;

    /*
    0 = nodes that contain no replicas
    1 = nodes containing unmuted replicas, which were also unmuted before the bestweightcalculation
    2 = nodes containing unmuted replicas, which were muted before the bestweightcalculation
    3 = nodes containing muted replicas, which were also muted before the bestweightcalculation
    4 = nodes containing muted replicas, which were unmuted before the bestweightcalculation
     */
    private ArrayList<FluidityGraphNode>[] nodeCategory = new ArrayList[5];

    @Override
    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment, LatencyStorage latencyStorage, int numberOfReplicasToMove) {
        this.fluidityGraph = fluidityGraph;
        this.bestWeightAssignment = bestWeightAssignment;
        this.latencyStorage = latencyStorage;
        this.numberOfReplicasToMove = numberOfReplicasToMove;

        latencyDistribution();

        return this.fluidityGraph;
    }

    public void notifyReconfiguration(Map<Integer, Double> results) {

    }

    public ArrayList<Integer> getReplicaIDsToMove() {
        ArrayList<Integer> mutedReplicas = new ArrayList<>();

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                mutedReplicas.add(processId);
            }
        }

        return selectReplicasToMove(mutedReplicas);
    }

    private ArrayList<Integer> selectReplicasToMove(ArrayList<Integer> mutedReplicas) {
        ArrayList<Integer> selectedReplicas = new ArrayList<>();

        for (int i = 0; i < numberOfReplicasToMove; i++) {
            boolean foundReplica = false;
            while (!foundReplica) {
                int repId = getRandomNumberForReplica(bestWeightAssignment.size());
                if (mutedReplicas.contains(repId)) {
                    selectedReplicas.add(repId);
                    foundReplica = true;
                }
            }
        }

        return selectedReplicas;
    }

    private int getRandomNumberForReplica(int range) {
        //TODO Check if outcome is always the same for all replicas
        Random randomGenerator = new Random(1234);
        return randomGenerator.nextInt(range);
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

        // not needed any more
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

        int[] nodeNr = getPossibleNodeForGraph(numOfReplicas);

        //TODO call DWgraph for validation
        WeightGraphReconfigurator weightGraphReconfigurator = new WeightGraphReconfigurator(latencyStorage, this);

        return returnNodes;
    }

//    private int getRandomNumberForNode(int range) {
//        //TODO Check if outcome is always the same for all replicas
//        Random randomGenerator = new Random(1234);
//        return randomGenerator.nextInt(range);
//    }

    private int[] getPossibleNodeForGraph(int numOfRequiredNodes) {
        ArrayList<FluidityGraphNode> possibleNodes = new ArrayList<>();
        int[] newNodes = new int[numOfRequiredNodes];
        ArrayList<NodeWeights> nodeWeights = new ArrayList<>();

        categorizeNodes();
        possibleNodes = nodeCategory[0];

        if (possibleNodes.size() >= numOfRequiredNodes) {
            for (FluidityGraphNode node : possibleNodes) {
                double weight = assignWeightToNode(node);
                nodeWeights.add(new NodeWeights(node.getNodeId(), weight));
            }
        } else {
            return null;
        }

        Collections.sort(nodeWeights, new Comparator<NodeWeights>() { //TODO Check if correct
            @Override
            public int compare(NodeWeights lhs, NodeWeights rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return lhs.getWeight() > rhs.getWeight() ? -1 : (lhs.getWeight() < rhs.getWeight() ) ? 1 : 0;
            }
        });

        for (int i = 0; i < numOfRequiredNodes; i++) {
            newNodes[i] = nodeWeights.get(i).getNodeId();
        }

        return newNodes;
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

    private double assignWeightToNode(FluidityGraphNode node) {
        double weightOfNode = 0.0d;
        ArrayList<FluidityGraphNode> unmutedNodes = nodeCategory[1];
        unmutedNodes.addAll(nodeCategory[2]);
        Set<FluidityGraphNode> uniqueNodes = new HashSet<>(unmutedNodes);

        //TODO Check if other weight with newly unmuted replicas
        for (FluidityGraphNode unmutedNode : uniqueNodes) {
            double tempLatency1 = fluidityGraph.getEdgeByNodes(node, unmutedNode).getLatencyValue();
            double tempLatency2 = fluidityGraph.getEdgeByNodes(unmutedNode, node).getLatencyValue();
            double tempLatency = (tempLatency1 + tempLatency2) / 2; //TODO what if one latency is unknown (-1)

            if (tempLatency >= 0) {
                tempLatency = tempLatency / getHighestWeightOfReplicas(unmutedNode.getReplicas());
                weightOfNode += tempLatency;
            }
        }


        return weightOfNode;
    }

    private double getHighestWeightOfReplicas(ArrayList<Integer> replicas) {
        double[] weights = fluidityGraph.getWeightsOfReplicas(replicas);

        double maxWeight = 0;
        for (double val : weights) {
            if (val > maxWeight) {
                maxWeight = val;
            }
        }

        return maxWeight;
    }

    public Map<Integer, Double> getLantenciesOfMutedReplica(int replicas) {
        HashMap<Integer, Double> latenciesOfMutedReplica = new HashMap<>();

        fluidityGraph.getLatencyBetweenReplicas()

    }

    private class NodeWeights{
        private int nodeId;
        private double weight;

        public NodeWeights(int nodeId, double weight) {
            this.nodeId = nodeId;
            this.weight = weight;
        }

        public int getNodeId() {
            return nodeId;
        }

        public double getWeight() {
            return weight;
        }
    }
}
