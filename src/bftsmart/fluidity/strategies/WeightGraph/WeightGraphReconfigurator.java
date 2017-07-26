package bftsmart.fluidity.strategies.WeightGraph;

import java.util.*;

import bftsmart.dynamicWeights.*;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.fluidity.strategies.StrategyLatency;
import bftsmart.tom.util.Logger;

public class WeightGraphReconfigurator implements Runnable {
    private LatencyStorage latStorage;
    private StrategyLatency strategyLatency;
    private int currentN;

    public WeightGraphReconfigurator(LatencyStorage latStorage, StrategyLatency strategyLatency, int currentN) {
        this.latStorage = latStorage;
        this.strategyLatency = strategyLatency;
        this.currentN = currentN;
    }

    @Override
    public void run() {
        Logger.println("Start Reconfiguration calculation");

        // get last 'windowSize' entries
        List<Latency[]> clientLatencies = latStorage.getClientLatencies();
        List<Latency[]> serverLatencies = latStorage.getServerLatencies();
        List<Latency[]> serverProposeLatencies = latStorage.getServerProposeLatencies();

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

        //TODO Delete the muted replicas and add latencies for new ones
        ArrayList<Integer> mutedReplicaIds = strategyLatency.getReplicaIDsToMove();
        Map<Integer, Double> latenciesOfMutedReplicas = strategyLatency.getLantenciesOfMutedReplicas();

        //Replace the reduced server latencies of the muted replicas
        for (int replicaId : mutedReplicaIds) {
            for (int j = 0; j < currentN; j++) {
                reducedServerValues[replicaId][j] = -1;
                reducedServerValues[j][replicaId] = -1;
            }
        }

        //Replace the reduced server propose latencies of the muted replicas
        for (int replicaId : mutedReplicaIds) {
            for (int j = 0; j < currentN; j++) {
                reducedServerProposeValues[replicaId][j] = -1;
                reducedServerProposeValues[j][replicaId] = -1;
            }
        }


        DecisionLogic dl = new WeightGraphDecisionLogic(svController, reducedClientValues, reducedServerProposeValues,
                reducedServerValues);

        dl.calculateBestGraph();
        dl.getBestLeader();
        dl.getBestWeightAssignment();
        dl.getBestLeader();

        strategyLatency.notifyReconfiguration(dl.getBestLeader(), dl.getBestWeightAssignment());

    }

}