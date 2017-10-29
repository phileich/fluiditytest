package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphEdge;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.reconfiguration.ServerViewController;

import java.util.*;

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
    private Random randomGenerator = new Random(1234);
    private boolean useGraph;

    /*
    0 = nodes that contain no replicas
    1 = nodes containing unmuted replicas, which were also unmuted before the bestweightcalculation
    2 = nodes containing unmuted replicas, which were muted before the bestweightcalculation
    3 = nodes containing muted replicas, which were also muted before the bestweightcalculation
    4 = nodes containing muted replicas, which were unmuted before the bestweightcalculation
     */
    private ArrayList<FluidityGraphNode>[] nodeCategory = new ArrayList[5];

    @Override
    public FluidityGraph calculateNewConfiguration(FluidityGraph fluidityGraph, Map<Integer, Double> bestWeightAssignment,
                                                   LatencyStorage latencyStorage, int numberOfReplicasToMove, ServerViewController serverViewController) {
        this.fluidityGraph = fluidityGraph;
        this.bestWeightAssignment = bestWeightAssignment;
        this.latencyStorage = latencyStorage;
        this.numberOfReplicasToMove = numberOfReplicasToMove;
        this.svController = serverViewController;

        numOfVariants = svController.getStaticConf().getNumberOfVariants();
        useGraph = true;
        replicaIds = this.fluidityGraph.getReplicasOfSystem();
        replicaIdsToReplace = new HashMap<>();
        oldReplicasToRemove = new ArrayList<>();
        variantsOfNewNodes = new ArrayList[numOfVariants];


        latencyDistribution();

        return this.fluidityGraph;
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

        // Get the new nodes for the system to place the new muted replicas
        newNodes = getNodesForNewReplica();

        // Delete old replicas from graph
        ArrayList<Integer> repToMove = getReplicaIDsToMove();

        for (int repId : repToMove) {
            fluidityGraph.removeReplicaFromNode(repId);
        }

        // Generate new muted replicas
        newReplicas = generateNewReplicas(numberOfReplicasToMove);

        // Add new replicas to the graph
        for (int proId : newReplicas) {
            FluidityGraphNode graphNode = newNodes.get(0);
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

    private ArrayList<FluidityGraphNode> getNodesForNewReplica() {
        ArrayList<FluidityGraphNode> nodeList = new ArrayList<>();
        int[] nodeNr = getPossibleNodeForGraph();

        for (int i = 0; i < numberOfReplicasToMove; i++) {
            nodeList.add(fluidityGraph.getNodeById(nodeNr[i]));
        }

        return nodeList;
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

        return variantsOfNewNodes[bestVariant];
    }

    private int[] getPossibleNodeForGraph() {
        ArrayList<NodeWeight> nodeWeights = new ArrayList<>();
        int[] sortedNodes;

        categorizeNodes();
        int[] newNodes = new int[nodeCategory[0].size()];
        ArrayList<FluidityGraphNode> possibleNodes = nodeCategory[0];

        for (FluidityGraphNode node : possibleNodes) {
            Vector<Double> latencyVector = assignLatencyVectorToNode(node);
            latencyVector.sort(new Comparator<Double>() {
                @Override
                public int compare(Double aDouble, Double t1) {
                    if (aDouble < t1) {
                        return -1;
                    } else if (aDouble > t1) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });

            double nodeLatency = latencyVector.get(svController.getOverlayQuorum() - 1);
            //TODO Add client latency
            nodeWeights.add(new NodeWeight(node.getNodeId(), nodeLatency));
        }

        Collections.sort(nodeWeights);
        sortedNodes = new int[nodeWeights.size()];

        for (int i = 0; i < nodeWeights.size(); i++) {
            sortedNodes[i] = nodeWeights.get(i).getNodeId();
        }

        return sortedNodes;
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

    private double assignWeightToNode(FluidityGraphNode node) {
        double weightOfNode = 0.0d;
        ArrayList<FluidityGraphNode> unmutedNodes = nodeCategory[1];
        unmutedNodes.addAll(nodeCategory[2]);
        Set<FluidityGraphNode> uniqueNodes = new HashSet<>(unmutedNodes);

        //TODO Check for other weight with newly unmuted replicas
        for (FluidityGraphNode unmutedNode : uniqueNodes) {
            double tempLatency;
            double tempLatency1 = fluidityGraph.getEdgeByNodes(node, unmutedNode).getLatencyValue();
            double tempLatency2 = fluidityGraph.getEdgeByNodes(unmutedNode, node).getLatencyValue();
            if (tempLatency1 != -1 && tempLatency2 != -1) {
                tempLatency = (tempLatency1 + tempLatency2) / 2;
            } else if (tempLatency1 != -1 || tempLatency2 != -1) {
                tempLatency = (tempLatency1 != -1 ? tempLatency1 : tempLatency2);
            } else {
                tempLatency = -1;
            }

            if (tempLatency >= 0) {
                tempLatency = tempLatency / getHighestWeightOfReplicas(unmutedNode.getReplicas());
                weightOfNode += tempLatency;
            } else {
                weightOfNode = -1.0d;
            }
        }


        return weightOfNode;
    }

    /**
     * Assigns the given node a vector consisting of its latencies.
     * @return
     */
    private Vector<Double> assignLatencyVectorToNode(FluidityGraphNode node) {
        Vector<Double> latencyVector = new Vector<>();
        ArrayList<FluidityGraphNode> unmutedNodes = nodeCategory[1];
        unmutedNodes.addAll(nodeCategory[2]);
        Set<FluidityGraphNode> uniqueNodes = new HashSet<>(unmutedNodes);

        // For every node get the latency information and the weights of the replicas to create the vector
        for (FluidityGraphNode unmutedNode : uniqueNodes) {
            List<Integer> replicasOfNode = unmutedNode.getReplicas();
            double nodeWeight = getSumOfReplicaWeights(replicasOfNode);

            double tempLatency1 = fluidityGraph.getEdgeByNodes(node, unmutedNode).getLatencyValue();
            double tempLatency2 = fluidityGraph.getEdgeByNodes(unmutedNode, node).getLatencyValue();
            double latency;
            if (tempLatency1 == -1.0d && tempLatency2 == -1.0d) {
                latency = -1;//TODO get a value for this case. Maybe get random nodes if there is no latency information
                // Add 150% of overall latency
            } else {
                latency = Math.max(tempLatency1, tempLatency2);
            }
//            else if (tempLatency1 == -1.0d) {
//                latency = tempLatency2 + tempLatency2;
//            } else if (tempLatency2 == -1.0d) {
//                latency = tempLatency1 + tempLatency1;
//            } else {
//                latency = tempLatency1 + tempLatency2;
//            }

            for (int i = 0; i < nodeWeight; i++) {
                latencyVector.add(latency);
            }
        }

        return latencyVector;

    }

    private double getSumOfReplicaWeights(List<Integer> replicas) {
        double weight = 0.0d;

        for (int replica : replicas) {
            weight =+ bestWeightAssignment.get(replica);
        }

        return weight;
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

    public double[][] getLantencyOfMutedReplica(ArrayList<Integer> replicasToReplace, int variant) {
        //Conversion of replicaToConversion to actual replica
        double[][] latencies = new double[replicaIds.length][replicaIds.length];

        for (int oldReplica : replicasToReplace) {
            for (int i = 0; i < variantsOfNewNodes[0].size(); i++) {
                replicaIdsToReplace.put(oldReplica, getOneOfNewNodes(variant, i));
                for (int otherReplica : replicaIds) {
                    FluidityGraphNode nodeStandard = fluidityGraph.getNodeById(fluidityGraph.getNodeIdFromReplicaId(otherReplica));
                    FluidityGraphNode nodeToReplace = replicaIdsToReplace.get(oldReplica);
                    FluidityGraphEdge fromEdge = fluidityGraph.getEdgeByNodes(nodeToReplace, nodeStandard);
                    FluidityGraphEdge toEdge = fluidityGraph.getEdgeByNodes(nodeStandard, nodeToReplace);

                    latencies[oldReplica][otherReplica] = fromEdge.getLatencyValue();
                    latencies[otherReplica][oldReplica] = toEdge.getLatencyValue();
                }
            }
        }
        return latencies;
    }

    private FluidityGraphNode getOneOfNewNodes(int variant, int nodeNr) {
        FluidityGraphNode tempNode = variantsOfNewNodes[variant].get(nodeNr);
        if (!replicaIdsToReplace.containsValue(tempNode)) {
            return tempNode;
        }

        return null;
    }

    public double getLantencyBetweenOthersAndMutedReplica(int replicaFrom, int replicaTo) {
        return fluidityGraph.getLatencyBetweenReplicas(replicaFrom,replicaTo);
    }

    public ArrayList<FluidityGraphNode> getRandomNodes() {
        ArrayList<FluidityGraphNode> possibleNodes = nodeCategory[0];
        ArrayList<FluidityGraphNode> selectedNodes = new ArrayList<>();

        int runUntil = Math.min(numberOfReplicasToMove, possibleNodes.size());
        for (int i = 0; i < runUntil; i++) {
            boolean notFound = true;
            while (notFound) {
                int randomNumber = getRandomNumberForReplica(possibleNodes.size());
                FluidityGraphNode tempNode = possibleNodes.get(randomNumber);
                if (!selectedNodes.contains(tempNode)) {
                    selectedNodes.add(possibleNodes.get(randomNumber));
                    notFound = false;
                }
            }
        }

        return selectedNodes;
    }

    private class NodeWeight implements Comparable<NodeWeight> {
        private int nodeId;
        private double weight;

        public NodeWeight(int nodeId, double weight) {
            this.nodeId = nodeId;
            this.weight = weight;
        }

        public int getNodeId() {
            return nodeId;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public int compareTo(NodeWeight nodeWeight) {
            if (this.weight < nodeWeight.getWeight()) {
                return -1;
            } else if (this.weight > nodeWeight.getWeight()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
