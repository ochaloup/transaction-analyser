package io.narayana.txdemo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import io.narayana.txdemo.tracing.TracingUtils;

@Path("/demo_auto")
public class DemoAutomaticStreamService {

	private ArrayList<Demo> demosAuto = new ArrayList<>();
	private static final int PARALELISM_DEGREE = 4;
	private ExecutorService pool;
	/**
	 * cap for the number of transactions which will be spawned on a click of the
	 * "start" button in the UI
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

	private void runOneDemo(int demoIndex) {
		try {
			demosAuto.get(demoIndex % demosAuto.size()).run(tm, em);
		} catch (Exception e) {
			e.printStackTrace();
			new DemoResult(-2, "exception " + e);
		}
	}

	@GET
	@Path("/{noTrans:[0-9][0-9]*}")
	public void action(@PathParam("noTrans") int noTrans) {
		Iterator<Integer> stream = new Random().ints(noTrans % NO_TRANS, 1, demosAuto.size()).iterator();
		pool = Executors.newFixedThreadPool(PARALELISM_DEGREE);
		while (stream.hasNext()) {
			int n = stream.next();
			pool.execute(() -> runOneDemo(n));
		}
		pool.shutdown();
		try {
			pool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
