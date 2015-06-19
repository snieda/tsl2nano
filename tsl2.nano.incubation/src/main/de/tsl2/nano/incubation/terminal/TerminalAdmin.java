/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 27.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.incubation.terminal.IItem.Type;
import de.tsl2.nano.incubation.terminal.item.Container;
import de.tsl2.nano.incubation.terminal.item.Input;
import de.tsl2.nano.incubation.terminal.item.Option;


/**
 * UNDER CONSTRUCTION
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class TerminalAdmin extends Terminal {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static final String ADMIN = "admin";

    IContainer items;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static TerminalAdmin create(String filename) {
        final TerminalAdmin t = new TerminalAdmin();
        final Container s = new Container(filename, "edit terminal");
        t.root = s;
        t.items = new Container(filename, "MyTerminal");
        s.add(new Input("name", null, filename, "Terminal name"));
        s.add(new Input("width", null, TextTerminal.SCREEN_WIDTH, "Terminal width"));
        s.add(new Input("height", null, TextTerminal.SCREEN_HEIGHT, "Terminal height"));
        s.add(new Input("style", null, TextTerminal.BLOCK_BAR, "Terminal style"));
        
        IContainer creator;
        s.add(creator = new Container("items", "terminal items"));
        creator.add(new Input("name", null, "<undefined>", "Item name"));
        creator.add(new Input("type", null, Type.Input, "Item type"));
        creator.add(new Input("default", null, "", "Items default value"));
//        creator.add(new Action("create", null, new IRunnable<IItem, Properties>() {
//            @Override
//            public IItem run(Properties context, Object... extArgs) {
//                IItem item = createItem((String)context.get("name"), (Type)context.get("type"), context.get("default"));
//                t.items.add(item);
//                return t.items;
//            }
//        }, "create the new item"));
        return t;
    }

    protected static IItem createItem(IContainer parent, String name, Type type, Object defaultValue) {
        switch (type) {
        case Option:
            return new Option(parent, name, null, defaultValue, "");
        case Input:
            return new Input(name, null, defaultValue, "");
        case Action:
//            return new Action(name, null, defaultValue, "");
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected void save() {
        XmlUtil.saveXml(name, new Terminal(items));
    }
}
