package dynamicWeight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bftsmart.dynamicWeights.Latency;
import bftsmart.dynamicWeights.MedianReducer;

public class LatencyReducerTest {
	// setup
	double[][] server = new double[][] { { 0.0, 42.0, 202.0, 402.0, 402.0 }, { 22.0, 0.0, 202.0, 402.0, 402.0 },
			{ 202.0, 221.0, 0.0, 401.0, 402.0 }, { 382.0, 401.0, 382.0, 0.0, 402.0 },
			{ 403.0, 422.0, 403.0, 423.0, 0.0 } };

	static List<Latency[]> serverLatencies = new ArrayList<Latency[]>();
	static List<Latency> clientLatencies = new ArrayList<Latency>();

	static Latency[] a = new Latency[] { new Latency(0, 0, 0.0), new Latency(0, 1, 8.0), new Latency(0, 2, 10.0),
			new Latency(0, 3, 9.0), new Latency(0, 4, 8.0) };
	static Latency[] aN = new Latency[] { new Latency(0, 0, 0.0), new Latency(0, 1, 42.0), new Latency(0, 2, 202.0),
			new Latency(0, 3, 402.0), null };

	static Latency[] a1 = new Latency[] { new Latency(0, 0, 0.0), null, new Latency(0, 2, 202.0),
			new Latency(0, 3, 402.0), new Latency(0, 4, 402.0) };
	static Latency[] a2 = new Latency[] { new Latency(0, 0, 0.0), new Latency(0, 1, 42.0), new Latency(0, 2, 202.0),
			null, null };
	static Latency[] a3 = new Latency[] { new Latency(0, 0, 10.0), new Latency(0, 1, 999.0), new Latency(0, 2, 1202.0),
			new Latency(0, 3, 1402.0), new Latency(0, 4, 1402.0) };

	static Latency[] b = new Latency[] { new Latency(1, 0, 8.0), new Latency(1, 1, 0.0), new Latency(1, 2, 7.0),
			new Latency(1, 3, 7.0), new Latency(1, 4, 6.0) };
	static Latency[] c = new Latency[] { new Latency(2, 0, 10.0), new Latency(2, 1, 6.5), new Latency(2, 2, 0.0),
			new Latency(2, 3, 7.0), new Latency(2, 4, 7.0) };
	static Latency[] d = new Latency[] { new Latency(3, 0, 382.0), new Latency(3, 1, 401.0), new Latency(3, 2, 382.0),
			new Latency(3, 3, 0.0), new Latency(3, 4, 402.0) };
	static Latency[] e = new Latency[] { new Latency(4, 0, 9.0), new Latency(4, 1, 6.0), new Latency(4, 2, 7.0),
			new Latency(4, 3, 7.0), new Latency(4, 4, 0.0) };

	static Latency[] eN = new Latency[] { null, new Latency(4, 1, 422.0), new Latency(4, 2, 403.0),
			new Latency(4, 3, 423.0), new Latency(4, 4, 0.0) };

	static Latency[] f = new Latency[] { new Latency(1, 0, 403.0), new Latency(2, 1, 422.0), new Latency(4, 2, 403.0),
			new Latency(0, 3, 423.0), new Latency(3, 4, 0.0) };

	public static void main(String[] args) {
		serverLatencies.add(c);
		serverLatencies.add(b);
		serverLatencies.add(e);
		serverLatencies.add(a);

		// serverLatencies.add(a1);
		// serverLatencies.add(a1);
		// serverLatencies.add(a1);

		clientLatencies.add(new Latency(1001, 0, 403.0));
		clientLatencies.add(new Latency(1001, 3, 403.0));
		clientLatencies.add(new Latency(1001, 0, 403.0));
		clientLatencies.add(new Latency(1001, 1, 403.0));
		clientLatencies.add(new Latency(1001, 3, 403.0));
		clientLatencies.add(new Latency(1001, 1, 403.0));
		clientLatencies.add(new Latency(1001, 1, 403.0));
		clientLatencies.add(new Latency(1001, 0, 403.0));
		// clientLatencies.add(new Latency(1001, 2, 403.0));
		clientLatencies.add(new Latency(1001, 4, 403.0));
		clientLatencies.add(new Latency(1001, 4, 303.0));
		clientLatencies.add(new Latency(1001, 4, 203.0));

		Latency[] reducedServerValues = new MedianReducer().reduce2d(serverLatencies, 5);
		System.out.println(Arrays.deepToString(reducedServerValues));

		// Latency[] reducedClientLat = new
		// MedianReducer().reduce(clientLatencies, 5);
		// System.out.println(Arrays.deepToString(reducedClientLat));
		 buildMatrix();

	}

	private static void buildMatrix() {
		// build server latency matrix
		double[][] reducedServerValues = new double[5][5];
		// init with -1
		for (int i = 0; i < reducedServerValues.length; i++) {
			for (int j = 0; j < reducedServerValues[0].length; j++) {
				reducedServerValues[i][j] = -1d;
			}
		}

		double maxValue = 0;
		for (Latency[] latencies : serverLatencies) {
			for (int i = 0; i < latencies.length; i++) {
				Latency lat = latencies[i];
				if (lat != null) {
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
		}
		// replace empty values with 150% maxValue
		for (int i = 0; i < reducedServerValues.length; i++) {
			for (int j = 0; j < reducedServerValues[i].length; j++) {
				if (reducedServerValues[i][j] == -1) {
					reducedServerValues[i][j] = 1.5 * maxValue;
				}
			}
		}

		System.out.println(Arrays.deepToString(reducedServerValues));
	}

}
