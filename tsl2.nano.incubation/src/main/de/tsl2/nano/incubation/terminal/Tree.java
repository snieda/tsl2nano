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
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class Tree<T> extends AItem<List<T>> implements ITree<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3656677742608173033L;

    @ElementList(type = AItem.class, inline = true, entry = "item", required = false)
    List<IItem<T>> nodes;

    transient private boolean isactive;
    boolean multiple = true;

    /**
     * constructor
     */
    public Tree() {
        super();
        //WORKAROUND: unable to save list of values through simple-xml
        value = new ArrayList<T>();
        type = Type.Tree;
        prefix.setCharAt(PREFIX, '+');
    }

    public Tree(String name, String description) {
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
    public Tree(String name, IConstraint<List<T>> constraints, List<T> selected, String description) {
        super(name, constraints, Type.Tree, selected, description);
        nodes = new ArrayList<IItem<T>>();
        prefix.setCharAt(PREFIX, '+');
    }

    @Override
    public List<IItem<T>> getNodes() {
        return nodes;
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
    public String ask() {
        isactive = true;
        return "Please enter a number between 1 and " + getNodes().size() + POSTFIX_QUESTION;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
        if (Util.isEmpty(input))
            return getParent();
        IItem next = null;
        if (input.matches("\\d+")) {
            //input: one-based index
            next = (IItem) getNodes().get(Integer.valueOf(input) - 1);
        } else {
            List<IItem<T>> childs = getNodes();
            input = input.toLowerCase();
            for (IItem i : childs) {
                if (i.getName().toLowerCase().startsWith(input)) {
                    next = i;
                    break;
                }
            }
            if (next == null)
                throw new IllegalArgumentException(input + " is not a known value!");
        }
        IItem nextnext = null;
        if (!next.isEditable()) {
            nextnext = next.react(this, null, in, out, env);
        }
        if (multiple)
            getValue().add((T) next.getValue());
        else
            getValue().set(0, (T) next.getValue());
        env.put(getName(), getValue());
        isactive = false;
        return nextnext != null ? nextnext : next;
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

    @Commit
    private void initDeserialization() {
        //fill yourself as parent for all children
        for (IItem n : getNodes()) {
            n.setParent(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription(boolean full) {
        if (isactive) {
            List<IItem<T>> list = getNodes();
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
//                buf.append(" " + ++i + "." + t.toString());
                buf.append(StringUtil.fixString(String.valueOf(++i), s, ' ', false) + "."
                    + StringUtil.fixString(t.getPresentationPrefix() + translate(t.getName()), kl, ' ', true)
                    + (t.getType().equals(Type.Tree) ? "" : POSTFIX_QUESTION + (full ? t.getDescription(full) : StringUtil.toString(t.getValue(), 50)))
                    + "\n");
            }
            return buf.toString();
        } else {
            return getPresentationPrefix() + name + "\n";
        }
    }
}