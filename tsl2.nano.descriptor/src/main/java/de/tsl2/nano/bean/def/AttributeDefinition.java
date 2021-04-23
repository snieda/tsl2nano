/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 13, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.Format;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.action.IStatus;
import de.tsl2.nano.annotation.extension.AnnotationFactory;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.bean.BeanUtil;
import de.tsl2.nano.bean.IAttributeDef;
import de.tsl2.nano.bean.IConnector;
import de.tsl2.nano.bean.IRuleCover;
import de.tsl2.nano.bean.ValueHolder;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.bean.annotation.RuleCover;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.IValueAccess;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.cls.UnboundAccessor;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.core.secure.ISecure;
import de.tsl2.nano.core.util.ByteUtil;
import de.tsl2.nano.core.util.CLI;
import de.tsl2.nano.core.util.DelegationHandler;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.RegExpFormat;

/**
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Default(value = DefaultType.FIELD, required = false)
public class AttributeDefinition<T> implements IAttributeDefinition<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1403875731423120506L;

    @Element(name = "declaring")
    protected IAttribute<T> attribute;
    protected EventController eventController;
    @Element(type = Constraint.class, required = false)
    protected IConstraint<T> constraint;
    @Attribute(required = false)
    private boolean id;
    @Attribute(required = false)
    private boolean unique;
    @Element(required = false)
    private Class<? extends Date> temporalType;
    @Element(required = false)
    protected String description;
    protected transient IStatus status;
    @Element(type = Presentable.class, required = false)
    private IPresentable presentable;
    @Element(type = ValueColumn.class, required = false)
    private IPresentableColumn columnDefinition;
    @Attribute(required = false)
    private boolean doValidation = true;
    /** see {@link #composition()} */
    @Attribute(required = false)
    private boolean composition;
    /** see {@link #cascading()} */
    @Attribute(required = false)
    private boolean cascading;
    /** see {@link #generatedValue()} */
    @Attribute(required = false)
    private boolean generatedValue;
    @Attribute(required = false)
    private boolean isTransient;

    /** optional encryption */
    @Element(required = false)
    private ISecure secure;

    /**
     * optional plugins.
     */
    @ElementList(inline = true, entry = "plugin", required = false)
    protected Collection<IConnector> plugins;

    private static final Log LOG = LogFactory.getLog(AttributeDefinition.class);

    /** internal definition to create a temporarily new beanvalue instance. */
    static final Method UNDEFINEDMETHOD = UNDEFINEDMETHOD();

    /**
     * constructor to be serializable
     * 
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    protected AttributeDefinition() {
        super();
        status = Status.STATUS_OK;
    }

    /**
     * constructor
     * 
     * @param attribute
     */
    public AttributeDefinition(IAttribute<T> attribute) {
        super();
        this.attribute = attribute;
        if (attribute.getAccessMethod() != null) {
            defineDefaults();
        }
    }

    public AttributeDefinition(String name, IConstraint<T> constraint) {
        this(new VAttribute<T>(name));
        this.constraint = constraint;
    }

    protected AttributeDefinition(Method readAccessMethod) {
        super();
        attribute = BeanAttribute.getBeanAttribute(readAccessMethod);
        defineDefaults();
    }

    /**
     * default readaccessmethod if default constructor was invoked
     */
    private static final Method UNDEFINEDMETHOD() {
        try {
            //get yourself
            return AttributeDefinition.class.getDeclaredMethod("UNDEFINEDMETHOD", new Class[0]);
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * sets default properties through the {@link BeanContainer}, if available. These defaults may be read from
     * jpa-annotations.
     */
    protected void defineDefaults() {
        defineFromAnnotations();
        if (BeanContainer.isInitialized() && BeanContainer.instance().isPersistable(getDeclaringClass())) {
            IAttributeDef def = BeanContainer.instance().getAttributeDef(getDeclaringClass(), getName());
            if (def != null) {
                LOG.debug("setting defaults from annotations for attribute: " + getName());
                setId(def.id());
                setUnique(def.unique());
                if (constraint == null || def.length() != -1 || !def.nullable())
                	setBasicDef(def.length(), def.nullable(), null, null, null);
                if (constraint == null || def.scale() != -1 || def.precision() != -1)
                	getConstraint().setNumberDef(def.scale(), def.precision());
                temporalType = def.temporalType();
                composition = def.composition();
                cascading = def.cascading();
                generatedValue = def.generatedValue();
            }
        }
        if (status == null)
            initDeserialization();
    }

    private void defineFromAnnotations() {
        Method m = getAccessMethod();
        if (m != null) {
            if (constraint == null && m.isAnnotationPresent(de.tsl2.nano.bean.annotation.Constraint.class)) {
                de.tsl2.nano.bean.annotation.Constraint c =
                    m.getAnnotation(de.tsl2.nano.bean.annotation.Constraint.class);
                Class<?> type = !c.equals(Object.class) && !Proxy.isProxyClass(c.getClass()) ? c.type() : getType();
                constraint = new Constraint(type, (Object[])ConstraintValueSet.preDefined(c.allowed()));
                RegExpFormat format = !Util.isEmpty(c.pattern()) ? new RegExpFormat(c.pattern(), ENV.get("field.default.length", 64)) : null;
				constraint.setBasicDef(c.length(), c.nullable(), format, (T)IConstraint.fromString(type, c.defaultValue()));               	
                constraint.setRange((Comparable<T>)IConstraint.fromString(type, c.min()), (Comparable<T>)IConstraint.fromString(type, c.max()));
            }
            if (presentable == null && m.isAnnotationPresent(de.tsl2.nano.bean.annotation.Presentable.class)) {
                de.tsl2.nano.bean.annotation.Presentable p =
                    m.getAnnotation(de.tsl2.nano.bean.annotation.Presentable.class);
                presentable = Presentable.createPresentable(p, this);
            }
            if (columnDefinition == null && m.isAnnotationPresent(de.tsl2.nano.bean.annotation.Column.class)) {
                de.tsl2.nano.bean.annotation.Column c =
                    m.getAnnotation(de.tsl2.nano.bean.annotation.Column.class);
                columnDefinition = new ValueColumn<T>(this);
                ValueColumn<T> vc = (ValueColumn<T>) columnDefinition;
                vc.name = c.name();
                vc.format = new RegExpFormat(c.pattern(), 255);
                vc.columnIndex = c.index();
                vc.width = c.width();
                vc.sortIndex = c.sortIndex();
                vc.isSortUpDirection = c.sortUp();
//                vc.minsearch = c.min();
//                vc.maxsearch = c.max();
            }
            if (presentable != null && m.isAnnotationPresent(RuleCover.class)) {
            	RuleCover c = m.getAnnotation(RuleCover.class);
            	AttributeCover.cover(c.implementationClass(), getDeclaringClass(), getName(), c.child(), c.rule());
            }
            AnnotationFactory.with(this, m);
//                getPrsenentationHelper().addRuleListener(ATTR_VALUE, RuleScript.PREFIX + calcTime.getName(), 2, ATTR_FROMTIME, ATTR_TOTIME, ATTR_PAUSE);
        }
    }

    @Persist
    private void initSerialization() {
        //disconnect from beandefinition to be serializable
        if (plugins != null) {
            for (IConnector p : plugins) {
                LOG.info("disconnecting plugin " + p + " from " + this);
                p.disconnect(this);
            }
        }
    }

    @Commit
    private void initDeserialization() {
        status = IStatus.STATUS_OK;
        if (getColumnDefinition() != null && getColumnDefinition() instanceof ValueColumn) {
            ((ValueColumn) getColumnDefinition()).attributeDefinition = this;
        }
        //injectAttributeOnChangeListeners() will be called from BeanDefinition
        injectIntoPlugins(this);
        if (hasAttributeCover())
        	AttributeCover.addRuleCover(this);
    }

    private boolean hasAttributeCover() {
    	PrivateAccessor<AttributeDefinition<T>> pa = new PrivateAccessor<>(this);
    	List<String> memberNames = pa.memberNames();
    	for (String m : memberNames) {
    		Object value = pa.member(m);
			if (value != null && Proxy.isProxyClass(value.getClass()))
				return true;
		}
		return false;
	}

    public boolean hasRuleCover() {
    	return AttributeCover.hasRuleCover(this);
    }
	/**
     * injectPlugins
     */
    protected void injectIntoPlugins(IAttributeDefinition<T> attr) {
        //connect optional plugins
        if (plugins != null) {
            for (IConnector p : plugins) {
                LOG.info("connecting plugin " + p + " to " + attr);
                p.connect(attr);
            }
        }
    }

    static <I> void injectIntoRuleCover(IAttributeDefinition attribute, Object instance) {
    	injectIntoRuleCover(new PrivateAccessor<>(attribute), instance);
    }

    /**
     * inject the given attribute as context - walks recursive to the member tree of acc
     * 
     * @param acc direct/indirect member of attr in member tree
     * @param instance to be set as context object in all RuleCover Proxies.
     */
    protected static <I> void injectIntoRuleCover(UnboundAccessor acc, Object instance) {
    	LOG.info("doing rule-cover injection on " + acc + " with instance " + instance);
    	injectIntoRuleCover(acc, instance, 0);
    }
    protected static <I> void injectIntoRuleCover(UnboundAccessor acc, Object instance, int level) {
//    	if (!IRuleCover.hasRuleCover(acc.instance())) {
//    		LOG.debug("no existing rule-covers for connection-end of type: " + instance.getClass());
//    		return;
//    	}
    		
    	if (level >= ENV.get("beancollector.rulecover.max.recursion", 4)) {
    		LOG.warn("maximum recursion of " + level + " reached. finishing rule-cover tree on " + acc.toString());
    		return;
    	}
        //connect optional rule-covers (use accessor instead of BeanDefinition to avoid stackoverflow
        Map members = acc.members();
        InvocationHandler handler;
        Object item;
        if (LOG.isDebugEnabled())
            LOG.debug("walking through " + members.size() + " members of instance " + Util.toObjString(acc.instance()) 
                    + " to inject " + Util.toObjString(instance) + " into existing rule-covers ");
        for (Object k : members.keySet()) {
            item = members.get(k);
            if (item != null) {
                //first inject the child tree - be careful, don't produce a stackoverflow
                if (item != instance && Util.isFrameworkClass(item.getClass()) && !item.getClass().isAnonymousClass()
                    && !(item instanceof IAttribute) && !(item instanceof BeanDefinition))
                    injectIntoRuleCover(new PrivateAccessor(item), instance, level+1);
                //now the own direct members
                if (Proxy.isProxyClass(item.getClass())) {
                    handler = Proxy.getInvocationHandler(item);
                    //create proxy for each bean instance
                    if (handler instanceof DelegationHandler && handler instanceof IRuleCover) {
                        // compare instances: if attr is a delegation-handler we must ignore its delegate!
                        if (item == instance) {
                            LOG.warn/*throw new IllegalStateException*/("the given instance " + instance
                                + " seems to be a rulecover itself!");
                            continue;
                        }
                        DelegationHandler<I> cover = ((DelegationHandler<I>) handler).clone();
                        item = DelegationHandler.createProxy(cover);
                        LOG.info("injecting (level:" + level + ") context " + " on cover " + cover + " into " + CLI.tag(acc + "->" + k, CLI.Color.GREEN));
                        acc.set((String) k, item);
                        ((IRuleCover)cover).setContext((Serializable)instance);
//                        new UnboundAccessor(handler).call("setContext", null, new Class[] { Serializable.class },
//                            instance);
                    }
                }
            }
        }
    }

    /**
     * uses the information of {@link #attributeID} to inject the real {@link AttributeDefinition} into registered
     * change listeners
     * 
     * @throws CloneNotSupportedException
     */
    void injectAttributeOnChangeListeners(BeanDefinition beandef) {
        //provide dependency listeners their attribute-definition
        if (hasListeners()) {
            Collection<IListener> listener = changeHandler().getListeners(null);
            boolean isBean = beandef instanceof Bean;
            for (IListener l : listener) {
                if (l instanceof AbstractDependencyListener) {
                    AbstractDependencyListener<?, ?> dl = (AbstractDependencyListener<?, ?>) l;
                    if (isBean) {//create a specific listener instance for the given bean!
                    	LOG.debug(beandef.getId() + ": re-assigning dependency-listener-clone " + dl);
                        dl = (AbstractDependencyListener<?, ?>) dl.clone();
                        Class eventType = changeHandler().getEventType(l);
                        changeHandler().removeListener(l);
                        changeHandler().addListener(dl, eventType);
                    }
                    if (dl.attributeID != null) {
                        String name = dl.propertyName; //StringUtil.substring(dl.attributeID, ".", null);
                        LOG.debug(beandef.getId() + ": resetting value of attribute " + name);
                        dl.setAttribute((AttributeDefinition) beandef.getAttribute(name));
                    }
                }
            }
        }
    }

    @Override
    public final IConstraint<T> getConstraint() {
        if (constraint == null) {
            constraint = new Constraint(BeanClass.getDefiningClass(attribute.getType()));
        }
        return constraint;
    }

    /**
     * setBasicDef
     * 
     * @param length {@link #length()}
     * @param nullable {@link #nullable()}
     * @param format {@link #getPattern()}
     * @param defaultValue {@link #getDefault()}
     * @param description {@link #getDescription()}
     */
    @Override
    public IAttributeDefinition<T> setBasicDef(int length,
            boolean nullable,
            Format format,
            T defaultValue,
            String description) {
        try {
            getConstraint().setBasicDef(length, nullable, format, defaultValue);
        } catch (Exception e) {
            if (doValidation) {
                ManagedException.forward(e);
            }
        }
        this.description = description;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue(Object beanInstance) {
        try {
            //using the default may result in problems on checking the value (e.g. in value-expression or isValue())
            T result = /*beanInstance == null && isVirtualAccess() ? getDefault() : */attribute.getValue(beanInstance);
            if (result != null && secure != null)
                result = (T) secure.decrypt((String) result);
            return result;
        } catch (Exception ex) {
            LOG.error("error evaluating value for attribute '" + getName() + "'", ex);
            status = new Status(ex);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setId(boolean isId) {
        this.id = isId;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setUnique(boolean isUnique) {
        this.unique = isUnique;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return getConstraint().getLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int scale() {
        return getConstraint().getScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int precision() {
        return getConstraint().getPrecision();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nullable() {
        return getConstraint().isNullable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean id() {
        return id;
    }

    @Override
    public boolean unique() {
        return unique;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Date> temporalType() {
        return temporalType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Format getFormat() {
        if (getConstraint().getFormat() == null) {
            Class<T> type = getType();
            if (Collection.class.isAssignableFrom(type)) {
                getConstraint().setFormat(new CollectionExpressionTypeFormat<T>(getGenericType(0)));
            } else if (Map.class.isAssignableFrom(type)) {
                getConstraint().setFormat(new MapExpressionFormat<T>(getGenericType(1)));
            } else if (type.isArray()) {
            	if (type.getComponentType().isPrimitive())
                    getConstraint().setFormat(new PrimitiveArrayExpressionFormat<T>(getGenericType(1)));
            	else
            		getConstraint().setFormat(new ArrayExpressionFormat<T>(getGenericType(1)));
            } else if (type.isEnum()) {
                getConstraint().setFormat(FormatUtil.getDefaultFormat(type, true));
            } else if (BeanUtil.isStandardType(type) && !ByteUtil.isByteStream(type)) {
                getConstraint().setFormat(ENV.get(BeanPresentationHelper.class).getDefaultRegExpFormat(this));
            } else {
                getConstraint().setFormat(new ValueExpressionTypeFormat<T>(type));
            }
        }
        return getConstraint().getFormat();
    }

    @Override
    public ValueExpression<T> getValueExpression() {
        Format f = getFormat();
        return f instanceof ValueExpressionFormat ? ((ValueExpressionFormat) f).getValueExpression() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        return temporalType() != null ? (Class<T>) temporalType() : attribute.getType();
    }

    @Override
    public Method getAccessMethod() {
        return attribute.getAccessMethod();
    }

    protected Class<T> getGenericType(int pos) {
        return (Class<T>) (getAccessMethod() != null ? BeanAttribute.getGenericType(getAccessMethod(), pos)
            : Object.class);
    }

    /**
     * isMultiValue
     * 
     * @return true, if the value type is a collection
     */
    @Override
    public boolean isMultiValue() {
        Class<?> type = getType();
        return Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type) || type.isArray();
    }

    /**
     * see {@link BeanClass#getActions()} and {@link BeanDefinition#isSelectable()}
     * 
     * @param beanValue attribute to evaluate
     * @return true, if bean type is selectable
     */
    public boolean isSelectable() {
        Class<T> type = getType();
        return isMultiValue()
            || (!BeanUtil.isStandardType(type) && BeanDefinition.getBeanDefinition(type).isSelectable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus isValid(T value) {
        return getConstraint().checkStatus(getId(), value);
    }

    /**
     * getParsedValue. throws a runtime exception on parsing error.
     * 
     * @param source text to parse
     * @return parsed value
     */
    public T getParsedValue(String source) {
        T value = null;
        if (getFormat() != null) {
            try {
                if (Util.isEmpty(source) && nullable())
                    return null;
                //the parser will decide, how to handle empty/null values
                value = (T) getFormat().parseObject(source);
            } catch (ParseException e) {
                ManagedException.forward(e);
            }
        } else if (String.class.isAssignableFrom(getType())) {
            value = (T) source;
        } else {
            throw new ManagedException("no format/parser available for field " + getName());
        }
        return value;
    }

    /**
     * hasStatusError
     * 
     * @return true, if has status.error != null
     */
    public boolean hasStatusError() {
        return status != null && status.error() != null;
    }

    /**
     * {@inheritDoc}
     */
    public void check() {
        if (hasStatusError()) {
            throw new RuntimeException(status.error());
        }
    }

    /**
     * {@inheritDoc}
     */
    public T getDefault() {
        IConstraint<T> c = getConstraint();
        if (c.getDefault() == null && getAccessMethod() != null && !c.isNullable()
            && !BeanPresentationHelper.isGeneratedValue(this)) {
            Object genType = getAccessMethod().getGenericReturnType();
            if (genType instanceof Class) {
                Class<T> gtype = temporalType() != null ? (Class<T>) temporalType() : (Class<T>) genType;
                T value = ObjectUtil.createDefaultInstance(gtype);
                    if (value != null)
                    	getConstraint().setDefault(value);
            }
            if (c.getDefault() == null) {
                if (NumberUtil.isNumber(getType())) {
                    getConstraint().setDefault((T) NumberUtil.getDefaultInstance((Class<Number>) getType()));
                } else if (ENV.isTestMode()) {
                    /*
                     * to create new entities without user input, these fields are filled on test mode
                     */
                    if (CharSequence.class.isAssignableFrom(getType())) {
                        c.setDefault((T) ("Y" + UUID.randomUUID().toString().substring(0, c.getLength() - 1)));
                    } else if (BeanContainer.instance().isPersistable(getType())) {
                        Collection<T> beans = BeanContainer.instance().getBeans(getType(), 0, 1);
                        if (isMultiValue()) {
                            c.setDefault((T) beans);
                        } else if (beans.size() > 0) {
                            c.setDefault(beans.iterator().next());
                        }
                    }
                }
            }
        }
        return getConstraint().getDefault();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        if (description == null && getName() != null) {
            if (getPresentation() != null) {
                description = getPresentation().getDescription();
            } else {
                description = StringUtil.toFirstUpper(getName());
            }
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setNumberDef(int scale, int precision) {
        getConstraint().setNumberDef(scale, precision);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setRange(Comparable<T> min, Comparable<T> max) {
        getConstraint().setRange(min, max);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setRange(Collection<T> allowedValues) {
        getConstraint().setRange(allowedValues);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setFormat(Format format) {
        getConstraint().setFormat(format);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setLength(int length) {
        getConstraint().setLength(length);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setScale(int scale) {
        getConstraint().setScale(scale);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setPrecision(int precision) {
        getConstraint().setPrecision(precision);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAttributeDefinition<T> setNullable(boolean nullable) {
        getConstraint().setNullable(nullable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus getStatus() {
        return status;
    }

    public void setStatus(IStatus status) {
        this.status = status;
    }
    
    /** see IVirtualDefinition#isRelation() */
    @Override
    public boolean isRelation() {
        return BeanContainer.isConnected()
            && BeanContainer.instance().isPersistable(getType());
    }

    /**
     * if the bean instance is of type {@link ValueHolder}, the attribute is virtual - no special bean-attribute is
     * available, the attribute name is always {@link ValueHolder#getValue()} .
     * 
     * @return true, if the declaring class is of type {@link IValueAccess}.
     */
    @Override
    public boolean isVirtual() {
        return attribute.isVirtual();
    }

    protected boolean isVirtualAccess() {
        return isVirtual() && getAccessMethod() != null;
    }

    /**
     * getPresentation
     * 
     * @return
     */
    @Override
    public IPresentable getPresentation() {
        //TODO: create presentation helper through MultipleInheritanceProxy
        if (presentable == null) {
            presentable = ENV.get(BeanPresentationHelper.class).createPresentable(this);
        }
        return presentable;
    }

    /**
     * setPresentation
     * 
     * @param label
     * @param type
     * @param style
     * @param enabler
     * @param visible
     * @param layout
     * @param layoutConstraints
     * @param description
     * @return
     */
    public IPresentable setPresentation(final String label,
            final int type,
            final int style,
            final IActivable enabler,
            final boolean visible,
            final Map<String, Object> layout,
            final Map<String, Object> layoutConstraints,
            final String description) {
        presentable = ENV.get(BeanPresentationHelper.class).createPresentable(this);
        presentable.setPresentation(label, type, style, enabler, visible, (Serializable) layout,
            (Serializable) layoutConstraints, description);
        return presentable;
    }

    /**
     * setPresentation
     * 
     * @param presentable
     */
    public void setPresentation(IPresentable presentable) {
        this.presentable = presentable;
    }

    /**
     * perhaps that definition was build yet. use BeanCollector.getColumnDefinition() to get the column - on first time
     * they will be created.
     * 
     * @return Returns the columnDefinition.
     */
    @Override
    public IPresentableColumn getColumnDefinition() {
        return columnDefinition;
    }

    /**
     * @param columnDefinition The columnDefinition to set.
     */
    public void setColumnDefinition(IPresentableColumn columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    /**
     * setColumnDefinition
     * 
     * @param index
     * @param sortIndex
     * @param sortUpDirection
     * @param width
     */
    @Override
    public void setColumnDefinition(int index, int sortIndex, boolean sortUpDirection, int width) {
        this.columnDefinition = new ValueColumn(this, index, sortIndex, sortUpDirection, width);
        this.columnDefinition.setPresentable(getPresentation());
    }

    /**
     * @return Returns the doValidation.
     */
    public boolean isDoValidation() {
        return doValidation;
    }

    /**
     * @param doValidation The doValidation to set.
     */
    public void setDoValidation(boolean doValidation) {
        this.doValidation = doValidation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean composition() {
        return composition;
    }

    @Override
    public boolean cascading() {
        return cascading;
    }

    @Override
    public boolean generatedValue() {
        return generatedValue;
    }

    @Override
	public boolean isTransient() {
		return isTransient;
	}

	@Override
    public void setAsRelation(String relationChain) {
        new PrivateAccessor<AttributeDefinition<T>>(this).set("name", relationChain);
    }

    /**
     * @return Returns the plugins.
     */
    @Override
    public Collection<IConnector> getPlugins() {
        return plugins;
    }

    /**
     * @param plugin The plugin to add.
     */
    @Override
    public void addPlugin(IConnector plugin) {
        if (plugins == null) {
            plugins = new LinkedList<IConnector>();
        }
        LOG.info("connecting plugin " + plugin + " to " + this);
        plugin.connect(this);
        plugins.add(plugin);
    }

    /**
     * removePlugin
     * 
     * @param plugin to remove
     * @return true, if plugin was removed
     */
    @Override
    public boolean removePlugin(IConnector plugin) {
        if (plugins == null) {
            LOG.warn("plugin " + plugin + " can't be removed. no plugins available yet!");
            return false;
        }
        LOG.info("disconnecting plugin " + plugin + " from " + this);
        plugin.disconnect(this);
        return plugins.remove(plugin);
    }

///////////////////////////////////////////////////////////////////////////////
// Delegators to IAttribute
///////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public Class getDeclaringClass() {
        return attribute.getDeclaringClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return attribute.getName();
    }

    @Override
    public void setName(String name) {
        if (!isVirtual()) {
            throw new IllegalStateException("name cannot be changed on non-virtual (=fixed) attributes!");
        }
        attribute.setName(name);
    }

    public String getPath() {
        return getDeclaringClass().getName() + "." + getName();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Object instance, T value) {
        if (secure != null)
            value = (T) secure.encrypt((String) value);
        if (value instanceof String) {
        	if (!isMultiValue()) {
				T fromVE = BeanDefinition.getBeanDefinition(getType()).getValueExpression().from((String)value);
				if (fromVE != null && getType().isAssignableFrom(fromVE.getClass()))
					value = fromVE;
	    	} else {
	    		final String str = (String) value;
	    		if (!Util.isEmpty(StringUtil.trim(str, "[]{}()\t\n ")))
	    			value = (T) Util.trY(() -> getFormat().parseObject(str));
	    	}
		}
        attribute.setValue(instance, value);
    }

    /**
     * @return Returns the secure.
     */
    public ISecure getSecure() {
        return secure;
    }

    /**
     * @param secure The secure to set.
     */
    public void setSecure(ISecure secure) {
        if (!String.class.isAssignableFrom(getType()))
            throw new IllegalStateException("encrypted fields must be of type String.class");
        this.secure = secure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return attribute.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasWriteAccess() {
        return attribute.hasWriteAccess();
    }

    /**
     * looks for registered change handlers without creating an {@link EventController} instance.
     * 
     * @return
     */
    public boolean hasListeners() {
        return eventController != null && eventController.hasListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventController changeHandler() {
        if (eventController == null) {
            eventController = new EventController();
        }
        return eventController;
    }

    IAttribute internalAttribute() {
    	return attribute;
    }
    
    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return attribute != null ? attribute.hashCode() : super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(IAttribute<T> o) {
        return attribute.compareTo(o);
    }

    @Override
	public IAttribute<?> getAttribute(String name) {
		return BeanDefinition.getBeanDefinition(getType()).getAttribute(name);
	}

    @Override
    public String toString() {
        return attribute != null ? attribute.toString() : super.toString();
    }

    public String toDebugString() {
        return Util.toString(getClass(), "declaringClass: " + getType(), "temporal-type: " + temporalType, "name: "
            + getName(),
            "id: " + id, "unique: " + unique, "cascading: " + cascading, "composition: " + composition, "\nattribute: "
                + attribute,
            "\nstatus: "
                + status,
            "\nconstraints: "
                + constraint,
            "\npresentable: " + presentable);
    }
}
