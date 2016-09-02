package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Collections;

import bftsmart.tom.util.Logger;

public class DynamicWeightGraphBuilder {
	private DynamicWeightGraph dwGraph;

	public DynamicWeightGraphBuilder() {
		dwGraph = new DynamicWeightGraph(new DynamicWeightGraphNode(0));
	}

	public DynamicWeightGraph build() {
		return dwGraph;
	}

	public DynamicWeightGraphBuilder setWeights(Double[] weights) {
		dwGraph.setWeights(weights);
		return this;
	}

	public DynamicWeightGraphBuilder setQuorumSize(int quorumSize) {
		dwGraph.setQuorumSize(quorumSize);
		return this;
	}

	/**
	 * Adds a one-to-all pattern to the graph. Only the leaf with the nodenr
	 * number will be used.
	 * 
	 * @param nodeNr
	 *            the number of the leaf from which the one-to-all pattern will
	 *            start. Starts with index 0.
	 * @param latencies
	 *            the latencies from the client to the nodes
	 * @params weights the weights for each source node
	 * @return the builder
	 */
	public DynamicWeightGraphBuilder addClientRequest(int nodeNr, double[] latencies, Double[] weights) {
		// check if nodenr is in range of leaves
		if (nodeNr > this.dwGraph.getLeaves().length) {
			Logger.println("Could not add ClientRequest to graph, because nodeNr is higher than the leafe number.");
			return this;
		}

		// check if number of latencies is equal to number of weights
		if (latencies.length != weights.length) {
			Logger.println(
					"Could not add ClientRequest to graph, because number of latencies is not equal to number of weights.");
			return this;
		}

		// create new leaves
		DynamicWeightGraphNode[] newLeaves = new DynamicWeightGraphNode[latencies.length];
		for (int i = 0; i < newLeaves.length; i++) {

			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0);
			new DynamicWeightGraphEdge(dwGraph.getLeaves()[nodeNr], tmpNode, latencies[i], weights[i]);
			newLeaves[i] = tmpNode;
		}

		// calculate
		for (int i = 0; i < newLeaves.length; i++) {
			// the value of the node is the value of the client + the time from
			// client to server i
			newLeaves[i].setValue(dwGraph.getLeaves()[nodeNr].getValue() + latencies[i]);
		}

