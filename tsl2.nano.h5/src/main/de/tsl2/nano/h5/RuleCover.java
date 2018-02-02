/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 17.10.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

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
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.IDelegationHandler;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.rules.RulePool;

/**
 * The RuleCover is a plugin to {@link AttributeDefinition}s and enables a container of properties to evaluate the
 * values of the properties through a rule engine. This is done by wrapping or covering the child property object into a
 * proxy delegating the evaluation of a value (through calling the getter method) to the rule engine.
 * 
 * @author Tom
 * @version $Revision$
 */
public class RuleCover<T> extends DelegationHandler<T> implements
        IRuleCover<T>,
        IConnector<IAttributeDefinition<?>> {
    /** serialVersionUID */
    private static final long serialVersionUID = -4157641723681767640L;

    private static final Log LOG = LogFactory.getLog(RuleCover.class);

    private static final String REF = "-REF-";

    /**
     * map holding the rule name (as value) for each property-name (as key) to be evaluated through a rule engine. the
     * property-names should be described by a path to the desired bean-attribute (e.g.: 'myBean.mysubbean.myattribute')
     */
    @ElementMap(entry = "rule", attribute = true, inline = true, key = "for-property", keyType = String.class, value = "name", valueType = String.class)
    Map<String, String> rules;
    /** contextObject: bean to be used as attribute through toValueMap() call. */
    transient Serializable contextObject;
    @Attribute(required = false)
    String name;

    /**
     * constructor
     */
    protected RuleCover() {
        super();
    }

    /**
     * constructor
     * 
     * @param delegate
     * @param context
     */
    public RuleCover(String name, Map<String, String> rules) {
        super();
        this.name = name;
        this.rules = rules;

//        //store its own property rules (cutting the prefix of its name)
//        Set<String> props = rules.keySet();
//        rules = new Hashtable<>();
//        for (String k : props) {
//            if (k.startsWith(name + ".")) {
//                rules.put(StringUtil.substring(k, name + ".", null), rules.get(k));
//            }
//        }
    }

    /**
     * convenience to create and connect a rulecover to an attribute through a string descriptor.
     * 
     * <pre>
     * syntax: <attribute>:<child>--><rule>
     * 
     * Example: value:presentable.layoutconstraints-->%presValueColor
     * </pre>
     * 
     * @param ruleCoverDescriptor
     * @return {@link RuleCover} instance
     */
    @SuppressWarnings({ "rawtypes" })
    public static RuleCover cover(Class cls, String ruleCoverDescriptor) {
        String attr = StringUtil.substring(ruleCoverDescriptor, null, ":");
        String child = StringUtil.substring(ruleCoverDescriptor, ":", "-->");
        String rule = StringUtil.substring(ruleCoverDescriptor, "-->", null);
        return cover(cls, attr, child, rule);
    }

    /**
     * covers the given child (as member!) of the instance attribute with the given rule
     * 
     * @param cls class holding the attribute
     * @param attr class attribute
     * @param child child (member!) of attribute
     * @param rule rule to cover the attribute child
     * @return {@link RuleCover} instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static RuleCover cover(Class cls, String attr, String child, String rule) {
        RuleCover cover = new RuleCover<>(rule, MapUtil.asMap(child, rule));
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
            parentProxy = privAcc.member(StringUtil.substring(child, ".", null));
        }
        if (!Proxy.isProxyClass(parentProxy.getClass()))
            LOG.error("no rule-cover found for " + attr + "." + child);
        InvocationHandler handler = Proxy.getInvocationHandler(parentProxy);
        if (handler instanceof RuleCover) {
            Object realObject = ((RuleCover) handler).getDelegate();
            if (privAcc != null) //-> as member
                privAcc.set(StringUtil.substring(child, ".", null), realObject);
            else //as bean attribute
                bean.setValue(parent, realObject);
        } else {
            LOG.error("proxy for " + attr + "." + child + " is not a RuleCover instance!");
        }
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
        boolean hasRule = rules.containsKey(propertyPath);
        boolean existRule = false;
        if (hasRule) {
            String ruleName = rules.get(propertyPath);
            existRule = ENV.get(RulePool.class).get(ruleName) != null;
            if (!existRule) {
                Message.send("couldn't find rule '" + ruleName + "' for property '" + propertyPath
                    + "' in specifications!");
            }
        }
        return hasRule && existRule;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object eval(String propertyPath) {
        return ENV.get(RulePool.class).get(rules.get(propertyPath)).run(getContext());
    }

    /**
     * @return Returns the context.
     */
    @Override
    public Map<String, Object> getContext() {
        return contextObject != null ? Bean.getBean(contextObject).toValueMap(contextObject, false, true, false)
            : new HashMap<String, Object>();
    }

    /**
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
                return eval(name);
            }
        }
        return method.invoke(delegate, args);
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
        return this;
    }

    @Override
    public void disconnect(IAttributeDefinition<?> connectionEnd) {
        throw new UnsupportedOperationException();
    }

}
