package io.narayana.txdemo.xaresources;

import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;

@Singleton
@Startup
public class DummyPersistentXAResourceInitializer {
    private static Logger log = Logger.getLogger(DummyPersistentXAResourceInitializer.class);

    /**
     * register the recovery module with the transaction manager.
     */
    @PostConstruct
    public void postConstruct() {
        log.infof("Adding instance of the MockXAResourceRecoveryHelper '%s' to XARecoveryModule", DummyPersistentXAResourceRecoveryHelper.INSTANCE);
        getRecoveryModule().addXAResourceRecoveryHelper(DummyPersistentXAResourceRecoveryHelper.INSTANCE);
        DummyPersistentXAResource.initPreparedXids(DummyPersistentXAResourceStorage.recoverFromDisk());
    }

    /**
     * unregister the recovery module from the transaction manager.
     */
    @PreDestroy
    public void preDestroy() {
        log.infof("Stopping MockXAResource initializer by removing instance of helper '%s' from XARecoveryModule", DummyPersistentXAResourceRecoveryHelper.INSTANCE);
        getRecoveryModule().removeXAResourceRecoveryHelper(DummyPersistentXAResourceRecoveryHelper.INSTANCE);
    }

    private XARecoveryModule getRecoveryModule() {
        for (RecoveryModule recoveryModule : ((Vector<RecoveryModule>) RecoveryManager.manager().getModules())) {
            if (recoveryModule instanceof XARecoveryModule) {
                return (XARecoveryModule) recoveryModule;
            }
        }
        return null;
    }
}
