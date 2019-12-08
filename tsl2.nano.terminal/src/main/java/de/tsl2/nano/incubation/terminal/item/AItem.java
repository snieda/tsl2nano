/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal.item;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Properties;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.action.IConstraint;
import de.tsl2.nano.bean.def.Constraint;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.util.CLI.Color;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.terminal.AsciiImage;
import de.tsl2.nano.incubation.terminal.IContainer;
import de.tsl2.nano.incubation.terminal.IItem;
import de.tsl2.nano.incubation.terminal.SIShell;
import de.tsl2.nano.incubation.terminal.TextTerminal.Frame;
import de.tsl2.nano.incubation.vnet.workflow.Condition;

/**
 * the base implementation for all item types.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
//@Default(value = DefaultType.FIELD, required = false)
public class AItem<T> implements IItem<T>, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 7142058494650831052L;

    @Attribute
    protected String name;
    /** presentation style. see the parent terminal style */
    @Attribute(required = false)
    protected Frame style;
    @Attribute(required = false)
    protected Color fgColor;
    @Attribute(required = false)
    protected Color bgColor;

    /** the type has to be set by the implementing class */
    transient Type type;
    @Element(type = Constraint.class, required = false)
    IConstraint<T> constraints;
    /** optional condition to evaluate through an expression, whether this item is visible. */
    @Element(required = false)
    Condition condition;

    protected T value;
    transient IContainer parent;
    transient byte[] origin;
    transient StringBuilder prefix = new StringBuilder("( ) ");
    transient boolean changed;
    /**
     * if description is null, the constraints toString() will be used. if description is an image file name, this image
     * will be converted to an ascii text.
     */
    @Element(required = false)
    protected String description;

    static final int PREFIX = 1;
    static final String POSTFIX_QUESTION = ": ";
    static final String NULL = "null";

    /**
     * constructor
     */
    public AItem() {
    }

    /**
     * constructor
     * 
     * @param name
     * @param constraints
     * @param type
     * @param value
     */
    public AItem(String name, IConstraint<T> constraints, Type type, T value, String description) {
        super();
        this.name = name;
        this.type = type;
        this.value = value;
        initDescription(description);
        initConstraints(constraints);
    }

    private void initDescription(String description) {
        this.description = description;
    }

    @SuppressWarnings("unchecked")
    protected void initConstraints(IConstraint<T> constraints) {
        if (constraints != null) {
            this.constraints = constraints;
        } else {
            Class<T> t = (Class<T>) (value != null ? value.getClass() : String.class);
            this.constraints = new Constraint<T>(t);
            this.constraints.setFormat(Constraint.createFormat(t));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IConstraint<T> getConstraints() {
        return constraints;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Element(required = false)
    public T getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Element(required = false)
    public void setValue(T value) {
        //constraints can only be null while deserialization (before call of commit)
        if (constraints != null) {
            constraints.check(name, value);
        }
        this.value = value;
        if (value != null)
            System.getProperties().put(name, value);
    }

    @Override
    public String ask(Properties env) {
        String ask = constraints.getType().getSimpleName();
        if (constraints.getMinimum() != null || constraints.getMaximum() != null) {
            ask += " between " + constraints.getMinimum() != null ? constraints.getMinimum() : "<any>"
                + constraints.getMaximum() != null ? constraints.getMaximum() : "<any>";
        } else if (constraints.getAllowedValues() != null) {
            ask += " as one of: " + StringUtil.toString(constraints.getAllowedValues(), 60);
        }
        return Messages.getFormattedString("tsl2nano.entervalue", ask, StringUtil.toFirstUpper(name))
            + POSTFIX_QUESTION;
    }

    /**
     * uses a scanner to wait for the nextLine
     */
    protected String nextLine(InputStream in, PrintStream out) {
        return SIShell.nextLine(in, out);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        try {
            if (input.equals(NULL)) {
                input = null;
            }
            setValue((T) getConstraints().getFormat().parseObject(input));
            if (!Util.isEmpty(input)) {
                env.put(getName(), getValue());
            } else {
                env.remove(getName());
            }
            changed = true;
        } catch (ParseException e) {
            ManagedException.forward(e);
        }
        return caller == this ? getParent().next(in, out, env) : this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChanged() {
        return changed;//ByteUtil.equals(ByteUtil.serialize(value), origin);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPresentationPrefix() {
        if (isChanged()) {
            prefix.setCharAt(PREFIX, 'x');
        }
        return prefix.toString();
    }

    @Override
    public String getDescription(Properties env, boolean full) {
        return getDescription(env, full, defaultHeight());
    }
    public String getDescription(Properties env, boolean full, int height) {
        //if sequential mode, show the parents (-->tree) description
        if (Util.get(SIShell.KEY_SEQUENTIAL, false) && getParent() != null) {
            return getParent().getDescription(env, full);
        } else if (description == null) {
            description = getConstraints() != null ? getConstraints().toString() : name;
        } else if (full) {
            if (hasImageDescription())
                description = printImageDescription(height);
            else if (hasFileDescription())
                description = FileUtil.getFileString(description);
        }
        return description;
    }

    protected String printImageDescription() {
        return printImageDescription(defaultHeight());
    }

    private int defaultHeight() {
        return Util.get(SIShell.KEY_HEIGHT, 20) - 6;
    }

    /**
     * printImageDescription
     */
    protected String printImageDescription(int height) {
        StringWriter stream = new StringWriter();
        try {
            new AsciiImage().convertToAscii(description, new PrintWriter(stream), Util.get(SIShell.KEY_WIDTH, 80) - 2,
                height);
            //remove windows returns
            return stream.toString().replaceAll("\r", "");
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean hasImageDescription() {
        return description != null && new File(description).isFile() && FileUtil.isBinary(new File(description));
    }
    /**
     * hasFileDescription
     * 
     * @return true, if description points to a file
     */
    public boolean hasFileDescription() {
        return description != null && new File(description).isFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IContainer getParent() {
        return parent;
    }

    /**
     * @param parent The {@link #parent} to set.
     */
    @Override
    public void setParent(IContainer parent) {
        this.parent = parent;
    }

    /**
     * @return Returns the condition.
     */
    @Override
    public Condition getCondition() {
        return condition;
    }

    /**
     * @param condition The condition to set.
     */
    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    /**
     * @return Returns the style.
     */
    public Frame getStyle() {
        return style;
    }

    /**
     * @param style The style to set.
     */
    public void setStyle(Frame style) {
        this.style = style;
    }

    @Override
    public Color getFgColor() {
        return fgColor;
    }
    /**
     * @param fgColor the fgColor to set
     */
    public void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
    }
    /**
     * @return the bgColor
     */
    @Override
    public Color getBgColor() {
        return bgColor;
    }
    /**
     * @param bgColor the bgColor to set
     */
    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
    }
    public static String translate(String name) {
        return SIShell.translate(name);
    }

    @Commit
    protected void initDeserialization() {
        initConstraints(constraints);
    }

    protected String getValueText() {
        return String.valueOf(constraints != null && constraints.getFormat() != null ? constraints.getFormat().format(
            value) : Util.value(value, "<>"));
    }

    protected String getName(int fixlength, char filler) {
        String str = getPresentationPrefix() + (name != null ? translate(name) : "");
        return fixlength != -1 ? StringUtil.fixString(str, fixlength, filler, true) : str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName(-1, (char) -1) + ": "
            + (value != null ? StringUtil.toString(value.toString().replace('\n', ' '), 60) : "<>") + "\n";
    }
}
