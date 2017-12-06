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
public class StrategyWeightGraph implements DistributionStrategy {
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


        weightGraphDistribution();

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

    private void weightGraphDistribution() {
        ArrayList<Integer> newlyMutedReplicas = new ArrayList<>();
        ArrayList<Integer> newReplicas;

        for (int processId : bestWeightAssignment.keySet()) {
            if (bestWeightAssignment.get(processId) == 0) {
                newlyMutedReplicas.add(processId);
            }
        }

        // Get the new nodes for the system to place the new muted replicas
        newNodes = getNodesForNewReplica();
        int actualNumberOfReplicasToMove = newNodes.size();

        // Delete the old replicas from the graph
        if (actualNumberOfReplicasToMove != numberOfReplicasToMove) {
            int[] actualReplicasToReplace = new int[actualNumberOfReplicasToMove];
            Iterator<Integer> keyIterator = replicaIdsToReplace.keySet().iterator();
            for (int i = 0; i < actualReplicasToReplace.length; i++) {
                actualReplicasToReplace[i] = keyIterator.next();
            }

            for (int repId : actualReplicasToReplace) {
                fluidityGraph.removeReplicaFromNode(repId);
            }
        } else {
            for (int repId : replicaIdsToReplace.keySet()) {
                fluidityGraph.removeReplicaFromNode(repId);
            }
        }


        // Generate new muted replicas
        newReplicas = generateNewReplicas(actualNumberOfReplicasToMove);


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
        int[] nodeNr = getPossibleNodeForGraph();
        int offset = 0;

        if (useGraph) {
            // Create variants here
            for (int i = 0; i < numOfVariants; i++) {
                variantsOfNewNodes[i] = new ArrayList<>();
                for (int j = 0; j < numberOfReplicasToMove; j++) {
                    if (j+offset < nodeNr.length) {
                        FluidityGraphNode node = fluidityGraph.getNodeById(nodeNr[j + offset]);
                        variantsOfNewNodes[i].add(node);
                    }
                }
                if (variantsOfNewNodes[i].size() != numberOfReplicasToMove) {
                    variantsOfNewNodes[i] = new ArrayList<>();
                    //i = numOfVariants;
                    numOfVariants = i;
                }
                offset++;
            }

            // Give graph variants
            double[] bestAssignment = new double[numOfVariants];
            oldReplicasToRemove = getReplicaIDsToMove();
            double[][] replaceLatencies;
            double[][] replaceProposeLatencies;
            double[] replaceClientLatencies;

            for (int i = 0; i < numOfVariants; i++) { //TODO Changed variantsOfNewNodes.length to numofvariants. Check!
                replaceLatencies = getLantencyOfMutedReplica(oldReplicasToRemove, i);
                replaceProposeLatencies = getProposeLantencyOfMutedReplica(oldReplicasToRemove, i);
                replaceClientLatencies = getClientLatencyOfMutedReplica(oldReplicasToRemove, i);

                for (int j = 0; j < numOfVariants; j++) {
                    WeightGraphReconfigurator weightGraphReconfigurator = new WeightGraphReconfigurator(svController,
                            latencyStorage, this, replicaIds.length);
                    boolean delete = ((j + 1) == numOfVariants) && ((i+1) == numOfVariants);
                    bestAssignment[j] = weightGraphReconfigurator.runGraph(oldReplicasToRemove, replaceLatencies,
                            replaceProposeLatencies, replaceClientLatencies, delete);
                }
            }

            return getBestNodes(bestAssignment);
        } else {
            return getRandomNodes();
        }
    }

    private ArrayList<FluidityGraphNode> getBestNodes(double[] bestAssignments) {
        int bestVariant = 0;
        double bestValue = Integer.MAX_VALUE;

        for (int i = 0; i < bestAssignments.length; i++) {
            double value = bestAssignments[i];

            if (value < bestValue) {
                bestVariant = i;
            }

            //double weights = 0;
//            for (int replicas : oldReplicasToRemove) {
//                weights += bestAssignments[i].get(replicas);
//            }
//            if (weights > maxWeight) {
//                bestVariant = i;
//            }
        }

        return variantsOfNewNodes[bestVariant];
    }

