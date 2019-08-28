package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.narayana.txdemo.tracing.TracingUtils;

@Path("/demo_auto")
public class DemoAutomaticStreamService {

	private final AtomicBoolean active = new AtomicBoolean(true);
	private ArrayList<Demo> demosAuto = new ArrayList<>();
	/**
	 * number of transactions which will be spawned on a click of the "start" button in the UI
	 */
	private static final int NO_TRANS = 50;
	
	@EJB
	private DemoDao dao;

	@EJB
	TwoXAResourcesDemoEJB twoXAResourcesEJB;

	@Inject
	TwoXAResourcesDemoCDI twoXAResourcesCDI;

	@PersistenceContext
	private EntityManager em;

	@Resource(lookup = "java:jboss/TransactionManager")
	private TransactionManager tm;

	@PostConstruct
	public void initDemos() {
		TracingUtils.getTracer();
		demosAuto.add(new SuccessTransactionDemo());
		demosAuto.add(new TimeoutTransactionDemo());
		demosAuto.add(new PrepareFailDemo());
		demosAuto.add(new ClientDrivenRollbackDemo());
		demosAuto.add(twoXAResourcesEJB);
		demosAuto.add(twoXAResourcesCDI);
	}

	private DemoResult runOneDemo(int demoIndex) {
		try {
			return demosAuto.get(demoIndex % demosAuto.size()).run(tm, em);
		} catch (Exception e) {
			e.printStackTrace();
			return new DemoResult(-2, "exception " + e);
		}
	}

	@GET
	@Path("/start")
	public void action() {
		new Runnable() {
			@Override
			public void run() {
				new Random().ints(NO_TRANS).forEach(i -> runOneDemo(i));
			}
		}.run();
	}
}
