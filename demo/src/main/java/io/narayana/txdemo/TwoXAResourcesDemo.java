package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateful;
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
import javax.transaction.Transactional;

@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class TwoXAResourcesDemo extends Demo {
    
    @Resource(mappedName = "java:/JmsXA")
    private XAConnectionFactory xaConnectionFactory;

    @Resource(mappedName = "java:/jms/queue/testQueue")
    private Queue queue;
    
    public TwoXAResourcesDemo() {
    	super(9, "Two-phase commit transaction on two different XA resources.", "-");
    }
    
    private static final Logger LOG = Logger.getGlobal();
    
    private List<DummyEntity> prepareDummies() {
    	int noDummies = 1 + new Random().nextInt(10);
    	List<DummyEntity> dummies = new ArrayList<>();
    	for(int i = 1; i <= noDummies; i++) {
    		dummies.add(new DummyEntity("dummy #" + i + " says hello"));
    	}
    	return dummies;
    }
    
    public static class DummyAppException extends RuntimeException {
    }
    
    @Override
    //@Transactional(value=Transactional.TxType.REQUIRED, rollbackOn=DummyAppException.class)
    public DemoResult run(TransactionManager tm, EntityManager em) {
    	try {
        	StringBuilder strBldr = new StringBuilder();
    		tm.begin();
    		
    		List<DummyEntity> dummies = prepareDummies();
        	for (DummyEntity de : dummies) {
        		//dbSave(em, de);
        		if (de.isTransient()) {
                    em.persist(de);
                } else {
                    em.merge(de);
                }
        	}
        	//for(DummyEntity de : dbGet(em)) {
        	for(DummyEntity de : em.createQuery("select e from DummyEntity e", DummyEntity.class)
            		.getResultList()) {
        		//jmsSend(de.getName());
        		//jmsGet().ifPresent(dummy -> strBldr.append(dummy + "\n"));
        		try(XAConnection connection = xaConnectionFactory.createXAConnection();
                        XASession session = connection.createXASession();
                		MessageProducer messageProducer = session.createProducer(queue);
        				MessageConsumer consumer = session.createConsumer(queue)) {
                    connection.start();
                    TextMessage textMessage = session.createTextMessage();
                    textMessage.setText(de.getName());
                    messageProducer.send(textMessage);
                    final TextMessage message = (TextMessage) consumer.receive(5000);
                    strBldr.append(message.getText() + "\n");
                } catch (JMSException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
        	}
        	if(new Random().nextInt() % 3 == 0) {
        	    LOG.fine("Simulating application exception being thrown...");
        	    throw new DummyAppException();
        	}
        	tm.commit();
        	return new DemoResult(0, "Commited two resources - JMS & DB, message:\n\n" + strBldr.toString());
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception has been thrown: " + e.getMessage());
		}
    }
    
    //@Transactional
    private List<DummyEntity> dbGet(EntityManager em) {
       return em.createQuery("select e from DummyEntity e", DummyEntity.class)
        		.getResultList();
    }

    //@Transactional
    private Long dbSave(EntityManager em, DummyEntity quickstartEntity) {
        if (quickstartEntity.isTransient()) {
            em.persist(quickstartEntity);
        } else {
            em.merge(quickstartEntity);
        }

        return quickstartEntity.getId();
    }
    
    //@Transactional
    private void jmsSend(final String message) {

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

    //@Transactional
    private Optional<String> jmsGet() {
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
