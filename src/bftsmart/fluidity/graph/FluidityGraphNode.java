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

    public FluidityGraphNode(int id, ArrayList<Integer> replicas, int maximumNumberOfReplicas) {
        this.dataCenterId = id;
        this.replicas = replicas;
        this.maximumNumberOfReplicas = maximumNumberOfReplicas;
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
}
