package bftsmart.dynamicWeights;

import java.util.Comparator;

import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.Extractor;

public class InternalServiceProxy extends ServiceProxy {	
	
	public InternalServiceProxy(int processId) {
		this(processId, null, null, null);
	}

	public InternalServiceProxy(int processId, String configHome) {
		this(processId, configHome, null, null);
	}

	public InternalServiceProxy(int processId, String configHome, Comparator<byte[]> replyComparator,
			Extractor replyExtractor) {
		super(processId);
	}

	public byte[] invokeInternal(byte[] request) {
		return invoke(request, TOMMessageType.INTERNAL_CONSENSUS);
	}
}
