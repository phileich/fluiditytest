package bftsmart.fluidity.strategies.WeightGraph;

import bftsmart.dynamicWeights.DynamicWeightGraph;
import bftsmart.dynamicWeights.DynamicWeightGraphBuilder;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.util.Logger;

import bftsmart.fluidity.strategies.WeightGraph.Permutations;

import java.util.*;

/**
 * Created by philipp on 23.07.17.
 */
public class WeightGraphDecisionLogic {

    private ServerViewController svController;
    private double[] reducedClientValues;
    private double[][] reducedServerValues;
    private double[][] reducedServerProposeValues;
    private double currentCalculatedValue = Double.MAX_VALUE;
    private int currentLeader;
    private Double[] currentWeightAssignment;
    private double bestCalculatedValue;
    private Map<Integer, Double> bestWeightAssignment;
    private int bestLeader;

    public    WeightGraphDecisionLogic(ServerViewController svController, double[] clientLatencies, double[][] proposeLatencies,
                         double[][] serverLatencies) {
        this(svController, svController.getCurrentLeader(), clientLatencies, proposeLatencies, serverLatencies);
    }

    public WeightGraphDecisionLogic(ServerViewController svController, int leader, double[] clientLatencies,
                         double[][] proposeLatencies, double[][] serverLatencies) {
        this.svController = svController;
        this.reducedClientValues = clientLatencies;
        this.reducedServerProposeValues = proposeLatencies;
        this.reducedServerValues = serverLatencies;

        currentLeader = leader;

        currentWeightAssignment = new Double[svController.getCurrentViewN()];
        for (int i = 0; i < currentWeightAssignment.length; i++) {
            currentWeightAssignment[i] = svController.getCurrentView().getWeight(i);
        }
    }

