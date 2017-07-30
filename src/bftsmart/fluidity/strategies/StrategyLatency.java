package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphEdge;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.fluidity.strategies.WeightGraph.WeightGraphReconfigurator;
import bftsmart.reconfiguration.ServerViewController;

import java.util.*;

/**
 * Created by philipp on 06.07.17.
 */
public class StrategyLatency implements DistributionStrategy {
    private FluidityGraph fluidityGraph;
    private Map<Integer, Double> bestWeightAssignment;
    private LatencyStorage latencyStorage;
    private int numberOfReplicasToMove;
    private int[] replicaIds;
    private ServerViewController svController;
    private Map<Integer, FluidityGraphNode> replicaIdsToReplace;
    private ArrayList<FluidityGraphNode> newNodes;
    private ArrayList<FluidityGraphNode>[] variantsOfNewNodes;
    private ArrayList<Integer> oldReplicasToRemove;
    private int numOfVariants;

    /*
    0 = nodes that contain no replicas
    1 = nodes containing unmuted replicas, which were also unmuted before the bestweightcalculation
    2 = nodes containing unmuted replicas, which were muted before the bestweightcalculation
    3 = nodes containing muted replicas, which were also muted before the bestweightcalculation
    4 = nodes containing muted replicas, which were unmuted before the bestweightcalculation
     */
    private ArrayList<FluidityGraphNode>[] nodeCategory = new ArrayList[5];

    @Override
    public FluidityGraph getReconfigGraph(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment,
                                          LatencyStorage latencyStorage, int numberOfReplicasToMove, ServerViewController serverViewController) {
        this.fluidityGraph = fluidityGraph;
        this.bestWeightAssignment = bestWeightAssignment;
        this.latencyStorage = latencyStorage;
        this.numberOfReplicasToMove = numberOfReplicasToMove;
        this.svController = serverViewController;

        numOfVariants = 3;
        replicaIds = this.fluidityGraph.getReplicasOfSystem();
        replicaIdsToReplace = new HashMap<>();
        oldReplicasToRemove = new ArrayList<>();
        variantsOfNewNodes = new ArrayList[numOfVariants];


        latencyDistribution();

        return this.fluidityGraph;
    }

    public void notifyReconfiguration(Map<Integer, Double> results) {
        //TODO evaluate the result and return the fluiditygraph

    }

    /**
     * Return the old muted replicas, which shall be deleted form the
     * FluidityGraph
     * @return
     */
    public ArrayList<Integer> getReplicaIDsToMove() {
        ArrayList<Integer> mutedReplicas = new ArrayList<>();

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                mutedReplicas.add(processId);
            }
        }

        return selectReplicasToMove(mutedReplicas);
    }

    //TODO Use a weight function?
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
        ArrayList<Integer> newReplicas;

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                newlyMutedReplicas.add(processId);
            }
        }

        // Small optimization to let old and new replicas running
        newNodes = getNodesForNewReplica(numberOfReplicasToMove);

        // not needed any more
//        for (int replicaId : newlyMutedReplicas) {
//            int nodeId = fluidityGraph.getNodeIdFromReplicaId(replicaId);
//            FluidityGraphNode node = fluidityGraph.getNodeById(nodeId);
//            if (newNodes.contains(node)) {
//                newNodes.remove(node);
//                newlyMutedReplicas.remove(replicaId);
//            }
//        }

        // Delete the old replicas from the graph
        for (int repId : replicaIdsToReplace.keySet()) {
            fluidityGraph.removeReplicaFromNode(repId);
        }

        newReplicas = generateNewReplicas(numberOfReplicasToMove);

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

        int[] nodeNr = getPossibleNodeForGraph(numOfReplicas); //TODO Check for null

        //TODO Create Variants here
        int offset = 0;
        for (int i = 0; i < numOfVariants; i++) {
            variantsOfNewNodes[i] = new ArrayList<>();
            for (int j = 0; j < numberOfReplicasToMove; j++) {
                if (j+offset < nodeNr.length) {
                    FluidityGraphNode node = fluidityGraph.getNodeById(nodeNr[j + offset]);
                    variantsOfNewNodes[i].add(node);
                } //TODO What happens when there are not enough new possible nodes?
            }
        }

        oldReplicasToRemove = getReplicaIDsToMove();
        //TODO Give Graph variant here so it does not get itself later in the process
        //TODO change 3 in for loop
        Map<Integer, Double>[] bestAssignment = new Map[3];
        for (int i = 0; i < 3; i++) {
            WeightGraphReconfigurator weightGraphReconfigurator = new WeightGraphReconfigurator(svController,
                    latencyStorage, this, replicaIds.length);
            bestAssignment[i] = weightGraphReconfigurator.runGraph(oldReplicasToRemove);
        }

        return getBestNodes(bestAssignment);
    }

    private ArrayList<FluidityGraphNode> getBestNodes(Map<Integer, Double>[] bestAssignments) {
        int bestVariant = 0;
        double maxWeight = 0;

        for (int i = 0; i < bestAssignments.length; i++) {
            double weights = 0;
            for (int replicas : oldReplicasToRemove) {
                weights += bestAssignments[i].get(replicas);
            }
            if (weights > maxWeight) {
                bestVariant = i;
            }
        }

        return getNodesForVariant(bestVariant);
    }

    private ArrayList<FluidityGraphNode> getNodesForVariant(int bestVariant) {
        //TODO return nodes for given variant number
    }

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

        for (int i = 0; i < nodeWeights.size(); i++) {
            newNodes[i] = nodeWeights.get(i).getNodeId();
        }

        //variantsOfNewNodes = newNodes;
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

    public double[] getLantencyOfMutedReplica(int replicaToReplace, int replicaStandard) {
        //Conversion of replicaToConversion to actual replica
        replicaIdsToReplace.put(replicaToReplace, getOneOfNewNodes());
        FluidityGraphNode nodeStandard = fluidityGraph.getNodeById(fluidityGraph.getNodeIdFromReplicaId(replicaStandard));
        FluidityGraphNode nodeToReplace = replicaIdsToReplace.get(replicaToReplace);
        FluidityGraphEdge toEdge = fluidityGraph.getEdgeByNodes(nodeStandard, nodeToReplace);
        FluidityGraphEdge fromEdge = fluidityGraph.getEdgeByNodes(nodeToReplace, nodeStandard);

        return new double[]{fromEdge.getLatencyValue(), toEdge.getLatencyValue()};
    }

    private FluidityGraphNode getOneOfNewNodes() {
        //TODO Generate the Variants here
        //TODO Problem with newNodes since they are not complete up until there
        // Global view of variants needed here
        for (int i = 0; i < variantsOfNewNodes.length; i++) {
            FluidityGraphNode tempNode = fluidityGraph.getNodeById(variantsOfNewNodes[i]);
            if (!replicaIdsToReplace.containsValue(tempNode)) {
                return tempNode;
            }
        }

        return null;
    }

    public double getLantencyBetweenOthersAndMutedReplica(int replicaFrom, int replicaTo) {
        return fluidityGraph.getLatencyBetweenReplicas(replicaFrom,replicaTo);
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
