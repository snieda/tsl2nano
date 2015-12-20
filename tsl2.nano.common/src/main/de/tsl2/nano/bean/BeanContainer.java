/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 4, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.bean;

import java.io.Serializable;
import java.sql.Time;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * class to be used as simple one instance bean container. initialize it with your service actions (through calls to
 * specific service factories). it is a delegating class to avoid depending to a service factory and its services.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "serial", "rawtypes", "unchecked" })
public class BeanContainer implements IBeanContainer {
    private static BeanContainer self = null;

    protected IAction<Collection<?>> typeFinderAction = null;
    protected IAction<Collection<?>> exampleFinderAction = null;
    protected IAction<Collection<?>> betweenFinderAction = null;
    protected IAction<Collection<?>> queryAction = null;
    protected IAction<Collection<?>> queryMapAction = null;
    protected IAction<?> lazyrelationInstantiateAction = null;
    protected IAction<?> saveAction = null;
    protected IAction<?> deleteAction = null;
    protected IAction<IAttributeDef> attrdefAction = null;
    protected IAction<?> permissionAction = null;
    protected IAction<?> persistableAction = null;
    protected IAction<Integer> executeAction = null;

    private BeanContainer() {
        super();
    }

    /**
     * you must have called {@link #initServiceActions(IAction, IAction, IAction)} before you can use the instance.
     * 
     * @return singelton instance
     */
    public static final BeanContainer instance() {
        assert self != null : "beancontainer not initialized! call initServiceActions(...) before!";
        return self;
    }

    /**
     * isInitialized
     * 
     * @return true, if {@link #initEmtpyServiceActions()} or
     *         {@link #initServiceActions(IAction, IAction, IAction, IAction, IAction, IAction, IAction, IAction, IAction, IAction)}
     *         was called before.
     */
    public static final boolean isInitialized() {
        return self != null;
    }

    /**
     * call this method only on initializing your new application.
     * 
     * @param relationFinder action to find beans through a service
     * @param lazyRelationResolver to fetch all lazy relations
     * @param saveAction action to save bean through service
     * @param deleteAction action to delete bean through service
     * @param exampleFinder action to find beans through a service
     * @param betweenFinder action to find beans through a service
     * @param attrdefAction action to evaluate attribute definitions
     * @param permissionAction action to evaluate the permission (through roles)
     * @param persistableAction asks, if the given bean is persistable
     */
    public static final void initServiceActions(IAction<Collection<?>> relationFinder,
            IAction<?> lazyRelationResolver,
            IAction<?> saveAction,
            IAction<?> deleteAction,
            IAction<Collection<?>> exampleFinder,
            IAction<Collection<?>> betweenFinder,
            IAction<Collection<?>> queryFinder,
            IAction<Collection<?>> queryMapFinder,
            IAction<IAttributeDef> attrdefAction,
            IAction<Boolean> permissionAction,
            IAction<Boolean> persistableAction,
            IAction<Integer> executeAction) {
        self = new BeanContainer();
        self.typeFinderAction = relationFinder;
        self.lazyrelationInstantiateAction = lazyRelationResolver;
        self.saveAction = saveAction;
        self.deleteAction = deleteAction;
        self.exampleFinderAction = exampleFinder;
        self.betweenFinderAction = betweenFinder;
        self.queryAction = queryFinder;
        self.queryMapAction = queryMapFinder;
        self.attrdefAction = attrdefAction;
        self.permissionAction = permissionAction;
        self.persistableAction = persistableAction;
        self.executeAction = executeAction;
    }

