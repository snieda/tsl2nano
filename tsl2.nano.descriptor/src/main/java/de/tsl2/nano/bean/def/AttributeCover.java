/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 17.10.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.IConnector;
import de.tsl2.nano.bean.IRuleCover;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.IDelegationHandler;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.historize.Volatile;

/**
 * The AttributeCover is a plugin to {@link AttributeDefinition}s and enables a container of properties to evaluate the
 * values of the properties through a rule engine. This is done by wrapping or covering the child property object into a
 * proxy delegating the evaluation of a value (through calling the getter method) to the rule engine.
 * 
 * @author Tom
 * @version $Revision$
 */
public abstract class AttributeCover<T> extends DelegationHandler<T> implements
        IRuleCover<T>,
        IConnector<IAttributeDefinition<?>> {
    /** serialVersionUID */
    private static final long serialVersionUID = -4157641723681767640L;

    private static final Log LOG = LogFactory.getLog(AttributeCover.class);

    private static final String REF = "-REF-";

    /**
     * map holding the rule name (as value) for each property-name (as key) to be evaluated through a rule engine. the
     * property-names should be described by a path to the desired bean-attribute (e.g.: 'myBean.mysubbean.myattribute')
     */
    @ElementMap(entry = "rule", attribute = true, inline = true, key = "for-property", keyType = String.class, value = "name", valueType = String.class)
    protected Map<String, String> rules;
    /** contextObject: bean to be used as attribute through toValueMap() call. */
    transient Serializable contextObject;
    @Attribute(required = false)
    String name;

	private transient Volatile<T> value;

    /**
     * constructor
     */
    protected AttributeCover() {
        super();
    }

    /**
     * constructor
     * 
     * @param delegate
     * @param context
     */
    public AttributeCover(String name, Map<String, String> rules) {
        super();
        this.name = name;
        this.rules = rules;

    }

    /**
     * convenience to create and connect a AttributeCover to an attribute through a string descriptor.
     * 
     * <pre>
     * syntax: <attribute>:<child>--><rule>
     * 
     * Example: value:presentable.layoutconstraints-->%presValueColor
     * </pre>
     * 
     * @param attributeCoverDescriptor
     * @return {@link AttributeCover} instance
     */
    @SuppressWarnings({ "rawtypes" })
    public static <R extends AttributeCover> R cover(Class<R> implementationClass, Class cls, String attributeCoverDescriptor) {
        String attr = StringUtil.substring(attributeCoverDescriptor, null, ":");
        String child = StringUtil.substring(attributeCoverDescriptor, ":", "-->");
        String rule = StringUtil.substring(attributeCoverDescriptor, "-->", null);
        return (R) cover(implementationClass, cls, attr, child, rule);
    }

    /**
     * covers the given child (as member!) of the instance attribute with the given rule
     * 
     * @param implementationClass AttributeCover implementation (e.g. RuleCover.class from tsl2.nano.h5)
     * @param cls class holding the attribute
     * @param attr class attribute
     * @param child child (member!) of attribute
     * @param rule rule to cover the attribute child
     * @return {@link AttributeCover} instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static AttributeCover cover(Class<? extends AttributeCover> implementationClass, Class cls, String attr, String child, String rule) {
        AttributeCover cover = BeanClass.createInstance(implementationClass, rule, MapUtil.asMap(child, rule));
        
        cover.connect(BeanDefinition.getBeanDefinition(cls).getAttribute(attr));
        return cover;
    }

    /**
     * covers the given child using the given childs interfaze
     * 
     * @param name cover name (normally the childs type name
     * @param child the origin instance to be covered
     * @param interfaze interface of child to be used on the covering proxy.
     * @return child covering proxy object.
     */
    @SuppressWarnings("unchecked")
    public T cover(T child, Class<T> interfaze, Serializable contextObject) {
        delegate = child;
        interfaces = new Class[] {interfaze};
        setContext(contextObject);
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { interfaze }, this);
    }

    /**
     * removes a given rule-cover instance and replaces the proxy with its delegate - the real origin object.
     * 
     * @param instance holding the attribute
     * @param attr instance attribute
     * @param child child of attribute
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void removeCover(Class cls, String attr, String child) {
        String parent = StringUtil.substring(child, null, ".", true);
        IAttributeDefinition attribute = BeanDefinition.getBeanDefinition(cls).getAttribute(attr);
        Bean<IAttributeDefinition> bean = Bean.getBean(attribute);
        //first we try it through bean-attribute relation path
        Object parentProxy = bean.getAttribute(parent, false);
        
        PrivateAccessor<IAttributeDefinition> privAcc = null;
        if (parentProxy == null) { //try it through its members
            privAcc = new PrivateAccessor<>(attribute);
            parentProxy = privAcc.member(StringUtil.substring(child, null, "."));
        }
        if (!Proxy.isProxyClass(parentProxy.getClass()))
            LOG.error("no rule-cover found for " + attr + "." + child);
        InvocationHandler handler = Proxy.getInvocationHandler(parentProxy);
        if (handler instanceof AttributeCover) {
            Object realObject = ((AttributeCover) handler).getDelegate();
            if (privAcc != null) //-> as member
                privAcc.set(StringUtil.substring(child, null, "."), realObject);
            else //as bean attribute
                bean.setValue(parent, realObject);
        } else {
            LOG.error("proxy for " + attr + "." + child + " is not a AttributeCover instance!");
        }
        cachedConnectionEndTypes.remove(attribute);
        
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRule(String propertyPath) {
//        String typedName = RuleExpression.expressionPattern().matches(propertyPath) ? propertyPath : AbstractRule.PREFIX + propertyPath;
        String ruleName = rules.get(propertyPath);
        boolean existRule = false;
        if (ruleName != null) {
            existRule = checkRule(ruleName);
            if (!existRule) {
                Message.send("couldn't find rule '" + ruleName + "' for property '" + propertyPath
                    + "' in specifications!");
            }
        }
        return ruleName != null && existRule;
    }

    abstract protected boolean checkRule(String ruleName);

	/**
     * @return Returns the context.
     */
    @Override
    public Map<String, Object> getContext() {
        return contextObject != null ? Bean.getBean(contextObject).toValueMap(contextObject, false, true, false)
            : new HashMap<String, Object>();
    }

    /**
     * Attention: this method is not called directly, but through reflection!
     * @param context The context to set.
     */
    @Override
    public void setContext(Serializable contextObject) {
        this.contextObject = contextObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (BeanAttribute.isGetterMethod(method)) {
            String name = BeanAttribute.getName(method);
            if (hasRule(name)) {
            	Volatile<T> vvalue = getVolatileValue();
				return vvalue.expired() ? vvalue.set((T)eval(name)) : vvalue.get();
            }
        }
        return method.invoke(delegate, args);
    }

    private Volatile<T> getVolatileValue() {
    	if (value == null)
    		value = new Volatile<T>(ENV.get("cache.expire.milliseconds.attributecover", 50));
		return value;
	}

	/**
     * {@inheritDoc} the connection will be done for definitions and instances!
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object connect(IAttributeDefinition<?> connectionEnd) {
        //first: extract the direct childs - and their parents as ref object-names
        Set<String> pks = rules.keySet();
        HashMap refs = new HashMap<>();
        String parentRef;
        for (String k : pks) {
            if (k.contains(".")) {
                //cut the last entry
                parentRef = StringUtil.substring(k, null, ".", true);
                if (!rules.containsKey(parentRef)) {
                    refs.put(parentRef, REF);
                }
            }
        }
        rules.putAll(refs);

        //now we cover the properties
//        BeanClass bcDef = BeanClass.getBeanClass(connectionEnd.getClass());
//        List<String> names = Arrays.asList(bcDef.getAttributeNames());

        //working on fields directly because not all attributedefinition-attributes are public
        boolean nameFound = false;
        PrivateAccessor<?> acc = new PrivateAccessor<>(connectionEnd);
        T origin;
        for (String k : pks) {
            if ((origin = (T) acc.forceMember(k.split("\\."))) != null && !Util.isJavaType(origin.getClass())) {
                nameFound = true;
                if (Proxy.isProxyClass(origin.getClass())) {//already covered, create a new proxy from invocationhandler
                    origin = (T) ((IDelegationHandler) Proxy.getInvocationHandler(origin)).getDelegate();
                }
                acc.set(k, cover(origin, acc.typeOf(k), connectionEnd));
            }
        }
        //remove class prefixes to be accessible on invoking
        for (String k : new HashSet<String>(pks)) {
            String ruleName = rules.remove(k);
            rules.put(StringUtil.substring(k, ".", null, true), ruleName);
        }

        if (!nameFound)
            throw new IllegalStateException("no attribute matches for attributedefinition for " + rules.keySet());
        addRuleCover(connectionEnd);
        return this;
    }

    @Override
    public void disconnect(IAttributeDefinition<?> connectionEnd) {
        throw new UnsupportedOperationException();
    }

    /* 
     * technical workaround for performance
     * TODO: to enhance performance, all beans with their proxies should be cached!
     */
    
    /** to perform on injecting instances into rule-covers, this set holds all attributes that have to be covered */
    static Set<IAttributeDefinition<?>> cachedConnectionEndTypes = new HashSet<>();
    static void addRuleCover(IAttributeDefinition<?> attr) {
    	cachedConnectionEndTypes.add(definitionOf(attr));
    }
	static boolean hasRuleCover(IAttributeDefinition<?> attr) {
		if (attr.isVirtual())
			return false; //TODO: howto get BeanDefinition of virtual attribute?
		return cachedConnectionEndTypes.contains(definitionOf(attr));
    }
    static IAttributeDefinition<?> definitionOf(IAttributeDefinition<?> attr) {
		return attr.getClass().equals(AttributeDefinition.class) ? attr : BeanDefinition.getBeanDefinition(attr.getDeclaringClass()).getAttribute(attr.getName());
	}

    public static void resetTypeCache() {
        cachedConnectionEndTypes.clear();
    }

    @Override
    public String toString() {
        return super.toString() + " name: " + name;
    }
}
