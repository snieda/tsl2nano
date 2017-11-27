/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 08.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.configuration;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;

import de.tsl2.nano.bean.annotation.Constraint;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.bean.def.IIPresentable;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.IPresentableColumn;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.bean.def.ValueExpressionFormat;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.Html5Presentation;
import de.tsl2.nano.h5.RuleCover;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.actions.ActionPool;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.incubation.specification.rules.RuleScript;
import de.tsl2.nano.core.messaging.IListener;

/**
 * Provides a specific set of Attribute members to configure.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */

@SuppressWarnings({"rawtypes", "unchecked"})
public class AttributeConfigurator implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    AttributeDefinition<?> attr;
    PrivateAccessor<AttributeDefinition<?>> attrAccessor;

    IIPresentable presentable;

    public AttributeConfigurator() {
        this(BeanClass.createInstance(AttributeDefinition.class,
            new ExpressionDescriptor(def().getDeclaringClass())));
    }

    public AttributeConfigurator(String attributeName) {
        this((AttributeDefinition<?>) def().getAttribute(attributeName));
    }

    /**
     * constructor
     * 
     * @param attr
     */
    public AttributeConfigurator(AttributeDefinition<?> attr) {
        this.attr = attr;
        attrAccessor = new PrivateAccessor<AttributeDefinition<?>>(attr);
        if (getDeclaration() instanceof ExpressionDescriptor) {
            ((ExpressionDescriptor)getDeclaration()).setDeclaringClass(def().getDeclaringClass());
            getPresentable().setType(IPresentable.TYPE_DEPEND);
            getPresentable().setStyle(IPresentable.UNDEFINED);
            
        }
    }

    static BeanDefinition def() {
        return ConcurrentUtil.getCurrent(BeanConfigurator.class).def;
    }

    public String getName() {
        return attr.getName();
    }

    public String getDescription() {
        return attr.getDescription();
    }

    public void setDescription(String description) {
        attrAccessor.set("description", description);
    }

    public String getType() {
        return attr.getType().getName();
    }

//    public BigDecimal getLength() {
//        return attr.scale() != 0 ? new BigDecimal(new BigInteger(String.valueOf(attr.length())), attr.scale())
//            : new BigDecimal(attr.length());
//    }
//
//    public void setLength(BigDecimal length) {
//        if (length != null) {
//            //TODO: define scale and prec
//            float fract = length.floatValue() - length.intValue();
//            int prec = length.precision();
//            int scale = length.unscaledValue().intValue() / length.intValue();
//            attrAccessor.set("length", length.intValue());
//            attrAccessor.set("scale", scale);
//            attrAccessor.set("precision", prec);
//        } else {
//            attrAccessor.set("length", IPresentable.UNDEFINED);
//            attrAccessor.set("scale", IPresentable.UNDEFINED);
//            attrAccessor.set("precision", IPresentable.UNDEFINED);
//        }
//    }
//
//    public Serializable getMin() {
//        return attr.getConstraint().getMininum() != null ? attr.getFormat().format(attr.getConstraint().getMininum()) : null;
//    }
//
//    public void setMin(Serializable min) {
//        attrAccessor.set("min", min);
//    }
//
//    public Serializable getMax() {
//        return attr.getConstraint().getMaxinum() != null ? attr.getFormat().format(attr.getConstraint().getMaxinum()) : null;
//    }
//
//    public void setMax(Serializable max) {
//        attrAccessor.set("max", max);
//    }
//
    public String getFormat() {
        Format f = attr.getFormat();
        if (f instanceof SimpleDateFormat)
            return ((SimpleDateFormat) f).toPattern();
        else if (f instanceof NumberFormat)
            return ((DecimalFormat) f).toPattern();
        else if (f instanceof RegExpFormat)
            return ((RegExpFormat) f).getPattern();
        else if (f instanceof ValueExpressionFormat)
            return ((ValueExpressionFormat) f).getPattern();
        else
            return f != null ? f.toString() : "";
    }

    public void setFormat(String format) {
        if (format != null) {
            Format f = attr.getFormat();
            if (f instanceof SimpleDateFormat)
                ((SimpleDateFormat) f).applyPattern(format);
            else if (f instanceof NumberFormat)
                ((DecimalFormat) f).applyPattern(format);
            else if (f instanceof ValueExpressionFormat)
                ((ValueExpressionFormat) f).applyPattern(format);
            else
                ((RegExpFormat) f).setPattern(format, null, attr.getConstraint().getLength(), 0);
        }
    }

