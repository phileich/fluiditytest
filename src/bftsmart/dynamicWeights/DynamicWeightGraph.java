package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DynamicWeightGraph implements Comparable<DynamicWeightGraph> {

	private DynamicWeightGraphNode root;
	private DynamicWeightGraphNode[] leaves;
	private Double[] weights;
	private int leader = -1;
	private int quorumSize = 1;

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

	public void setLeader(int leader) {
		this.leader = leader;
	}

	public int getLeader() {
		return leader;
	}

	public void setQuorumSize(int quorumSize) {
		this.quorumSize = Math.max(1, quorumSize);
	}

	public int getQuorumSize() {
		return quorumSize;
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
	 * 
	 * @return
	 */
	public double getValue() { //TODO This could be the ordering of the arraylist with the weights
		ArrayList<Double> valueList = new ArrayList<>();
		for (int i = 0; i < leaves.length; i++) {
			for (int j = 0; j < weights[i]; j++) { //TODO problem with j < weight 0
				valueList.add(leaves[i].getValue());
			}
		}
		Collections.sort(valueList);
		return valueList.get(Math.min(quorumSize, valueList.size()) - 1);
	}

	@Override
	public String toString() {
		return "(leader: " + leader + ", weightassignment: " + Arrays.toString(weights) + ", calculatedValue: "
				+ getValue() + ")";

	}

}
