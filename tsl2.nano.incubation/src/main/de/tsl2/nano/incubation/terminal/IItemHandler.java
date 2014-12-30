/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 13.12.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.incubation.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

/**
 * is able to do an interaction between user input and an print stream.
 * 
 * @author Tom
 * @version $Revision$
 */
public interface IItemHandler extends Runnable {
    Object getUserInterface();

    /** prints user info screen. should be able to do paging. */
    void printScreen(IItem item, PrintStream out);

    /**
     * waits for user input and calls {@link #printScreen(IItem, PrintStream)} to show informations. all inputs and
     * result will be stored inside the environment 'env'.
     */
    void serve(IItem item, InputStream in, PrintStream out, Properties env);
}
