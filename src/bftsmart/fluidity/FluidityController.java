package bftsmart.fluidity;

import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.LatencyMonitor;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.fluidity.strategies.FluidityReconfigurator;
import bftsmart.fluidity.strategies.StrategyConstant;
import bftsmart.fluidity.strategies.StrategyLatency;
import bftsmart.fluidity.strategies.StrategyRandom;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.reconfiguration.views.View;
import bftsmart.tom.util.Logger;

import java.util.ArrayList;

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
    private FluidityGraph oldFluidityGraph;
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
                fluidityReconfigurator.setDistributionStrategy(new StrategyRandom());
                Thread randomStrategy = new Thread(fluidityReconfigurator, "RandomStrategyThread");
                randomStrategy.setPriority(Thread.MIN_PRIORITY);
                randomStrategy.start();
                break;

            case "Latency Distribution":
                fluidityReconfigurator.setDistributionStrategy(new StrategyLatency());
                Thread latencyStrategy = new Thread(fluidityReconfigurator, "LatencyStrategyThread");
                latencyStrategy.setPriority(Thread.MIN_PRIORITY);
                latencyStrategy.start();
                break;

            case "Constant Distribution":
                fluidityReconfigurator.setDistributionStrategy(new StrategyConstant());
                Thread constantStrategy = new Thread(fluidityReconfigurator, "ConstantStrategyThread");
                constantStrategy.setPriority(Thread.MIN_PRIORITY);
                constantStrategy.start();
                break;

            default:
                Logger.println("Distribution unknown");
                break;
        }
    }

    public DynamicWeightController getDwc() {
        return dwc;
    }

    public int getNumberOfReplicasToMove() {
        return svController.getStaticConf().getNumberOfReplicasToMove();
    }

    public void notifyNewFluidityGraph(FluidityGraph newFluidityGraph, FluidityGraph oldFluidityGraph) {
        this.newFluidityGraph = newFluidityGraph;
        this.oldFluidityGraph = oldFluidityGraph;

        // TODO Check difference between graphs (deep copy)
        // compare nodes and check for differences (relevant for cloud connection)
        for (FluidityGraphNode newNode : newFluidityGraph.getNodes()) {
            FluidityGraphNode oldNode = oldFluidityGraph.getNodeById(newNode.getNodeId());
            ArrayList<Integer> oldReplicaIds = oldFluidityGraph.getReplicasFromNode(oldNode);
            ArrayList<Integer> newReplicaIds = newFluidityGraph.getReplicasFromNode(newNode);

            for (int repId : newReplicaIds) {
                if (!oldReplicaIds.contains(repId)) {
                    // new replica created
                }
            }

            for (int repId : oldReplicaIds) {
                if (!newReplicaIds.contains(repId)) {
                    // old replica deleted
                }
            }
        }
    }
}
