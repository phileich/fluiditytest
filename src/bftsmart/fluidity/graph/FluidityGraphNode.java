package bftsmart.fluidity.graph;

import java.util.ArrayList;

/**
 * This class implements a node of the FluidityGraph representing a data center. This data center contains
 * information about the replicas etc which are running within that data center
 */
public class FluidityGraphNode {
    private int dataCenterId;
    private ArrayList<Integer> replicas;
    private int maximumNumberOfReplicas;
    private ArrayList<FluidityGraphEdge> inEdges;
    private ArrayList<FluidityGraphEdge> outEdges;

    public FluidityGraphNode(int id, ArrayList<Integer> replicas, int maximumNumberOfReplicas) {
        this.dataCenterId = id;
        this.replicas = replicas;
        this.maximumNumberOfReplicas = maximumNumberOfReplicas;
    }

    public int getDataCenterId() {
        return dataCenterId;
    }

    public void addReplica(int replicaId) {
        this.replicas.add(replicaId);
    }

    public void deleteReplica(int replicaId) {
        this.replicas.remove(replicaId);
    }

    public int getMaximumNumberOfReplicas() {
        return maximumNumberOfReplicas;
    }

    public void setMaximumNumberOfReplicas(int maximumNumberOfReplicas) {
        this.maximumNumberOfReplicas = maximumNumberOfReplicas;
    }

    public void addInEdge(FluidityGraphEdge inEdge) {
        this.inEdges.add(inEdge);
    }

    public void addOutEdge(FluidityGraphEdge outEdge) {
        this.outEdges.add(outEdge);
    }

    public void deleteInEdge(FluidityGraphEdge inEdge) {
        this.inEdges.remove(inEdge);
    }

    public void deleteOutEdge(FluidityGraphEdge outEdge) {
        this.outEdges.remove(outEdge);
    }
}
