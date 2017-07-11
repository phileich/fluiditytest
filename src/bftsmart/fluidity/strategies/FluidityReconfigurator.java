package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.Latency;
import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.FluidityController;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.reconfiguration.ServerViewController;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by philipp on 06.07.17.
 */
public class FluidityReconfigurator implements Runnable {
    private DistributionStrategy strategy;
    private ServerViewController serverViewController;
    private FluidityController fluidityController;
    private DynamicWeightController dynamicWeightController;

    private FluidityGraph newFluidityGraph;


    public FluidityReconfigurator(DistributionStrategy strategy, ServerViewController svController,
                                  FluidityController fluidityController) {
        this.strategy = strategy;
        this.serverViewController = svController;
        this.fluidityController = fluidityController;
        this.dynamicWeightController = this.fluidityController.getDwc();
    }

    @Override
    public void run() {
        LatencyStorage latencyStorage = fluidityController.getDwc().getLatStorage();
        FluidityGraph filledFluidityGraph = fillGraphWithLatency(latencyStorage.getServerLatencies());

        newFluidityGraph = strategy.getReconfigGraph(filledFluidityGraph, dynamicWeightController.getBestWeightAssignment());

        fluidityController.notifyNewFluidityGraph(newFluidityGraph);
    }



    private FluidityGraph fillGraphWithLatency(List<Latency[]> serverLatencies) {
        FluidityGraph returnGraph = serverViewController.getCurrentView().getFluidityGraph();
        List<FluidityGraphLatency> fluidityGraphLatencies = new ArrayList<>();

        // This class first completes the latency information of the graph with the one from the latency
        // storage and then calls the strategy
        // TODO The above

        for (Latency[] latency : serverLatencies) {
            for (int i = 0; i < latency.length; i++) {
                Latency tempLatency = latency[i];
                int replicaFrom = tempLatency.getFrom();
                int replicaTo = tempLatency.getTo();
                double latencyValue = tempLatency.getValue();

                int nodeFrom = returnGraph.getNodeIdFromReplicaId(replicaFrom);
                int nodeTo = returnGraph.getNodeIdFromReplicaId(replicaTo);

                FluidityGraphLatency latencyEntry = new FluidityGraphLatency(nodeFrom, nodeTo, latencyValue);
                fluidityGraphLatencies.add(latencyEntry);

                // TODO Erst alle latenzen zwischen gleichen nodes speichern und dann
                // erst auf einen Wert reduzieren und anschließend in Graph eintragen
            }
        }

        return getGraphWithReducedLatencies(fluidityGraphLatencies);
    }

    private FluidityGraph getGraphWithReducedLatencies(List<FluidityGraphLatency> graphLatencies) {
        FluidityGraph returnGraph = serverViewController.getCurrentView().getFluidityGraph();

        for (FluidityGraphLatency fgL : graphLatencies) {
            int from = fgL.getNodeIdFrom();
            int to = fgL.getNodeIdTo();
            FluidityGraphLatency tempObj = new FluidityGraphLatency(from, to);

            ArrayList<Integer> indexList = indexOfAll(tempObj, graphLatencies);
            ArrayList<FluidityGraphLatency> reduceList = new ArrayList<>();

            for(int index : indexList) {
                reduceList.add(graphLatencies.get(index));
            }


        }

        return returnGraph;
    }

    private ArrayList<Integer> indexOfAll(FluidityGraphLatency obj, List<FluidityGraphLatency> latencyList) {
        ArrayList<Integer> tempList = new ArrayList<>();

        for (int i = 0; i < latencyList.size(); i++) {
            if (obj.equals(latencyList.get(i))) {
                tempList.add(i);
            }
        }

        return tempList;
    }

    private double medianReducer(ArrayList<Double> list) {
        double[] value = new double[list.size()];
        value = list.toArray(value); //TODO Fix it
        Median median = new Median();

        median.setData(value);

        return median.evaluate();
    }

    private class FluidityGraphLatency {
        private int nodeIdFrom;
        private int nodeIdTo;
        private double latencyValue;

        public FluidityGraphLatency(int nodeIdFrom, int nodeIdTo) {
            this.nodeIdFrom = nodeIdFrom;
            this.nodeIdTo = nodeIdTo;
            this.latencyValue = -1;
        }

        public FluidityGraphLatency(int from, int to, double value) {
            this.nodeIdFrom = from;
            this.nodeIdTo = to;
            this.latencyValue = value;
        }

        public int getNodeIdFrom() {
            return nodeIdFrom;
        }

        public int getNodeIdTo() {
            return nodeIdTo;
        }

        public double getLatencyValue() {
            return latencyValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FluidityGraphLatency that = (FluidityGraphLatency) o;

            if (nodeIdFrom != that.nodeIdFrom) return false;
            return nodeIdTo == that.nodeIdTo;
        }

        @Override
        public int hashCode() {
            int result = nodeIdFrom;
            result = 31 * result + nodeIdTo;
            return result;
        }
    }
}
