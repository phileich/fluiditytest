package dynamicWeight;

import bftsmart.dynamicWeights.DecisionLogic;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class GraphCalcualtion {
	static private double[] reducedClientValues = new double[] { 32.0, 32.0, 112.0, 212.0, 212.0 };
	static private double[][] reducedServerValues = new double[][] { { 0.0, 430.0, 409.0, 425.0, 406.0 },
			{ 430.0, 0.0, 407.0, 406.0, 404.0 }, { 409.0, 407.0, 0.0, 222.0, 205.0 },
			{ 425.0, 406.0, 222.0, 0.0, 42.0 }, { 406.0, 404.0, 205.0, 42.0, 0.0 } };
	static private double[][] reducedServerProposeValues = new double[][] { { 0.0, 21.5, 102.5, 203.0, 203.5 },
			{ 21.5, 0.0, 102.5, 203.5, 203.5 }, { 102.5, 102.5, 0.0, 102.5, 102.5 }, { 203.0, 203.5, 102.5, 0.0, 22.5 },
			{ 203.5, 203.5, 102.5, 22.5, 0.0 } };

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
