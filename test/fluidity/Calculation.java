package fluidity;

import bftsmart.dynamicWeights.DecisionLogic;
import bftsmart.dynamicWeights.Latency;
import bftsmart.dynamicWeights.LatencyReducer;
import bftsmart.dynamicWeights.MedianReducer;
import bftsmart.reconfiguration.ServerViewController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by philipp on 15.05.17.
 */
public class Calculation {
    public static void main(String[] args) {
        ServerViewController svController = new ServerViewController(0, "");
        int currentN = 6;

        // get last 'windowSize' entries
        List<Latency[]> clientLatencies = new ArrayList<Latency[]>();
        clientLatencies.add(new Latency[] { new Latency(1001, 0, 32.0), new Latency(1001, 1, 32.0),
                new Latency(1001, 2, 112.0), new Latency(1001, 3, 212.0), new Latency(1001, 4, 212.0), new Latency(1001, 5, 215.0) });
        clientLatencies.add(new Latency[] { new Latency(1001, 0, 32.0), new Latency(1001, 1, 32.0),
                new Latency(1001, 2, 112.0), new Latency(1001, 3, 212.0), new Latency(1001, 4, 212.0), new Latency(1001, 5, 215.0) });
        clientLatencies.add(new Latency[] { new Latency(1001, 0, 32.0), new Latency(1001, 1, 32.0),
                new Latency(1001, 2, 112.0), new Latency(1001, 3, 212.0), new Latency(1001, 4, 212.0), new Latency(1001, 5, 215.0) });
        clientLatencies.add(new Latency[] { new Latency(1001, 0, 32.0), new Latency(1001, 1, 32.0),
                new Latency(1001, 2, 112.0), new Latency(1001, 3, 212.0), new Latency(1001, 4, 212.0), new Latency(1001, 5, 215.0) });
        clientLatencies.add(new Latency[] { new Latency(1001, 0, 32.0), new Latency(1001, 1, 32.0),
                new Latency(1001, 2, 112.0), new Latency(1001, 3, 212.0), new Latency(1001, 4, 212.0), new Latency(1001, 5, 215.0) });
        clientLatencies.add(new Latency[] { new Latency(1001, 0, 32.0), new Latency(1001, 1, 32.0),
                new Latency(1001, 2, 112.0), new Latency(1001, 3, 212.0), new Latency(1001, 4, 212.0), new Latency(1001, 5, 215.0) });

        List<Latency[]> serverLatencies = new ArrayList<Latency[]>();
        serverLatencies.add(new Latency[] { new Latency(2, 0, 406.0), new Latency(2, 1, 407.0), new Latency(2, 2, 0.0),
                new Latency(2, 3, 222.0), new Latency(2, 4, 204.0), new Latency(2, 5, 210.0) });
        serverLatencies.add(new Latency[] { new Latency(0, 0, 0.0), new Latency(0, 1, 430.0), new Latency(0, 2, 409.0),
                new Latency(0, 3, 425.0), new Latency(0, 4, 406.0), new Latency(0, 5, 460.0) });
        serverLatencies.add(new Latency[] { new Latency(1, 0, 407.0), new Latency(1, 1, 0.0), new Latency(1, 2, 387.0),
                new Latency(1, 3, 403.0), new Latency(1, 4, 385.0), new Latency(1, 5, 410.0) });
        serverLatencies.add(new Latency[] { new Latency(4, 0, 405.0), new Latency(4, 1, 404.0),
                new Latency(4, 2, 205.0), new Latency(4, 3, 42.0), new Latency(4, 4, 0.0), new Latency(4, 5, 310.0) });
        serverLatencies.add(new Latency[] { new Latency(3, 0, 406.0), new Latency(3, 1, 406.0),
                new Latency(3, 2, 206.0), new Latency(3, 3, 0.0), new Latency(3, 4, 23.0), new Latency(3, 5, 210.0) });
        serverLatencies.add(new Latency[] { new Latency(5, 0, 408.0), new Latency(5, 1, 404.0),
                new Latency(5, 2, 210.0), new Latency(5, 3, 0.0), new Latency(5, 4, 23.0), new Latency(5, 5, 0.0) });

        List<Latency[]> serverProposeLatencies = new ArrayList<Latency[]>();
        serverProposeLatencies.add(new Latency[] { new Latency(2, 0, 102.0), new Latency(2, 1, 102.0),
                new Latency(2, 2, 0.0), new Latency(2, 3, 102.5), new Latency(2, 4, 102.5), new Latency(2, 5, 102.0) });
        serverProposeLatencies.add(new Latency[] { new Latency(0, 0, 0.0), new Latency(0, 1, 21.5),
                new Latency(0, 2, 102.5), new Latency(0, 3, 203.0), new Latency(0, 4, 203.5), new Latency(0, 5, 203.0) });
        serverProposeLatencies.add(new Latency[] { new Latency(1, 0, 21.5), new Latency(1, 1, 0.0),
                new Latency(1, 2, 102.5), new Latency(1, 3, 203.5), new Latency(1, 4, 203.5), new Latency(1, 5, 203.0) });
        serverProposeLatencies.add(new Latency[] { new Latency(4, 0, 201.75), new Latency(4, 1, 202.0),
                new Latency(4, 2, 102.0), new Latency(4, 3, 22.5), new Latency(4, 4, 0.0), new Latency(4, 5, 202.0) });
        serverProposeLatencies.add(new Latency[] { new Latency(3, 0, 202.0), new Latency(3, 1, 201.75),
                new Latency(3, 2, 101.25), new Latency(3, 3, 0.0), new Latency(3, 4, 22.25), new Latency(3, 5, 201.0) });
        serverProposeLatencies.add(new Latency[] { new Latency(3, 0, 202.0), new Latency(3, 1, 201.75),
                new Latency(3, 2, 101.25), new Latency(3, 3, 0.0), new Latency(3, 4, 22.25), new Latency(5, 5, 0.0) });

        System.out.println("clientLatencies: " + Arrays.deepToString(clientLatencies.toArray()));
        System.out.println("serverLatencies: " + Arrays.deepToString(serverLatencies.toArray()));
        System.out.println("serverProposeLatencies: " + Arrays.deepToString(serverProposeLatencies.toArray()));

        double[] reducedClientValues = new double[0];
        double[][] reducedServerValues = new double[0][0];
        double[][] reducedServerProposeValues = new double[0][0];

        if (clientLatencies.size() > 0) {
            // reduce Data
            LatencyReducer mean = new MedianReducer();
            Latency[] reducedClients = mean.reduce2d(clientLatencies, currentN);
            reducedClientValues = new double[reducedClients.length];
            for (int i = 0; i < reducedClientValues.length; i++) {
                if (reducedClients[i] != null) {
                    reducedClientValues[i] = reducedClients[i].getValue();
                }
            }
            System.out.println("reducedClients: " + Arrays.toString(reducedClientValues));
        }

        // --------------------------------- SERVER
        // ---------------------------------------
        if (serverLatencies.size() > 0) {
            // init with -1
            reducedServerValues = new double[currentN][currentN];
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
            System.out.println("reducedServer: " + Arrays.deepToString(reducedServerValues));
        }

        // --------------------------------- PROPOSE
        // ---------------------------------------
        if (serverProposeLatencies.size() > 0) {
            // build server propose latency matrix
            reducedServerProposeValues = new double[currentN][currentN];
            for (int i = 0; i < reducedServerProposeValues.length; i++) {
                for (int j = 0; j < reducedServerProposeValues[0].length; j++) {
                    reducedServerProposeValues[i][j] = -1d;
                }
            }

            double maxProposeValue = 0;
            for (Latency[] latencies : serverProposeLatencies) {
                for (int i = 0; i < latencies.length; i++) {
                    Latency lat = latencies[i];
                    if (lat != null) {
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
            }
            // replace empty values with 150% maxValue
            for (int i = 0; i < reducedServerProposeValues.length; i++) {
                for (int j = 0; j < reducedServerProposeValues[i].length; j++) {
                    if (reducedServerProposeValues[i][j] == -1) {
                        reducedServerProposeValues[i][j] = 1.5 * maxProposeValue;
                    }
                }
            }
            System.out.println("reducedServerPropose: " + Arrays.deepToString(reducedServerProposeValues));
        }

        DecisionLogic dl = new DecisionLogic(svController, 0, reducedClientValues, reducedServerProposeValues,
                reducedServerValues);

        dl.calculateBestGraph();
        dl.getBestLeader();
        dl.getBestWeightAssignment();
        dl.getBestLeader();
    }
}
