package dynamicWeight;

import bftsmart.dynamicWeights.DecisionLogic;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class GraphCalcualtion {
	static private double[] reducedClientValues = new double[] { 33.0, 33.0, 112.0, 212.0, 213.0 };
	static private double[][] reducedServerValues = new double[][] { { -1.0, -1.0, -1.0, -1.0, -1.0 },
			{ -1.0, -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0, -1.0 }, { -1.0, -1.0, -1.0, -1.0, -1.0 },
			{ -1.0, -1.0, -1.0, -1.0, -1.0 } };

	// { { 0.0, 435.5, 414.5, 433.0, 421.0 },
	// { 435.5, 653.25, 410.0, 413.0, 411.0 }, { 414.5, 410.0, 653.25, 226.0,
	// 217.0 },
	// { 433.0, 413.0, 226.0, 653.25, 43.0 }, { 421.0, 411.0, 217.0, 43.0,
	// 653.25 } };
	static private double[][] reducedServerProposeValues = new double[][] { { 0.0, 22.0, 102.5, 203.5, 204.75 },
			{ 22.0, 0.0, 102.5, 203.5, 203.25 }, { 102.5, 102.5, 0.0, 103.0, 103.5 },
			{ 203.5, 203.5, 103.0, 0.0, 24.0 }, { 204.75, 203.25, 103.5, 24.0, 0.0 } };

	public static void main(String[] args) {
		ServerViewController svController = new ServerViewController(0, "");
		Logger.debug = true;
		DecisionLogic dl = new DecisionLogic(svController, 0, reducedClientValues, reducedServerProposeValues,
				reducedServerValues);

		dl.calculateBestGraph();
		dl.getBestLeader();
		dl.getBestWeightAssignment();
		dl.getBestLeader();
	}

}
