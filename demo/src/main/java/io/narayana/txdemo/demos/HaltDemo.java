package io.narayana.txdemo.demos;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import io.narayana.txdemo.DemoResult;
import io.narayana.txdemo.xaresources.DummyXAResource;

public class HaltDemo extends Demo {

	public HaltDemo() {
		super(42, "Halt before commit", "Enlist two dummy XAResources and then halt the VM.");		
	}
	
	@Override
	public DemoResult run(TransactionManager tm, EntityManager em) throws Exception {

        tm.begin();

        tm.getTransaction().enlistResource(new DummyXAResource("demo1"));
        tm.getTransaction().enlistResource(new DummyXAResource("demo2"));
        create(em, "test");

        System.exit(-1);
        tm.commit();

        return new DemoResult(42, "commit nok");
	}

}
