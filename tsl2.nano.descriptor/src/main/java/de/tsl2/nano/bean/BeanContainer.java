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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.NumberUtil;
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
    /** we provide a thread invariant and a thread variant singelton instance - this is not secure but practicable. */
    private static BeanContainer self = null;
    private static ThreadLocal<BeanContainer> selfThread = new ThreadLocal<BeanContainer>();

    private static final Log LOG = LogFactory.getLog(BeanContainer.class);

    /** on initializing with {@link #initEmtpyServiceActions()} it is true */
    private boolean emptyServices = false;

    protected IAction<Collection<?>> idFinderAction = null;
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
        assert self() != null : "beancontainer not initialized! call initServiceActions(...) before!";
        return self();
    }
    
    private static BeanContainer self() {
        return selfThread.get() != null ? selfThread.get() : self;
    }

    /**
     * isInitialized
     * 
     * @return true, if {@link #initEmtpyServiceActions()} or
     *         {@link #initServiceActions(IAction, IAction, IAction, IAction, IAction, IAction, IAction, IAction, IAction, IAction)}
     *         was called before.
     */
    public static final boolean isInitialized() {
        return self() != null;
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
    public static final void initServiceActions(IAction<Collection<?>> idFinder,
            IAction<Collection<?>> relationFinder,
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
        selfThread.set(self);
        
        self.idFinderAction = idFinder;
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
    public static final Collection initEmtpyServiceActions(Object...testInstances) {
        final Collection<?> EMPTY_LIST = new ListSet(testInstances);
        final IAction idFinder = new CommonAction("empty.service.idFinder") {
            @Override
            public Object action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return EMPTY_LIST.size() > 0 ? EMPTY_LIST.iterator().next() : null;
            }
        };
        final IAction<Collection<?>> relationFinder = new CommonAction<Collection<?>>("empty.service.relationFinder") {
            @Override
            public Collection<?> action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> exampleFinder = new CommonAction<Collection<?>>("empty.service.exampleFinder") {
            @Override
            public Collection<?> action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> betweenFinder = new CommonAction<Collection<?>>("empty.service.betweenFinder") {
            @Override
            public Collection<?> action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> queryFinder = new CommonAction<Collection<?>>("empty.service.queryFinder") {
            @Override
            public Collection<?> action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return EMPTY_LIST;
            }
        };
        final IAction<Collection<?>> queryMapFinder = new CommonAction<Collection<?>>("empty.service.queryMapFinder") {
            @Override
            public Collection<?> action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return EMPTY_LIST;
            }
        };
        final IAction lazyRelationResolver = new CommonAction("empty.service.lazyRelationResolver") {
            @Override
            public Object action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return getParameter()[0];
            }
        };
        final IAction saveAction = new CommonAction("empty.service.saveAction") {
            @Override
            public Object action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                //do nothing, return the instance itself
                return getParameter()[0];
            }
        };
        final IAction deleteAction = new CommonAction("empty.service.deleteAction") {
            @Override
            public Object action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return null;
            }
        };
        final IAction<IAttributeDef> attrAction = new CommonAction<IAttributeDef>("empty.service.attrAction") {
            @Override
            public IAttributeDef action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return null;
            }
        };
        final IAction permissionAction = new CommonAction("empty.service.permissionAction") {
            @Override
            public Object action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return true;
            }
        };
        final IAction persistableAction = new CommonAction("empty.service.persistableAction") {
            @Override
            public Object action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return Serializable.class.isAssignableFrom((Class<?>) getParameter()[0]);
            }
        };
        final IAction<Integer> executeAction = new CommonAction<Integer>("empty.service.executeAction") {
            @Override
            public Integer action() {
            	LOG.debug("call to '" + this.getId() + "' (parameter: " + StringUtil.toString(getParameter(), 80) + "): on empty service actions -> doing nothing!");
                return null;
            }
        };
        BeanContainer.initServiceActions(idFinder,
            relationFinder,
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
        self.emptyServices = true;
		return EMPTY_LIST;
    }

    /**
     * @return see {@link #emptyServices}
     */
    public static boolean isConnected() {
        return isInitialized() && !self().emptyServices;
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
            initDefaults(bean);
            return bean;
        } catch (final Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * initializes collections not to be null.
     * @param bean new created bean.
     */
    public static <T> void initDefaults(T bean) {
        //to fulfill bindings on onetomany, we bind empty lists
        final BeanClass clazz = BeanClass.getBeanClass(bean.getClass());
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
    public <T> T getByID(Class<T> type, Object id) {
        idFinderAction.setParameter(new Object[] { type, id });
        return (T) idFinderAction.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Collection<T> getBeans(Class<T> type, int startIndex, int maxResult) {
        typeFinderAction.setParameter(new Object[] { type, startIndex, maxResult });
        return (Collection<T>) typeFinderAction.activate();
    }

    @Override
    public <T> Collection<T> getBeans(BeanFindParameters<T> parameters) {
        typeFinderAction.setParameter(new Object[] { parameters });
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

    @Override
    public <T> Collection<T> getBeansByExample(T exampleBean, Boolean useLike, BeanFindParameters<T> parameters) {
        exampleFinderAction.setParameter(new Object[] { exampleBean, useLike, parameters });
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

    @Override
    public <T> Collection<T> getBeansBetween(T firstBean, T secondBean, BeanFindParameters<T> parameters) {
        betweenFinderAction.setParameter(new Object[] { firstBean, secondBean, parameters });
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
        Collection<Object> beanCount = instance()
            .getBeansByQuery("select count(*) from " + beanType.getSimpleName(), true, (Object[]) null);
		return beanCount.size() > 0 ? ((Number) beanCount.iterator().next()).longValue() : 0 /*may be on empty tests*/;
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
     * @return true, if bean is persistable but id attribute is null
     */
    public boolean isTransient(Object bean) {
        final BeanAttribute idAttribute = getIdAttribute(bean);
        Object id;
        if (idAttribute != null && (id = idAttribute.getValue(bean)) != null) {
            Class type = idAttribute.getDeclaringClass();
            return !isPersistable(type) || getByID(type, id) == null;
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
        selfThread.remove(); //how to remove all other thread-values?
    }

    /**
     * reloads all referenced entities (having only an id)
     * 
     * @param obj transient (example) entity holding attached entities as relations
     * @return the obj itself
     */
    public static Object attachEntities(Object obj) {
        List<IAttributeDefinition<?>> attributes = Bean.getBean(obj).getBeanAttributes();
        BeanValue bv;
        for (IAttributeDefinition<?> a : attributes) {
            if (a.isRelation() && (bv = (BeanValue) a).getValue() != null) {
                Bean b = Bean.getBean(bv.getValue());
                Object n = BeanContainer.instance().getByID(bv.getType(), b.getId());
                bv.setValue(n);
            }
        }
        return obj;
    }

    /**
     * replaces attached entities with copies holding only the id
     * 
     * @param obj transient (example) entity holding attached entities as relations
     * @return the obj itself
     */
    public static Object detachEntities(Object obj) {
        List<IAttributeDefinition<?>> attributes = Bean.getBean(obj).getBeanAttributes();
        BeanValue bv;
        for (IAttributeDefinition<?> a : attributes) {
            if (a.isRelation() && (bv = (BeanValue) a).getValue() != null) {
                Bean b = Bean.getBean(bv.getValue());
                Object n = BeanClass.createInstance(a.getType());
                Bean.getBean(n).getIdAttribute().setValue(n, b.getId());
                bv.setValue(n);
            }
        }
        return obj;
    }

    /**
     * creates and sets a generated value for the id. if jpa annotation @GenerateValue is present, it will overwrite this id.
     * 
     * @param newItem
     */
    public static boolean createId(Object newItem) {
        if (ENV.get("value.id.fill.uuid", true)) {
            final BeanAttribute idAttribute = BeanContainer.getIdAttribute(newItem);
            if (idAttribute != null) {
                Object value = null;
                if (String.class.isAssignableFrom(idAttribute.getType())) {
                    IAttributeDef def = ENV.get(IBeanContainer.class).getAttributeDef(newItem,
                        idAttribute.getName());
                    //TODO: through string cut, the uuid may not be unique
                    value =
                        StringUtil.fixString(BeanUtil.createUUID(), (def.length() > -1 ? def.length() : 0), ' ', true);
                } else if (NumberUtil.isNumber(idAttribute.getType())) {
                    //subtract the years from 1970 to 2015 to be castable to an int
                    //TODO: use a more unique value
                    if (ENV.get("value.id.use.timestamp", false)) {
                        value = System.currentTimeMillis();
                        if (NumberUtil.isInteger(idAttribute.getType())) {
                            value = DateUtil.getMillisWithoutYear((Long) value);
                        }
                    } else {
                        value = ENV.counter("collector.new.id.number.counter.start", 1);
                    }
                } else if (ENV.get("container.try.generate.multi.primary.key", true) 
                        && (idAttribute.getType().getPackage() == null || idAttribute.getType().getPackage().equals(newItem.getClass().getPackage()))) {
                    value = createCompositeKey(newItem, idAttribute);
                    if (value == null)
                        LOG.warn("the id-attribute " + idAttribute + " can't be created for "
                                + idAttribute.getType());
                } else {
                    LOG.warn("the id-attribute " + idAttribute + " can't be assigned to a generated value of type "
                        + idAttribute.getType());
                }
                idAttribute.setValue(newItem, value);
                return true;
            }
        }
        LOG.debug("no id will be generated for new item " + newItem);
        return false;
    }

    /**
     * tries to create a composite key
     * @param newItem
     * @param idAttribute
     * @return
     */
    private static Object createCompositeKey(Object newItem, BeanAttribute idAttribute) {
        Object value = BeanClass.createInstance(idAttribute.getType());
        //TODO: implement going through all item attributes, get their idAttributes and try to set them to idValue
        Bean<Object> bean = Bean.getBean(newItem);
        BeanDefinition idAttrs = BeanDefinition.getBeanDefinition(idAttribute.getType());
        IAttribute originAttr;
        Object originValue;
        for (Object idAttrObj : idAttrs.getAttributes()) {
            IAttribute idAttr = (IAttribute) idAttrObj;
            try {
                originAttr = bean.getAttribute(idAttr.getName());
                if (originAttr != null) {
                    originValue = originAttr.getValue(newItem);
                    if (originValue != null)
                        idAttr.setValue(value, getIdAttribute(originValue).getValue(originValue));
                }
            } catch (Exception ex) {
                LOG.error("can't generate composite key for " + idAttribute);
                return null;
            }
        }
        return value;
    }
}
