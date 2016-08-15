package bftsmart.dynamicWeights;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class DynamicWeightGraphBuilder {
	private ServerViewController svController;
	private DynamicWeightGraph[] dwGraphs;
	private int currentLeader;
	private Double[] currentWeightAssignment;
	private double currentCalculatedValue;

	DynamicWeightGraphBuilder(ServerViewController svController, int currentLeader, Double[] currentWeightAssignment) {
		this.svController = svController;
		this.currentLeader = currentLeader;
		this.currentWeightAssignment = currentWeightAssignment;
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

	public DynamicWeightGraph[] buildGraphs(double[][] serverLatencies, double[] clientlatencies) {
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

		dwGraphs = new DynamicWeightGraph[comb];

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
				DynamicWeightGraph dwGraph = new DynamicWeightGraph(new DynamicWeightGraphNode(0, 1d), clientlatencies,
						serverLatencies, permutation);

				dwGraph.addClientResponse(
						dwGraph.addDoubleMultiCast(dwGraph.addClientRequest(dwGraph.getRoot()), getReplyQuorum()),
						getReplyQuorum());

				if (Arrays.deepEquals(dwGraph.getWeightAssignment(), currentWeightAssignment)
						&& dwGraph.getLeaderID() == currentLeader) {
					this.currentCalculatedValue = dwGraph.getCalculatedValue();
				}
				Logger.println("" +dwGraph.getCalculatedValue());
				// add graph
				dwGraphs[combCount] = dwGraph;

				combCount++;
			}

		}
		return dwGraphs;

	}

	private int getReplyQuorum() {

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

	

	public double getCurrentCalculatedValue() {
		return currentCalculatedValue;
	}
	
	public Double[] getCurrentWeightAssignment(){
		return currentWeightAssignment;
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