		dwGraph.setLeaves(newLeaves);
		return this;
	}

	public DynamicWeightGraphBuilder addEmptyClientRequest(int nodeNr, int latencySize) {
		// check if nodenr is in range of leaves
		if (nodeNr > this.dwGraph.getLeaves().length) {
			Logger.println("Could not add ClientRequest to graph, because nodeNr is higher than the leafe number.");
			return this;
		}

		// create new leaves
		DynamicWeightGraphNode[] newLeaves = new DynamicWeightGraphNode[latencySize];
		for (int i = 0; i < newLeaves.length; i++) {
			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0);
			new DynamicWeightGraphEdge(dwGraph.getLeaves()[nodeNr], tmpNode, 0, 0);
			newLeaves[i] = tmpNode;
		}

		// calculate
		for (int i = 0; i < newLeaves.length; i++) {
			// the value of the node is the value of the client + the time from
			// client to server i
			newLeaves[i].setValue(dwGraph.getLeaves()[nodeNr].getValue());
		}

		dwGraph.setLeaves(newLeaves);
		return this;
	}

	/**
	 * Adds a one-to-all pattern to the graph. Only the leaf with the nodenr
	 * number will be used.
	 * 
	 * @param leaderNr
	 *            the number of the leader from which the one-to-all pattern
	 *            will start. Starts with index 0.
	 * @param latencies
	 *            the latencies from the leader to the nodes
	 * @params weights weights the weights for each source node
	 * @return the builder
	 */
	public DynamicWeightGraphBuilder addLeaderPropose(int leaderNr, double[] latencies, Double[] weights) {
		// check if leaderNr is in range of leaves
		if (leaderNr > this.dwGraph.getLeaves().length) {
			Logger.println("Could not add LeaderPropose to graph, because leaderNr is higher than the leafe number.");
			return this;
		}

		// check if number of latencies is equal to number of weights
		if (latencies.length != weights.length) {
			Logger.println(
					"Could not add LeaderPropose to graph, because number of latencies is not equal to number of weights.");
			return this;
		}

		// check if number of latencies is equal to number of leaves
		if (latencies.length != dwGraph.getLeaves().length) {
			Logger.println(
					"Could not add LeaderPropose to graph, because number of latencies is not equal to number of leaves.");
			return this;
		}

		// create new leaves
		DynamicWeightGraphNode[] newLeaves = new DynamicWeightGraphNode[latencies.length];
		for (int i = 0; i < newLeaves.length; i++) {
			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0);
			new DynamicWeightGraphEdge(dwGraph.getLeaves()[leaderNr], tmpNode, latencies[i], weights[i]);
			newLeaves[i] = tmpNode;
		}

		// calculate
		for (int i = 0; i < newLeaves.length; i++) {
			// the value of the node is the maximum of the value of the leader +
			// the time from
			// the leader to server i and the value of the server node i
			newLeaves[i].setValue(Math.max(dwGraph.getLeaves()[leaderNr].getValue() + latencies[i],
					dwGraph.getLeaves()[i].getValue()));
		}

		dwGraph.setLeaves(newLeaves);
		return this;
	}

	/**
	 * Adds a all-to-all pattern to the graph. Each leaf will have a edge to all
	 * new leaves.
	 * 
	 * @param latencies
	 *            the latency matrix for each node to node
	 * @param weights
	 *            the weights for each source node
	 * @param quorumSize
	 *            the quorumSize of the pattern
	 * @return
	 */
	public DynamicWeightGraphBuilder addMultiCast(double[][] latencies, Double[] weights) {
		// check if latencies size is equal
		for (int i = 0; i < latencies.length; i++) {
			if (latencies[i].length != latencies.length) {
				Logger.println(
						"Could not add MultiCast to graph, because latencies collumn size is not equal to its row size.");
				return this;
			}
		}
		// check if quroumSize is lesser than latency size
		if (dwGraph.getQuorumSize() > latencies.length) {
			Logger.println("Could not add MultiCast to graph, because quorumSize is greater than latencies  size.");
			return this;
		}
		// check if quorumSize is greater than zero
		if (dwGraph.getQuorumSize() < 1) {
			Logger.println("Could not add MultiCast to graph, because quorumSize has to be greater than zero.");
			return this;
		}

		// check if number of latencies is equal to number of leaves
		if (latencies.length != dwGraph.getLeaves().length) {
			Logger.println(
					"Could not add MultiCast to graph, because number of latencies is not equal to number of leaves.");
			return this;
		}

		// Create
		DynamicWeightGraphNode[] newLeaves = new DynamicWeightGraphNode[latencies.length];
		// nodes
		for (int i = 0; i < newLeaves.length; i++) {
			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0);
			newLeaves[i] = tmpNode;
		}
		// edges from each nodes to each leaf
		for (int i = 0; i < dwGraph.getLeaves().length; i++) {
			for (int j = 0; j < newLeaves.length; j++) {
				new DynamicWeightGraphEdge(dwGraph.getLeaves()[i], newLeaves[j], latencies[i][j], weights[i]);
			}
		}

		// calculate
		for (int i = 0; i < newLeaves.length; i++) {
			ArrayList<Double> values = new ArrayList<Double>();
			// calculate every value of incoming edges
			for (DynamicWeightGraphEdge edge : newLeaves[i].getIncomingEdges()) {
				double value = edge.getFrom().getValue() + edge.getValue();
				// add for each weight
				for (int j = 0; j < edge.getWeight(); j++) {
					values.add(value);
				}
			}
			Collections.sort(values);
			newLeaves[i].setValue(values.get(dwGraph.getQuorumSize() - 1));
		}

		dwGraph.setLeaves(newLeaves);
		return this;
	}

	/**
	 * Adds a all-to-one pattern to the graph
	 * 
	 * @param latencies
	 * @param weights
	 * @param quorumSize
	 * @return
	 */
	public DynamicWeightGraphBuilder addClientResponse(double[] latencies, Double[] weights) {
		// check if number of latencies is equal to number of weights
		if (latencies.length != weights.length) {
			Logger.println(
					"Could not add ClientResponse to graph, because number of latencies is not equal to number of weights.");
			return this;
		}

		// check if number of latencies is equal to number of leaves
		if (latencies.length != dwGraph.getLeaves().length) {
			Logger.println(
					"Could not add ClientResponse to graph, because number of latencies is not equal to number of leaves.");
			return this;
		}

		// check if quroumSize is lesser than latency size
		if (dwGraph.getQuorumSize() > latencies.length) {
			Logger.println(
					"Could not add ClientResponse to graph, because quorumSize is greater than latencies  size.");
			return this;
		}

		// check if quorumSize is greater than zero
		if (dwGraph.getQuorumSize() < 1) {
			Logger.println("Could not add ClientResponse to graph, because quorumSize has to be greater than zero.");
			return this;
		}

		// Create
		DynamicWeightGraphNode[] newLeaves = new DynamicWeightGraphNode[1];
		newLeaves[0] = new DynamicWeightGraphNode(0);

		for (int i = 0; i < dwGraph.getLeaves().length; i++) {
			new DynamicWeightGraphEdge(dwGraph.getLeaves()[i], newLeaves[0], latencies[i], weights[i]);
		}

		// Calculate

		ArrayList<Double> values = new ArrayList<Double>();
		// calculate every value of incoming edges
		for (DynamicWeightGraphEdge edge : newLeaves[0].getIncomingEdges()) {
			double value = edge.getFrom().getValue() + edge.getValue();
			// add for each weight
			for (int j = 0; j < edge.getWeight(); j++) {
				values.add(value);
			}
		}
		Collections.sort(values);
		newLeaves[0].setValue(values.get(dwGraph.getQuorumSize() - 1));

		dwGraph.setLeaves(newLeaves);
		return this;
	}
}
