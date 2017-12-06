package bftsmart.fluidity.cloudconnection;

import bftsmart.tom.ServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.util.Extractor;

import java.util.Comparator;

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
        byte[] invokeReply;
        try {
            invokeReply = invoke(request, TOMMessageType.INTERNAL_FLUIDITY_CONSENSUS);
        } catch (RuntimeException e) {
            invokeReply = null;
        }
        return invokeReply;
    }
}
