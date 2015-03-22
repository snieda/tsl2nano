/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 17.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * The tree selector is a {@link Selector} that provides walking through a tree to select exactly one node.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public abstract class TreeSelector<T> extends Selector<T> {
    /** roots */
    @ElementList(entry="root", inline=true)
    List<T> roots;
    @Element(required=false)
    String include;

    /**
     * constructor
     */
    public TreeSelector() {
        super();
    }

    public TreeSelector(String name, String description) {
        super(name, description);
    }

    @Override
    public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
//        if (Util.isEmpty(input))
            return super.react(caller, input, in, out, env);
//        else {
//            IItem next = getNode(input, env);
//            roots = Arrays.asList(value);
//            nodes = null;
//            return super.react(caller, input, in, out, env);
//        }
    }

    @Override
    public String toString() {
        return super.toString() + " [roots: " + StringUtil.toString(roots, 30) + ", include: " + include + "]";
    }
}
