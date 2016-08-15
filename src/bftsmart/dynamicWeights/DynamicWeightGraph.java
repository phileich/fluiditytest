package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DynamicWeightGraph implements Comparable<DynamicWeightGraph> {

	private DynamicWeightGraphNode root;
	private double[][] serverLatencies;
	private double[] clientLatency;
	private int nrOfNodes;
	private Double[] weightassignment;
	private double calculatedValue = -1;
	private int leaderID;

	public DynamicWeightGraph(DynamicWeightGraphNode root, double[] clientLatency, double[][] serverLatencies,
			Double[] weightassignment) {
		this.root = root;
		this.clientLatency = clientLatency;
		this.serverLatencies = serverLatencies;
		this.weightassignment = weightassignment;
		this.nrOfNodes = weightassignment.length;

	}

	public DynamicWeightGraphNode getRoot() {
		return root;
	}

	public void setRoot(DynamicWeightGraphNode root) {
		this.root = root;
	}

	public DynamicWeightGraphNode[] addClientRequest(DynamicWeightGraphNode node) {
		// create
		DynamicWeightGraphNode[] leafes = new DynamicWeightGraphNode[nrOfNodes];
		for (int i = 0; i < nrOfNodes; i++) {
			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0, weightassignment[i]);
			new DynamicWeightGraphEdge(node, tmpNode, clientLatency[i]);
			leafes[i] = tmpNode;
		}

		// calculate
		for (int i = 0; i < leafes.length; i++) {
			// the value is the time from client to server i + the value of the
			// other node
			leafes[i].setValue(clientLatency[i] + node.getValue());
		}
		return leafes;
	}

	public DynamicWeightGraphNode[] addLeaderPropose(DynamicWeightGraphNode[] nodes, int leaderID) {
		this.leaderID = leaderID;
		// Create
		DynamicWeightGraphNode[] leafes = new DynamicWeightGraphNode[nrOfNodes];
		for (int i = 0; i < nrOfNodes; i++) {
			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0, weightassignment[i]);
			new DynamicWeightGraphEdge(nodes[leaderID], tmpNode, serverLatencies[leaderID][i]);
			if (i != leaderID) {
				// no double edges ;)
				new DynamicWeightGraphEdge(nodes[i], tmpNode, 0);
			}
			leafes[i] = tmpNode;
		}

		// calculate
		for (int i = 0; i < leafes.length; i++) {
			// the value is the max of every incoming edge + the other nodes
			// value
			ArrayList<DynamicWeightGraphEdge> incEdges = leafes[i].getIncomingEdges();
			double[] maxTmp = new double[incEdges.size()];
			for (int j = 0; j < incEdges.size(); j++) {
				maxTmp[j] = incEdges.get(j).getEdgeValueSumNode();
			}

			leafes[i].setValue(max(maxTmp));
		}
		return leafes;
	}

	private double max(double[] values) {
		double max = values[0];
		for (int i = 1; i < values.length; i++) {
			if (values[i] > max) {
				max = values[i];
			}
		}
		return max;
	}

	public DynamicWeightGraphNode[] addMultiCast(DynamicWeightGraphNode[] nodes, int quorumSize) {
		// Create
		DynamicWeightGraphNode[] leafes = new DynamicWeightGraphNode[nrOfNodes];
		// nodes
		for (int i = 0; i < leafes.length; i++) {
			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0, weightassignment[i]);
			leafes[i] = tmpNode;
		}
		// edges from each nodes to each leaf
		for (int i = 0; i < nodes.length; i++) {
			for (int j = 0; j < leafes.length; j++) {
				new DynamicWeightGraphEdge(nodes[i], leafes[j], serverLatencies[i][j]);
			}
		}

		// calculate
		for (int i = 0; i < leafes.length; i++) {
			ArrayList<DynamicWeightGraphEdge> incEdges = leafes[i].getIncomingEdges();
			// fastest first
			Collections.sort(incEdges);

			ArrayList<DynamicWeightGraphEdge> fastestNodes = new ArrayList<>();
			double quroumCounter = 0;
			for (int j = 0; j < incEdges.size(); j++) {
				quroumCounter = quroumCounter + incEdges.get(j).getFromWeight();
				fastestNodes.add(incEdges.get(j));

				if (quroumCounter >= quorumSize) {
					break;
				}
			}

			double[] maxTmp = new double[fastestNodes.size()];
			for (int j = 0; j < fastestNodes.size(); j++) {
				maxTmp[j] = fastestNodes.get(j).getEdgeValueSumNode();
			}
			leafes[i].setValue(max(maxTmp));
		}

		return leafes;
	}
	
	public DynamicWeightGraphNode[] addDoubleMultiCast(DynamicWeightGraphNode[] nodes, int quorumSize) {
		// Create
		DynamicWeightGraphNode[] leafes = new DynamicWeightGraphNode[nrOfNodes];
		// nodes
		for (int i = 0; i < leafes.length; i++) {
			DynamicWeightGraphNode tmpNode = new DynamicWeightGraphNode(0, weightassignment[i]);
			leafes[i] = tmpNode;
		}
		// edges from each nodes to each leaf
		for (int i = 0; i < nodes.length; i++) {
			for (int j = 0; j < leafes.length; j++) {
				new DynamicWeightGraphEdge(nodes[i], leafes[j], serverLatencies[i][j]);
			}
		}

		// calculate
		for (int i = 0; i < leafes.length; i++) {
			ArrayList<DynamicWeightGraphEdge> incEdges = leafes[i].getIncomingEdges();
			// fastest first
			Collections.sort(incEdges);

			ArrayList<DynamicWeightGraphEdge> fastestNodes = new ArrayList<>();
			double quroumCounter = 0;
			for (int j = 0; j < incEdges.size(); j++) {
				quroumCounter = quroumCounter + incEdges.get(j).getFromWeight();
				fastestNodes.add(incEdges.get(j));

				if (quroumCounter >= quorumSize) {
					break;
				}
			}

			double[] maxTmp = new double[fastestNodes.size()];
			for (int j = 0; j < fastestNodes.size(); j++) {
				maxTmp[j] = fastestNodes.get(j).getEdgeValueSumNode();
			}
			leafes[i].setValue(max(maxTmp));
		}

		return leafes;
	}

	public DynamicWeightGraphNode addClientResponse(DynamicWeightGraphNode[] nodes, int quorumSize) {
		// Create
		DynamicWeightGraphNode leaf = new DynamicWeightGraphNode(0, 1d);

		for (int i = 0; i < nodes.length; i++) {
			new DynamicWeightGraphEdge(nodes[i], leaf, clientLatency[i]);
		}

		// Calculate
		ArrayList<DynamicWeightGraphEdge> incEdges = leaf.getIncomingEdges();
		// fastest first
		Collections.sort(incEdges);

		ArrayList<DynamicWeightGraphEdge> fastestNodes = new ArrayList<>();
		double quroumCounter = 0;
		for (int j = 0; j < incEdges.size(); j++) {
			quroumCounter = quroumCounter + incEdges.get(j).getFromWeight();
			fastestNodes.add(incEdges.get(j));

			if (quroumCounter >= quorumSize) {
				break;
			}
		}

		double[] maxTmp = new double[fastestNodes.size()];
		for (int j = 0; j < fastestNodes.size(); j++) {
			maxTmp[j] = fastestNodes.get(j).getEdgeValueSumNode();
		}
		leaf.setValue(max(maxTmp));
		calculatedValue = max(maxTmp);
		return leaf;
	}

	public double getCalculatedValue() {
		return calculatedValue;
	}

	@Override
	public int compareTo(DynamicWeightGraph o) {
		if (calculatedValue > o.getCalculatedValue()) {
			return 1;
		} else if (calculatedValue == o.getCalculatedValue()) {
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return "leader: " + leaderID + ", weightassignment: " + Arrays.toString(weightassignment)
				+ ", calculatedValue: " + calculatedValue;

	}

	public Double[] getWeightAssignment() {
		return weightassignment;
	}

	public int getLeaderID() {
		return leaderID;
	}
}
