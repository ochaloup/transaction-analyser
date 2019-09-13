package io.narayana.txdemo.xaresources;

import javax.transaction.xa.XAResource;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

public class DummyPersistentXAResourceRecoveryHelper implements XAResourceRecoveryHelper {
    public static final DummyPersistentXAResourceRecoveryHelper INSTANCE = new DummyPersistentXAResourceRecoveryHelper();
    private static final DummyPersistentXAResource mockXARecoveringInstance = new DummyPersistentXAResource();

    private DummyPersistentXAResourceRecoveryHelper() {
        if(INSTANCE != null) {
            throw new IllegalStateException("singleton instance can't be instantiated twice");
        }
    }

    @Override
    public boolean initialise(String p) throws Exception {
        // this is never called, probably...
        return true;
    }

    @Override
    public XAResource[] getXAResources() throws Exception {
        return new XAResource[] { mockXARecoveringInstance };
    }

}