    /**
     * will call {@link #initServiceActions(IAction, IAction, IAction)} with simple actions, doing nothing. useful for
     * testing.
     */
    public static final void initEmtpyServiceActions() {
        final Collection<?> EMPTY_LIST = null;//new LinkedList();
        final IAction<Collection<?>> relationFinder = new CommonAction<Collection<?>>("empty.service.relationFinder") {
            @Override
            public Collection<?> action() {
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> exampleFinder = new CommonAction<Collection<?>>("empty.service.exampleFinder") {
            @Override
            public Collection<?> action() {
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> betweenFinder = new CommonAction<Collection<?>>("empty.service.betweenFinder") {
            @Override
            public Collection<?> action() {
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> queryFinder = new CommonAction<Collection<?>>("empty.service.queryFinder") {
            @Override
            public Collection<?> action() {
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> queryMapFinder = new CommonAction<Collection<?>>("empty.service.queryMapFinder") {
            @Override
            public Collection<?> action() {
                return EMPTY_LIST;
            }
        };
        final IAction lazyRelationResolver = new CommonAction("empty.service.lazyRelationResolver") {
            @Override
            public Object action() {
                //do nothing, return the instance itself
                return getParameter()[0];
            }
        };
        final IAction saveAction = new CommonAction("empty.service.saveAction") {
            @Override
            public Object action() {
                //do nothing, return the instance itself
                return getParameter()[0];
            }
        };
        final IAction deleteAction = new CommonAction("empty.service.deleteAction") {
            @Override
            public Object action() {
                //do nothing
                return null;
            }
        };
        final IAction<IAttributeDef> attrAction = new CommonAction<IAttributeDef>("empty.service.attrAction") {
            @Override
            public IAttributeDef action() {
                //do nothing
                return null;
            }
        };
        final IAction permissionAction = new CommonAction("empty.service.permissionAction") {
            @Override
            public Object action() {
                //do nothing
                return true;
            }
        };
        final IAction persistableAction = new CommonAction("empty.service.persistableAction") {
            @Override
            public Object action() {
                return Serializable.class.isAssignableFrom((Class<?>) getParameter()[0]);
            }
        };
        final IAction<Integer> executeAction = new CommonAction<Integer>("empty.service.executeAction") {
            @Override
            public Integer action() {
                return null;
            }
        };
        BeanContainer.initServiceActions(relationFinder,
            lazyRelationResolver,
            saveAction,
            deleteAction,
            exampleFinder,
            betweenFinder,
            queryFinder,
            queryMapFinder,
            attrAction,
            permissionAction,
            persistableAction,
            executeAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T createBean(Class<T> type) {
        return createBeanInstance(type);
    }

    /**
     * createBeanInstance
     * 
     * @param <T>
     * @param type
     * @return
     */
    public static <T> T createBeanInstance(Class<T> type) {
        try {
            T bean;
            if (type.isInterface()) {
                bean = BeanProxy.createBeanImplementation(type, null, null, Thread.currentThread()
                    .getContextClassLoader());
            } else {
                bean = BeanClass.createInstance(type);
            }
            //to fulfil bindings on onetomany, we bind empty lists
            final BeanClass clazz = BeanClass.getBeanClass(type);
            final Collection<BeanAttribute> multiValueAttributes = clazz.getMultiValueAttributes();
            for (final BeanAttribute beanAttribute : multiValueAttributes) {
                if (Set.class.isAssignableFrom(beanAttribute.getType())) {
                    beanAttribute.setValue(bean, new LinkedHashSet());
                } else if (Collection.class.isAssignableFrom(beanAttribute.getType())) {
                    beanAttribute.setValue(bean, new LinkedList());
                } else if (Properties.class.isAssignableFrom(beanAttribute.getType())) {
                    beanAttribute.setValue(bean, new Properties());
                } else if (Map.class.isAssignableFrom(beanAttribute.getType())) {
                    beanAttribute.setValue(bean, new LinkedHashMap());
                }
            }
            // clean date and time values
            List<IAttribute> attributes = clazz.getSingleValueAttributes();
            for (IAttribute beanAttribute : attributes) {
                if (Time.class.isAssignableFrom(beanAttribute.getType())) {
                    if (ENV.get("default.field.date.clear.time", true))
                        DateUtil.clearSeconds((Date) beanAttribute.getValue(bean));
                } else if (Date.class.isAssignableFrom(beanAttribute.getType())) {
                    if (ENV.get("default.field.time.clear.seconds", true))
                        DateUtil.clearTime((Date) beanAttribute.getValue(bean));
                }
            }
            return bean;
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void delete(T bean) {
        deleteAction.setParameter(new Object[] { bean });
        deleteAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T save(T bean) {
        saveAction.setParameter(new Object[] { bean });
        return (T) saveAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getBeans(Class<T> type, int startIndex, int maxResult) {
        typeFinderAction.setParameter(new Object[] { type, startIndex, maxResult });
        return (Collection<T>) typeFinderAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getBeansByExample(T exampleBean) {
        return getBeansByExample(exampleBean, false, 0, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getBeansByExample(T exampleBean, Boolean useLike, int startIndex, int maxResult) {
        exampleFinderAction.setParameter(new Object[] { exampleBean, useLike, startIndex, maxResult });
        return (Collection<T>) exampleFinderAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getBeansBetween(T firstExampleBean, T secondExampleBean, int startIndex, int maxResult) {
        betweenFinderAction.setParameter(new Object[] { firstExampleBean, secondExampleBean, startIndex, maxResult });
        return (Collection<T>) betweenFinderAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T resolveLazyRelations(T bean) {
        lazyrelationInstantiateAction.setParameter(new Object[] { bean });
        return (T) lazyrelationInstantiateAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDef getAttributeDef(Object bean, String attributeName) {
        attrdefAction.setParameter(new Object[] { bean, attributeName });
        return attrdefAction.activate();
    }

    @Override
    public Boolean hasPermission(String roleName, String action) {
        permissionAction.setParameter(new Object[] { roleName, action });
        return (Boolean) permissionAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPersistable(Class<?> beanClass) {
        persistableAction.setParameter(new Object[] { beanClass });
        return (Boolean) persistableAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getBeansByQuery(String query, Boolean nativeQuery, Object[] args, Class... lazyRelations) {
        queryAction.setParameter(new Object[] { query, nativeQuery, args, lazyRelations });
        return (Collection<T>) queryAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getBeansByQuery(String query,
            Boolean nativeQuery,
            Map<String, Object> par,
            Class... lazyRelations) {
        queryMapAction.setParameter(new Object[] { query, nativeQuery, par, lazyRelations });
        return (Collection<T>) queryMapAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer executeStmt(String query, Boolean nativeQuery, Object[] args) {
        executeAction.setParameter(new Object[] { query, nativeQuery, args });
        return executeAction.activate();
    }

    /**
     * isConstraintError
     * 
     * @param e server exception
     * @return true, if error-cause contains a constraint violation
     */
    public static boolean isConstraintError(Exception e) {
        //etwas unsauber!
        Throwable cause = e;
        Throwable c = cause;
        while (c != null) {
            cause = c;
            c = c.getCause();
        }
        if (cause.toString().contains("onstraint")) {
            return true;
        }
        return false;
    }

    /**
     * convenience method to ask for a user role depending on a bean presenters action.
     * 
     * @param beanType bean class. this should be a specific class like an entity - no collection of entities!
     * @param packedInList true, if the bean type is packed into a collection. e.g. in
     *            BeanContainerView/BeanContainerDialog.
     * @param actionName translated action name.
     * @return true, if user has role for that action, false otherwise
     */
    public static final boolean hasPermission(Class<?> beanType, boolean packedInList, String actionName) {
        return instance().hasPermission(getActionId(beanType, packedInList, actionName), null);
    }

    /**
     * data rows for the given type
     * 
     * @param beanType bean type
     * @return count(*) for type
     */
    public static final long getCount(Class<?> beanType) {
        return ((Number) instance()
            .getBeansByQuery("select count(*) from " + beanType.getSimpleName(), true, (Object[]) null).iterator()
            .next()).longValue();
    }

    /**
     * getActionId
     * 
     * @param beanType presenters bean type to create an action for
     * @param packedInList whether it is a bean instance or a bean container
     * @param actionName simple action name
     * @return full action id
     */
    public static final String getActionId(Class<?> beanType, boolean packedInList, String actionName) {
        final String type =
            BeanClass.getName(beanType).toLowerCase() + (packedInList ? Messages.getString("tsl2nano.list")
                .toLowerCase()
                : "");
        return type + "." + actionName.toLowerCase();
    }

    /**
     * will search for the actionId in the resource bundle - then in tsl2nano base bundle - if not found, the action
     * simple name will be returned
     * 
     * @param actionId full action id
     * @param actionSimpleName simple action name
     * @param if true, the tooltip for the action will be evaluated
     * @return text to show on action
     */
    public static final String getActionText(String actionId, boolean tooltip) {
        final String tooltip_postfix = (tooltip ? Messages.POSTFIX_TOOLTIP : "");
        final String actionSimpleName = actionId.substring(actionId.lastIndexOf(".") + 1);
        String text = Messages.getString(actionId + tooltip_postfix);
        if (Messages.unknown(text)) {
            text = Messages.getString("tsl2nano." + actionSimpleName + tooltip_postfix);
            if (Messages.unknown(text)) {
                return StringUtil.toFirstUpper(actionSimpleName);
            } else if (tooltip) {
                return "";
            } else {
                return text;
            }
        }
        return text;
    }

    /**
     * evaluates the given entities bean id. don't call that method, if the bean is not an ejb entity!
     * 
     * @param beanOrClass entity bean or type to evaluate
     * @return bean id attribute or null, if not existent
     */
    public static BeanAttribute getIdAttribute(Object beanOrClass) {
        if (!isInitialized()) {
            return null;
        }
        Class cls =
            (Class) (beanOrClass == null || beanOrClass instanceof Class ? beanOrClass : beanOrClass.getClass());
        if (cls == null || !instance().isPersistable(cls))
            return null;
        final BeanClass bc = BeanClass.getBeanClass(cls);
        final Collection<BeanAttribute> attributes = bc.getSingleValueAttributes();
        for (final BeanAttribute attr : attributes) {
            final IAttributeDef attributeDef = BeanContainer.instance().getAttributeDef(cls, attr.getName());
            if (attributeDef != null && attributeDef.id()) {
                return attr;
            }
        }
        return null;
    }

    /**
     * see {@link #getIdAttribute(Object)}
     * 
     * @param bean instance
     * @return true, if bean is persistable and id attribute is null
     */
    public static final boolean isTransient(Object bean) {
        final BeanAttribute idAttribute = getIdAttribute(bean);
        if (idAttribute != null && idAttribute.getValue(bean) != null) {
            return false;
        } else {
            return true;
        }
    }

    //TODO: implement getValueBetween using new action
//    public static final <A, T> Collection<A> getValueBetween(String attributeName, Class<A>attributeType, T first, T second) {
//        ServiceUtil.
//        BeanContainer.instance().getBeansBetween(first, second, -1);
//        Collection<T> result;
//        
//    }
    public static void reset() {
        self = null;
    }
}
