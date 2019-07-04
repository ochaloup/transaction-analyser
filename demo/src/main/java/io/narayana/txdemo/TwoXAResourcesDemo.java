package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
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

@Stateless
public class TwoXAResourcesDemo extends Demo {

    @PersistenceContext(name = "io.narayana.txdemo", unitName = "io.narayana.txdemo")
    EntityManager entityManager;
    
    @Resource(mappedName = "java:/JmsXA")
    private XAConnectionFactory xaConnectionFactory;

    @Resource(mappedName = "java:/jms/queue/testQueue")
    private Queue queue;
    
    public TwoXAResourcesDemo() {
    	super(9, "Two-phase commit transaction on two different XA resources.", "-");
    }
    
    private static final Logger LOG = Logger.getGlobal();
    
    private static final int DUMMIES_CAP = 5;
    
    private List<DummyEntity> prepareDummies() {
    	int noDummies = new Random().nextInt(8);
    	List<DummyEntity> dummies = new ArrayList<>();
    	for(int i = 1; i <= noDummies; i++) {
    		dummies.add(new DummyEntity("dummy #" + i + " says hello"));
    	}
    	return dummies;
    }
    
    public static class CantHandleThatManyDummiesException extends RuntimeException {
    }
    
    @Override
    @Transactional(value=Transactional.TxType.REQUIRED, rollbackOn=CantHandleThatManyDummiesException.class)
    public DemoResult run(TransactionManager utx, EntityManager em) {
    	List<DummyEntity> dummies = prepareDummies();
    	for (DummyEntity de : dummies) {
    		dbSave(de);
    	}
    	int retrievedNoDummies = 0;
    	for(DummyEntity de : dbGet()) {
    		jmsSend(de.getName());
    		jmsGet().ifPresent(dummy -> LOG.fine(dummy));
    		retrievedNoDummies++;
    	}
    	if(retrievedNoDummies > DUMMIES_CAP) {
    	    throw new CantHandleThatManyDummiesException();
    	}
    	return new DemoResult(0, "Commited two resources - JMS & DB");
    }
    
    @Transactional(Transactional.TxType.MANDATORY)
    public List<DummyEntity> dbGet() {
       return entityManager.createQuery("select e from DummyEntity e", DummyEntity.class)
        		.getResultList();
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public Long dbSave(DummyEntity quickstartEntity) {
        if (quickstartEntity.isTransient()) {
            entityManager.persist(quickstartEntity);
        } else {
            entityManager.merge(quickstartEntity);
        }

        return quickstartEntity.getId();
    }
    
    @Transactional(Transactional.TxType.MANDATORY)
    public void jmsSend(final String message) {

        try(XAConnection connection = xaConnectionFactory.createXAConnection();
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

    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<String> jmsGet() {
        try(XAConnection connection = xaConnectionFactory.createXAConnection();
                XASession session = connection.createXASession();
        		MessageConsumer consumer = session.createConsumer(queue)) {
            connection.start();
            final TextMessage message = (TextMessage) consumer.receive(5000);
            String text = message.getText();
            return text == null ? Optional.<String>empty() : Optional.of(text);
        } catch (JMSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
