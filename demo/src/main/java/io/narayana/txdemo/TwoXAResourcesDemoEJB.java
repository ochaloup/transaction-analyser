package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import io.narayana.txdemo.tracing.TracingUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

/**
 * EJB and declarative based transaction programming.
 *
 */
@Stateless
public class TwoXAResourcesDemoEJB extends Demo {

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

	public static class DummyAppException extends RuntimeException {
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