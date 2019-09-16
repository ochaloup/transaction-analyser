package io.narayana.txdemo.xaresources;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.logging.Logger;
import org.jboss.tm.XAResourceWrapper;

public class DummyPersistentXAResource implements XAResource, XAResourceWrapper, Serializable {
	
	// using Set for two Xids would not be part of the collection
    private static final Collection<Xid> preparedXids = ConcurrentHashMap.newKeySet();
    private static Logger log = Logger.getLogger(DummyPersistentXAResource.class);
	private String name;
	private FaultType fault = FaultType.NONE;
	private static boolean noRollbackYet = true;
	private int transactionTimeout;

	public enum FaultType {
		TIMEOUT, PREPARE_FAIL, NONE, FIRST_ROLLBACK_RMFAIL, FIRST_COMMIT_RMFAIL
	}
	
	public DummyPersistentXAResource() {
		this("default xares");
	}
	
	public DummyPersistentXAResource(String name) {
		this(name, FaultType.NONE);
	}

	public DummyPersistentXAResource(String name, FaultType fault) {
		this.name = name;
		this.fault = fault;
	}

	@Override
	public void commit(Xid xid, boolean b) throws XAException {
		if (fault == FaultType.TIMEOUT)
			throw new XAException(XAException.XA_RBTIMEOUT);
		removeLog(xid);
	}

	@Override
	public void end(Xid xid, int i) throws XAException {
	}

	@Override
	public void forget(Xid xid) throws XAException {
		log.infof("forget '%s' xid:[%s]", this, xid);
        removeLog(xid);
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return transactionTimeout;
	}

	@Override
	public boolean isSameRM(XAResource xaResource) throws XAException {
		return equals(xaResource);
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		log.infof("prepare '%s' xid: [%s]", this, xid);
		if (fault == FaultType.PREPARE_FAIL) {
			throw new XAException(XAException.XAER_RMFAIL);
		}
		preparedXids.add(xid);
        DummyPersistentXAResourceStorage.writeToDisk(preparedXids);
		return XAResource.XA_OK;
	}

	@Override
	public Xid[] recover(int i) throws XAException {
		log.debugf("recover '%s' with flags: %s, returning list of xids '%s'", this, i, preparedXids);
        return preparedXids.toArray(new Xid[preparedXids.size()]);
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		if (fault == FaultType.FIRST_ROLLBACK_RMFAIL && noRollbackYet) {
			noRollbackYet = false;
			throw new XAException(XAException.XAER_RMFAIL);
		}
		log.infof("rollback '%s' xid: [%s]", this, xid);
        removeLog(xid);
	}

	@Override
	public boolean setTransactionTimeout(int seconds) throws XAException {
		log.tracef("setTransactionTimeout: setting timeout: %s", seconds);
        this.transactionTimeout = seconds;
        return true;
	}

	@Override
	public void start(Xid xid, int i) throws XAException {
	}
	
	/**
     * Loading 'prepared' xids from the persistent file storage.
     * Expected to be used just at the start of the application.
     */
    static synchronized void initPreparedXids(Collection<Xid> xidsToBeDefinedAsPrepared) {
        preparedXids.addAll(xidsToBeDefinedAsPrepared);
    }
    
    private void removeLog(Xid xid) {
        preparedXids.remove(xid);
        DummyPersistentXAResourceStorage.writeToDisk(preparedXids);
    }
     
    @Override
    public XAResource getResource() {
        throw new UnsupportedOperationException("getResource() method from "
                + XAResourceWrapper.class.getName() + " is not implemented yet");
    }

    @Override
    public String getProductName() {
        return DummyPersistentXAResource.class.getSimpleName();
    }

    @Override
    public String getProductVersion() {
        return "0.1.Mock";
    }

    @Override
    public String getJndiName() {
        String jndi = "java:/" + DummyPersistentXAResource.class.getSimpleName();
        log.debugf("getJndiName()[return %s]", jndi);
        return jndi;
    }

	@Override
	public String toString() {
		return "XAResourceWrapperImpl@[xaResource=" + super.toString() + " pad=false overrideRmValue=null productName="
				+ name + " productVersion=1.0 jndiName=java:jboss/" + name + "]";
	}

}
