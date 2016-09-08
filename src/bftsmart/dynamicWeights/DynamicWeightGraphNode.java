package bftsmart.dynamicWeights;

import java.util.ArrayList;

public class DynamicWeightGraphNode {
	private ArrayList<DynamicWeightGraphEdge> in_edges = new ArrayList<>();
	private ArrayList<DynamicWeightGraphEdge> out_edges = new ArrayList<>();
	private double value;

	// public DynamicWeightGraphNode(double value) {
	// this.value = value;
	// this.setWeight(1);
	//
	// }

	public DynamicWeightGraphNode(double value) {
		this.value = value;
	}

	public ArrayList<DynamicWeightGraphEdge> getOutgoingEdges() {
		return out_edges;
	}

	public void addOutgoingEdge(DynamicWeightGraphEdge edge) {
		out_edges.add(edge);
	}

	public ArrayList<DynamicWeightGraphEdge> getIncomingEdges() {
		return in_edges;
	}

	public void addIncomingEdge(DynamicWeightGraphEdge edge) {
		in_edges.add(edge);
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return getValue() + "";
	}

}
