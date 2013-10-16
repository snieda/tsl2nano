/*
 * 
 * 
 * Copyright © 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */
package de.tsl2.nano.bean;

import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.util.StringUtil;

/**
 * reflection helper class.
 * 
 * @author ts 21.11.2008
 * @version $Revision: 1.0 $
 */
public class ClassReflector {
    private static final Log LOG = LogFactory.getLog(ClassReflector.class);

    /** flag to indicate a code enhancing class */
    public static final String ENHANCER_PREATTACHMENT = "$$";

    /**
     * Returns the class name (with package). Inner classes and enhancements will be ignored. A class with name
     * 'mypackage.MyClassName$$EnhancerByCGLIB$$b29f1660' will return 'MyClassName'! Proxy classes will return the first
     * implementing interface name!
     * 
     * @param clazz the beans class
     * @return full class name
     */
    public static String getDefaultBeanClassName(Class clazz) {
        if (Proxy.isProxyClass(clazz)) {
            clazz = clazz.getInterfaces()[0];
        }
        final String tmpClassName = clazz.getName();
        //filter enhancement-classname (e.g. from CGLIB)
        final String realClassName = StringUtil.substring(tmpClassName, null, ENHANCER_PREATTACHMENT);
        if (!tmpClassName.equals(realClassName)) {
            System.out.println(" ClassReflector: using classname " + realClassName + " instead of " + tmpClassName);
        }
        return realClassName;
    }

    /**
     * @see #getDefaultBeanClassName(Class)
     * @param type to extract 'real' (not enhanced) bean class
     * @param loader to load the 'real' bean class.
     * @return new loaded 'real' bean class.
     */
    public static Class getBeanClass(Class type, ClassLoader loader) {
//    ClassLoader save = null;
        try {
            if (loader != null) {
//        save = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(loader);
            } else {
                loader = ClassReflector.class.getClassLoader();
            }
            return loader.loadClass(getDefaultBeanClassName(type));
        } catch (final ClassNotFoundException ex) {
            LOG.warn("failed to load bean class", ex);
            return null;
        } finally {
//      if (save != null)
//        Thread.currentThread().setContextClassLoader(save);
        }
    }

}
