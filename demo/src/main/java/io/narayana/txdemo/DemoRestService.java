/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.narayana.txdemo;

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
import io.opentracing.Scope;
import io.opentracing.Span;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:zfeng@redhat.com">Amos Feng</a>
 */
@Path("/demos")
public class DemoRestService {

	private ArrayList<Demo> demos = new ArrayList<>();
	private static Span root;

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
		demos.add(new SuccessTransactionDemo());
		demos.add(new TimeoutTransactionDemo());
		demos.add(new PrepareFailDemo());
		demos.add(new ClientDrivenRollbackDemo());
		demos.add(twoXAResourcesEJB);
		demos.add(twoXAResourcesCDI);
		demos.add(new HaltDemo());
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Demo> listAllDemos() {
		return demos;
	}

	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public DemoResult getDemo(@PathParam("id") int id) {

		for (Demo demo : demos) {
			if (demo.getId() == id) {
				root = TracingUtils.getTracer().buildSpan("User TX - wrapper").start();
				try(Scope scope = TracingUtils.getTracer().activateSpan(root)) {
					return demo.run(tm, em);
				} catch (Exception e) {
					e.printStackTrace();
					return new DemoResult(-2, "exception " + e);
				} finally {
					root.finish();
				}
			}
		}
		return new DemoResult(-1, "no " + id + " demo");
	}
	
	public static Span getRootSpan() {
		return root;
	}
}
