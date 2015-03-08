/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 24.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.bean.def.IConstraint;
import de.tsl2.nano.collection.CollectionUtil;
import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * the Container of items. if only one item is available, it should delegate the request directly to that item.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class Container<T> extends AItem<List<T>> implements IContainer<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3656677742608173033L;

    /** child nodes */
    @ElementList(type = AItem.class, inline = true, entry = "item", required = false)
    List<IItem<T>> nodes;

    transient private boolean isactive;
    /** if result is a collection */
    boolean multiple = true;
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
        value = new ArrayList<T>();
        type = Type.Container;
        prefix.setCharAt(PREFIX, '+');
    }

    public Container(String name, String description) {
        this(name, null, new ArrayList<T>(), description);
    }

    /**
     * constructor
     * 
     * @param name
     * @param constraints
     * @param type
     * @param value
     */
    public Container(String name, IConstraint<List<T>> constraints, List<T> selected, String description) {
        super(name, constraints, Type.Container, selected, description);
        nodes = new ArrayList<IItem<T>>();
        prefix.setCharAt(PREFIX, '+');
    }

    @Override
    public List<IItem<T>> getNodes() {
        return nodes;
    }

    /**
     * filters all child nodes where the condition return false
     * 
     * @param context application context
     * @return filtered child nodes
     */
    public List<IItem<T>> getFilteredNodes(final Properties context) {
        return CollectionUtil.getFiltering(getNodes(), new IPredicate<IItem<T>>() {
            @Override
            public boolean eval(IItem<T> arg0) {
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
        if (selected.getType() == Type.Container && ((Container) selected).getFilteredNodes(env).size() == 1) {
            //if only one tree child is available, delegate directly to that item
            return selected.react(this, "1", in, out, env);
        } else
            return selected;
    }

    @Override
    protected void initConstraints(IConstraint<List<T>> constraints) {
    }

    @Override
    @Transient
    //WORKAROUND: unable to save list of values through simple-xml
//    @ElementList(type=Object.class, inline = true, entry = "value", required = false)
    public List<T> getValue() {
        return super.getValue();
    }

    @Override
    @Transient
    //WORKAROUND: unable to save list of values through simple-xml
//    @ElementList(type=Object.class, inline = true, entry = "value", required = false)
    public void setValue(List<T> value) {
        super.setValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue(int i) {
        return value.get(i);
    }

    @Override
    public String ask(Properties env) {
        isactive = true;
        List<IItem<T>> children = getFilteredNodes(env);
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
        List<IItem<T>> filteredNodes = getFilteredNodes(env);
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
            nextnext = next.react(this, null, in, out, env);
        } else if (next.getType() == Type.Container && ((Container) next).getFilteredNodes(env).size() == 1) {
            //if only one tree child is available, delegate directly to that item
            nextnext = next.react(this, "1", in, out, env);
        }
        //assign the new result
        if (multiple)
            getValue().add((T) next.getValue());
        else
            getValue().set(0, (T) next.getValue());
        env.put(getName(), getValue());
        isactive = false;
        return nextnext != null ? nextnext : next;
    }

    public IItem getNode(String input, Properties env) {
        if (input.matches("\\d+")) {
            //input: one-based index
            return (IItem) getFilteredNodes(env).get(Integer.valueOf(input) - 1);
        } else {
            List<IItem<T>> childs = getFilteredNodes(env);
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
        return getNodes().add(item);
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
            if (++seqIndex < getFilteredNodes(env).size()) {
                next = (IItem) getFilteredNodes(env).get(seqIndex);
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
        for (IItem n : getNodes()) {
            n.setParent(this);
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
            List<IItem<T>> list = getFilteredNodes(env);
            StringBuilder buf = new StringBuilder(list.size() * 60);
            int i = 0;
            //evaluate key string length for formatted output
            int kl = 0;
            for (IItem<T> t : list) {
                kl = Math.max(kl, t.getPresentationPrefix().length() + translate(t.getName()).length());
            }
            kl++;
            //print the child item list
            int s = String.valueOf(list.size()).length() + 1;
            for (IItem t : list) {
                buf.append(StringUtil.fixString(String.valueOf(++i), s, ' ', false)
                    + "."
                    + StringUtil.fixString(t.getPresentationPrefix() + translate(t.getName()), kl, ' ', true)
                    + (t.getType().equals(Type.Container) ? "" : POSTFIX_QUESTION
                        + (full ? t.getDescription(env, full) : StringUtil.toString(t.getValue(), 50)))
                    + "\n");
            }
            return buf.toString();
        } else {
            return getPresentationPrefix() + name + "\n";
        }
    }

}
