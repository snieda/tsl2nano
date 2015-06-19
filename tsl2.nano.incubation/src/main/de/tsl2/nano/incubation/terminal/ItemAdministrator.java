/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.06.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.execution.AntRunner;
import de.tsl2.nano.execution.SystemUtil;
import de.tsl2.nano.incubation.terminal.item.AItem;
import de.tsl2.nano.incubation.terminal.item.Action;
import de.tsl2.nano.incubation.terminal.item.Container;
import de.tsl2.nano.incubation.terminal.item.Input;
import de.tsl2.nano.incubation.terminal.item.MainAction;
import de.tsl2.nano.incubation.terminal.item.Option;
import de.tsl2.nano.incubation.terminal.item.selector.DirSelector;
import de.tsl2.nano.incubation.terminal.item.selector.FileSelector;
import de.tsl2.nano.incubation.terminal.item.selector.PropertySelector;
import de.tsl2.nano.incubation.vnet.workflow.Condition;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * item administration shell
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ItemAdministrator<T> extends Container<T> {
    /** mostly needed to define an action */
    private static final String ARGS = "args[]";
    /** the items type (full class name) */
    private static final String TYPE = "type";

    private static final long serialVersionUID = 5127419882040627794L;

    transient IItem parent;
    transient Terminal terminal;
    /** item as descriptor for new items */
    transient Properties item;
    private Action<T> addingAction;
    private Action<T> removingAction;
    private Action<T> changingAction;
    private Action<T> terminalAction;
    private static int count = 0;

    static Map<String, String[]> itemTypes;
    static {
        String file = "admin.properties";
        Properties adminProperties =
            FileUtil.hasResource(file) ? FileUtil.loadProperties(file, null) : new Properties();
        itemTypes = new HashMap<String, String[]>();
        if (adminProperties.isEmpty()) {
            //add special parameter to available items
            itemTypes.put("container", new String[] { Container.class.getName(), "multiple", "sequential" });
            itemTypes.put("option", new String[] { Option.class.getName() });
            itemTypes.put("input", new String[] { Input.class.getName() });
            itemTypes.put("action", new String[] { Action.class.getName(), "mainClass", "method", ARGS });
            itemTypes.put("mainaction", new String[] { MainAction.class.getName(), "mainClass", ARGS });
            itemTypes.put("shell", new String[] { Action.class.getName(), "mainClass=" + SystemUtil.class.getName(),
                "method=execute", ARGS });
            itemTypes.put("ant", new String[] { Action.class.getName(), "mainClass=" + AntRunner.class.getName(),
                "method=runTask", ARGS /*"task", "properties", "fileset"*/});
            itemTypes.put("file", new String[] { FileSelector.class.getName(), "roots", "include" });
            itemTypes.put("dir", new String[] { DirSelector.class.getName(), "roots", "include" });
            itemTypes.put("properties", new String[] { PropertySelector.class.getName() });
            itemTypes.put("csv", new String[] { PropertySelector.class.getName(), "csv", "pattern" });

            Properties p = new Properties();
            for (String k : itemTypes.keySet()) {
                p.put(k, StringUtil.concat(", ".toCharArray(), (Object[]) itemTypes.get(k)));
            }
            FileUtil.saveProperties(file, p);
        } else {
            String k, v;
            for (Object key : adminProperties.keySet()) {
                k = (String) key;
                v = adminProperties.getProperty(k);
                itemTypes.put(k, v.split("\\,\\s*"));
            }
        }
    }

    /**
     * constructor
     */
    public ItemAdministrator(Terminal terminal, Container<T> toAdmin) {
        init(terminal, toAdmin);
    }

    protected void init(final Terminal terminal, Container<T> toAdmin) {
        this.terminal = terminal;
        this.parent = toAdmin;
        this.nodes = (List<AItem<T>>) new PrivateAccessor(toAdmin).member("nodes");
        item = new Properties();
        name = "Admin: " + toAdmin.getName();
        setDescription("Provides administration of item " + toAdmin.getName());

        //action to add items
        addingAction = new Action<T>(this, "addItem") {
            @Override
            public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                askItem(in, out, env, this, item, TYPE, "name", "value", "description", "condition", "style");
                return super.react(caller, input, in, out, env);
            }

        };
        addingAction.setParent(this);
        nodes.add(addingAction);

        //action to change items
        changingAction = new Action<T>(this, "changeItem") {
            @Override
            public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                //which item - show some infos
                out.print("select item to change: ");
                item.put("index", nextLine(in, out));
                out.println(StringUtil.toFormattedString(new PrivateAccessor(nodes.get(getSelectedIndex())).members(), -1));
                
                askItem(in, out, env, this, item, "name", "value", "description", "condition", "style");
                return super.react(caller, input, in, out, env);
            }

        };
        changingAction.setParent(this);
        nodes.add(changingAction);

        //action to remove items
        removingAction = new Action<T>(this, "removeItem") {
            @Override
            public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                out.print("select item to remove: ");
                item.put("index", nextLine(in, out));
                env.put("instance", this);
                return super.react(caller, input, in, out, env);
            }
        };
        removingAction.setParent(this);
        nodes.add(removingAction);

        //action to change the terminal
        terminalAction = new Action<T>(this, "changeTerminal") {
            @Override
            public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                out.println("\nchanging terminal:\n" + terminal);
                askItem(in, out, env, this, item, Terminal.presentalMembers());
                return super.react(caller, input, in, out, env);
            }

        };
        terminalAction.setParent(this);
        nodes.add(terminalAction);

        setDescription("the type should be one of: [container, input, option] or:\n"
            + StringUtil.toFormattedString(itemTypes, -1, true));
    }

    /**
     * asks for the given item attributes like name, value, description etc.
     * 
     * @param in user input stream
     * @param out user information print stream
     * @param env action context to add this instance to.
     */
    public static void askItem(InputStream in,
            PrintStream out,
            Properties env,
            Object instance,
            Properties item,
            String... itemAttributes) {
        //first, the standard attributes
        for (int i = 0; i < itemAttributes.length; i++) {
            out.print(itemAttributes[i] + ": ");
            item.put(itemAttributes[i], Terminal.nextLine(in, out));
        }

        //some defaults and instances
        boolean hasType = item.containsKey(TYPE);
        if (!Util.isEmpty(item.get("condition")))
            item.put("condition", new Condition(item.getProperty("condition")));
        //now, the type specific attributes
        if (hasType) {
            String type = item.getProperty(TYPE);
            if (Util.isEmpty(type))
                type = "input";
            if (Util.isEmpty(item.get("name")))
                item.put("name", type + "-" + ++count);

            String description[] = itemTypes.get(type.toLowerCase());
            if (description != null) {
                item.put(TYPE, description[0]);
                for (int i = 1; i < description.length; i++) {
                    if (!description[i].equals(ARGS)) {
                        if (!description[i].contains("=")) {
                            out.print(description[i] + ": ");
                            item.put(description[i], Terminal.nextLine(in, out));
                        } else {
                            String kv[] = description[i].split("=");
                            item.put(kv[0], kv[1]);
                        }
                    } else {
                        //method arguments
                        List<String> argNames = new LinkedList<String>();
                        String arg;
                        while (true) {
                            out.print("argument name " + argNames.size() + ": ");
                            if (Util.isEmpty(arg = Terminal.nextLine(in, out)))
                                break;
                            argNames.add(arg);
                        }
                        item.put("argNames", argNames.toArray(new String[0]));
                    }
                }
            } else if (hasType) {
                out.println("The type should be one of:\n" + StringUtil.toString(itemTypes.keySet(), -1));
                type = StringUtil.toFirstUpper(type);
                String cls = type.contains(".") ? type : AItem.class.getPackage().getName() + "." + type;
                out.println("\nTrying type: " + cls);
                out.println("<<<please hit enter to confirm>>>");
                Terminal.nextLine(in, out);
                item.put(TYPE, cls);
            }
        }
        env.put("instance", instance);
    }

    /**
     * clean, to be called, if administration will be left.
     */
    public void clean() {
        nodes.remove(addingAction);
        nodes.remove(removingAction);
        nodes.remove(changingAction);
        nodes.remove(terminalAction);
    }

    /**
     * adds a new item
     */
    public void addItem() {
        item.put("parent", parent);
        add(nodes, item);
    }

    int getSelectedIndex() {
        return Integer.valueOf(item.getProperty("index")) - 1;
    }

    /**
     * add a new item
     * 
     * @param nodes node list to add the new item to
     * @param item to be added
     */
    public static <T> void add(List<AItem<T>> nodes, Properties item) {
        AItem instance = BeanClass.createInstance(item.getProperty(TYPE));
        item.remove(TYPE);
        nodes.add(PrivateAccessor.assign(instance, item, false));
        item.clear();
    }

    public void removeItem() {
        nodes.remove(getSelectedIndex());
        item.clear();
    }

    public void changeItem() {
        IItem selectedItem = nodes.get(getSelectedIndex());
        item.remove(TYPE);
        item.remove("index");
        PrivateAccessor.assign(selectedItem, item, false);
        item.clear();
    }

    public void changeTerminal() {
        PrivateAccessor.assign(terminal, item, false);
        item.clear();
    }

    @Override
    public String getDescription(Properties env, boolean full) {
        if (full)
            return "Provides administration of item " + parent.getName() + "\n"
                + StringUtil.toFormattedString(itemTypes, -1, true);
        return super.getDescription(env, full);
    }
}
