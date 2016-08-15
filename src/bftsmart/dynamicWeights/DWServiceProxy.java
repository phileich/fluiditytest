package bftsmart.dynamicWeights;

import java.util.Comparator;

import bftsmart.tom.ServiceProxy;
import bftsmart.tom.util.Extractor;

public class DWServiceProxy extends ServiceProxy {
	public DWServiceProxy(int processId) {
		this(processId, null, null, null);
	}

	public DWServiceProxy(int processId, String configHome) {
		this(processId, configHome, null, null);
	}

	public DWServiceProxy(int processId, String configHome, Comparator<byte[]> replyComparator,
			Extractor replyExtractor) {
		super(processId);		
	}
}
