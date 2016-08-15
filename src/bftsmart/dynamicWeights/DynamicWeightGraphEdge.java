package bftsmart.dynamicWeights;

public class DynamicWeightGraphEdge implements Comparable<DynamicWeightGraphEdge> {
	private DynamicWeightGraphNode from;
	private DynamicWeightGraphNode to;
	private double value;

	public DynamicWeightGraphEdge(DynamicWeightGraphNode from, DynamicWeightGraphNode to, double value) {
		this.from = from;
		this.to = to;
		this.value = value;

		this.from.addOutgoingEdge(this);
		this.to.addIncomingEdge(this);
	}

	public DynamicWeightGraphNode getFrom() {
		return from;
	}

	public void setFrom(DynamicWeightGraphNode from) {

		this.from = from;
	}

	public DynamicWeightGraphNode getTo() {
		return to;
	}

	public void setTo(DynamicWeightGraphNode to) {
		this.to = to;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public Double getFromWeight() {
		return from.getWeight();
	}

	public double getEdgeValueSumNode() {
		return value + from.getValue();
	}

	@Override
	public int compareTo(DynamicWeightGraphEdge o) {
		if (getEdgeValueSumNode() > o.getEdgeValueSumNode()) {
			return 1;
		} else if (getEdgeValueSumNode() == o.getEdgeValueSumNode()) {
			return 0;
		} else {
			return -1;
		}
	}
}
