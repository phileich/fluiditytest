package bftsmart.dynamicWeights;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.TOMLayer;

public class DynamicWeightController implements Runnable{
	private ServerViewController svController;
	private int id;
	private TOMLayer tl;
	private Synchronizer sync;
	private Storage latencyMonitor;

	public DynamicWeightController(int id, ServerViewController svController, Storage latencyMonitor) {
		this.svController = svController;
		this.id = id;
		this.sync = new Synchronizer();
		this.latencyMonitor = latencyMonitor;
	}

	@Override
	public void run() {
		//Start calculation of reconfiguration
		
		//synchronize Data
		sync.synchronize(latencyMonitor, svController.getCurrentViewN());
	}

	public void setTOMLayer(TOMLayer tl) {
		this.tl = tl;
	}

	public int getInExec() {
		int id = tl.getInExec();
		if (id != -1) {
			return id;
		} else {
			return tl.getLastExec();
		}
	}

	public void receiveExec(int exec) {
		if (exec != -1) {
			// trigger sync every 100 consensus
			if (exec % 100 == 0 && exec != 0) {
				new Thread(this).start();
			}
		}
	}
}
