package de.tsl2.nano.codegen;
import org.apache.commons.logging.Log;
import org.apache.velocity.app.event.MethodExceptionEventHandler;

import de.tsl2.nano.core.log.LogFactory;

public class DontEscalateExceptionHandler implements MethodExceptionEventHandler {
	private static final Log LOG = LogFactory.getLog(MethodExceptionEventHandler.class);
	
    public Object methodException(Class cls, String method, Exception e) throws Exception {
    	LOG.info("handling exception " + e + " on " + cls.getName() + "." + method + "()");
        if (cls.getName().matches(getClassNameToHandle()) && method.matches(getMethodToHandle())) {
        	LOG.info("don't escalate on catched class+method matching class: '" + getClassNameToHandle() + "' method: '" + getMethodToHandle() + "'");
        	return null;
        } else 
        	throw e;
    }
	String getMethodToHandle() {
		return System.getProperty(ACodeGenerator.KEY_PREFIX + "catchedmethod", "evalOrNull");
	}
	String getClassNameToHandle() {
		return System.getProperty(ACodeGenerator.KEY_PREFIX + "catchedclass", GeneratorUtility.class.getName());
	}
}