package io.narayana.txdemo.demos;

import java.util.List;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.XAConnectionFactory;
import javax.persistence.EntityManager;

import io.narayana.txdemo.DummyEntity;
import io.narayana.txdemo.tracing.TracingStartup;
import io.opentracing.Tracer;
import io.opentracing.contrib.jms.common.TracingMessageConsumer;
import io.opentracing.contrib.jms2.TracingConnection;
import io.opentracing.contrib.jms2.TracingMessageProducer;
import io.opentracing.contrib.jms2.TracingSession;
import io.opentracing.util.GlobalTracer;

public abstract class TwoResourcesDemo extends Demo {
    protected TwoResourcesDemo(int i, String name, String desc) {
        super(i, name, desc);
    }

    protected abstract XAConnectionFactory getFactory();

    protected abstract Queue getQueue();

    protected void jmsSend(final String message) {
        Tracer t = GlobalTracer.get();
        try (TracingConnection connection = new TracingConnection(getFactory().createXAConnection(), t);
                TracingSession session = new TracingSession(connection.createSession(), GlobalTracer.get());
                TracingMessageProducer producer = new TracingMessageProducer(session.createProducer(getQueue()), t)) {
            connection.start();
            TextMessage textMessage = session.createTextMessage();
            textMessage.setText(message);
            producer.send(textMessage);
        } catch (JMSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected Optional<String> jmsGet() {
        Tracer t = GlobalTracer.get();
        try (TracingConnection connection = new TracingConnection(getFactory().createXAConnection(), t);
                TracingSession session = new TracingSession(connection.createSession(), t);
                TracingMessageConsumer consumer = new TracingMessageConsumer(session.createConsumer(getQueue()), t)) {
            connection.start();
            final TextMessage message = (TextMessage) consumer.receive(5000);
            return message == null ? Optional.<String>empty() : Optional.of(message.getText());
        } catch (JMSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static class DummyAppException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    protected List<DummyEntity> dbGet(EntityManager em) {
        return em.createQuery("select e from DummyEntity e", DummyEntity.class).getResultList();
    }

    protected Long dbSave(EntityManager em, DummyEntity quickstartEntity) {
        if (quickstartEntity.isTransient()) {
            em.persist(quickstartEntity);
        } else {
            em.merge(quickstartEntity);
        }
        return quickstartEntity.getId();
    }
}
