package bftsmart.fluidity.strategies;

import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.Latency;
import bftsmart.dynamicWeights.LatencyStorage;
import bftsmart.fluidity.FluidityController;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.reconfiguration.ServerViewController;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.io.*;
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

    private FluidityGraph oldFluidityGraph;
    private FluidityGraph newFluidityGraph;


    public FluidityReconfigurator(ServerViewController svController,
                                  FluidityController fluidityController) {
        this.serverViewController = svController;
        this.fluidityController = fluidityController;
        this.dynamicWeightController = this.fluidityController.getDwc();
    }

    @Override
    public void run() {
        LatencyStorage latencyStorage = fluidityController.getDwc().getLatStorage();
        FluidityGraph filledFluidityGraph = fillGraphWithLatency(latencyStorage.getServerLatencies(false)); //TODO Problem old latency storage
        int numOfReplicasToMove = fluidityController.getNumberOfReplicasToMove();

        oldFluidityGraph = deepCopyFluidityGraph(filledFluidityGraph);
        newFluidityGraph = strategy.getReconfigGraph(filledFluidityGraph, dynamicWeightController.getBestWeightAssignment(),
                latencyStorage, numOfReplicasToMove, serverViewController);

        fluidityController.notifyNewFluidityGraph(newFluidityGraph, oldFluidityGraph);
    }

    public void setDistributionStrategy(DistributionStrategy strategy) {
        this.strategy = strategy;
    }

    private FluidityGraph deepCopyFluidityGraph(FluidityGraph fluidityGraph) {
        FluidityGraph clonedGraph = null;


        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(fluidityGraph);
            oos.flush();
            oos.close();
            baos.close();
            byte[] byteGraph = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(byteGraph);
            clonedGraph = (FluidityGraph) new ObjectInputStream(bais).readObject();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return clonedGraph;
    }

    private FluidityGraph fillGraphWithLatency(List<Latency[]> serverLatencies) {
        FluidityGraph returnGraph = serverViewController.getCurrentView().getFluidityGraph();
        List<FluidityGraphLatency> fluidityGraphLatencies = new ArrayList<>();

        // This class first completes the latency information of the graph with the one from the latency
        // storage and then calls the strategy

        for (Latency[] latency : serverLatencies) { //TODO Check for null in replicas
            for (int i = 0; i < latency.length; i++) {
                Latency tempLatency = latency[i];
                int replicaFrom = tempLatency.getFrom(); //TODO Nullpointer here
                int replicaTo = tempLatency.getTo();
                double latencyValue = tempLatency.getValue();

                int nodeFrom = returnGraph.getNodeIdFromReplicaId(replicaFrom);
                int nodeTo = returnGraph.getNodeIdFromReplicaId(replicaTo);

                FluidityGraphLatency latencyEntry = new FluidityGraphLatency(nodeFrom, nodeTo, latencyValue);
                fluidityGraphLatencies.add(latencyEntry);
            }
        }

        return getGraphWithReducedLatencies(fluidityGraphLatencies);
    }

    private FluidityGraph getGraphWithReducedLatencies(List<FluidityGraphLatency> graphLatencies) {
        FluidityGraph returnGraph = serverViewController.getCurrentView().getFluidityGraph();
        ArrayList<Integer> tempGraphLatencyIndex = new ArrayList<>();;

        for (FluidityGraphLatency fgL : graphLatencies) {
            int from = fgL.getNodeIdFrom();
            int to = fgL.getNodeIdTo();
            FluidityGraphLatency tempObj = new FluidityGraphLatency(from, to);

            ArrayList<Integer> indexList = indexOfAll(tempObj, graphLatencies);
            ArrayList<FluidityGraphLatency> reduceList = new ArrayList<>();

            if (!tempGraphLatencyIndex.contains(indexList.get(0))) {
                for(int index : indexList) {
                    reduceList.add(graphLatencies.get(index));
                }

                ArrayList<Double> reduceValues = new ArrayList<>();
                for (FluidityGraphLatency fgl : reduceList) {
                    reduceValues.add(fgl.getLatencyValue());
                }

                double medianValue = medianReducer(reduceValues);

                returnGraph.changeEdgeLatencyData(from, to, medianValue);

                tempGraphLatencyIndex.addAll(indexList);
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
        Double[] tempValue = new Double[list.size()];
        tempValue = list.toArray(tempValue);
        value = convertDouble(tempValue);
        Median median = new Median();

        median.setData(value);

        return median.evaluate();
    }

    private double[] convertDouble(Double[] array) {
        double[] tempArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            tempArray[i] = array[i];
        }
        return tempArray;
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
