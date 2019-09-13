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
package io.narayana.txdemo.demos;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import io.narayana.txdemo.DemoData;
import io.narayana.txdemo.DemoResult;

/**
 * @author <a href="mailto:zfeng@redhat.com">Amos Feng</a>
 */
public abstract class Demo {

    private int id;
    private String name;
    private String desc;

    public Demo() {

    }

    public Demo(int id, String name, String desc) {

        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public String getName() {

        return name;
    }

    public int getId() {

        return id;
    }

    public String getDesc() {
        return desc;
    }

    public void create(EntityManager em, String name) {
        DemoData data = new DemoData();
        data.setName(name);
        em.persist(data);
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Demo)) {
			return false;
		}
		Demo other = (Demo) obj;
		return id == other.id;
	}

	public abstract DemoResult run(TransactionManager utx, EntityManager em) throws Exception;
}
