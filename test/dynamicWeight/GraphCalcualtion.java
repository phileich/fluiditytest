package dynamicWeight;

import bftsmart.dynamicWeights.DecisionLogic;
import bftsmart.reconfiguration.ServerViewController;

public class GraphCalcualtion {
	static private double[] reducedClientValues = new double[] { 16.0, 16.0, 56.0, 105.5, 105.5 };
	static private double[][] reducedServerValues = new double[][] { 
		{ 0.0, 425.0, 406.0, 424.0, 407.0 },
		{ 425.0, 0.0, 404.0, 403.0, 404.0 }, 
		{ 406.0, 404.0, 0.0, 223.0, 204.0 },
		{ 424.0, 403.0, 223.0, 0.0, 42.0 }, 
		{ 407.0, 404.0, 204.0, 42.0, 0.0 } };
	static private double[][] reducedServerProposeValues = new double[][] { 
		{ 0.0, 21.0, 101.5, 201.5, 201.5 },
		{ 21.0, 0.0, 102.0, 202.0, 201.5 }, 
		{ 101.5, 102.0, 0.0, 101.5, 102.0 }, 
		{ 201.5, 202.0, 101.5, 0.0, 21.5 },
		{ 201.5, 201.5, 102.0, 21.5, 0.0 } };

	public static void main(String[] args) {
		ServerViewController svController = new ServerViewController(0, "");

		DecisionLogic dl = new DecisionLogic(svController, 0, reducedClientValues, reducedServerProposeValues,
				reducedServerValues);

		dl.calculateBestGraph();
		dl.getBestLeader();
		dl.getBestWeightAssignment();
		dl.getBestLeader();
	}

}
