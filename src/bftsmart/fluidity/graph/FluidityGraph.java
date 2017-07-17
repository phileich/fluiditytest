package bftsmart.fluidity.graph;

import org.w3c.dom.NodeList;

import javax.xml.soap.Node;
import java.util.ArrayList;

/**
 * Created by philipp on 19.06.17.
 */
public class FluidityGraph {
    private ArrayList<FluidityGraphNode> nodes;
    private ArrayList<FluidityGraphEdge> edges;

    public FluidityGraph() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public void addNode(int id, ArrayList<Integer> replicas, int maxNumOfRep) {
        FluidityGraphNode tempNode = new FluidityGraphNode(id, replicas, maxNumOfRep);
        nodes.add(tempNode);
    }

    public void addEdge(int idNodeFrom, int idNodeTo, double latencyValue) {
        FluidityGraphNode nodeFrom = getNodeById(idNodeFrom);
        FluidityGraphNode nodeTo = getNodeById(idNodeTo);

        if (nodeFrom != null && nodeTo != null) {
            FluidityGraphEdge tempEdge = new FluidityGraphEdge(nodeFrom, nodeTo, latencyValue);
            edges.add(tempEdge);
//            nodeFrom.addOutEdge(tempEdge);
//            nodeTo.addInEdge(tempEdge);
        }
    }

    public void changeEdgeLatencyData(int idNodeFrom, int idNodeTo, double newLatencyData) {
        FluidityGraphNode nodeFrom = getNodeById(idNodeFrom);
        FluidityGraphNode nodeTo = getNodeById(idNodeTo);

        FluidityGraphEdge tempEdge = getEdgeByNodes(nodeFrom, nodeTo);
        tempEdge.setLatencyValue(newLatencyData);
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

    public boolean hasAlreadyActiveReplica(FluidityGraphNode node) {
        if (node.getReplicas().size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public int numOfPossibleNodes() {
        int counter = 0;

        for (FluidityGraphNode node : nodes) {
            if (!hasAlreadyActiveReplica(node)) {
                counter++;
            }
        }

        return counter;
    }

    public FluidityGraphNode getNodeById(int nodeId) {
        FluidityGraphNode neededNode = new FluidityGraphNode(nodeId, null, -1);

        int index = nodes.indexOf(neededNode);

        return nodes.get(index);
    }

    private FluidityGraphEdge getEdgeByNodes(FluidityGraphNode nodeFrom, FluidityGraphNode nodeTo) {
        FluidityGraphEdge neededEdge = new FluidityGraphEdge(nodeFrom, nodeTo, -1);

        int index = edges.indexOf(neededEdge);

        return edges.get(index);
    }
}
