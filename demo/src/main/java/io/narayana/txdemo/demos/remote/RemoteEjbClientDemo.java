package io.narayana.txdemo.demos.remote;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.jboss.as.quickstarts.ejb.remote.stateful.RemoteCounter;
import org.jboss.as.quickstarts.ejb.remote.stateless.RemoteCalculator;
import org.jboss.logging.Logger;

import io.narayana.txdemo.DemoResult;
import io.narayana.txdemo.demos.Demo;

import java.util.Hashtable;

@Stateless
public class RemoteEjbClientDemo extends Demo {

	private static final Logger LOG = Logger.getLogger(RemoteEjbClientDemo.class);
	
    private static final String HTTP = "http";
    private static final String HOSTNAME = "localhost";
    private static final String PORT = "8180";
    private static final String BASE_URL = HTTP + "://" + HOSTNAME + ":" + PORT;

    public RemoteEjbClientDemo() {
		super(35, "Remote EJB client call. Requires a running server which will perform the actual remote call.",
				"Remote EJB client call. Requires a running server which will perform the actual remote call.");
	}
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DemoResult run(TransactionManager tm, EntityManager em) throws Exception {
        invokeStatefulBean();
        invokeStatelessBean();
        return new DemoResult(0, "EJB remote call");
    }
    
    private static void invokeStatefulBean() throws NamingException {
        final RemoteCounter statefulRemoteCounter = lookupRemoteStatefulCounter();
        LOG.info("Obtained a remote stateful counter for invocation");
        final int NUM_TIMES = 5;
        LOG.debug("Counter will now be incremented " + NUM_TIMES + " times");

        Span span = GlobalTracer.get().buildSpan("MMMMYYYY")
                                     .withTag("lucky_number", 42)
                                     .start();
        GlobalTracer.get().activateSpan(span);
        LOG.infof("Spaaan: %s", GlobalTracer.get().activeSpan());


        GlobalTracer.get().buildSpan("myspan").start();
        LOG.infof("Active span here: %s", GlobalTracer.get().activeSpan());

        for (int i = 0; i < NUM_TIMES; i++) {
            LOG.debug("Incrementing counter");
            statefulRemoteCounter.increment();
            LOG.debug("Count after increment is " + statefulRemoteCounter.getCount());
        }
        LOG.debug("Counter will now be decremented " + NUM_TIMES + " times");
        for (int i = NUM_TIMES; i > 0; i--) {
            LOG.debug("Decrementing counter");
            statefulRemoteCounter.decrement();
            LOG.debug("Count after decrement is " + statefulRemoteCounter.getCount());
        }
    }

    private static void invokeStatelessBean() throws NamingException {
        final RemoteCalculator statelessRemoteCalculator = lookupRemoteStatelessCalculator();
        LOG.debug("Obtained a remote stateless calculator for invocation");
        int a = 204;
        int b = 340;
        LOG.debug("Adding " + a + " and " + b + " via the remote stateless calculator deployed on the server");
        int sum = statelessRemoteCalculator.add(a, b);
        LOG.debug("Remote calculator returned sum = " + sum);
        if (sum != a + b) {
            throw new RuntimeException("Remote stateless calculator returned an incorrect sum " + sum + " ,expected sum was "
                + (a + b));
        }
        int num1 = 3434;
        int num2 = 2332;
        LOG.debug("Subtracting " + num2 + " from " + num1
            + " via the remote stateless calculator deployed on the server");
        int difference = statelessRemoteCalculator.subtract(num1, num2);
        LOG.debug("Remote calculator returned difference = " + difference);
        if (difference != num1 - num2) {
            throw new RuntimeException("Remote stateless calculator returned an incorrect difference " + difference
                + " ,expected difference was " + (num1 - num2));
        }
    }

    /**
     * Looks up and returns the proxy to remote stateless calculator bean
     *
     * @return
     * @throws NamingException
     */
    private static RemoteCalculator lookupRemoteStatelessCalculator() throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        if(Boolean.getBoolean(HTTP)) {
            //use HTTP based invocation. Each invocation will be a HTTP request
            jndiProperties.put(Context.PROVIDER_URL, BASE_URL + "/wildfly-services");
        } else {
            //use HTTP upgrade, an initial upgrade requests is sent to upgrade to the remoting protocol
            jndiProperties.put(Context.PROVIDER_URL,"remote+" + BASE_URL);
        }
        final Context context = new InitialContext(jndiProperties);

        // The JNDI lookup name for a stateless session bean has the syntax of:
        // ejb:<appName>/<moduleName>/<distinctName>/<beanName>!<viewClassName>
        //
        // <appName> The application name is the name of the EAR that the EJB is deployed in
        // (without the .ear). If the EJB JAR is not deployed in an EAR then this is
        // blank. The app name can also be specified in the EAR's application.xml
        //
        // <moduleName> By the default the module name is the name of the EJB JAR file (without the
        // .jar suffix). The module name might be overridden in the ejb-jar.xml
        //
        // <distinctName> : EAP allows each deployment to have an (optional) distinct name.
        // This example does not use this so leave it blank.
        //
        // <beanName> : The name of the session been to be invoked.
        //
        // <viewClassName>: The fully qualified classname of the remote interface. Must include
        // the whole package name.

        // let's do the lookup
        return (RemoteCalculator) context.lookup("ejb:/ejb-remote-server-side-jar-with-dependencies/CalculatorBean!"
            + RemoteCalculator.class.getName());
    }
    
    /**
     * Looks up and returns the proxy for a remote stateful counter bean
     */
    private static RemoteCounter lookupRemoteStatefulCounter() throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        //jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        if(Boolean.getBoolean(HTTP)) {
            //use HTTP based invocation. Each invocation will be a HTTP request
            jndiProperties.put(Context.PROVIDER_URL, BASE_URL + "/wildfly-services");
        } else {
            //use HTTP upgrade, an initial upgrade requests is sent to upgrade to the remoting protocol
            jndiProperties.put(Context.PROVIDER_URL,"remote+" + BASE_URL);
        }
        final Context context = new InitialContext(jndiProperties);
        // The JNDI lookup name for a stateful session bean has the syntax of:
        // ejb:<appName>/<moduleName>/<distinctName>/<beanName>!<viewClassName>?stateful
        //
        // <appName> The application name is the name of the EAR that the EJB is deployed in
        // (without the .ear). If the EJB JAR is not deployed in an EAR then this is
        // blank. The app name can also be specified in the EAR's application.xml
        //
        // <moduleName> By the default the module name is the name of the EJB JAR file (without the
        // .jar suffix). The module name might be overridden in the ejb-jar.xml
        //
        // <distinctName> : EAP allows each deployment to have an (optional) distinct name.
        // This example does not use this so leave it blank.
        //
        // <beanName> : The name of the session been to be invoked.
        //
        // <viewClassName>: The fully qualified classname of the remote interface. Must include
        // the whole package name.

        // let's do the lookup
        return (RemoteCounter) context.lookup("ejb:/ejb-remote-server-side-jar-with-dependencies/CounterBean!"
            + RemoteCounter.class.getName() + "?stateful");
    }
}
