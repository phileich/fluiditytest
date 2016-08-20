package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class DecisionLogic {
	private ServerViewController svController;
	private double[] reducedClientValues;
	private double[][] reducedServerValues;
	private double[][] reducedServerProposeValues;
	private double currentCalculatedValue = Double.MAX_VALUE;
	private int currentLeader;
	private Double[] currentWeightAssignment;
	private double bestCalculatedValue;
	private Double[] bestWeightAssignment;
	private int bestLeader;

	public DecisionLogic(ServerViewController svController, double[] clientLatencies, double[][] proposeLatencies,
			double[][] serverLatencies) {
		this.svController = svController;
		this.reducedClientValues = clientLatencies;
		this.reducedServerProposeValues = serverLatencies;
		this.reducedServerValues = serverLatencies;

		currentLeader = svController.getCurrentLeader();

		currentWeightAssignment = new Double[svController.getCurrentViewN()];
		for (int i = 0; i < currentWeightAssignment.length; i++) {
			currentWeightAssignment[i] = svController.getCurrentView().getWeight(i);
		}
	}

	private DynamicWeightGraph[] buildGraphs() {
		// Build all graphs
		Logger.println("Building Calculation Graphs");
		int f = svController.getCurrentViewF();
		int n = svController.getCurrentViewN();
		double vMin = 1;
		// 3f+1 for BFT
		int requiredN = (3 * f) + 1;
		int deltaN = n - requiredN;
		double vMax = 1 + (deltaN / f);
		// nr of combinations
		int comb = n * binCoeff(n, 2 * f);

		DynamicWeightGraph[] dwGraphs = new DynamicWeightGraph[comb];

		// create weight assignment list
		// 2f replicas have weight vmax
		Double[] weightassignment = new Double[n];
		for (int i = 0; i < (2 * f); i++) {
			weightassignment[i] = vMax;
		}
		for (int i = (2 * f); i < weightassignment.length; i++) {
			weightassignment[i] = vMin;
		}

		// for each leader
		int combCount = 0;
		for (int i = 0; i < n; i++) {
			Logger.println("---------------------------------------");
			Logger.println("Leader: " + i);
			Permutations<Double> perm = new Permutations<Double>(weightassignment);
			while (perm.hasNext()) {
				Double[] permutation = perm.next();
				Logger.println(Arrays.toString(permutation));

				// Create the graph
				DynamicWeightGraphBuilder dwgBuilder = new DynamicWeightGraphBuilder().setWeights(permutation);
				DynamicWeightGraph dwGraph = dwgBuilder.addClientRequest(0, reducedClientValues, permutation)
						.addLeaderPropose(i, reducedServerProposeValues[i], permutation)
						.addMultiCast(reducedServerValues, permutation, getReplyQuorum())
						.addClientResponse(reducedClientValues, permutation, getReplyQuorum()).build();

				if (Arrays.deepEquals(permutation, currentWeightAssignment) && i == currentLeader) {
					currentCalculatedValue = dwGraph.getLeaves()[0].getValue(); // should
																				// be
																				// only
																				// one
																				// leaf!!
					
				}
				Logger.println("" + dwGraph.getLeaves()[0].getValue());
				// add graph
				dwGraphs[combCount] = dwGraph;

				combCount++;
			}

		}
		Logger.println("Current Leader is " + getCurrentLeader());
		Logger.println("Current Weightassignment is " + Arrays.toString(getCurrentWeightAssignment()));
		Logger.println("Current Value is " + getCurrentValue());
		return dwGraphs;
	}

	private int binCoeff(long n, long k) {
		if (k > n)
			return 0;
		else {
			int a = 1;
			for (long i = n - k + 1; i <= n; i++)
				a *= i;
			int b = 1;
			for (long i = 2; i <= k; i++)
				b *= i;
			return a / b;
		}
	}

	public void calculateBestGraph() {

		DynamicWeightGraph[] dwGraphs = buildGraphs();
		double betterPercentage = 1.0;
		// decide
		// if any new result is better than 10% of the current result ->
		// reconfig

		ArrayList<DynamicWeightGraph> newPossibleGraphs = new ArrayList<>();
		for (DynamicWeightGraph dynamicWeightGraph : dwGraphs) {
			if (dynamicWeightGraph.getLeaves()[0].getValue() < (currentCalculatedValue * betterPercentage)) {
				newPossibleGraphs.add(dynamicWeightGraph);
			}
		}

		if (newPossibleGraphs.size() > 0) {
			Logger.println("possible reconfigs: " + newPossibleGraphs);
			// get Min value of these
			DynamicWeightGraph newConfig = getMin(
					newPossibleGraphs.toArray(new DynamicWeightGraph[newPossibleGraphs.size()]));
			Logger.println("Reconfig to:  " + newConfig);

			// reconfig to newConfig Graph

			// map weights to processes
			HashMap<Integer, Double> weights = new HashMap<Integer, Double>();
			for (int i = 0; i < newConfig.getWeights().length; i++) {
				weights.put(i, newConfig.getWeights()[i]);
			}

			TreeMap<Integer, Double> sortedMap = sortMapByValue(weights);

			int[] newProcesses = new int[sortedMap.size()];
			Integer[] newProcessInteger = sortedMap.keySet().toArray(new Integer[sortedMap.size()]);
			for (int i = 0; i < newProcesses.length; i++) {
				newProcesses[i] = newProcessInteger[i].intValue();
			}

			Logger.println("new Weights@process " + Arrays.toString(newProcesses));
		} else {
			Logger.println("No configuration is better than the current one! NO RECONFIG");
		}

	}

	private DynamicWeightGraph getMin(DynamicWeightGraph[] graphs) {
		double min = graphs[0].getLeaves()[0].getValue();
		DynamicWeightGraph currentMin = graphs[0];
		for (int i = 1; i < graphs.length; i++) {
			if (graphs[i].getLeaves()[0].getValue() < min) {
				min = graphs[i].getLeaves()[0].getValue();
				currentMin = graphs[i];
			}
		}

		return currentMin;

	}

	private TreeMap<Integer, Double> sortMapByValue(HashMap<Integer, Double> map) {
		Comparator<Integer> comparator = new ValueComparator(map);
		// TreeMap is a map sorted by its keys.
		// The comparator is used to sort the TreeMap by keys.
		TreeMap<Integer, Double> result = new TreeMap<Integer, Double>(comparator);
		result.putAll(map);
		return result;
	}

	protected int getReplyQuorum() {

		// code for classic quorums
		/*
		 * if (getViewManager().getStaticConf().isBFT()) { return (int)
		 * Math.ceil((getViewManager().getCurrentViewN() +
		 * getViewManager().getCurrentViewF()) / 2) + 1; } else { return (int)
		 * Math.ceil((getViewManager().getCurrentViewN()) / 2) + 1; }
		 */

		// code for vote schemes

		if (svController.getStaticConf().isBFT()) {
			return (int) Math.ceil(
					(svController.getCurrentView().getOverlayN() + (svController.getCurrentView().getOverlayF()) + 1)
							/ 2);
		} else {

			// code for simple majority (of votes)
			// return (int)
			// Math.ceil(((getViewManager().getCurrentView().getOverlayN()) + 1)
			// / 2);

			// Code to only wait one reply
			Logger.println("(ServiceProxy.getReplyQuorum) only one reply will be gathered");
			return 1;
		}
	}

	public double getCurrentValue() {
		return currentCalculatedValue;
	}

	public Double[] getCurrentWeightAssignment() {
		return currentWeightAssignment;
	}

	public int getCurrentLeader() {
		return currentLeader;
	}

	public int getBestLeader() {
		return bestLeader;
	}

	public Double[] getBestWeightAssignment() {
		return bestWeightAssignment;
	}

	public double getBestCalculatedValue() {
		return bestCalculatedValue;
	}

}

class ValueComparator implements Comparator<Integer> {

	HashMap<Integer, Double> map = new HashMap<Integer, Double>();

	public ValueComparator(HashMap<Integer, Double> map) {
		this.map.putAll(map);
	}

	@Override
	public int compare(Integer s1, Integer s2) {
		if (map.get(s1) >= map.get(s2)) {
			return -1;
		} else {
			return 1;
		}
	}

}