package de.tsl2.nano.h5;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import de.tsl2.nano.bean.annotation.Action;
import de.tsl2.nano.bean.annotation.Constraint;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.replication.EntityReplication;

public class Replication {
	Collection<?> data;
	
    public Replication(Collection<?> data) {
    	assert !Util.isEmpty(data) : "data must not be empty!";
		this.data = data;
	}

	@Action(name = "replicate", argNames = {"source", "destination"})
    public void replicate(@Constraint(defaultValue = "PUNIT1", allowed = {"PUNIT1", "PUNIT2", "JNDI", "XML", "JAXB", "BYTES"}) String src
    		     , @Constraint(defaultValue = "XML", allowed = {"PUNIT1", "PUNIT2", "JNDI", "XML", "JAXB", "BYTES"}) String dest) {
    	String p1 = getTransition(src);
    	String p2 = getTransition(dest);
    	if (src.startsWith("PUNIT") && dest.startsWith("PUNIT"))
    		System.setProperty("use.hibernate.replication", "true");
    	Collection<String> args = new ArrayList<String>(data.size() + 3);
    	args.add(p1);
    	args.add(p2);
    	args.add(data.iterator().next().getClass().getName());
    	for (Object d : data) {
			args.add(Bean.getBean(d).getId().toString());
		}
    	try {
			EntityReplication.main(args.toArray(new String[0]));
		} catch (ClassNotFoundException e) {
			ManagedException.forward(e);
		}
    }

	private String getTransition(String src) {
		switch (src) {
		case "PUNIT1":
			return Persistence.current().getPersistenceUnit();
		case "PUNIT2":
			return Persistence.current().getReplication().getPersistenceUnit();
		default:
			return src;
		}
	}

	public static Method getReplicationMethod() {
		try {
			return Replication.class.getMethod("replicate", new Class[] {String.class, String.class});
		} catch (NoSuchMethodException | SecurityException e) {
			ManagedException.forward(e);
			return null;
		}
	}
}
