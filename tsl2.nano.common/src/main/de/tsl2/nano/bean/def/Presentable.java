/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jun 25, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.Util;

/**
 * simple gui properties
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class Presentable implements IIPresentable, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -7970100238668263393L;

    @Attribute
    int type = UNSET;
    @Attribute
    int style = UNSET;
    @Element(required = false)
    String label;
    @Element(required = false)
    String description;
    //normally only serializable - but simple-xml has problems to serialize hashmaps, if they aren't directly declared.
    @ElementMap(entry = "layout", key = "name", attribute = true, inline = true, valueType = String.class, required = false)
    protected LinkedHashMap<String, String> layout;
    //normally only serializable - but simple-xml has problems to serialize hashmaps, if they aren't directly declared.
    @ElementMap(entry = "layoutconstraint", key = "name", attribute = true, inline = true, valueType = String.class, required = false)
    protected LinkedHashMap<String, String> layoutConstraints;
    @Attribute
    boolean visible = true;
    @Element(required = false)
    IActivable enabler;
    @Element(required = false)
    String icon;

    @ElementArray(required = false)
    int[] foreground;
    @ElementArray(required = false)
    int[] background;

    public Presentable() {
    }

    public Presentable(IAttribute<?> attr) {
        IAttributeDefinition def = (IAttributeDefinition) (attr instanceof IAttributeDefinition ? attr : null);
        label = Environment.translate(attr.getId(), true);
        BeanPresentationHelper<?> helper = Environment.get(BeanPresentationHelper.class);
        type = def != null ? helper.getDefaultType(def) : helper.getDefaultType(attr);
        style = def != null ?
            helper.getDefaultHorizontalAlignment(def) : helper.getDefaultHorizontalAlignment(attr);
        style |= helper.getDefaultStyle(attr);
        description = Environment.translate(attr.getId() + Messages.POSTFIX_TOOLTIP, false);
        if (description.startsWith(Messages.TOKEN_MSG_NOTFOUND))
            description = label;
        /*
         * to be enabled, the attribute must be 
         * - writable through setter-method
         * - not persistable or persistable and if it is a multi-value, cascading must be activated!
         */
        enabler =
            attr.hasWriteAccess()
                && !(def != null && def.generatedValue())
                && (!BeanContainer.isInitialized() || !BeanContainer.instance().isPersistable(attr.getDeclaringClass())
                    || !(def != null) || (!def.isMultiValue() || def
                    .cascading()))
                ? IActivable.ACTIVE : IActivable.INACTIVE;
    }

    /**
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }

    /**
     * @return Returns the style.
     */
    public int getStyle() {
        return style;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Returns the layout.
     */
    public Serializable getLayout() {
        return layout;
    }

    /**
     * @return Returns the layoutConstraints.
     */
    public Serializable getLayoutConstraints() {
        return layoutConstraints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L extends Serializable, T extends IPresentable> T setPresentation(String label,
            int type,
            int style,
            IActivable enabler,
            boolean visible,
            L layout,
            L layoutConstraints,
            String description) {
        this.label = label;
        this.type = type;
        this.style = style;
        this.enabler = enabler;
        this.visible = visible;
        this.layout = (LinkedHashMap) layout;
        this.layoutConstraints = (LinkedHashMap) layoutConstraints;
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IPresentable> T setPresentationDetails(int[] foreground, int[] background, String icon) {
        this.foreground = foreground;
        this.background = background;
        this.icon = icon;
        return (T) this;
    }

    @Override
    public IActivable getEnabler() {
        if (enabler == null)
            enabler = IActivable.ACTIVE;
        return enabler;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * @return Returns the icon.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @return Returns the foreground.
     */
    public int[] getForeground() {
        return foreground;
    }

    /**
     * @return Returns the background.
     */
    public int[] getBackground() {
        return background;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return UNDEFINED;//throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        return UNDEFINED;//throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPresentable setType(int type) {
        this.type = type;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPresentable setStyle(int style) {
        this.style = style;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L extends Serializable, T extends IPresentable> T setLayout(L layout) {
        this.layout = (LinkedHashMap) layout;
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <L extends Serializable, T extends IPresentable> T setLayoutConstraints(L layoutConstraints) {
        this.layoutConstraints = (LinkedHashMap) layoutConstraints;
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IPresentable> T setEnabler(IActivable enabler) {
        this.enabler = enabler;
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IPresentable> T setVisible(boolean visible) {
        this.visible = visible;
        return (T) this;
    }

    public String layout(String name) {
        return layout(name, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T layout(String name, T defaultValue) {
        T value = null;
        if (layout instanceof Map)
            value = (T) ((Map) layout).get(name);
        if (value == null && layoutConstraints instanceof Map)
            value = (T) ((Map) layoutConstraints).get(name);
        if (value == null)
            value = defaultValue;
        return value;
    }

    @Override
    public String toString() {
        return Util.toString(getClass(), label, "type:" + type, "style:" + style
            , (visible ? "visible" : "invisible)")
            , (enabler == null || enabler.isActive() ? "enabled" : "disabled")
            , "icon: " + icon);
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setLayout(LinkedHashMap<String, String> layout) {
        this.layout = layout;
    }

    @Override
    public void setLayoutConstraints(LinkedHashMap<String, String> layoutConstraints) {
        this.layoutConstraints = layoutConstraints;
    }

    @Override
    public <T extends IPresentable> T addLayoutConstraints(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public void setForeground(int[] foreground) {
        this.foreground = foreground;
    }

    @Override
    public void setBackground(int[] background) {
        this.background = background;
    }
}
