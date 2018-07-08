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

import java.util.Map;

/**
 * Simple {@link Tree} using a simple counter as connection info
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class STree<T> extends Tree<Integer, T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 6791740823737461396L;

    /** key-creation strategy. see {@link #createKey(Object)} */
    TreeOrderStrategy strategy = TreeOrderStrategy.HASHCODE;

    /**
     * constructor
     */
    protected STree() {
    }

    /**
     * constructor
     * @param node
     * @param parent
     * @param m
     */
    public STree(T node, Tree<Integer, T> parent, Map<? extends Integer, ? extends Tree<Integer, T>> m) {
        super(node, parent, m);
    }

    public STree(T node, Tree<Integer, T> parent, T...children) {
        this(node, parent, TreeOrderStrategy.HASHCODE, children);
    }
    
    /**
     * constructor
     * @param node
     * @param parent
     * @param m
     */
    public STree(T node, Tree<Integer, T> parent, TreeOrderStrategy strategy, T...children) {
        super(node, parent);
        this.strategy = strategy;
        add(children);
    }

    /**
     * constructor
     * @param node
     * @param parent
     */
    public STree(T node, Tree<Integer, T> parent) {
        super(node, parent);
    }

    public void add(T... childs) {
        for (int i = 0; i < childs.length; i++) {
            add(createKey(childs[i]), childs[i]);
        }
    }
    @Override
    public void add(Integer connection, T child) {
        put(connection, new STree<T>(child, this));
    }
    /**
     * creates a new connection-info as map-key for the map of child-trees. see {@link TreeOrderStrategy} for several creation-strategies.
     * @param node child node
     * @return new key for child element
     */
    protected int createKey(T node) {
      switch(strategy) {
        case SEQUENCE:
          return size();
        case HASHCODE:
          return node.hashCode();
        default:
          throw new IllegalStateException("strategy must be one of: SEQUENCE, HASHCODE");
      }
    }
  }

  /**
  * The connection info between a node and its children can be evaluated through different strategies.
  * @author Thomas Schneider
  *
  */
  enum TreeOrderStrategy {
    /** increasing number on each addition */
    SEQUENCE,
    /** uses the hashcode of the node */
    HASHCODE;
  }