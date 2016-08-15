package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class Reconfigurator implements Runnable {
	private LatencyStorage latStorage;
	private ServerViewController svController;

	public Reconfigurator(LatencyStorage latStorage, ServerViewController svController) {
		this.latStorage = latStorage;
		this.svController = svController;
	}

	private void calculateBest(double currentCalculatedValue, DynamicWeightGraph[] dwGraphs) {
		double betterPercentage = 1.0;
		// decide
		// if any new result is better than 10% of the current result ->
		// reconfig

		ArrayList<DynamicWeightGraph> newPossibleGraphs = new ArrayList<>();
		for (DynamicWeightGraph dynamicWeightGraph : dwGraphs) {
			if (dynamicWeightGraph.getCalculatedValue() < (currentCalculatedValue * betterPercentage)) {
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
			for (int i = 0; i < newConfig.getWeightAssignment().length; i++) {
				weights.put(i, newConfig.getWeightAssignment()[i]);
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
		double min = graphs[0].getCalculatedValue();
		DynamicWeightGraph currentMin = graphs[0];
		for (int i = 1; i < graphs.length; i++) {
			if (graphs[i].getCalculatedValue() < min) {
				min = graphs[i].getCalculatedValue();
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

	@Override
	public void run() {
		int currentN = svController.getCurrentViewN();
		int currentLeader = svController.getCurrentLeader();

		Double[] currentWeightAssignment = new Double[svController.getCurrentViewN()];
		for (int i = 0; i < currentWeightAssignment.length; i++) {
			currentWeightAssignment[i] = svController.getCurrentView().getWeight(i);
		}
		// get Data
		List<Latency[]> clientLatencies = latStorage.getClientLatencies();
		List<Latency[]> serverLatencies = latStorage.getServerLatencies();

		// reduce Data
		LatencyReducer mean = new MedianReducer();
		Latency[] reducedClients = mean.reduce2d(clientLatencies, currentN);
		double[] reducedClientValues = new double[reducedClients.length];
		for (int i = 0; i < reducedClientValues.length; i++) {
			reducedClientValues[i] = reducedClients[i].getValue() / 2;
		}

		// build server latency matrix
		double[][] reducedServerValues = new double[currentN][currentN];

		double maxValue = 0;
		for (Latency[] latencies : serverLatencies) {
			for (int i = 0; i < latencies.length; i++) {
				Latency lat = latencies[i];
				reducedServerValues[lat.getFrom()][lat.getTo()] = Math
						.max(reducedServerValues[lat.getFrom()][lat.getTo()], lat.getValue());
				// symmetric
				reducedServerValues[lat.getTo()][lat.getFrom()] = Math
						.max(reducedServerValues[lat.getTo()][lat.getFrom()], lat.getValue());
				if (lat.getValue() > maxValue) {
					maxValue = lat.getValue();
				}
			}
		}
		// replace empty values with 150% maxValue
		for (int i = 0; i < reducedServerValues.length; i++) {
			for (int j = 0; j < reducedServerValues[i].length; j++) {
				if (i != j && reducedServerValues[i][j] == 0) {
					reducedServerValues[i][j] = 1.5 * maxValue;
				}
			}
		}
		Logger.println("reducedClients: " + Arrays.toString(reducedClientValues));
		Logger.println("reducedServer: " + Arrays.deepToString(reducedServerValues));

		DynamicWeightGraphBuilder dwgBuilder = new DynamicWeightGraphBuilder(svController, currentLeader,
				currentWeightAssignment);
		DynamicWeightGraph[] dwGraphs = dwgBuilder.buildGraphs(reducedServerValues, reducedClientValues);
		Logger.println("Current Value is " + dwgBuilder.getCurrentCalculatedValue());
		Logger.println("Current Weightassignment is " + Arrays.toString(dwgBuilder.getCurrentWeightAssignment()));
		calculateBest(dwgBuilder.getCurrentCalculatedValue(), dwGraphs);
	}
}
