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
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.bean.IConnector;
import de.tsl2.nano.bean.IRuleCover;
import de.tsl2.nano.bean.IValueAccess;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * The RuleCover is a plugin to {@link AttributeDefinition}s and enables a container of properties to evaluate the
 * values of the properties through a rule engine. This is done by wrapping or covering the child property object into a
 * proxy delegating the evaluation of a value (through calling the getter method) to the rule engine.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RuleCover implements IConnector<AttributeDefinition<?>> {
    /** serialVersionUID */
    private static final long serialVersionUID = -7935692478754934118L;

    /**
     * map holding the rule name for each property-name to be evaluated through a rule engine. the property-names should
     * be described by a path to the desired bean-attribute (e.g.: 'myBean.mysubbean.myattribute')
     */
    @ElementMap(entry = "rule", key = "for-property", attribute = true, inline = true, value = "name", valueType = String.class, required = true)
    Map<String, String> propertyRules;

    transient Collection<IRuleCover<?>> covers;

    public boolean hasRules(Object child) {
        return false;
    }

    /**
     * {@inheritDoc} the connection will be done for definitions and instances!
     */
    @Override
    public Object connect(AttributeDefinition<?> connectionEnd) {
        //first: extract the direct childs
        Set<String> pks = propertyRules.keySet();
        for (String k : pks) {
            if (k.contains(".")) {
                String c = StringUtil.substring(k, null, ".", true);
                if (!propertyRules.containsKey(c))
                    propertyRules.put(c, "---");
            }
        }
        //now we cover the properties
        BeanClass bcDef = BeanClass.getBeanClass(connectionEnd.getClass());
        List<String> names = Arrays.asList(bcDef.getAttributeNames());

        PrivateAccessor<?> acc = new PrivateAccessor<>(connectionEnd);
        for (String k : pks) {
            if (names.contains(k)) {
                Object origin = acc.member(k);
                if (!Proxy.isProxyClass(origin.getClass())) {
                    Object cover = cover(k, origin, acc.typeOf(k), connectionEnd);
                    acc.set(k, cover);
                } else {//already covered, set the instance of the current context object
                    ((IRuleCover) Proxy.getInvocationHandler(origin))
                        .setContext((Serializable) ((IValueDefinition) connectionEnd).getInstance());
                }
            }
        }

        return this;
    }

    @Override
    public void disconnect(AttributeDefinition<?> connectionEnd) {
        BeanClass bcDef = BeanClass.getBeanClass(connectionEnd.getClass());
        for (IRuleCover<?> cover : getCovers()) {
            bcDef.setValue(connectionEnd, cover.getName(), cover.getDelegate());
            //release the context instance to avoid memory leaks
            cover.setContext(null);
        }
    }

    /**
     * covers the given child using the given childs interfaze
     * 
     * @param name cover name (normally the childs type name
     * @param child the origin instance to be covered
     * @param interfaze interface of child to be used on the covering proxy.
     * @return child covering proxy object.
     */
    public <T> T cover(String name, T child, Class<T> interfaze, Serializable contextObject) {
        RuleCoverProxy invHandler = new RuleCoverProxy(this, name, child);
        invHandler.setContext(contextObject);
        T cover =
            (T) Proxy.newProxyInstance(Environment.get(ClassLoader.class), new Class[] { interfaze },
                invHandler);
        if (covers == null)
            covers = new LinkedList<>();
        covers.add((IRuleCover<?>) Proxy.getInvocationHandler(cover));
        return cover;
    }

    /**
     * getContentObject
     * 
     * @param cover
     * @return the origin object of the given covering proxy.
     */
    public <T> T getContentObject(IRuleCover<T> cover) {
        return cover.getDelegate();
    }

    /**
     * getCovers
     * 
     * @return all created covers of this plugin
     */
    public Collection<IRuleCover<?>> getCovers() {
        return covers;
    }

}

class RuleCoverProxy<T> implements IRuleCover<T>, InvocationHandler {
    Map<String, String> rules;
    Serializable contextObject;
    T delegate;
    String name;

    /**
     * constructor
     * 
     * @param delegate
     */
    public RuleCoverProxy(RuleCover parent, String name, T delegate) {
        this(parent, name, delegate, null);
    }

    /**
     * constructor
     * 
     * @param delegate
     * @param context
     */
    public RuleCoverProxy(RuleCover parent, String name, T delegate, Map<String, Object> context) {
        super();
        this.delegate = delegate;
        this.contextObject = contextObject;

        //store its own property rules (cutting the prefix of its name)
        Set<String> props = parent.propertyRules.keySet();
        rules = new Hashtable<>();
        for (String k : props) {
            if (k.startsWith(name + ".")) {
                rules.put(StringUtil.substring(k, name + ".", null), parent.propertyRules.get(k));
            }
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
        boolean hasRule = rules.containsKey(propertyPath);
        boolean existRule = false;
        if (hasRule) {
            String ruleName = rules.get(propertyPath);
            existRule = Environment.get(RulePool.class).get(ruleName) != null;
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
        return Environment.get(RulePool.class).get(rules.get(propertyPath)).run(getContext());
    }

    /**
     * @return Returns the context.
     */
    @Override
    public Map<String, Object> getContext() {
        return Bean.getBean(contextObject).toValueMap(null);
    }

    /**
     * @param context The context to set.
     */
    @Override
    public void setContext(Serializable contextObject) {
        this.contextObject = contextObject;
    }

    /**
     * @return Returns the delegate.
     */
    @Override
    public T getDelegate() {
        return delegate;
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

}
