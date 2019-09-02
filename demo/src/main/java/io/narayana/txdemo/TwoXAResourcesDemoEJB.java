package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Queue;
import javax.jms.XAConnectionFactory;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

/**
 * EJB and declarative based transaction programming.
 *
 */
@Stateless
public class TwoXAResourcesDemoEJB extends TwoResourcesDemo {

	@Resource(lookup = "java:/jboss/DummyXaConnectionFactory")
	private XAConnectionFactory xaConnectionFactory;

	@Resource(lookup = "java:/jms/queue/DummyQueue")
	private Queue queue;

	public TwoXAResourcesDemoEJB() {
		super(9, "[EJB backed] Two-phase commit transaction on two different XA resources.",
				"[EJB backed] Two-phase commit transaction on two different XA resources.");
	}

	private static final Logger LOG = Logger.getGlobal();

	private List<DummyEntity> prepareDummies() {
		int noDummies = 1 + new Random().nextInt(3);
		List<DummyEntity> dummies = new ArrayList<>();
		for (int i = 1; i <= noDummies; i++) {
			dummies.add(new DummyEntity("dummy #" + i + " says hello"));
		}
		return dummies;
	}

	private static enum RandEvs {
		NONE, COMMON;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public DemoResult run(TransactionManager tm, EntityManager em) {
		runJdbcPart(em);
		String message = runJmsPart(em);
		randomEvents(RandEvs.NONE);
		return new DemoResult(0, "Commited two resources - JMS & DB, message:\n\n" + message);
	}

	private void runJdbcPart(EntityManager em) {
		for (DummyEntity de : prepareDummies()) {
			dbSave(em, de);
		}
	}

	private String runJmsPart(EntityManager em) {
		StringBuilder strBldr = new StringBuilder();
		for (DummyEntity de : dbGet(em)) {
			jmsSend(de.getName());
			jmsGet().ifPresent(dummy -> strBldr.append(dummy + "\n"));
		}
		return strBldr.toString();
	}

	private static final void randomEvents(RandEvs re) {
		if (re != RandEvs.NONE && new Random().nextInt() % 3 == 0) {
			LOG.fine("Simulating application exception being thrown...");
			throw new DummyAppException();
		}
	}

	@Override
	protected XAConnectionFactory getFactory() {
		return xaConnectionFactory;
	}

	@Override
	protected Queue getQueue() {
		return queue;
	}
}