    private DynamicWeightGraph[] buildGraphs() {
        // Build all graphs
        System.out.println("Building Calculation Graphs");
        int f = svController.getCurrentViewF();
        int n = svController.getCurrentViewN();
        double vMin = 1;
        double vZero = 0.0;
        boolean isBFT = svController.isCurrentViewUseBFT();
        int u = (isBFT ? (2*f) : f);

        // 3f+1 for BFT
        //int requiredN = (3 * f) + 1;
        int requiredN = (isBFT ? (3*f) : (2*f)) + 1;

        int deltaN;
        boolean useFluidity = svController.isCurrentViewUseFluidity();
        if (useFluidity) {
            deltaN = svController.getCurrentViewDelta();
        } else {
            deltaN = n - requiredN;
        }

        int dynWheatN = (isBFT ? (3*f) : (2*f)) + 1 + deltaN;
        int fluidityDelta = n - dynWheatN;
        double vMax = 1 + (deltaN / f);
        // nr of combinations

        int numOfFluidityComp = binCoeff(n, fluidityDelta);
        int comb = n * binCoeff(dynWheatN, 2 * f) * numOfFluidityComp; //TODO Check if 2*f is also correct for CFT mode

        DynamicWeightGraph[] dwGraphs = new DynamicWeightGraph[comb];

        // create weight assignment list
        // 2f replicas have weight vmax
        Double[] weightassignment = new Double[n];
        for (int i = 0; i < u; i++) {
            weightassignment[i] = vMax;
        }

        // The rest of the replicas which are no special replicas
        int numOfSpecialReplica = n - ((isBFT ? (3*f) : (2*f)) + 1 + deltaN);
        int dynWheatLength =  weightassignment.length - numOfSpecialReplica;
        for (int i = (2 * f); i < dynWheatLength; i++) {
            weightassignment[i] = vMin;
        }

        // Assign weight 0 to all the rest
        for (int i = dynWheatLength; i < n; i++) {
            weightassignment[i] = vZero;
        }

        // for each leader
        int combCount = 0;
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            System.out.println("---------------------------------------");
            System.out.println("Leader: " + i);
            Permutations<Double> perm = new Permutations<Double>(weightassignment);
            while (perm.hasNext()) {
                Double[] permutation = perm.next().clone();
                System.out.println(Arrays.toString(permutation));

                // Create the graph
                DynamicWeightGraphBuilder dwgBuilder = new DynamicWeightGraphBuilder().setWeights(permutation)
                        .setQuorumSize(getReplyQuorum()).setLeader(i);

                if (svController.getStaticConf().measureClients()) {
                    dwgBuilder.addClientRequest(0, reducedClientValues, permutation);
                } else {
                    // just for creating nodes
                    dwgBuilder.addEmptyClientRequest(0, permutation.length);
                }
                if (svController.getStaticConf().measureServers()) {
                    dwgBuilder.addLeaderPropose(i, reducedServerProposeValues[i], permutation);
                    dwgBuilder.addMultiCast(reducedServerValues, permutation);
                    if (svController.getStaticConf().useWriteResponse()) {
                        //add a second for the accept phase
                        dwgBuilder.addMultiCast(reducedServerValues, permutation);
                    }
                }
                if (svController.getStaticConf().measureClients()) {
                    dwgBuilder.addClientResponse(reducedClientValues, permutation);
                }
                DynamicWeightGraph dwGraph = dwgBuilder.build();

                if (Arrays.deepEquals(permutation, currentWeightAssignment) && i == currentLeader) {
                    currentCalculatedValue = dwGraph.getValue();
                }
                System.out.println("" + dwGraph);
                // add graph
                dwGraphs[combCount] = dwGraph;

                combCount++;
            }

        }
        long end = System.currentTimeMillis();
        System.out.println("Building complete - " + (end - start) + "ms");
        System.out.println("Current Leader is " + getCurrentLeader());
        System.out.println("Current Weightassignment is " + Arrays.toString(getCurrentWeightAssignment()));
        System.out.println("Current Value is " + getCurrentValue());
        return dwGraphs;
    }

    private int binCoeff(long n, long k) {
        if (k > n)
            return 0;
        else {
            int a = 1;
            for (long i = n - k + 1; i <= n; i++)
                a *= i;
            int b = 1;
            for (long i = 2; i <= k; i++)
                b *= i;
            return a / b;
        }
    }

    public void calculateBestGraph() {
        long start = System.currentTimeMillis();
        System.out.println("--------- Calculation started -------------");
        DynamicWeightGraph[] dwGraphs = buildGraphs();
        long endBuild = System.currentTimeMillis();
        System.out.println("--------- Build Graphs complete (Duration: " + (endBuild - start) + "ms) -------------");
        double betterPercentage = 1.0;
        // decide
        // if any new result is better than 10% of the current result ->
        // reconfig
        long startBest = System.currentTimeMillis();
        System.out.println("--------- calc Best -------------");
        ArrayList<DynamicWeightGraph> newPossibleGraphs = new ArrayList<DynamicWeightGraph>();
        for (DynamicWeightGraph dynamicWeightGraph : dwGraphs) {
            // Logger.println(dynamicWeightGraph);
            if (dynamicWeightGraph.getValue() < (currentCalculatedValue * betterPercentage)) {
                newPossibleGraphs.add(dynamicWeightGraph);
            }
        }

        if (newPossibleGraphs.size() > 0) {
            System.out.println("possible reconfigs: " + newPossibleGraphs);
            // get Min value of these
            DynamicWeightGraph newConfig = getMin(
                    newPossibleGraphs.toArray(new DynamicWeightGraph[newPossibleGraphs.size()]));
            System.out.println("Reconfig to:  " + newConfig);

            // reconfig to newConfig graph

            // map weights to processes
            HashMap<Integer, Double> weights = new HashMap<Integer, Double>();
            for (int i = 0; i < newConfig.getWeights().length; i++) {
                weights.put(i, newConfig.getWeights()[i]); //TODO Is i really the process ID? normally they start with 1
            }

            bestWeightAssignment = weights;

            TreeMap<Integer, Double> sortedMap = sortMapByValue(weights);

            int[] newProcesses = new int[sortedMap.size()];
            Integer[] newProcessInteger = sortedMap.keySet().toArray(new Integer[sortedMap.size()]);
            for (int i = 0; i < newProcesses.length; i++) {
                newProcesses[i] = newProcessInteger[i].intValue();
            }

            System.out.println("new Weights@process " + Arrays.toString(newProcesses));
        } else {
            System.out.println("No configuration is better than the current one! NO RECONFIG");
        }
        long end = System.currentTimeMillis();
        System.out.println("--------- calc Best finished - " + (end - startBest) + "ms -------------");
        System.out.println("--------- Calculation finished - " + (end - start) + "ms -------------");
    }

    private DynamicWeightGraph getMin(DynamicWeightGraph[] graphs) {
        double min = graphs[0].getLeaves()[0].getValue();
        DynamicWeightGraph currentMin = graphs[0];
        for (int i = 1; i < graphs.length; i++) {
            if (graphs[i].getLeaves()[0].getValue() < min) {
                min = graphs[i].getLeaves()[0].getValue();
                currentMin = graphs[i];
            }
        }

        return currentMin;

    }

    private TreeMap<Integer, Double> sortMapByValue(HashMap<Integer, Double> map) {
        Comparator<Integer> comparator = new bftsmart.fluidity.strategies.WeightGraph.ValueComparator(map);
        // TreeMap is a map sorted by its keys.
        // The comparator is used to sort the TreeMap by keys.
        TreeMap<Integer, Double> result = new TreeMap<Integer, Double>(comparator);
        result.putAll(map);
        return result;
    }

    protected int getReplyQuorum() { //TODO Check if those calculations are correct

        // code for classic quorums
		/*
		 * if (getViewManager().getStaticConf().isBFT()) { return (int)
		 * Math.ceil((getViewManager().getCurrentViewN() +
		 * getViewManager().getCurrentViewF()) / 2) + 1; } else { return (int)
		 * Math.ceil((getViewManager().getCurrentViewN()) / 2) + 1; }
		 */

        // code for vote schemes

        if (svController.getStaticConf().isBFT()) {
            return (int) Math.ceil(
                    (svController.getCurrentView().getOverlayN() + (svController.getCurrentView().getOverlayF()) + 1)
                            / 2);
        } else {

            // code for simple majority (of votes)
            // return (int)
            // Math.ceil(((getViewManager().getCurrentView().getOverlayN()) + 1)
            // / 2);

            // Code to only wait one reply
            Logger.println("(ServiceProxy.getReplyQuorum) only one reply will be gathered");
            return 1;
        }
    }

    public double getCurrentValue() {
        return currentCalculatedValue;
    }

    public Double[] getCurrentWeightAssignment() {
        return currentWeightAssignment;
    }

    public int getCurrentLeader() {
        return currentLeader;
    }

    public int getBestLeader() {
        return bestLeader;
    }

    public Map<Integer, Double> getBestWeightAssignment() {
        return bestWeightAssignment;
    }

    public double getBestCalculatedValue() {
        return bestCalculatedValue;
    }

}

class ValueComparator implements Comparator<Integer> {

    HashMap<Integer, Double> map = new HashMap<Integer, Double>();

    public ValueComparator(HashMap<Integer, Double> map) {
        this.map.putAll(map);
    }

    @Override
    public int compare(Integer s1, Integer s2) {
        if (map.get(s1) >= map.get(s2)) {
            return -1;
        } else {
            return 1;
        }
    }
}
