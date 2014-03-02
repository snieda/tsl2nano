/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Nov 13, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.enhance;

import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import org.apache.commons.logging.Log;
import de.tsl2.nano.log.LogFactory;

import de.tsl2.nano.bean.BeanClass;
import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.util.StringUtil;

/**
 * enhance bean to have getter/setter methods.
 * <p/>
 * a tutorial for javassist can be found here:
 * http://www.csg.ci.i.u-tokyo.ac.jp/~chiba/javassist/tutorial/tutorial.html.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class BeanEnhancer<T> {
    private static final Map<Class<?>, CtClass> cache = new HashMap<Class<?>, CtClass>();

    static final String PRE_INTERFACE = "I";
    static final String PRE_GETTER = "get";
    static final String PRE_SETTER = "set";
    static final String POST_CLASS = "BE";
    static final CtClass[] EMPTY_CTCLASS_ARR = new CtClass[] {};

    private static final Log LOG = LogFactory.getLog(BeanEnhancer.class);

    /**
     * delegates to {@link #enhance(Object, String, boolean)} with createInterface=false
     */
    public static <T> T enhance(T bean) {
        return enhance(bean, null, false);
    }

    /**
     * enhance bean to have getter/setter methods.
     * <p/>
     * 
     * @param <T>
     * @param bean without getter/setter methods
     * @param newName (optional) new class name
     * @param createInterface if true, an interface with getters/setters will be defined. then, a BeanProxy implementing
     *            that interface will be returned.
     * @return enhanced bean (new instance of new bean class, or bean proxy)
     */
    public static <T> T enhance(T bean, String newName, boolean createInterface) {
        ClassPool cp = ClassPool.getDefault();

        try {
            Class<T> bc = (Class<T>) bean.getClass();
            newName = newName == null ? bc.getSimpleName() : newName;
            CtClass cls;
            CtClass bCls = cp.getCtClass(bc.getName());
            CtField[] fields = bCls.getFields();
            if (createInterface) {
                cls = cp.makeInterface(bc.getPackage().getName() + "." + PRE_INTERFACE + newName);
            } else {
                cls = cp.getAndRename(bc.getName(), bc.getPackage().getName() + "." + newName + POST_CLASS);
                /*
                 * on anonymous inner classes we need the default constructor, that is not defined in the original class.
                 * but, how to replace that inner class and its outer class in the context classloader?
                 */
                if (bc.isAnonymousClass()) {
//                    //first: create the default constructor for the origin class
//                  CtClass acls = cp.getCtClass(bc.getName());
//                    CtConstructor defaultConstructor = CtNewConstructor.make(EMPTY_CTCLASS_ARR, null, acls);
//                    defaultConstructor.setModifiers(AccessFlag.PUBLIC);
//                    acls.addConstructor(defaultConstructor);
//                    acls.toClass();
                    //second: default constructor for the new class
                    CtConstructor defaultConstructor = CtNewConstructor.make(EMPTY_CTCLASS_ARR, null, cls);
                    defaultConstructor.setModifiers(AccessFlag.PUBLIC);
                    cls.addConstructor(defaultConstructor);
                } else {
                    cls.setSuperclass(cp.getCtClass(bc.getName()));
                }
            }
            String getterBody = null, setterBody = null;
            for (int i = 0; i < fields.length; i++) {
                String name = fields[i].getName();
                if (!createInterface) {
                    getterBody = "return " + name + ";";
                    setterBody = "this." + name + "=$1;";
                }
                addMethod(cls, fields[i].getType(), getMethodName(PRE_GETTER, name), getterBody);
                addMethod(cls,
                    CtClass.voidType,
                    getMethodName(PRE_SETTER, name),
                    setterBody,
                    fields[i].getType());
            }
//            if (LOG.isDebugEnabled()) {
            cls.debugWriteFile("c:/test");
            System.out.println(cls.toString());
            cache.put(bean.getClass(), cls);
            return createInterface ? (T) BeanProxy.createBeanImplementation(cls.toClass(),
                null,
                null,
                Thread.currentThread().getContextClassLoader()) : createInstance(bean, cls);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * TODO: NOT TESTED/USED YET
     * enhance bean to do something before/after getting/setting an attribute
     * @param <T>
     * @param bean bean to enhance
     * @param invokeBefore to be done before...
     * @param invokeAfter to be done after...
     * @param attributes attributes to enhance
     * @return enhanced bean
     */
    public static final <T> T enhance(T bean, String invokeBefore, String invokeAfter, String... attributes) {
        try {
            String clsName = bean.getClass().getName();
            CtClass cls = ClassPool.getDefault().getAndRename(clsName, clsName + POST_CLASS);
            for (int i = 0; i < attributes.length; i++) {
                //TODO: method-descriptor?
                CtMethod method = cls.getMethod(getMethodName(PRE_GETTER, attributes[i]), null);
                method.insertBefore(invokeBefore);
                method.insertAfter(invokeAfter);
            }
            return createInstance(bean, cls);
        } catch (Exception ex) {
            ManagedException.forward(ex);
            return null;
        }
    }

    /**
     * enhance bean to do something before/after calling a method
     * @param <T>
     * @param bean bean to enhance
     * @param invokeBefore to be done before...
     * @param invokeAfter to be done after...
     * @param methods methods to enhance
     * @return enhanced bean
     */
    public static final <T> T enhance(T bean, String invokeBefore, String invokeAfter, Method... methods) {
        try {
            ClassPool cp = ClassPool.getDefault();
            String clsName = bean.getClass().getName();
            CtClass cls = cp.getAndRename(clsName, clsName + POST_CLASS);
//            CtClass args[];
//            Class a[];
            for (int i = 0; i < methods.length; i++) {
//                a = methods[i].getParameterTypes();
//                args = new CtClass[a.length];
//                for (int j = 0; j < a.length; j++) {
//                    args[j] = cp.getCtClass(a[i].getName());
//                }
                CtMethod method;
                try {
                    method = cls.getDeclaredMethod(methods[i].getName(), null/*new MethodDescriptor(methods[i]).getName()*/);
                } catch (Exception e) {
                    //second try on all other methods
                    method = cls.getMethod(methods[i].getName(), null/*new MethodDescriptor(methods[i]).getName()*/);
                }
                if (invokeBefore != null)
                    method.insertBefore(invokeBefore);
                if (invokeAfter != null)
                    method.insertAfter(invokeAfter);
            }
            //TODO: duplicated - all methods are overridden...
            cls.setSuperclass(cp.getCtClass(clsName));

            return createInstance(bean, cls);
        } catch (Exception ex) {
            ManagedException.forward(ex);
            return null;
        }
    }

    /**
     * creates a new interface for the given attributes. for each attribute there will be a getter method. if
     * writeAccess=true, each attribute will have a setter method. the resulting object will be a bean proxy,
     * implementing that new interface and holding all given objects.
     * 
     * @param interfaceName new interface name (only for logging and debugging)
     * @param writeAccess if true, the new interface will define setter methods for all given attributes.
     * @param attributes attribute names and their values. if you don't have a value, you have to give the attribute
     *            type. nulls are not allowed!
     * @return
     */
    public static final Object createObject(String interfaceName, boolean writeAccess, Map<String, Object> attributes) {
        ClassPool cp = ClassPool.getDefault();
        try {
            CtClass cls = cp.makeInterface(interfaceName);
            Set<String> names = attributes.keySet();
            HashMap<String, Object> values = new HashMap<String, Object>();
            for (String n : names) {
                Object obj = attributes.get(n);
                CtClass fieldType = cp.getCtClass(getClassName(obj));
                addMethod(cls, fieldType, getMethodName(PRE_GETTER, n), null);
                addMethod(cls, CtClass.voidType, getMethodName(PRE_SETTER, n), null, fieldType);
                /*
                 * remap the attributes: all class/type definitions should be null values inside the proxy
                 */
                if (obj instanceof Class)
                    values.put(n, null);
                else
                    values.put(n, obj);
            }
            if (LOG.isDebugEnabled()) {
                cls.debugWriteFile("c:/test");
                System.out.println(cls.toString());
            }
            return BeanProxy.createBeanImplementation(cls.toClass(), values, null, Thread.currentThread()
                .getContextClassLoader());
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private static final String getMethodName(String prefix, String name) {
        return prefix + StringUtil.toFirstUpper(name);
    }
    private static final String getClassName(Object o) {
        return o instanceof Class ? ((Class) o).getName() : o.getClass().getName();
    }

    /**
     * creates a new instance of type cls containing content of bean.
     * 
     * @param <T> cls type
     * @param bean bean to copy
     * @param cls type of new instance
     * @return new instance
     */
    private static <T> T createInstance(T bean, CtClass cls) {
        try {
            return (T) BeanUtil.copy(bean, BeanClass.getBeanClass(cls.toClass()).createInstance());
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * if no parameters are given, a return-type will be defined, otherwise not!
     * 
     * @param cls class to add the method to
     * @param field field properties (type)
     * @param name
     * @param body method body
     * @param parameter (optional) method call argument types
     */
    static void addMethod(CtClass cls, CtClass returnType, String name, String body, CtClass... parameter) {
        try {
            CtMethod ctMethod = CtNewMethod.make(returnType, name, parameter, EMPTY_CTCLASS_ARR, body, cls);
            if (cls.isInterface()) {
                ctMethod.setModifiers(ctMethod.getModifiers() | AccessFlag.ABSTRACT);
                /*
                 * without removing the "code attribute" on interfaces we get the following error:
                 *  "java.lang.ClassFormatError: Code attribute in native or abstract methods in class file Test"
                 * 
                 */
                ctMethod.getMethodInfo().removeCodeAttribute();
            }
            cls.addMethod(ctMethod);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }
}
