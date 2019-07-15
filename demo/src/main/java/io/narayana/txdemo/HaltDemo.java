package io.narayana.txdemo;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

public class HaltDemo extends Demo {

	public HaltDemo() {
		super(42, "Halt before commit", "Enlist two dummy XAResources and then halt the VM.");		
	}
	
	@Override
	public DemoResult run(TransactionManager tm, EntityManager em) throws Exception {

        tm.begin();

        tm.getTransaction().enlistResource(new DemoDummyXAResource("demo1"));
        tm.getTransaction().enlistResource(new DemoDummyXAResource("demo2"));
        create(em, "test");

        System.exit(-1);
        tm.commit();

        return new DemoResult(42, "commit nok");
	}

}
