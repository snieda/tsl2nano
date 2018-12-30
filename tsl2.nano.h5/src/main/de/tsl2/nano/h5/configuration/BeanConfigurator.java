/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 07.01.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.configuration;

import static de.tsl2.nano.h5.HtmlUtil.ATTR_BGCOLOR;
import static de.tsl2.nano.h5.HtmlUtil.COLOR_LIGHT_GRAY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.bean.annotation.ConstraintValueSet;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.Presentable;
import de.tsl2.nano.bean.def.ValueColumn;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.bean.def.ValueExpressionFormat;
import de.tsl2.nano.bean.def.ValueGroup;
import de.tsl2.nano.collection.Entry;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.cls.PrivateAccessor;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.Html5Presentable;
import de.tsl2.nano.h5.SpecifiedAction;
import de.tsl2.nano.h5.collector.CSheet;
import de.tsl2.nano.h5.collector.Compositor;
import de.tsl2.nano.h5.collector.Controller;
import de.tsl2.nano.h5.collector.Increaser;
import de.tsl2.nano.h5.expression.Query;
import de.tsl2.nano.h5.expression.QueryPool;
import de.tsl2.nano.h5.expression.WebClient;
import de.tsl2.nano.h5.expression.WebPool;
import de.tsl2.nano.incubation.specification.actions.Action;
import de.tsl2.nano.incubation.specification.actions.ActionPool;
import de.tsl2.nano.incubation.specification.rules.Rule;
import de.tsl2.nano.incubation.specification.rules.RulePool;
import de.tsl2.nano.incubation.specification.rules.RuleScript;

