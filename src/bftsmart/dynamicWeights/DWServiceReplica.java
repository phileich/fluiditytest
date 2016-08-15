package bftsmart.dynamicWeights;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.RequestVerifier;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import bftsmart.tom.util.Logger;
import io.netty.util.ResourceLeakDetector.Level;

public class DWServiceReplica extends ServiceReplica {

	private LatencyMonitorPiggybackServer lmps = null;
	private DynamicWeightController dwc = null;

	/**
	 * Constructor
	 *
	 * @param id
	 *            Replica ID
	 * @param executor
	 *            Executor
	 * @param recoverer
	 *            Recoverer
	 */
	public DWServiceReplica(int id, Executable executor, Recoverable recoverer) {
		this(id, "", executor, recoverer, null, new DefaultReplier());
	}

	/**
	 * Constructor
	 *
	 * @param id
	 *            Replica ID
	 * @param executor
	 *            Executor
	 * @param recoverer
	 *            Recoverer
	 * @param verifier
	 *            Requests verifier
	 */
	public DWServiceReplica(int id, Executable executor, Recoverable recoverer, RequestVerifier verifier) {
		this(id, "", executor, recoverer, verifier, new DefaultReplier());
	}

	/**
	 * Constructor
	 * 
	 * @param id
	 *            Replica ID
	 * @param executor
	 *            Executor
	 * @param recoverer
	 *            Recoverer
	 * @param verifier
	 *            Requests verifier
	 * @param replier
	 *            Replier
	 */
	public DWServiceReplica(int id, Executable executor, Recoverable recoverer, RequestVerifier verifier,
			Replier replier) {
		this(id, "", executor, recoverer, verifier, replier);
	}

	/**
	 * Constructor
	 *
	 * @param id
	 *            Process ID
	 * @param configHome
	 *            Configuration directory for JBP
	 * @param executor
	 *            Executor
	 * @param recoverer
	 *            Recoverer
	 * @param verifier
	 *            Requests verifier
	 * @param replier
	 *            Replier
	 */
	public DWServiceReplica(int id, String configHome, Executable executor, Recoverable recoverer,
			RequestVerifier verifier, Replier replier) {
		super(id, configHome, executor, recoverer, verifier, replier);
		this.lmps = new LatencyMonitorPiggybackServer(this.SVController, this.id);
		this.dwc.setServerCommunicationSystem(cs);
	}

	// this method initializes the object

	protected void init() {
		try {
			cs = new ServerCommunicationSystem(this.SVController, this, lmps, dwc);
			dwc.setServerCommunicationSystem(cs);
		} catch (Exception ex) {
			Logger.getLogger(ServiceReplica.class.getName()).log(Level.SEVERE, null, ex);
			throw new RuntimeException("Unable to build a communication system.");
		}

		if (this.SVController.isInCurrentView()) {
			System.out.println("In current view: " + this.SVController.getCurrentView());
			initTOMLayer(); // initiaze the TOM layer
		} else {
			System.out.println("Not in current view: " + this.SVController.getCurrentView());

			// Not in the initial view, just waiting for the view where the join
			// has been executed
			System.out.println("Waiting for the TTP: " + this.SVController.getCurrentView());
			waitTTPJoinMsgLock.lock();
			try {
				canProceed.awaitUninterruptibly();
			} finally {
				waitTTPJoinMsgLock.unlock();
			}

		}
		initReplica();
	}
}
