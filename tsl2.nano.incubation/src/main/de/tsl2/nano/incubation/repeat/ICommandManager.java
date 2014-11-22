/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 21.11.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.repeat;

/**
 * all you need for undo/redo and macros. this generic solution allows to write a small framework to be enhanced by an
 * application developer through {@link ICommand#runWith(IChange...)}.
 * <p/>
 * Each command holds its own own context to do changes (see {@link IChange}) on it. These changes can be {@link #undo()}ne
 * and then {@link #redo()}ne.
 * <p/>
 * To use macros, the implementation should implement the {@link IMacroManager}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public interface ICommandManager {
    /**
     * doIt
     * 
     * @param cmd command to execute
     * @return true, if command was executed and stored for undo.
     */
    boolean doIt(ICommand<?>... cmd);

    /**
     * undo a command that was done by {@link #doIt(ICommand)}.
     * 
     * @return command that was un-done.
     */
    ICommand<?> undo();

    /**
     * redo a command that was un-done.
     * 
     * @return command that was re-done
     */
    ICommand<?> redo();

    /**
     * canUndo
     * 
     * @return true, if at least one command was done.
     */
    boolean canUndo();

    /**
     * canRedo
     * 
     * @return true, if at least one command was un-done.
     */
    boolean canRedo();
}
