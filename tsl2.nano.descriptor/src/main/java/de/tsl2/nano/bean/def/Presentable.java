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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.cls.IAttribute;
import de.tsl2.nano.core.util.MapUtil;
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

    @Attribute(required=false)
    int type = UNSET;
    @Attribute(required=false)
    int style = UNSET;
    @Element(required = false)
    String label;
    transient String translatedLabel;
    @Element(required = false)
    String description;
    transient String translatedDescription;
    //normally only serializable - but simple-xml has problems to serialize hashmaps, if they aren't directly declared.
    @ElementMap(entry = "layout", key = "name", attribute = true, inline = true, valueType = String.class, required = false)
    protected LinkedHashMap<String, String> layout;
    //normally only serializable - but simple-xml has problems to serialize hashmaps, if they aren't directly declared.
    @ElementMap(entry = "layoutconstraint", key = "name", attribute = true, inline = true, valueType = String.class, required = false)
    protected LinkedHashMap<String, String> layoutConstraints;
    @Attribute(required=false)
    boolean visible = true;
    @Attribute(required=false)
    boolean searchable = true;
    
    @Element(required = false)
    IActivable enabler;
    @Element(required = false)
    String icon;

    @ElementArray(required = false)
    int[] foreground;
    @ElementArray(required = false)
    int[] background;

    @ElementList(required = false)
    List<?> itemlist;

    @Attribute(required = false)
    boolean nesting;
    
    /** needed for instances that wont be serialized */
    private transient boolean initialized;

    public Presentable() {
    }

    public Presentable(IAttribute<?> attr) {
        IAttributeDefinition def = (IAttributeDefinition) (attr instanceof IAttributeDefinition ? attr : null);
        //translation will be done in initDeserialization()
        label = attr.getId();//!Messages.hasKey(attr.getId()) ? Environment.translate(attr.getId(), true) : attr.getId();
        description = label;
        
        BeanPresentationHelper<?> helper = ENV.get(BeanPresentationHelper.class);
        type = def != null ? helper.getDefaultType(def) : helper.getDefaultType(attr);
        style = def != null ?
            helper.getDefaultHorizontalAlignment(def) : helper.getDefaultHorizontalAlignment(attr);
        style |= helper.getDefaultStyle(attr);
        /*
         * to be enabled, the attribute must be 
         * - writable through setter-method
         * - not persistable or persistable and if it is a multi-value, cascading must be activated!
         */
        enabler =
            attr.hasWriteAccess()
                && !(def != null && BeanPresentationHelper.isGeneratedValue(def))
                && (!BeanContainer.isInitialized() || !BeanContainer.instance().isPersistable(attr.getDeclaringClass())
                    || !(def != null) || (!def.isMultiValue() || def
                    .cascading()))
                ? IActivable.ACTIVE : IActivable.INACTIVE;
    }

    /**
     * @return Returns the type.
     */
    @Override
    public int getType() {
        return type;
    }

    /**
     * @return Returns the style.
     */
    @Override
    public int getStyle() {
        return style;
    }

    /**
     * @return Returns the description.
     */
    @Override
    public String getDescription() {
        if (!initialized) {
            initDeserialization();
        }
        return translatedDescription;
    }

    /**
     * @return Returns the layout.
     */
    @Override
    public Serializable getLayout() {
        return layout;
    }

    /**
     * @return Returns the layoutConstraints.
     */
    @Override
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
        if (enabler == null) {
            enabler = IActivable.ACTIVE;
        }
        return enabler;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return Returns the searchable.
     */
    @Override
    public boolean isSearchable() {
        return searchable;
    }

    @Override
    public String getLabel() {
        if (!initialized) {
            initDeserialization();
        }
        return translatedLabel;
    }

    /**
     * @return Returns the icon.
     */
    @Override
    public String getIcon() {
        return icon;
    }

    /**
     * @return Returns the foreground.
     */
    @Override
    public int[] getForeground() {
        return foreground;
    }

    /**
     * @return Returns the background.
     */
    @Override
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

    @Override
    public String layout(String name) {
        return layout(name, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T layout(String name, T defaultValue) {
        T value = null;
        if (layout instanceof Map) {
            value = (T) ((Map) layout).get(name);
        }
        if (value == null && layoutConstraints instanceof Map) {
            value = (T) ((Map) layoutConstraints).get(name);
        }
        if (value == null) {
            value = defaultValue;
        }
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

    @Override
    public List<?> getItemList() {
        return itemlist;
    }
    @Override
    public <T extends IPresentable> T setItemList(List<?> itemlist) {
        this.itemlist = itemlist;
        return (T) this;
    }
    
    /**
     * @return Returns the nesting.
     */
    @Override
    public boolean isNesting() {
        return nesting;
    }

    /**
     * @param nesting The nesting to set.
     */
    @Override
    public <T extends IPresentable> T setNesting(boolean nesting) {
        this.nesting = nesting;
        return (T) this;
    }

    @Commit
    protected void initDeserialization() {
        //try to translate after first loading
        translatedDescription = description;
        if (description != null && description.equals(label)) {
            String d = ENV.translate(label + Messages.POSTFIX_TOOLTIP, false);
            if (d != null && !d.startsWith(Messages.TOKEN_MSG_NOTFOUND)) {
                translatedDescription = d;
            }
        }
        if (label != null) {
            translatedLabel= ENV.translate(label, true);
        }
        initialized = true;
    }
    
    public static final String asText(int value) {
        return value == UNDEFINED ? null : String.valueOf(value);
    }
	public static Presentable createPresentable(de.tsl2.nano.bean.annotation.Presentable p) {
		return createPresentable(p, null);
	}
	public static Presentable createPresentable(de.tsl2.nano.bean.annotation.Presentable p, IAttribute attr) {
		Presentable presentable = attr != null ? new Presentable(attr) : new Presentable();
		fillPresentable(p, presentable);
		return presentable;
	}

	public static void fillPresentable(de.tsl2.nano.bean.annotation.Presentable p, Presentable presentable) {
		presentable.setPresentation(
				!Util.isEmpty(p.label()) ? p.label() : presentable.label, 
				p.type() != UNDEFINED ? p.type() : presentable.type, 
				p.style() != UNDEFINED ? p.style() : presentable.style, 
				p.enabled() ? IActivable.ACTIVE : IActivable.INACTIVE, 
				p.visible()
		    , (Serializable)MapUtil.asMap(p.layout()), (Serializable)MapUtil.asMap(p.layoutConstraints()), p.description());
		presentable.setNesting(p.nesting());
		if (!Util.isEmpty(p.icon()))
			presentable.setIcon(p.icon());
		if (!Util.isEmpty(p.background()))
			presentable.setBackground(p.background());
		if (!Util.isEmpty(p.foreground()))
			presentable.setForeground(p.foreground());
		if (!Util.isEmpty(p.items()))
			presentable.setItemList(Arrays.asList(p.items()));
	}

}
