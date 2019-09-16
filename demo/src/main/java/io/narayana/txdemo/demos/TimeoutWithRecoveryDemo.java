package io.narayana.txdemo.demos;

import java.sql.Timestamp;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import io.narayana.txdemo.DemoResult;
import io.narayana.txdemo.xaresources.DummyPersistentXAResource;
import io.narayana.txdemo.xaresources.DummyPersistentXAResource.FaultType;

public class TimeoutWithRecoveryDemo extends Demo {

	public TimeoutWithRecoveryDemo() {

		super(25, "Transaction Timeout With Recovery", "The passed XAResources force the recovery manager to run a (proper) recovery. First, two dummy XAResources"
				+ "are enlisted and after that, a rollback of the transaction is initiated.");
	}

	@Override
	public DemoResult run(TransactionManager tm, EntityManager em) throws Exception {

		tm.begin();
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		tm.getTransaction().enlistResource(new DummyPersistentXAResource("demo" + ts.getTime(), FaultType.FIRST_ROLLBACK_RMFAIL));
		tm.getTransaction().enlistResource(new DummyPersistentXAResource("demo" + ts.getTime() + 1, FaultType.PREPARE_FAIL));
		
		create(em, "test");
		tm.commit();

		return new DemoResult(0, "client driven rollback with recovery");
	}
}
