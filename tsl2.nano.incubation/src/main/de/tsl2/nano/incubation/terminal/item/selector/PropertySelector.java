/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.06.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal.item.selector;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.incubation.terminal.IItem;
import de.tsl2.nano.incubation.terminal.ItemAdministrator;
import de.tsl2.nano.incubation.terminal.item.AItem;
import de.tsl2.nano.incubation.terminal.item.Action;
import de.tsl2.nano.incubation.terminal.item.Input;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@Default(value = DefaultType.FIELD, required = false)
public class PropertySelector<T> extends Selector<T> {
    private static final long serialVersionUID = 5127419882040627794L;

    /** TODO: how to serialize any map in simple-xml? */
    @ElementMap(required = false, inline = true, entry = "property", key = "key", attribute=true, keyType=String.class)
    Map properties;

    transient Properties item;
    
    /**
     * constructor
     */
    public PropertySelector() {
    }

    /**
     * constructor
     * 
     * @param name
     * @param description
     */
    public PropertySelector(String name, String description, Map properties) {
        super(name, description);
        this.properties = properties;
    }

    @Override
    protected List<?> createItems(Map context) {
        item = new Properties();
        
        if (properties != null) {
            for (Object key : properties.keySet()) {
                add(new Input((String) key, context.get(key), null));
            }
        } else {
            properties = new Properties();
        }
        return new LinkedList();
    }

    @Override
    protected void createLastNode(List<AItem<T>> nodes, Properties props) {
        super.createLastNode(nodes, props);
        //action to add actions
        Action<T> addingAction = new Action<T>(this, "addInput") {
            @Override
            public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                ItemAdministrator.askItem(in, out, env, this, item, "name", "value");
                return super.react(caller, input, in, out, env);
            }
        };
        addingAction.setParent(this);
        nodes.add(addingAction);
        
        //action to remove items
        Action<T> removingAction = new Action<T>(this, "removeInput") {
            @Override
            public IItem react(IItem caller, String input, InputStream in, PrintStream out, Properties env) {
                out.print("select item to remove: ");
                item.put("id", nextLine(in, out));
                env.put("instance", this);
                return super.react(caller, input, in, out, env);
            }
        };
        removingAction.setParent(this);
        nodes.add(removingAction);
    }

    public void addInput() {
        properties.put(item.getProperty("name"), item.get("value"));
        nodes.add(new Input<T>(item.getProperty("name"), (T) item.get("value"), null));
    }

    public void removeInput() {
        properties.remove(item.get("name"));
        nodes.remove(Integer.valueOf(item.getProperty("id")) - 1);
    }

    @Override
    public T getValue() {
        //the cast is not correct...but we ignore that at the moment
        return (T) properties;
    }
    
}
