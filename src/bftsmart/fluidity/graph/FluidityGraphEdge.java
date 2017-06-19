package bftsmart.fluidity.graph;

/**
 * This class represents an edge of the fluidity graph containing information about the latencies between
 * the data centers, which are represented by the FluidityGraphNodes
 */
public class FluidityGraphEdge {
    private FluidityGraphNode nodeFrom;
    private FluidityGraphNode nodeTo;
    private double latencyValue;

    public FluidityGraphEdge(FluidityGraphNode nodeFrom, FluidityGraphNode nodeTo, double value) {
        this.nodeFrom = nodeFrom;
        this.nodeTo = nodeTo;
        this.latencyValue = value;
    }

    public FluidityGraphNode getNodeFrom() {
        return nodeFrom;
    }

    public FluidityGraphNode getNodeTo() {
        return nodeTo;
    }

    public double getLatencyValue() {
        return latencyValue;
    }

    public void setNodeFrom(FluidityGraphNode nodeFrom) {
        this.nodeFrom = nodeFrom;
    }

    public void setNodeTo(FluidityGraphNode nodeTo) {
        this.nodeTo = nodeTo;
    }

    public void setLatencyValue(double latencyValue) {
        this.latencyValue = latencyValue;
    }
}