    private int[] getPossibleNodeForGraph() {
        ArrayList<NodeWeights> nodeWeights = new ArrayList<>();

        categorizeNodes();
        int[] newNodes = new int[nodeCategory[0].size()];
        ArrayList<FluidityGraphNode> possibleNodes = nodeCategory[0];

        for (FluidityGraphNode node : possibleNodes) {
            double weight = assignWeightToNode(node);
            nodeWeights.add(new NodeWeights(node.getNodeId(), weight));
        }

        int counter = 0;
        for (NodeWeights weight : nodeWeights) {
            if (weight.getWeight() != -1.0d) {
                counter++;
            }
        }

        if (counter < numberOfReplicasToMove) {
            useGraph = false;
        }

        if (useGraph) {
            Collections.sort(nodeWeights, new Comparator<NodeWeights>() {
                @Override
                public int compare(NodeWeights lhs, NodeWeights rhs) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return lhs.getWeight() > rhs.getWeight() ? 1 : (lhs.getWeight() < rhs.getWeight() ) ? -1 : 0;
                }
            });

            //Shift the -1 weights to the end
            boolean stop = false;
            int position = 0;
            while (!stop) {
                if (position < nodeWeights.size()) {
                    if (nodeWeights.get(position).getWeight() == -1.0d) {
                        NodeWeights tempWeights = nodeWeights.get(position);
                        nodeWeights.remove(position);
                        nodeWeights.add(tempWeights);
                        position++;
                    } else {
                        stop = true;
                    }
                } else {
                    stop = true;
                }
            }

            for (int i = 0; i < nodeWeights.size(); i++) {
                newNodes[i] = nodeWeights.get(i).getNodeId();
            }
        } else {
            for (int i = 0; i < nodeWeights.size(); i++) {
                newNodes[i] = nodeWeights.get(i).getNodeId();
            }
        }

        //variantsOfNewNodes = newNodes;
        return newNodes;
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

    public double[][] getProposeLantencyOfMutedReplica(ArrayList<Integer> replicasToReplace, int variant) {
        //Conversion of replicaToConversion to actual replica
        double[][] latencies = new double[replicaIds.length][replicaIds.length];

        for (int oldReplica : replicasToReplace) {
            for (int i = 0; i < variantsOfNewNodes[0].size(); i++) {
                //replicaIdsToReplace.put(oldReplica, getOneOfNewNodes(variant, i));
                for (int otherReplica : replicaIds) {
                    FluidityGraphNode nodeStandard = fluidityGraph.getNodeById(fluidityGraph.getNodeIdFromReplicaId(otherReplica));
                    FluidityGraphNode nodeToReplace = replicaIdsToReplace.get(oldReplica);
                    FluidityGraphEdge fromEdge = fluidityGraph.getEdgeByNodes(nodeToReplace, nodeStandard);
                    FluidityGraphEdge toEdge = fluidityGraph.getEdgeByNodes(nodeStandard, nodeToReplace);

                    latencies[oldReplica][otherReplica] = fromEdge.getLatencyProposeValue();
                    latencies[otherReplica][oldReplica] = toEdge.getLatencyProposeValue();
                }
            }
        }
        return latencies;
    }

    public double[] getClientLatencyOfMutedReplica(ArrayList<Integer> replicasToReplace, int variant) {
        double[] latencies = new double[replicasToReplace.size()];

        for (int oldReplica : replicasToReplace) {
            for (int i = 0; i < variantsOfNewNodes[0].size(); i++) {
                //replicaIdsToReplace.put(oldReplica, getOneOfNewNodes(variant, i));

                FluidityGraphNode nodeToReplace = replicaIdsToReplace.get(oldReplica); //TODO Check for array index out of bounds
                latencies[i] = nodeToReplace.getClientLatency();

//                    FluidityGraphNode nodeStandard = fluidityGraph.getNodeById(fluidityGraph.getNodeIdFromReplicaId(otherReplica));
//                    FluidityGraphNode nodeToReplace = replicaIdsToReplace.get(oldReplica);
//                    FluidityGraphEdge fromEdge = fluidityGraph.getEdgeByNodes(nodeToReplace, nodeStandard);
//                    FluidityGraphEdge toEdge = fluidityGraph.getEdgeByNodes(nodeStandard, nodeToReplace);

//                    latencies[oldReplica]= fromEdge.getLatencyValue();
//                    latencies[otherReplica][oldReplica] = toEdge.getLatencyValue();

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
