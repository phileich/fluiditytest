package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Median;

public class MedianReducer implements LatencyReducer {

	@Override
	public Latency[] reduce(List<Latency> latencies, int currentN) {
		if (latencies.isEmpty()) {
			return null;
		}

		// convert List to array
		ArrayList<ArrayList<Latency>> latenciesArr = listTo2dArray(latencies, currentN);
		ClientLatency[] reducedLatencies = new ClientLatency[currentN];
		for (int i = 0; i < latenciesArr.size(); i++) {
			ArrayList<Latency> latencyOfReplica = latenciesArr.get(i);
			if (!latencyOfReplica.isEmpty()) {
				// Logger.println("Data of Replica " + i + ": " +
				// latencyOfReplica);
				double reducedLatencyValue = median(latencyOfReplica);
				ClientLatency reducedLatency = new ClientLatency();
				reducedLatency.setValue(reducedLatencyValue);
				reducedLatency.setFrom(latenciesArr.get(i).get(0).getFrom());
				reducedLatency.setTo(latenciesArr.get(i).get(0).getTo());
				reducedLatencies[i] = reducedLatency;
			}
		}

		return reducedLatencies;
	}

	private double median(ArrayList<Latency> data) {
		data.removeAll(Collections.singleton(null));
		Median median = new Median();
		// copy arraylist in array
		double[] dataArray = new double[data.size()];
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i) != null) {
				dataArray[i] = data.get(i).getValue();
			}
		}

		median.setData(dataArray);
		double medianValue = median.evaluate();

		return medianValue;
	}

	private ArrayList<ArrayList<Latency>> listTo2dArray(List<Latency> latencies, int currentN) {
		// sort first
		Collections.sort(latencies);
		ArrayList<ArrayList<Latency>> latenciesArrayList = new ArrayList<ArrayList<Latency>>();
		// init arraylist
		for (int i = 0; i < currentN; i++) {
			latenciesArrayList.add(new ArrayList<Latency>());
		}

		for (Latency latency : latencies) {
			ArrayList<Latency> row = latenciesArrayList.get(latency.getTo());
			row.add(latency);
		}
		return latenciesArrayList;
	}

	private ArrayList<ArrayList<Latency>> list2dTo2dArray(List<Latency[]> latencies, int currentN) {
		ArrayList<ArrayList<Latency>> latenciesArrayList = new ArrayList<ArrayList<Latency>>();
		// init arraylist
		for (int i = 0; i < currentN; i++) {
			latenciesArrayList.add(new ArrayList<Latency>());
		}

		for (Latency[] latencyArray : latencies) {
			for (int i = 0; i < currentN; i++) {
				if (latencyArray[i] != null) {
					ArrayList<Latency> row = latenciesArrayList.get(i);
					row.add(latencyArray[i]);
				}
			}
		}

		// Sort
		for (int i = 0; i < currentN; i++) {
			Collections.sort(latenciesArrayList.get(i));
		}

		return latenciesArrayList;
	}

	@Override
	public Latency[] reduce2d(List<Latency[]> latencies, int currentN) {
		if (latencies.isEmpty()) {
			return null;
		}
		// convert List to array
		ArrayList<ArrayList<Latency>> latenciesArr = list2dTo2dArray(latencies, currentN);
		ServerLatency[] reducedLatencies = new ServerLatency[currentN];
		for (int i = 0; i < latenciesArr.size(); i++) {
			ArrayList<Latency> latencyOfReplica = latenciesArr.get(i);
			if (!latencyOfReplica.isEmpty()) {
				// Logger.println("Data of Replica " + i + ": " +
				// latencyOfReplica);
				double reducedLatencyValue = median(latencyOfReplica);
				ServerLatency reducedLatency = new ServerLatency();
				reducedLatency.setValue(reducedLatencyValue);
				reducedLatency.setFrom(latenciesArr.get(i).get(0).getFrom());
				reducedLatency.setTo(latenciesArr.get(i).get(0).getTo());				
				reducedLatencies[i] = reducedLatency;
			}
		}

		return reducedLatencies;
	}
}
