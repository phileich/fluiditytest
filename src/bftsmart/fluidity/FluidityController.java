package bftsmart.fluidity;

import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.LatencyMonitor;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphBuilder;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.fluidity.strategies.FluidityReconfigurator;
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

    private FluidityGraph newFluidityGraph;
    private FluidityReconfigurator fluidityReconfigurator;


    public FluidityController(int id, ServerViewController svController, LatencyMonitor latencyMonitor,
                              DynamicWeightController dynamicWeightController,
                              FluidityGraph fluidityGraph) {
        this.replicaId = id;
        this.svController = svController;
        this.latencyMonitor = latencyMonitor;
        this.dwc = dynamicWeightController;
        this.fluidityGraph = fluidityGraph;
        this.fluidityReconfigurator = new FluidityReconfigurator(svController, this);


    }

    @Override
    public void run() {
        //TODO Run once when called from DWC
        String distributionStrategy = svController.getStaticConf().getFluidityDistributionStrategy();

        // TODO Use strategypattern

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

    }


    private void dataCenterDistribution() {

    }

    private void staticPlacement() {

    }

    private void latencyDistribution() {

    }




    public DynamicWeightController getDwc() {
        return dwc;
    }

    public void notifyNewFluidityGraph(FluidityGraph fluidityGraph) {
        this.newFluidityGraph = fluidityGraph;
    }
}
