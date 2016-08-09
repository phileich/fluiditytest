package bftsmart.dynamicWeights;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.filter.KalmanFilter;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.AbstractPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;

public class Kalman implements LatencyReducer {
	private KalmanFilter filter;

	// Plotting
	private boolean plotKalman = false;
	private ArrayList<Double> realValues = new ArrayList<>();
	private ArrayList<Double> estimatedValues = new ArrayList<>();

	public double[] reduce(List<ClientLatency> latencies, int currentN) {
		initKalman(currentN);
		// convert List to array
		ArrayList<ArrayList<Double>> latenciesArr = listTo2dArray(latencies, currentN);
		double[] reducedLatencies = new double[currentN];
		for (int i = 0; i < latenciesArr.size(); i++) {
			// for each replica do one kalman
			ArrayList<Double> latencyOfReplica = latenciesArr.get(i);
			double reducedLatency = kalman(latencyOfReplica);
			reducedLatencies[i] = reducedLatency;
		}

		return reducedLatencies;
	}

	private void initKalman(int currentN) {
		Double a_value = 1d;
		Double h_value = 1d;
		Double q_value = 0.1d;
		Double p0_value = 10d;
		Double r_value = 0.1d;

		// A = [ 1 ]
		RealMatrix A = MatrixUtils.createRealDiagonalMatrix(new double[] { a_value });
		// RealMatrix A = new Array2DRowRealMatrix(new double[][] { { 1d, 0, 0
		// }, { 0, 1d, 0 }, { 0, 0, 1d } });

		// B = null
		RealMatrix B = null;

		// H = [ 1 ]

		RealMatrix H = MatrixUtils.createRealDiagonalMatrix(new double[] { h_value });
		// RealMatrix H = new Array2DRowRealMatrix(new double[][] { { 1d, 0, 0
		// }, { 0, 1d, 0 }, { 0, 0, 1d } });

		// x = [ 10 ]
		RealVector x = new ArrayRealVector(new double[] { 0d });

		// Q = [ 0.01 ]
		RealMatrix Q = MatrixUtils.createRealDiagonalMatrix(new double[] { q_value });
		// RealMatrix Q = new Array2DRowRealMatrix(new double[][] { { 0.1d, 0, 0
		// }, { 0, 0.1d, 0 }, { 0, 0, 0.1d } });

		// P = [ 1 ]
		RealMatrix P0 = MatrixUtils.createRealDiagonalMatrix(new double[] { p0_value });
		// RealMatrix P0 = new Array2DRowRealMatrix(new double[][] { { 10, 0, 0
		// }, { 0, 10, 0 }, { 0, 0, 10 } });

		// R = [ 0.1 ]
		RealMatrix R = MatrixUtils.createRealDiagonalMatrix(new double[] { r_value });
		// RealMatrix R = new Array2DRowRealMatrix(new double[][] { { 0.1, 0, 0
		// }, { 0, 0.1, 0 }, { 0, 0, 0.1 } });

		ProcessModel pm = new DefaultProcessModel(A, B, Q, x, P0);
		MeasurementModel mm = new DefaultMeasurementModel(H, R);
		filter = new KalmanFilter(pm, mm);
	}

	private double kalman(ArrayList<Double> data) {
		for (int i = 0; i < data.size(); i++) {
			filter.predict();
			double[] z = new double[] { data.get(i) };
			filter.correct(z);
			if (plotKalman) {
				realValues.add(data.get(i));
				estimatedValues.add(filter.getStateEstimation()[0]);
			}
		}
		if (plotKalman) {
			plotKalmanFilter();
		}

		return filter.getStateEstimation()[0];
	}

	private ArrayList<ArrayList<Double>> listTo2dArray(List<ClientLatency> clientLatencies, int currentN) {
		ArrayList<ArrayList<Double>> clientLatenciesArrayList = new ArrayList<ArrayList<Double>>();
		// double[][] clientLatenciesArray = new double[currentN][];
		// init arraylist
		for (int i = 0; i < currentN; i++) {
			clientLatenciesArrayList.add(new ArrayList<Double>());
		}

		for (ClientLatency clientLatency : clientLatencies) {
			ArrayList<Double> row = clientLatenciesArrayList.get(clientLatency.getTo());
			row.add((double) clientLatency.getValue());
		}
		return clientLatenciesArrayList;
	}

	private void plotKalmanFilter() {
		// convert resultarray to 2d
		double[][] resultPlot = new double[estimatedValues.size()][1];
		for (int i = 0; i < estimatedValues.size(); i++) {
			resultPlot[i][0] = estimatedValues.get(i);
		}

		double[][] referencePlot = new double[realValues.size()][1];
		for (int i = 0; i < realValues.size(); i++) {
			referencePlot[i][0] = realValues.get(i);
		}

		JavaPlot p = new JavaPlot();

		p.addPlot(resultPlot);
		p.addPlot(referencePlot);

		PlotStyle ps = ((AbstractPlot) p.getPlots().get(0)).getPlotStyle();
		ps.setStyle(Style.LINES);

		ps = ((AbstractPlot) p.getPlots().get(1)).getPlotStyle();
		// ps.setStyle(Style.LINES);

		p.plot();

	}
}
