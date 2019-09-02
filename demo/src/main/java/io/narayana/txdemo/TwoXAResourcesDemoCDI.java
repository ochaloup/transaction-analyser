package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import io.narayana.txdemo.tracing.TracingUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

/**
 * CDI and declarative based transaction programming.
 *
 */
@RequestScoped
public class TwoXAResourcesDemoCDI extends Demo {

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

	public static class DummyAppException extends RuntimeException {
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

	private List<DummyEntity> dbGet(EntityManager em) {
		return em.createQuery("select e from DummyEntity e", DummyEntity.class).getResultList();
	}

	private Long dbSave(EntityManager em, DummyEntity quickstartEntity) {
		if (quickstartEntity.isTransient()) {
			em.persist(quickstartEntity);
		} else {
			em.merge(quickstartEntity);
		}
		return quickstartEntity.getId();
	}

	private void jmsSend(final String message) {
		try (XAConnection connection = xaConnectionFactory.createXAConnection();
				XASession session = connection.createXASession();
				MessageProducer messageProducer = session.createProducer(queue)) {
			connection.start();
			TextMessage textMessage = session.createTextMessage();
			textMessage.setText(message);
			messageProducer.send(textMessage);
		} catch (JMSException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private Optional<String> jmsGet() {
		try (XAConnection connection = xaConnectionFactory.createXAConnection();
				XASession session = connection.createXASession();
				MessageConsumer consumer = session.createConsumer(queue)) {
			connection.start();
			final TextMessage message = (TextMessage) consumer.receive(5000);
			return message == null ? Optional.<String>empty() : Optional.of(message.getText());
		} catch (JMSException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
