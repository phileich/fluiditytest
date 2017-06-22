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
//    private ArrayList<FluidityGraphEdge> inEdges;
//    private ArrayList<FluidityGraphEdge> outEdges;

    protected FluidityGraphNode(int id, ArrayList<Integer> replicas, int maximumNumberOfReplicas) {
        this.dataCenterId = id;
        this.replicas = replicas;
        this.maximumNumberOfReplicas = maximumNumberOfReplicas;
    }

    protected int getDataCenterId() {
        return dataCenterId;
    }

    protected void addReplica(int replicaId) {
        this.replicas.add(replicaId);
    }

    protected void deleteReplica(int replicaId) {
        this.replicas.remove(replicaId);
    }

    protected int getMaximumNumberOfReplicas() {
        return maximumNumberOfReplicas;
    }

    protected void setMaximumNumberOfReplicas(int maximumNumberOfReplicas) {
        this.maximumNumberOfReplicas = maximumNumberOfReplicas;
    }

//    protected void addInEdge(FluidityGraphEdge inEdge) {
//        this.inEdges.add(inEdge);
//    }
//
//    protected void addOutEdge(FluidityGraphEdge outEdge) {
//        this.outEdges.add(outEdge);
//    }
//
//    protected void deleteInEdge(FluidityGraphEdge inEdge) {
//        this.inEdges.remove(inEdge);
//    }
//
//    protected void deleteOutEdge(FluidityGraphEdge outEdge) {
//        this.outEdges.remove(outEdge);
//    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FluidityGraphNode that = (FluidityGraphNode) o;

        return dataCenterId == that.dataCenterId;
    }

    @Override
    public int hashCode() {
        return dataCenterId;
    }
}