//
//    public boolean isNullable() {
//        return attr.getConstraint().isNullable();
//    }
//
    public IConstraint<?> getConstraint() {
        return attr.getConstraint();
    }

    public void setConstraint(IConstraint<?> c) {
        attrAccessor.set("constraint", c);
    }

    public IPresentable getPresentable() {
        return attr.getPresentation();
//        if (presentable == null) {
//            if (IPresentable.class.isAssignableFrom(attr.getDeclaringClass()))
//                attr.getPresentation().setEnabler(IActivable.ACTIVE);
//            presentable = SetterExtenderPoxy.setterExtender(IIPresentable.class, attr.getPresentation());
//        }
//        return presentable;
    }

    public void setPresentable(IPresentable p) {
        attrAccessor.set("presentable", /*SetterExtenderPoxy.instanceOf((ProxyWrapper) */p/*)*/);
    }

    public IPresentableColumn getColumnDefinition() {
        return attr.getColumnDefinition();
    }

    public void setColumnDefinition(IPresentableColumn c) {
        attrAccessor.set("columnDefinition", c);
    }

    public IAttribute<?> getDeclaration() {
        return attrAccessor.member("attribute", IAttribute.class);
    }

    public void setDeclaration(IAttribute<?> a) {
        if (a instanceof ExpressionDescriptor) {
            ExpressionDescriptor ae = (ExpressionDescriptor)a;
            if (ae.getExpression() != null)
                a = ((ExpressionDescriptor)a).toInstance();
        }
        attrAccessor.set("attribute", a);
    }

    public boolean isDoValidation() {
        return attr.isDoValidation();
    }

    public boolean isComposition() {
        return attr.composition();
    }

    public Collection<IListener> getListener() {
        return attr.changeHandler().getListeners(null);
    }

    public void setListener(Collection<IListener> listener) {
        //do nothing - enables , setting listeners
    }

    public Object getDefault() {
        return attr.getDefault();
    }

    public ValueExpression getValueExpression() {
        return attr.getValueExpression();
    }

    public IAttributeDefinition<?> unwrap() {
        return attr;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), attr);
    }

    @de.tsl2.nano.bean.annotation.Action(name = "addListener", argNames = { "observerAttribute",
        "observableAttribute", "ruleName" })
    public void actionAddListener(
            @Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String observer,
            @Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String observable,
            @Constraint(allowed=ConstraintValueSet.ALLOWED_ENVFILES + ".*specification/rule.*") String rule) {
        BeanDefinition def = def();
        Html5Presentation helper = (Html5Presentation) def.getPresentationHelper();
        observer = StringUtil.substring(observer, ".", null, true);
        observable = StringUtil.substring(observable, ".", null, true);
        rule = StringUtil.substring(FileUtil.replaceWindowsSeparator(rule), "/", ".", true);
        helper.addRuleListener(observer, rule, 2, observable);
    }

//    public void actionRemoveListener(String child, String rule) {
//        RuleCover.removeCover(attr.getDeclaringClass(), attr.getName(), child);
//    }
//
    @de.tsl2.nano.bean.annotation.Action(name = "addRuleCover", argNames = { "propertyOfAttribute", "ruleName" })
    public void actionAddRuleCover(
            @Constraint(defaultValue = "presentable.layoutConstraints", pattern = "(\\w+[\\.]?)+", allowed = {
                "presentable", "presentable.layout", "columnDefinition" }) String child,
            @Constraint(allowed=ConstraintValueSet.ALLOWED_ENVFILES + ".*specification/rule.*") String rule) {
        rule = StringUtil.substring(FileUtil.replaceWindowsSeparator(rule), "/", ".", true);
        RuleCover.cover(attr.getDeclaringClass(), attr.getName(), child, rule);
    }

    public void actionRemoveRuleCover(String child) {
        RuleCover.removeCover(attr.getDeclaringClass(), attr.getName(), child);
    }

    @de.tsl2.nano.bean.annotation.Action(name = "createRuleOrAction", argNames = { "newRuleName", "actionType",
        "actionExpression" })
    public void actionCreateRuleOrAction(String name,
            @de.tsl2.nano.bean.annotation.Constraint(defaultValue = "%: RuleScript (--> JavaScript)", allowed = {
                "§: Rule (--> Operation)", "%: RuleScript (--> JavaScript)", "!: Action (--> Java)" }) String type,
            @de.tsl2.nano.bean.annotation.Constraint(pattern = ".*") String expression) {

        if (type.startsWith("%"))
            ENV.get(RulePool.class).add(new RuleScript<>(name, expression, null));
        else if (type.startsWith("§"))
            ENV.get(RulePool.class).add(new Rule(name, expression, null));
        else
            ENV.get(ActionPool.class).add(new Action(name, expression));
    }

}
