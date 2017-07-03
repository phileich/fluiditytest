package bftsmart.fluidity.graph;

import java.io.Serializable;

/**
 * This class represents an edge of the fluidity graph containing information about the latencies between
 * the data centers, which are represented by the FluidityGraphNodes
 */
public class FluidityGraphEdge {
    private FluidityGraphNode nodeFrom;
    private FluidityGraphNode nodeTo;
    private double latencyValue;

    protected FluidityGraphEdge(FluidityGraphNode nodeFrom, FluidityGraphNode nodeTo, double value) {
        this.nodeFrom = nodeFrom;
        this.nodeTo = nodeTo;
        this.latencyValue = value;
    }

    protected FluidityGraphNode getNodeFrom() {
        return nodeFrom;
    }

    protected FluidityGraphNode getNodeTo() {
        return nodeTo;
    }

    protected double getLatencyValue() {
        return latencyValue;
    }

    protected void setNodeFrom(FluidityGraphNode nodeFrom) {
        this.nodeFrom = nodeFrom;
    }

    protected void setNodeTo(FluidityGraphNode nodeTo) {
        this.nodeTo = nodeTo;
    }

    protected void setLatencyValue(double latencyValue) {
        this.latencyValue = latencyValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FluidityGraphEdge that = (FluidityGraphEdge) o;

        if (!nodeFrom.equals(that.nodeFrom)) return false;
        return nodeTo.equals(that.nodeTo);
    }

    @Override
    public int hashCode() {
        int result = nodeFrom.hashCode();
        result = 31 * result + nodeTo.hashCode();
        return result;
    }

}
