/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 21.11.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.repeat.impl;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import de.tsl2.nano.incubation.repeat.IChange;
import de.tsl2.nano.incubation.repeat.ICommand;
import de.tsl2.nano.incubation.repeat.ICommandManager;

/**
 * simple implementation of undo/redo and macro mechanism. you have to implement one simple method
 * {@link ACommand#runWith(IChange...)} to have undo/redo and macros.
 * <p/>
 * call {@link #doIt(ICommand...)} for each command that should be executed and perhaps un-done through {@link #undo()}.
 * only if at least one {@link #undo()} was called, a {@link #redo()} is possible.
 * <p/>
 * Example, creating, deleting a bean and changing its attributes through reflection.
 * 
 * <pre>
 * &#064;Override
 * public void runWith(IChange... changes) {
 *     for (int i = 0; i &lt; changes.length; i++) {
 *         if (changes[i].getItem() != null) {
 *             //while the context can change on item=null, we have to do it inside the loop
 *             Class&lt;?&gt; type = getContext().getClass();
 *             BeanClass&lt;Serializable&gt; b = (BeanClass&lt;Serializable&gt;) BeanClass.getBeanClass(type);
 *             b.setValue(getContext(), (String) changes[i].getItem(), changes[i].getNew());
 *         } else {
 *             setContext((Serializable) changes[i].getNew());
 *         }
 *     }
 * }
 * </pre>
 * 
 * Macros are available through {@link MacroRecorder}. Call {@link #getRecorder()}
 * {@link MacroRecorder#record(String, ICommand...)} with an id (no commands) to start the recording. If recording
 * should be finished, call the stop() method and the play(CONTEXT) method to do a replay on the given context object.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CommandManager implements ICommandManager {
    Deque<ICommand<?>> done;
    Deque<ICommand<?>> undone;
    MacroRecorder recorder;

    /**
     * constructor
     */
    public CommandManager() {
        this(Integer.MAX_VALUE);
    }

    /**
     * constructor
     * 
     * @param capacity
     */
    public CommandManager(int capacity) {
        super();
        done = new LinkedBlockingDeque<ICommand<?>>(capacity);
        undone = new LinkedBlockingDeque<ICommand<?>>(capacity);
        recorder = new MacroRecorder(capacity);
    }

    @Override
    public boolean doIt(ICommand<?>... cmd) {
        Boolean hasDone = null;
        for (int i = 0; i < cmd.length; i++) {
            cmd[i].run();
            hasDone = (hasDone == null ? true : hasDone) & done.add(cmd[i]);
        }
        if (recorder.isRecording())
            recorder.record(null, cmd);
        return hasDone != null && hasDone;
    }

    /**
     * getDone
     * @return all done commands (through calling {@link #doIt(ICommand...)}).
     */
    Deque<ICommand<?>> getDone() {
        return done;
    }
    
    @Override
    public ICommand<?> undo() {
        ICommand<?> cmd = done.poll();
        if (cmd != null) {
            undone.add(cmd);
            cmd.undo();
        }
        return cmd;
    }

    @Override
    public ICommand<?> redo() {
        ICommand<?> cmd = undone.poll();
        if (cmd != null) {
            doIt(cmd);
        }
        return cmd;
    }

    @Override
    public boolean canUndo() {
        return done.size() > 0;
    }

    @Override
    public boolean canRedo() {
        return undone.size() > 0;
    }

    /**
     * getRecorder
     * 
     * @return
     */
    public MacroRecorder getRecorder() {
        return recorder;
    }
}
