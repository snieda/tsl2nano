/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 27.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.terminal;

import de.tsl2.nano.core.serialize.XmlUtil;
import de.tsl2.nano.terminal.IItem.Type;
import de.tsl2.nano.terminal.item.Container;
import de.tsl2.nano.terminal.item.Input;
import de.tsl2.nano.terminal.item.Option;


/**
 * UNDER CONSTRUCTION
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class TerminalAdmin extends SIShell {
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
        s.add(new Input("name", null, filename, "SIShell name"));
        s.add(new Input("width", null, TextTerminal.SCREEN_WIDTH, "SIShell width"));
        s.add(new Input("height", null, TextTerminal.SCREEN_HEIGHT, "SIShell height"));
        s.add(new Input("style", null, TextTerminal.Frame.BAR, "SIShell style"));
        
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
    protected void save(boolean saveConfiguration) {
        if (saveConfiguration)
            XmlUtil.saveXml(name, new SIShell(items));
    }
}
