package bftsmart.dynamicWeights;

import java.util.Arrays;

public class DynamicWeightGraph implements Comparable<DynamicWeightGraph> {

	private DynamicWeightGraphNode root;
	private DynamicWeightGraphNode[] leaves;
	private Double[] weights;

	public DynamicWeightGraph(DynamicWeightGraphNode root) {
		this.root = root;
		this.leaves = new DynamicWeightGraphNode[] { this.root };
		this.weights = new Double[] { 0d };
	}

	public DynamicWeightGraphNode[] getLeaves() {
		return leaves;
	}

	public void setLeaves(DynamicWeightGraphNode[] newLeaves) {
		this.leaves = newLeaves;
	}

	public void setWeights(Double[] weights) {
		this.weights = weights;
	}

	public Double[] getWeights() {
		return weights;
	}

	@Override
	public int compareTo(DynamicWeightGraph o) {
		if (getValue() > o.getValue()) {
			return 1;
		} else if (getValue() == o.getValue()) {
			return 0;
		} else {
			return -1;
		}
	}

	/**
	 * returns the value of the graph.
	 * @return
	 */
	public double getValue() {
		double value = 0;
		for (int i = 0; i < leaves.length; i++) {
			value = value + leaves[i].getValue();
		}
		return value;
	}

	@Override
	public String toString() {
		return "weightassignment: " + Arrays.toString(weights) + ", calculatedValue: " + getValue();

	}

}
