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

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.terminal.IContainer;
import de.tsl2.nano.incubation.terminal.IItem;
import de.tsl2.nano.incubation.terminal.Terminal;

/**
 * the Container of items. if only one item is available, it should delegate the request directly to that item.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class Container<T> extends AItem<T> implements IContainer<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3656677742608173033L;

    /** child nodes */
    @ElementList(type = AItem.class, inline = true, entry = "item", required = false)
    protected List<AItem<T>> nodes;

    transient private boolean isactive;
    /** (default:true) if true, result (=value) is a collection! */
    @Attribute(required=false)
    boolean multiple = false;
    /** if true, all tree items will be accessed directly and sequentially */
    transient boolean sequential = false;
    /** on sequential mode, this index points to the actual child-item */
    transient int seqIndex = -1;

    /**
     * constructor
     */
    public Container() {
        super();
        //WORKAROUND: unable to save list of values through simple-xml
        value = (T) (multiple ? new ArrayList<T>() : value);
        type = Type.Container;
        prefix.setCharAt(PREFIX, '+');
    }

    public Container(String name, String description) {
        this(name, null, null, description);
    }

    /**
     * constructor
     * 
     * @param name
     * @param constraints
     * @param type
     * @param value
     */
    public Container(String name, IConstraint<T> constraints, List<T> selected, String description) {
        super(name, constraints, Type.Container, (T)selected, description);
        multiple = selected != null;
        nodes = new ArrayList<AItem<T>>();
        prefix.setCharAt(PREFIX, '+');
    }

    protected List<AItem<T>> getNodes() {
        return nodes;
    }

    /**
     * filters all child nodes where the condition return false
     * 
     * @param context application context
     * @return filtered child nodes
     */
    @Override
    public List<AItem<T>> getNodes(final Map context) {
        return CollectionUtil.getFiltering(nodes, new IPredicate<AItem<T>>() {
            @Override
            public boolean eval(AItem<T> arg0) {
                return arg0.getCondition() == null || arg0.getCondition().isTrue(context);
            }
        });
    }

    /**
     * if the selected child is again of type tree but has only one active child, this child will be activated.
     * 
     * @param selected selected child
     * @param in input
     * @param out output
     * @param env application context
     * @return the selected item, or if it is a tree having only one active child - this child
     */
    public IItem<T> delegateToUniqueChild(IItem<T> selected, InputStream in, PrintStream out, Properties env) {
        if (selected.getType() == Type.Container && ((Container) selected).getNodes(env).size() == 1) {
            //if only one tree child is available, delegate directly to that item
            return selected.react(this, "1", in, out, env);
        } else
            return selected;
    }

    @Override
    protected void initConstraints(IConstraint<T> constraints) {
    }

    @Override
//    @Transient
    //WORKAROUND: unable to save list of values through simple-xml
//    @ElementList(type=Object.class, inline = true, entry = "value", required = false)
    public T getValue() {
        return value;
    }

    @Override
//    @Transient
    //WORKAROUND: unable to save list of values through simple-xml
//    @ElementList(type=Object.class, inline = true, entry = "value", required = false)
    public void setValue(T value) {
        super.setValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue(int i) {
        return multiple ? ((List<T>)value).get(i) : value;
    }

    @Override
    public String ask(Properties env) {
        isactive = true;
        List<AItem<T>> children = getNodes(env);
        return sequential && seqIndex > -1 && seqIndex < children.size() ? children.get(seqIndex).ask(env)
            : "Please enter a number between 1 and "
                + children.size() + POSTFIX_QUESTION;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        sequential = Util.get(Terminal.KEY_SEQUENTIAL, false);
        if (Util.isEmpty(input) && !sequential)
            return getParent();
        IItem next = null;
        List<AItem<T>> filteredNodes = getNodes(env);
        /*
         * sequential mode
         */
        if (sequential && seqIndex < filteredNodes.size()) {
            return next(in, out, env);
        }

        /*
         * standard menu selection mode
         */
        //find the item through current user input
        next = getNode(input, env);

        IItem nextnext = null;
        if (!next.isEditable()) {
            nextnext = next.react(this, Util.asString(next.getValue()), in, out, env);
        } else if (next instanceof IContainer && ((Container) next).getNodes(env).size() == 1) {
            //if only one tree child is available, delegate directly to that item
            nextnext = next.react(this, "1", in, out, env);
        }
        //assign the new result
        if (multiple)
            ((List<T>)value).add((T) next.getValue());
        else
            setValue((T) next.getValue());
        
        if (getValue() != null)
            env.put(getName(), getValue());
        isactive = false;
        return nextnext != null ? nextnext : next;
    }

    public IItem getNode(String input, Properties env) {
        if (input.matches("\\d+")) {
            //input: one-based index
            return (IItem) getNodes(env).get(Integer.valueOf(input) - 1);
        } else {
            List<AItem<T>> childs = getNodes(env);
            input = input.toLowerCase();
            for (IItem i : childs) {
                if (i.getName().toLowerCase().startsWith(input)) {
                    return i;
                }
            }
            throw new IllegalArgumentException(input + " is not a known value!");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean add(IItem item) {
        item.setParent(this);
        return getNodes().add((AItem)item);
    }

    @Override
    public boolean remove(IItem item) {
        item.setParent(this);
        return getNodes().remove(item);
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IItem<T> next(InputStream in, PrintStream out, Properties env) {
        IItem<T> next;
        if (sequential) {
            if (++seqIndex < getNodes(env).size()) {
                next = (IItem) getNodes(env).get(seqIndex);
                //ask for all tree items
                if (next.getType() == Type.Container)
                    return next.react(this, null, in, out, env);
                else
                    return next;
            } else {
//                sequential = false;
//                seqIndex = -1;
                next = (IItem<T>) getParent().next(in, out, env);
            }
        } else {
            next = (IItem<T>) this;
        }
        return next;
    }

    @Commit
    protected void initDeserialization() {
        super.initDeserialization();
        //fill yourself as parent for all children
        if (getNodes() != null) {
            for (IItem n : getNodes()) {
                n.setParent(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription(Properties env, boolean full) {
        if (isactive || sequential) {
            if (sequential && hasFileDescription())
                return super.getDescription(env, full);
            List<AItem<T>> list = getNodes(env);
            StringBuilder buf = new StringBuilder(list.size() * 60);
            int i = 0;
            //evaluate key string length for formatted output
            int kl = 0;
            for (IItem<T> t : list) {
                kl = Math.max(kl, ((AItem)t).getName(-1, (char)-1).length());
            }
            kl++;
            int vwidth = Util.get(Terminal.KEY_WIDTH, 80) - (kl + 9);
            //print the child item list
            int s = String.valueOf(list.size()).length() + 1;
            for (AItem t : list) {
                buf.append(StringUtil.fixString(String.valueOf(++i), s, ' ', false)
                    + "."
                    + ((AItem)t).getName(kl, ' ') + POSTFIX_QUESTION
                        + (full && !t.getType().equals(Type.Container) ? t.getDescription(env, full) : StringUtil.toString(t.getValueText(), vwidth))
                    + "\n");
            }
            return buf.toString();
        } else {
            return getName(-1, (char)-1) + "\n";
        }
    }

}
