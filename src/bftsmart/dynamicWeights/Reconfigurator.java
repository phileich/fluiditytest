package bftsmart.dynamicWeights;

import java.util.Arrays;
import java.util.List;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

public class Reconfigurator implements Runnable {
	private LatencyStorage latStorage;
	private ServerViewController svController;
	private DynamicWeightController dwController;

	public Reconfigurator(LatencyStorage latStorage, ServerViewController svController,
			DynamicWeightController dwController) {
		this.latStorage = latStorage;
		this.svController = svController;
		this.dwController = dwController;
	}

	@Override
	public void run() {
		Logger.println("Start Reconfiguration calculation");
		int currentN = svController.getCurrentViewN();

		// get last 'windowSize' entries
		List<Latency[]> clientLatencies = latStorage.getClientLatencies(dwController.getWindowSize());
		List<Latency[]> serverLatencies = latStorage.getServerLatencies(dwController.getWindowSize());
		List<Latency[]> serverProposeLatencies = latStorage.getServerProposeLatencies(dwController.getWindowSize());

		double[] reducedClientValues = new double[0];
		double[][] reducedServerValues = new double[0][0];
		double[][] reducedServerProposeValues = new double[0][0];

		if (clientLatencies.size() > 0) {
			// reduce Data
			LatencyReducer mean = new MedianReducer();
			Latency[] reducedClients = mean.reduce2d(clientLatencies, currentN);
			reducedClientValues = new double[reducedClients.length];
			for (int i = 0; i < reducedClientValues.length; i++) {
				reducedClientValues[i] = reducedClients[i].getValue() / 2;
			}
			Logger.println("reducedClients: " + Arrays.toString(reducedClientValues));
		}

		if (serverLatencies.size() > 0) {
			// build server latency matrix
			reducedServerValues = new double[currentN][currentN];

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
			Logger.println("reducedServer: " + Arrays.deepToString(reducedServerValues));
		}

		if (serverProposeLatencies.size() > 0) {
			// build server propose latency matrix
			reducedServerProposeValues = new double[currentN][currentN];

			double maxProposeValue = 0;
			for (Latency[] latencies : serverProposeLatencies) {
				for (int i = 0; i < latencies.length; i++) {
					Latency lat = latencies[i];
					reducedServerProposeValues[lat.getFrom()][lat.getTo()] = Math
							.max(reducedServerProposeValues[lat.getFrom()][lat.getTo()], lat.getValue());
					// symmetric
					reducedServerProposeValues[lat.getTo()][lat.getFrom()] = Math
							.max(reducedServerProposeValues[lat.getTo()][lat.getFrom()], lat.getValue());
					if (lat.getValue() > maxProposeValue) {
						maxProposeValue = lat.getValue();
					}
				}
			}
			// replace empty values with 150% maxValue
			for (int i = 0; i < reducedServerProposeValues.length; i++) {
				for (int j = 0; j < reducedServerProposeValues[i].length; j++) {
					if (i != j && reducedServerProposeValues[i][j] == 0) {
						reducedServerProposeValues[i][j] = 1.5 * maxProposeValue;
					}
				}
			}
			Logger.println("reducedServerPropose: " + Arrays.deepToString(reducedServerProposeValues));
		}

		DecisionLogic dl = new DecisionLogic(svController, reducedClientValues, reducedServerProposeValues,
				reducedServerValues);

		dl.calculateBestGraph();
		dl.getBestLeader();
		dl.getBestWeightAssignment();
		dl.getBestLeader();

		dwController.notifyReconfigFinished();

	}

}
