package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.jms.Queue;
import javax.jms.XAConnectionFactory;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
/**
 * CDI and declarative based transaction programming.
 *
 */
@RequestScoped
public class TwoXAResourcesDemoCDI extends TwoResourcesDemo {

	// TODO: annotation lookup does not work, why?
	// org.jboss.weld.exceptions.DeploymentException: WELD-001408
	// @Inject
	private XAConnectionFactory xaConnectionFactory;

	// @Inject
	private Queue queue;

	public TwoXAResourcesDemoCDI() {
		super(9, "[CDI backed] Two-phase commit transaction on two different XA resources.",
				"[CDI backed] Two-phase commit transaction on two different XA resources.");
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

	@Override
	@Transactional(value = TxType.REQUIRED)
	public DemoResult run(TransactionManager _tm, EntityManager em) {
		StringBuilder strBldr = new StringBuilder();
		try {
			List<DummyEntity> dummies = prepareDummies();
			for (DummyEntity de : dummies) {
				dbSave(em, de);
			}
			for (DummyEntity de : dbGet(em)) {
				jmsSend(de.getName());
				jmsGet().ifPresent(dummy -> strBldr.append(dummy + "\n"));
			}
			return new DemoResult(0, "Commited two resources - JMS & DB, message:\n\n" + strBldr.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
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
