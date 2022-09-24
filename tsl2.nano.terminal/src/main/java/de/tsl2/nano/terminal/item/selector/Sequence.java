/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 19.08.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.terminal.item.selector;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.collection.Entry;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.core.util.ListSet;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.terminal.IItem;
import de.tsl2.nano.terminal.SIShell;
import de.tsl2.nano.terminal.TextTerminal;
import de.tsl2.nano.terminal.item.AItem;
import de.tsl2.nano.terminal.item.Action;

/**
 * Starts an {@link Action} for all items of a {@link Selector} in a daemon thread. implementing the {@link Runnable}
 * interface tells SiShell to do a printscreen every 5 sec.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Sequence<T, R> extends Selector<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 1977032828721824363L;

    @Element(required = false)
    Action<R> doAction;
    @Element(required = false)
    Selector<T> sequence;
    //TODO: use concurrent list
    transient List<Entry<T, R>> result;
    transient Properties context;

    public Sequence() {
    }

    public Sequence(Action<R> action, Selector<T> selector, String description) {
        super(action.getName(), description);
        this.doAction = action;
        sequence = selector;
        initResult();
    }

    /**
     * starts the defined action on each item in the {@link #sequence} - in a daemon thread
     */ 
    @Override
    public IItem react(IItem caller, String input, final InputStream in, final PrintStream out, final Properties env) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (AItem<T> item : sequence.getNodes(context)) {
                    context.put(sequence.getName(), item.getValue());
                    System.out.print(sequence + " running...");
                    Object r = doAction.run(context);
                    result.add(new Entry(item, r));
                    SIShell.printScreen(getDescription(env, false), in, out, ask(env),
                        Util.get(SIShell.KEY_WIDTH, TextTerminal.SCREEN_WIDTH),
                        Util.get(SIShell.KEY_HEIGHT, TextTerminal.SCREEN_HEIGHT), TextTerminal.Frame.BAR, false, false);
                }
            }
        };
        ConcurrentUtil.startDaemon(getName(), runnable);
        return super.react(caller, input, in, out, env);
    }

    @Override
    public String ask(Properties env) {
        return "Please enter <stop> to stop the sequence!\n";
    }

    /**
     * initResult
     */
    protected void initResult() {
        //prefill the result map
        result = new ListSet<Entry<T, R>>();
        for (AItem<T> item : sequence.getNodes(new Properties())) {
            result.add(new Entry(item, null));
        }
    }

    @Override
    protected List<?> createItems(Map props) {
        return result;
    }

    public void stop() {
        ConcurrentUtil.stopOrInterrupt(getName());
    }

    @Commit
    protected void initDeserialization() {
        initResult();
        context = new Properties();
    }

}
