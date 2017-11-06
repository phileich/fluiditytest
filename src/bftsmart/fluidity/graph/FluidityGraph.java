package bftsmart.fluidity.graph;

import bftsmart.reconfiguration.views.View;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by philipp on 19.06.17.
 */
public class FluidityGraph implements Serializable{
    private static final long serialVersionUID = 5117021765431291618L;

    private ArrayList<FluidityGraphNode> nodes;
    private ArrayList<FluidityGraphEdge> edges;
    private View view;

    public FluidityGraph(View view) {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        this.view = view;
    }

    public void addNode(int id, ArrayList<Integer> replicas, int maxNumOfRep, double clientLatency) {
        FluidityGraphNode tempNode = new FluidityGraphNode(id, replicas, maxNumOfRep, clientLatency);
        nodes.add(tempNode);
    }

    public void addEdge(int idNodeFrom, int idNodeTo, double latencyValue, double latencyProposeValue) {
        FluidityGraphNode nodeFrom = getNodeById(idNodeFrom);
        FluidityGraphNode nodeTo = getNodeById(idNodeTo);

        if (nodeFrom != null && nodeTo != null) {
            FluidityGraphEdge tempEdge = new FluidityGraphEdge(nodeFrom, nodeTo, latencyValue, latencyProposeValue);
            edges.add(tempEdge);
//            nodeFrom.addOutEdge(tempEdge);
//            nodeTo.addInEdge(tempEdge);
        }
    }

    protected void addRemainingEdges() {
        for (FluidityGraphNode nodeFrom : nodes) {
            for (FluidityGraphNode nodeTo : nodes) {
                if (!edges.contains(new FluidityGraphEdge(nodeFrom, nodeTo, -1, -1)) &&
                        !nodeFrom.equals(nodeTo)) {
                    addEdge(nodeFrom.getNodeId(), nodeTo.getNodeId(), -1, -1);
                }
            }
        }
    }

    public void changeEdgeLatencyData(int idNodeFrom, int idNodeTo, double newLatencyData) {
        FluidityGraphNode nodeFrom = getNodeById(idNodeFrom);
        FluidityGraphNode nodeTo = getNodeById(idNodeTo);

        FluidityGraphEdge tempEdge = getEdgeByNodes(nodeFrom, nodeTo);
        tempEdge.setLatencyValue(newLatencyData);
    }

    public void changeEdgeLatencyProposeData(int idNodeFrom, int idNodeTo, double newLatencyData) {
        FluidityGraphNode nodeFrom = getNodeById(idNodeFrom);
        FluidityGraphNode nodeTo = getNodeById(idNodeTo);

        FluidityGraphEdge tempEdge = getEdgeByNodes(nodeFrom, nodeTo);
        tempEdge.setLatencyProposeValue(newLatencyData);
    }

    public int getNodeIdFromReplicaId(int replicaId) {
        int nodeId = -1;

        for (FluidityGraphNode tempNode : nodes) {
            for (int tempReplicaId : tempNode.getReplicas()) {
                if (tempReplicaId == replicaId) {
                    nodeId = tempNode.getDataCenterId();
                }
            }
        }

        return nodeId;
    }

    public void addReplicaToNode(int nodeId, int replicaId) {
        getNodeById(nodeId).addReplica(replicaId);

    }

    public void addReplicaToNode(FluidityGraphNode node, int replicaId) {
        node.addReplica(replicaId);
    }

    public void removeReplicaFromNode(int nodeId, int replicaId) {
        getNodeById(nodeId).deleteReplica(replicaId);
    }

    public void removeReplicaFromNode(int replicaId) {
        int nodeId = getNodeIdFromReplicaId(replicaId);
        getNodeById(nodeId).deleteReplica(replicaId);
    }

    public void changeMaxNumOfRep(int nodeId, int maxNumOfRep) {
        getNodeById(nodeId).setMaximumNumberOfReplicas(maxNumOfRep);
    }

    public ArrayList<FluidityGraphNode> getNodes() {
        return nodes;
    }

    public ArrayList<FluidityGraphEdge> getEdges() {
        return edges;
    }

    public int getMaxNumOfRepByNode(FluidityGraphNode node) {
        return node.getMaximumNumberOfReplicas();
    }

    // Checks whether the maximum capacity is already reached or not
    public boolean checkForCapacity(FluidityGraphNode node) {
        if (node.getReplicas().size() < node.getMaximumNumberOfReplicas()) {
            return true;
        } else {
            return false;
        }
    }

    /**
    Returns true if there are unmuted replicas in the given node, else false.
     */
    public boolean hasAlreadyUnmutedReplica(FluidityGraphNode node) {
        if (node.getReplicas().size() > 0) {
            ArrayList<Integer> nodeReplicas = node.getReplicas();
            for (int proId : nodeReplicas) {
                if (view.getWeight(proId) > 0) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public int numOfPossibleNodes() {
        int counter = 0;

        for (FluidityGraphNode node : nodes) {
            if (!hasAlreadyUnmutedReplica(node)) {
                counter++;
            }
        }

        return counter;
    }

    public FluidityGraphNode getNodeById(int nodeId) {
        FluidityGraphNode neededNode = new FluidityGraphNode(nodeId, null, -1, -1);

        int index = nodes.indexOf(neededNode);

        return nodes.get(index);
    }

    public ArrayList<Integer> getReplicasFromNode(FluidityGraphNode node) {
        return node.getReplicas();
    }

    public FluidityGraphEdge getEdgeByNodes(FluidityGraphNode nodeFrom, FluidityGraphNode nodeTo) {
        if (!nodeFrom.equals(nodeTo)) {
            FluidityGraphEdge neededEdge = new FluidityGraphEdge(nodeFrom, nodeTo, -1, -1);

            int index = edges.indexOf(neededEdge);

            return edges.get(index);
        } else {
            return new FluidityGraphEdge(nodeFrom, nodeTo, 0.0d, 0.0d);
        }
    }

    public boolean checkForConsistencyWithRules() {
        for (FluidityGraphNode node : nodes) {
            int count = 0;
            for (int proId : node.getReplicas()) {
                if (view.getWeight(proId) == 0) {
                    count++;
                }
            }

            if (count > 1) {
                return false;
            }
        }

        return true;
    }

    public double[] getWeightsOfReplicas(ArrayList<Integer> replicaIds) {
        double[] weightsOfReplicas = new double[replicaIds.size()];
        int i = 0;

        for (int rep : replicaIds) {
            weightsOfReplicas[i] = view.getWeight(rep);
            i++;
        }

        return weightsOfReplicas;
    }

    public double getLatencyBetweenReplicas(int replicaFrom, int replicaTo) {
        FluidityGraphNode nodeFrom = getNodeById(getNodeIdFromReplicaId(replicaFrom));
        FluidityGraphNode nodeTo = getNodeById(getNodeIdFromReplicaId(replicaTo));

        FluidityGraphEdge edge = getEdgeByNodes(nodeFrom, nodeTo);

        return edge.getLatencyValue();
    }

    public void updateClientLatencyToNode(FluidityGraphNode node, double clientLatency) {
        node.updateClientLatency(clientLatency);
    }

    public int[] getReplicasOfSystem() {
        return view.getProcesses();
    }

    @Override
    public String toString() {
        return "FluidityGraph{" +
                "nodes=" + nodes +
                '}' + "\n";
    }
}
