package bftsmart.fluidity;

import bftsmart.dynamicWeights.DynamicWeightController;
import bftsmart.dynamicWeights.LatencyMonitor;
import bftsmart.reconfiguration.ServerViewController;

/**
 * This class is the main controller for the fluidity approach
 */
public class FluidityController implements Runnable {
    private DynamicWeightController dwc;
    private int replicaId;
    private ServerViewController svController;
    private LatencyMonitor latencyMonitor;

    public FluidityController(int id, ServerViewController svController, LatencyMonitor latencyMonitor) {
        this.replicaId = id;
        this.svController = svController;
        this.latencyMonitor = latencyMonitor;
        this.dwc = new DynamicWeightController(replicaId, svController, latencyMonitor);
    }

    @Override
    public void run() {

    }

    public DynamicWeightController getDynamicWeightController() {
        return dwc;
    }
}
