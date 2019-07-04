package io.narayana.txdemo;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class DummyEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public DummyEntity() {
	}
    
    public DummyEntity(String name) {
    	this.name = name;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isTransient() {
    	return id == null;
    }
}
