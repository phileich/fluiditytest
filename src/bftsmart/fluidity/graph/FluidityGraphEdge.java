package bftsmart.fluidity.graph;

import bftsmart.fluidity.FluidityGraph;

/**
 * This class represents an edge of the fluidity graph containing information about the latencies between
 * the data centers, which are represented by the FluidityGraphNodes
 */
public class FluidityGraphEdge {
    private FluidityGraphNode nodeA;
    private FluidityGraphNode nodeB;
    private double latencyValue;

    public FluidityGraphEdge(FluidityGraphNode nodeA, FluidityGraphNode nodeB, double value) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.latencyValue = value;
    }

    public FluidityGraphNode getNodeA() {
        return nodeA;
    }

    public FluidityGraphNode getNodeB() {
        return nodeB;
    }

    public double getLatencyValue() {
        return latencyValue;
    }

    public void setNodeA(FluidityGraphNode nodeA) {
        this.nodeA = nodeA;
    }

    public void setNodeB(FluidityGraphNode nodeB) {
        this.nodeB = nodeB;
    }

    public void setLatencyValue(double latencyValue) {
        this.latencyValue = latencyValue;
    }
}
