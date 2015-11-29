/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 29.11.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.tree;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import de.tsl2.nano.core.IPredicate;
import de.tsl2.nano.core.ITransformer;
import de.tsl2.nano.core.util.StringUtil;

/**
 * uni-directed simple tree providing walking methods. the extended hashmap holds the childs.
 * 
 * @param <C> connection descriptor
 * @param <T> node type
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Tree<C, T> extends TreeMap<C, Tree<C, T>> {
    /** serialVersionUID */
    private static final long serialVersionUID = 2185167946970170425L;

    Tree<C, T> parent;
    protected T node;
    
    /**
     * constructor
     */
    protected Tree() {
        super();
    }

    /**
     * constructor
     * 
     * @param node
     */
    public Tree(T node, Tree<C, T> parent) {
        super();
        init(node, parent);
    }

    /**
     * constructor
     * 
     * @param comparator
     */
    public Tree(T node, Tree<C, T> parent, Comparator<? super C> comparator) {
        super(comparator);
        init(node, parent);
    }

    /**
     * constructor
     * 
     * @param m
     */
    public Tree(T node, Tree<C, T> parent, Map<? extends C, ? extends Tree<C, T>> m) {
        super(m);
        init(node, parent);
    }

    /**
     * constructor
     * 
     * @param m
     */
    public Tree(T node, Tree<C, T> parent, SortedMap<C, ? extends Tree<C, T>> m) {
        super(m);
        init(node, parent);
    }

    /**
     * init
     * @param node
     * @param parent
     */
    protected void init(T node, Tree<C, T> parent) {
        this.node = node;
        this.parent = parent;
    }

    /**
     * getNode
     * 
     * @return tree node
     */
    public T getNode() {
        return node;
    }

    /**
     * isLeaf
     * 
     * @return true, if this tree node doesn't have childs
     */
    public boolean isLeaf() {
        return size() == 0;
    }

    public Tree<C, T> getParent() {
        return parent;
    }
    
    public Tree<C, T> getRoot() {
        return parent != null ? parent.getRoot() : this;
    }
    
    public void add(C connection, T child) {
        put(connection, new Tree<C, T>(child, this));
    }
    
    /**
     * uses {@link #collectTree(IPredicate)} to find the given node
     * @param node node to find
     * @return tree node or null
     */
    public Tree<C, T> getNode(final T node) {
        List<Tree<C, T>> list = collectTree(new IPredicate<Tree<C, T>>() {
            @Override
            public boolean eval(Tree<C, T> n) {
                return node.equals(n);
            }
        });
        return list.size() > 0 ? list.iterator().next() : null;
    }

    /**
     * @delegates to {@link #collect(IPredicate, List)}.
     */
    public List<T> collect(IPredicate<T> condition) {
        return collect(condition, new LinkedList<T>());
    }

    /**
     * TODO: test
     * <p/>
     * collects all tree nodes that satisfy condition.
     * 
     * @param root INode implementation holding the root object.
     * @param condition collecting condition
     * @return all nodes that satisfy the given condition.
     */
    public List<T> collect(IPredicate<T> condition, List<T> c) {
        if (condition.eval(node))
            c.add(node);
        for (Tree<C, T> n : values()) {
            n.collect(condition, c);
        }
        return c;
    }

    /**
     * @delegates to {@link #collect(IPredicate, List)}.
     */
    public List<Tree<C, T>> collectTree(IPredicate<Tree<C, T>> condition) {
        return collectTree(condition, new LinkedList<Tree<C, T>>());
    }

    /**
     * TODO: test
     * <p/>
     * collects all tree nodes that satisfy condition.
     * 
     * @param root INode implementation holding the root object.
     * @param condition collecting condition
     * @return all nodes that satisfy the given condition.
     */
    public List<Tree<C, T>> collectTree(IPredicate<Tree<C, T>> condition, List<Tree<C, T>> c) {
        if (condition.eval(this))
            c.add(this);
        for (Tree<C, T> n : values()) {
            n.collectTree(condition, c);
        }
        return c;
    }

    /**
     * TODO: test
     * <p/>
     * walks through the tree nodes and calls the transformer on each node.
     * 
     * @param transformer transformer
     */
    public void transform(ITransformer<T, T> transformer) {
        transformer.transform(node);
        for (Tree<C, T> n : values()) {
            n.transform(transformer);
        }
    }

    /**
     * TODO: test
     * <p/>
     * walks through the tree nodes and calls the transformer on each node.
     * 
     * @param transformer transformer
     */
    protected void transformTree(ITransformer<Tree<C, T>, Tree<C, T>> transformer) {
        transformer.transform(this);
        for (Tree<C, T> n : values()) {
            n.transformTree(transformer);
        }
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }

    public static Tree<String, String> fromString(Scanner scanner) {
        TreeMap<String,Tree<String,String>> treeMap = new TreeMap<String, Tree<String, String>>();
        Tree<String, String> node, parent = null;
        String n, child, connection, sep = "->";
        for (String s = scanner.nextLine(); scanner.hasNext();) {
            n = StringUtil.substring(s, null, sep).trim();
            node = treeMap.get(n);
            if (node == null)
                node = new Tree<String, String>(n, parent);
            connection = StringUtil.substring(s, sep, ";");
            child = StringUtil.extract(connection, "\\w+");
            connection = StringUtil.substring(connection, child, null);
            node.put(connection, new Tree<String, String>(child, node));
            parent = node;
        }
        return treeMap.isEmpty() ? null : treeMap.firstEntry().getValue().getRoot();
    }

    /**
     * creates a graphviz like string.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        transformTree(new ITransformer<Tree<C, T>, Tree<C, T>>() {
            @Override
            public Tree<C, T> transform(Tree<C, T> t) {
                for (java.util.Map.Entry<C, Tree<C, T>> e : entrySet()) {
                    buf.append(t.node + " -> " + e.getValue().node + ";" + "/*" + e.getKey() + "*/");
                }
                return null;
            }
        });
        return buf.toString();
    }
}