/**
 * wrapper class to handle presentation of a bean-definition. at a time, only one BeanConfigurator is active. this
 * instance will be registered to the environment to be usable as something like a singleton from outside.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BeanConfigurator<T> implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    BeanDefinition<T> def;
    private PrivateAccessor<BeanDefinition<?>> defAccessor;
    private transient List<AttributeConfigurator> attrConfigurators;

    /**
     * factory method to create a bean configurator for the given instance type.
     * 
     * @param instance to evaluate the type and {@link BeanDefinition} for.
     * @return new bean configurator instance
     */
    public static <I extends Serializable> Bean<BeanConfigurator<I>> create(Class<I> type) {
        boolean autopersistEnv = ENV.isAutopersist();
        BeanConfigurator<?> configurer = ConcurrentUtil.getCurrent(BeanConfigurator.class);
        if (configurer == null || !configurer.def.getDeclaringClass().equals(type)) {
            try {
                ENV.setAutopersist(false);
                //wrap the bean-def into a bean-configurator and pack it into an own bean
                configurer = new BeanConfigurator(BeanDefinition.getBeanDefinition(type));
                //register it to be used by creating new AttributeConfigurators 
                ConcurrentUtil.setCurrent(configurer); //avoid stackoverflow
    
                //define the presentation
                Bean<?> configBean = Bean.getBean(configurer);
    
                if (configBean.isDefault()) {
                    Serializable layout = (Serializable) MapUtil.asMap(ATTR_BGCOLOR, COLOR_LIGHT_GRAY);
    
                    BeanDefinition<Html5Presentable> configPres = BeanDefinition.getBeanDefinition(Html5Presentable.class);
                    configPres.setAttributeFilter("label", "description", "icon", "type", "style", "visible", "searchable",
                        "nesting", "width",
                        "height", "layout",
                        "layoutConstraints", "groups");
                    configPres.getPresentable().setLayout(layout);
    //            configPres.saveDefinition();
    
                    BeanDefinition<ValueGroup> configValueGroup = BeanDefinition.getBeanDefinition(ValueGroup.class);
                    configValueGroup.setAttributeFilter("label", "description", "icon", "type", "style", "width", "height",
                        "layout",
                        "layoutConstraints", "attributes");
                    configValueGroup.getPresentable().setLayout(layout);
    //            configValueGroup.saveDefinition();
    
                    BeanDefinition<ValueColumn> configColDef = BeanDefinition.getBeanDefinition(ValueColumn.class);
                    configColDef.setAttributeFilter("name", "description", "index", "sortIndex", "sortUpDirection",
                        "format",
                        "width"
                    /*                    "standardSummary",
                    "presentable",
                    "minSearchValue",
                    "maxSearchValue"*/
                    );
                    configColDef.getPresentable().setLayout(layout);
    //            configColDef.saveDefinition();
    
                    BeanDefinition<Constraint> configConstraint = BeanDefinition.getBeanDefinition(Constraint.class);
                    configConstraint.setAttributeFilter("type", "minimum", "maximum", "format", "length", "scale",
                        "precision",
                        "nullable");
                    configConstraint.getPresentable().setLayout(layout);
    //            configConstraint.saveDefinition();
    
                    defineAction(layout);
    
                    BeanDefinition<Entry> configEntry = BeanDefinition.getBeanDefinition(Entry.class);
                    configEntry.setAttributeFilter("key", "value");
                    configEntry.getPresentable().setLayout(layout);
    //            configEntry.saveDefinition();
    
                    BeanDefinition<AttributeConfigurator> configAttr =
                        BeanDefinition.getBeanDefinition(AttributeConfigurator.class);
                    configAttr.setAttributeFilter("name", "description", "type", "format",
                        "constraint"/*, "length", "min", "max"*/,
                        "presentable", "columnDefinition", "declaration", "valueExpression", "default", "listener");
                    configAttr.getPresentable().setLayout(layout);
                    configAttr.setValueExpression(new ValueExpression("{name}", AttributeConfigurator.class));
    //                configAttr.saveDefinition();
    
                    configBean.setAttributeFilter("name", "valueExpression", "presentable", "valueGroups", "attributes");
                    configBean.getPresentable().setLayout(layout);
                    ((ValueExpressionFormat) configBean.getAttribute("attributes").getFormat())
                        .getValueExpression()
                        .setExpression("{name}");
    //          configBean.saveDefinition();
    
                }
                return (Bean<BeanConfigurator<I>>) configBean;
            } finally {
                ENV.setAutopersist(autopersistEnv);
            }
        }
        return (Bean<BeanConfigurator<I>>) (Object)Bean.getBean(configurer);
    }

    /**
     * defineAction
     * 
     * @param layout
     */
    public static void defineAction(Serializable layout) {
        BeanDefinition<CommonAction> configAction = BeanDefinition.getBeanDefinition(CommonAction.class);
        configAction.setAttributeFilter("id", "shortDescription", "longDescription", "keyStroke", "enabled", "default",
            "imagePath");
        configAction.setIdAttribute("id");
        configAction.setValueExpression(new ValueExpression<CommonAction>("{shortDescription}", CommonAction.class));
        if (layout != null) {
            configAction.getPresentable().setLayout(layout);
        }
    }

    /**
     * constructor
     * 
     * @param def bean-def
     */
    protected BeanConfigurator(BeanDefinition<T> def) {
        super();
        this.def = def;
        defAccessor = new PrivateAccessor<BeanDefinition<?>>(def);
    }

    /**
     * @return Returns the attributeDefinitions.
     */
    public List<AttributeConfigurator> getAttributes() {
        if (attrConfigurators == null) {
            //we know that there are attribute-defs inside!
            List<IAttribute> attributes = def.getAttributes();
            attrConfigurators = new ArrayList<AttributeConfigurator>(attributes.size());
            for (IAttribute<?> a : attributes) {
                attrConfigurators.add(new AttributeConfigurator((AttributeDefinition<?>) a));
            }
        }
        return attrConfigurators;
    }

    /**
     * @param attributes The attributeDefinitions to set.
     */
    public void setAttributes(List<AttributeConfigurator> attributes) {
        Map<String, IAttributeDefinition> definitions = defAccessor.call("getAttributeDefinitions", Map.class);
        Map<String, IAttributeDefinition> invisibles = new LinkedHashMap<>(definitions);
        definitions.clear();
        int i = 0;
        for (AttributeConfigurator cattr : attributes) {
            IAttributeDefinition a = cattr.unwrap();
            if (a.getColumnDefinition() == null)
                a.setColumnDefinition(++i, IPresentable.UNDEFINED, true, IPresentable.UNDEFINED);
            else //IMPROVE: enhance interfaces to set index without reflection
                new PrivateAccessor(a.getColumnDefinition()).set("columnIndex", ++i);
            def.addAttribute(a);
            invisibles.remove(a.getName());
        }
        //add not-selected attributes as invisibles at the end
        for (IAttributeDefinition<?> a : invisibles.values()) {
            //IMPROVE: enhance interfaces to set index without reflection
            a.getPresentation().setVisible(false);
            if (a.getColumnDefinition() != null) {
                new PrivateAccessor(a.getColumnDefinition()).set("columnIndex", ++i);
                a.getColumnDefinition().getPresentable().setVisible(false);
            }
            def.addAttribute(a);
        }
        attrConfigurators = null;
    }

    /**
     * @return Returns the presentable.
     */
    public Presentable getPresentable() {
        return (Presentable) def.getPresentable();
    }

    /**
     * @param presentable The presentable to set.
     */
    public void setPresentable(Presentable presentable) {
        def.setPresentable(presentable);
    }

    /**
     * @return Returns the presentable.
     */
    public Collection<ValueGroup> getValueGroups() {
        return defAccessor.member("valueGroups", Collection.class);
    }

    /**
     * @param valueGroups The presentable to set.
     */
    public void setValueGroups(Collection<ValueGroup> valueGroups) {
        defAccessor.set("valueGroups", valueGroups);
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return def.getName();
    }

    /**
     * @return Returns the valueExpression.
     */
    public String getValueExpression() {
        return def.getValueExpression().getExpression();
    }

    /**
     * @param valueExpression The valueExpression to set.
     */
    public void setValueExpression(String valueExpression) {
        def.setValueExpression(new ValueExpression<T>(valueExpression, def.getClazz()));
    }

    /**
     * saves the current bean configuration.<br/>
     * this method will trigger the bean-framework to provide an action to be presented as button in a gui.
     * 
     * @return null
     */
    public Object actionSave() {
        defAccessor.set("isdefault", false);
        //WORKAROUND: doesn't call setAttributes(), so we do it here
        if (attrConfigurators != null)
            setAttributes(attrConfigurators);
        def.saveDefinition();
        ConcurrentUtil.removeCurrent(BeanConfigurator.class);

        /*
         * refresh all beans
         */
        Bean.clearCache();

        //return null to let the session-navigation return to the last element.
        return null;
    }

    public Object actionReset() {
        defAccessor.set("isdefault", true);
        def.deleteDefinition();
        ConcurrentUtil.removeCurrent(BeanConfigurator.class);
        /*
         * refresh all beans
         */
        Bean.clearCache();

        return null;
    }

    @Override
    public int hashCode() {
        return def.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BeanConfigurator ? def.equals(((BeanConfigurator) obj).def) : false;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), def);
    }

    @de.tsl2.nano.bean.annotation.Action(name = "createRuleOrAction", argNames = { "newActionName", "actionType",
        "actionExpression" })
    public void actionCreateRuleOrAction(String newActionName,
            @de.tsl2.nano.bean.annotation.Constraint(defaultValue = "%: RuleScript (--> JavaScript)", allowed = {
                "§: Rule  (--> Operation)", "%: RuleScript (--> JavaScript)", "!: Action (--> Java)"
                , "?: Query (--> SQL statement)", "@: Web   (--> URL/REST)" }) String actionType,
            @de.tsl2.nano.bean.annotation.Constraint(pattern = ".*") String actionExpression) {

        if (actionType.startsWith("%"))
            ENV.get(RulePool.class).add(new RuleScript<>(newActionName, actionExpression, null));
        else if (actionType.startsWith("§"))
            ENV.get(RulePool.class).add(new Rule(newActionName, actionExpression, null));
        else if (actionType.startsWith("!"))
            ENV.get(ActionPool.class).add(new Action(newActionName, actionExpression));
        else if (actionType.startsWith("@"))
            ENV.get(WebPool.class).add(WebClient.create(actionExpression, def.getDeclaringClass()));
        else if (actionType.startsWith("?"))
            ENV.get(QueryPool.class).add(new Query(newActionName, actionExpression, true, null));
        
    }

    @de.tsl2.nano.bean.annotation.Action(name = "addAction", argNames = { "specifiedAction" })
    public void actionAddAction(
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_ENVFILES + ".*specification/action.*") String specifiedAction) {
        //check, if action available
        specifiedAction = StringUtil.substring(FileUtil.replaceToJavaSeparator(specifiedAction), "/", ".", true);
        ENV.get(ActionPool.class).get(specifiedAction);
        SpecifiedAction<Object> action = new SpecifiedAction(specifiedAction, null);
        def.addAction(action);
    }

    @de.tsl2.nano.bean.annotation.Action(name = "createCompositor", argNames = { "baseType", "baseAttributeName",
        "targetAttributeName", "iconAttributeName" })
    public void actionCreateCompositor(
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPCLASSES) String baseType, 
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String baseAttribute, 
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String targetAttribute, 
            @de.tsl2.nano.bean.annotation.Constraint(nullable=true, allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String iconAttribute) {
        createCompositor(baseType, baseAttribute, targetAttribute, iconAttribute);
        Bean.clearCache();
    }

    public void createCompositor(String baseType, String baseAttribute, String targetAttribute, String iconAttribute) {
        Compositor compositor = createCompositorBean(Compositor.class, "icons/properties.png", baseType, baseAttribute, null, targetAttribute, iconAttribute);
        compositor.saveDefinition();
    }

    @de.tsl2.nano.bean.annotation.Action(name = "createController"
            , argNames = {"baseType", "baseAttributeName",
                          "targetType", "targetAttributeName", "iconAttributeName",
                          "increaseAttribute", "increaseCount", "increaseStep", })
    public void actionCreateController (
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPCLASSES) String baseType, 
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String baseAttribute, 
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPCLASSES) String targetType, 
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String targetAttribute, 
            @de.tsl2.nano.bean.annotation.Constraint(nullable=true, allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String iconAttribute,
            @de.tsl2.nano.bean.annotation.Constraint(allowed=ConstraintValueSet.ALLOWED_APPBEANATTRS) String increaseAttribute,
            int increaseCount,
            int increaseStep
            ) {
        createControllerBean(notnull(baseType), notnull(baseAttribute), nullable(targetType), notnull(targetAttribute), nullable(iconAttribute), 
            nullable(increaseAttribute), increaseCount, increaseStep, false, true, false); //TODO: let the user configure the presentationvalues 
        Bean.clearCache();
    }

    private String nullable(String arg) {
        return ConstraintValueSet.NULL_CLASS.equals(arg) ? null : arg;
    }

    private String notnull(String arg) {
        ManagedException.assertion(!ConstraintValueSet.NULL_CLASS.equals(arg), arg);
        return arg;
    }

    public Controller createControllerBean(String baseType, String baseAttribute, String targetType, String targetAttribute, String iconAttribute,
            String increaseAttribute, int increaseCount, int increaseStep, boolean showText, boolean transparent, boolean creationOnly) {
        Controller controller = createCompositorBean(Controller.class, "icons/cascade.png", baseType, baseAttribute, targetType, targetAttribute, iconAttribute);
        controller.setPresentationValues(showText, transparent, creationOnly);
        if (!Util.isEmpty(increaseAttribute)) {
            increaseAttribute = StringUtil.substring(increaseAttribute, ".", null, true);
            controller.setItemProvider(new Increaser(increaseAttribute, increaseCount, increaseStep));
        }
        controller.saveDefinition();
        return controller;
    }

    /**
     * createCompositorBean
     * @param baseType
     * @param baseAttribute
     * @param targetAttribute
     * @param iconAttribute
     */
    public <C extends Compositor> C createCompositorBean(Class<C> compositorExtension, String compositorIcon, String baseType, String baseAttribute, String targetType, String targetAttribute, String iconAttribute) {
        BeanClass bcBaseType = BeanClass.createBeanClass(baseType);
        BeanClass bcTargetType = !Util.isEmpty(targetType) ? BeanClass.createBeanClass(targetType) : null;
        //check the attributes
        baseAttribute = StringUtil.substring(baseAttribute, ".", null, true);
        targetAttribute = Util.nonEmpty(StringUtil.substring(targetAttribute, ".", null, true));
        iconAttribute = Util.nonEmpty(StringUtil.substring(iconAttribute, ".", null, true));
        bcBaseType.getAttribute(baseAttribute);
        if (iconAttribute != null)
            bcBaseType.getAttribute(iconAttribute);
        if (targetAttribute != null)
            if (bcTargetType != null)
                bcTargetType.getAttribute(targetAttribute);
            else
                def.getAttribute(targetAttribute);
        //now create the compositor
        Compositor compositor = BeanClass.createInstance(compositorExtension, def.getClazz(), bcBaseType.getClazz(), baseAttribute, targetAttribute, iconAttribute);
        if (bcTargetType != null)
            compositor.setTargetType(bcTargetType.getClazz());
        compositor.getPresentable().setIcon(compositorIcon);
//        compositor.getActions();
        return (C) compositor;
    }

    @de.tsl2.nano.bean.annotation.Action(name = "createSheet", argNames = { "title", "cols", "rows" })
    public void actionCreateSheet(
            String title, 
            int cols, 
            int rows) {
        new CSheet(title, cols, rows).save();
    }

    @de.tsl2.nano.bean.annotation.Action(name = "addAttribute", argNames = { "attributeType", "attributeExpression"})
    public void actionAddAttribute(
            @de.tsl2.nano.bean.annotation.Constraint(allowed = {" :      (--> PathExpression)",
                "§: Rule  (--> Operation)", "%: RuleScript (--> JavaScript)", "!: Action (--> Java)"
                , "?: Query (--> sql statement)", "@: Web   (--> URL/REST)" }) String attributeType,
            String attributeExpression) {
        addAttribute(attributeType, attributeExpression);
        def.saveDefinition();
        Bean.clearCache();
    }

    public void addAttribute(String attributeType, String attributeExpression) {
        if (Util.isEmpty(attributeType)) {
            ManagedException.assertion(!Util.isEmpty(attributeExpression), "At least attributeExpression or attributeType must be filled!", attributeExpression, attributeType);
            attributeType = String.valueOf(attributeExpression.charAt(0));
        }
        else
            attributeType = String.valueOf(attributeType.charAt(0)).trim();
        
        attributeExpression = attributeType.length() > 0 && !Util.isEmpty(attributeExpression) && attributeType.charAt(0) == attributeExpression.charAt(0) ? attributeExpression : attributeType + attributeExpression;
        ExpressionDescriptor<Object> exDescr = new ExpressionDescriptor<>(def.getDeclaringClass(), attributeExpression);
        AttributeDefinition attr = def.addAttribute(exDescr.getName(), exDescr.toInstance(), null, null);
        attr.getPresentation().setType(IPresentable.TYPE_DEPEND);
        attr.getPresentation().setStyle(IPresentable.UNDEFINED);
    }

}
