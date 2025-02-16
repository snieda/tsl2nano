package de.tsl2.nano.h5;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import de.tsl2.nano.bean.annotation.Action;
import de.tsl2.nano.bean.annotation.Constraint;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.replication.EntityReplication;

public class Replication {
	Collection<?> data;
	
    public Replication(Collection<?> data) {
    	assert !Util.isEmpty(data) : "data must not be empty!";
		this.data = data;
	}

	@Action(name = "replicate", argNames = {"source", "destination"})
    public void replicate(@Constraint(defaultValue = "PUNIT1", allowed = {"PUNIT1", "PUNIT2", "JNDI", "XML", "JAXB", "BYTES", "SIMPLE_XML", "YAML", "JSON"}) String src
    		     , @Constraint(defaultValue = "XML", allowed = {"PUNIT1", "PUNIT2", "JNDI", "XML", "JAXB", "BYTES", "SIMPLE_XML", "YAML", "JSON"}) String dest) {
    	String p1 = getTransition(src);
    	String p2 = getTransition(dest);
    	if (src.startsWith("PUNIT") && dest.startsWith("PUNIT"))
    		System.setProperty("use.hibernate.replication", "true");
    	Collection<String> args = new ArrayList<String>(data.size() + 3);
    	args.add(p1);
    	args.add(p2);
    	args.add(!Util.isEmpty(data) ? BeanClass.getDefiningClass(data.iterator().next()).getName() : "java.lang.Object");
    	for (Object d : data) {
			args.add(Bean.getBean(d).getId().toString());
		}
    	try {
			EntityReplication.main(args.toArray(new String[0]));
			Message.send("replication done from: " + p1 + " to " + p2 + " on " + data.size() + " elements");
		} catch (ClassNotFoundException e) {
			ManagedException.forward(e);
		} finally { // this may slow down the performance, but unused beans are removed from cache
			Bean.clearCache();
		}
    }

	private String getTransition(String src) {
		switch (src) {
		case "PUNIT1":
			return Persistence.current().getPersistenceUnit();
		case "PUNIT2":
			if (Persistence.current().getReplication() != null) {
				try {
					DatabaseTool.runDBServer(ENV.getConfigPath(), Persistence.current().getReplication().getPort(), Persistence.current().getReplication().getDatabase());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return Persistence.current().getReplication().getPersistenceUnit();
			}
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
