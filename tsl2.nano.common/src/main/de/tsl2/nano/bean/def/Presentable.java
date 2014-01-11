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
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;

import de.tsl2.nano.Environment;
import de.tsl2.nano.action.IActivator;
import de.tsl2.nano.bean.BeanAttribute;

/**
 * simple gui properties
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("unchecked")
public class Presentable implements IPresentable, Serializable {
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
    @Element(required = false)
    Serializable layout;
    @Element(required = false)
    Serializable layoutConstraints;
    @Attribute
    boolean visible = true;
    transient IActivator enabler;
    @Element(required = false)
    String icon;

    @ElementArray(required = false)
    int[] foreground;
    @ElementArray(required = false)
    int[] background;

    public Presentable() {
    }

    public Presentable(AttributeDefinition<?> attr) {
        label = Environment.translate(attr.getName(), true);
        type = Environment.get(BeanPresentationHelper.class).getDefaultType(attr);
        style =
            Environment.get(BeanPresentationHelper.class).getDefaultHorizontalAlignment((IAttributeDefinition<?>) attr);
        description = label;
        enabler = attr.hasWriteAccess() ? IActivator.ACTIVE : IActivator.INACTIVE;
    }

    public Presentable(BeanAttribute attr) {
        label = Environment.translate(attr.getName(), true);
        type = Environment.get(BeanPresentationHelper.class).getDefaultType(attr);
        style = Environment.get(BeanPresentationHelper.class).getDefaultHorizontalAlignment(attr);
        description = label;
        enabler = IActivator.ACTIVE;
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
    public <T extends IPresentable> T setPresentation(String label,
            int type,
            int style,
            IActivator enabler,
            boolean visible,
            Serializable layout,
            Serializable layoutConstraints,
            String description) {
        this.label = label;
        this.type = type;
        this.style = style;
        this.enabler = enabler;
        this.visible = visible;
        this.layout = layout;
        this.layoutConstraints = layoutConstraints;
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
    public IActivator getEnabler() {
        if (enabler == null)
            enabler = IActivator.ACTIVE;
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
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getHeight() {
        throw new UnsupportedOperationException();
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
    public <T extends IPresentable> T setLayout(Serializable layout) {
        this.layout = layout;
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IPresentable> T setLayoutConstraints(Serializable layoutConstraints) {
        this.layoutConstraints = layoutConstraints;
        return (T) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IPresentable> T setEnabler(IActivator enabler) {
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
        return getClass().getSimpleName() + "(" + label + ", type:" + type + ", style:" + style
            + (visible ? ")" : ", invisible)");
    }
}
