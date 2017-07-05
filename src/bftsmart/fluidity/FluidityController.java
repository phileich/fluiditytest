package bftsmart.fluidity;

import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.LatencyMonitor;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphBuilder;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.util.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

/**
 * This class is the main controller for the fluidity approach
 */
public class FluidityController implements Runnable {
    private DynamicWeightController dwc;
    private int replicaId;
    private ServerViewController svController;
    private LatencyMonitor latencyMonitor;
    private FluidityGraph fluidityGraph;
    private View currentView;
    private ArrayList<FluidityGraphNode> nodeOfGraph;


    public FluidityController(int id, ServerViewController svController, LatencyMonitor latencyMonitor,
                              DynamicWeightController dynamicWeightController,
                              FluidityGraph fluidityGraph) {
        this.replicaId = id;
        this.svController = svController;
        this.latencyMonitor = latencyMonitor;
        this.dwc = dynamicWeightController;
        this.fluidityGraph = fluidityGraph;


    }

    @Override
    public void run() {
        //TODO Run once when called from DWC
        String distributionStrategy = svController.getStaticConf().getFluidityDistributionStrategy();

        switch (distributionStrategy) {
            case "Random Distribution":
                randomDistribution();
                break;

            case "Latency Distribution":
                latencyDistribution();
                break;

            case "Static Placement":
                staticPlacement();
                break;

            case "Data Center Distribution":
                dataCenterDistribution();
                break;

            default:
                Logger.println("Distribution unknown");
                break;
        }
    }

    private void randomDistribution() {
        currentView = svController.getCurrentView();
        nodeOfGraph = fluidityGraph.getNodes();

        // Get the weight assignment from the old and current view
        //Map<Integer, Double> oldWeightAssignment = oldView.getWeights();
        //Map<Integer, Double> currentWeightAssignment = currentView.getWeights();

        Map<Integer, Double>  weightAssignment = dwc.getBestWeightAssignment();

        ArrayList<Integer> newlyMutedReplicas = new ArrayList<>();

        //Check whether the assignment has changes since the last reconfiguration
        for (int processId :
                weightAssignment.keySet()) {
            if (weightAssignment.get(processId) == 0) {
                newlyMutedReplicas.add(processId);
            }
            //TODO Set processId replica passive and randomly create a new replica
            getNodesForNewReplica(newlyMutedReplicas.size());
        }


    }

    private void dataCenterDistribution() {

    }

    private void staticPlacement() {

    }

    private void latencyDistribution() {

    }


    private ArrayList<FluidityGraphNode> getNodesForNewReplica(int numOfReplicas) {
        //TODO Randomly select nodes for replicas
        ArrayList<FluidityGraphNode> returnNodes = new ArrayList<>();

        for (int i = 0; i < numOfReplicas; i++) {
            boolean notYetFound = true;
            while (notYetFound) {
                int nodeNr = getRandomNumberForNode(nodeOfGraph.size());
                FluidityGraphNode tempNode = nodeOfGraph.get(nodeNr);

                if (fluidityGraph.checkForCapacity(tempNode)) {
                    returnNodes.add(tempNode);
                    notYetFound = false;
                }
            }
        }

        return returnNodes;
    }

    private int getRandomNumberForNode(int range) {
        Random randomGenerator = new Random(1234);
        return randomGenerator.nextInt(range);
    }
}
