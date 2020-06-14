package de.tsl2.nano.codegen;
import org.apache.velocity.app.event.MethodExceptionEventHandler;

import de.tsl2.nano.core.cls.BeanClass;

public class EvalOrNullExceptionHandler implements MethodExceptionEventHandler {
    public Object methodException(Class cls, String method, Exception e) throws Exception {
        if (getClassToHandle().isAssignableFrom(cls) && method.equals(getMethodToHandle()))
        	return null;
        else 
        	throw e;
    }
	Object getMethodToHandle() {
		return System.getProperty(ACodeGenerator.KEY_PREFIX + "catchedmethod", "evalOrNull");
	}
	Class getClassToHandle() {
		String notThrowingName = System.getProperty(ACodeGenerator.KEY_PREFIX + "catchedclass", GeneratorUtility.class.getName());
		return BeanClass.load(notThrowingName);
	}
}