/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Feb 27, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.execution;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.Properties;

import de.tsl2.nano.core.execution.ICRunnable;

/**
 * Simple java main runner for {@link ICRunnable} implementations. Uses a {@link Properties} from file-load as arguments.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class Runner {
    public static final void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Please give at least the ICRunnable to start!");
            return;
        } else if (args.length < 2) {
            System.out.println("Please give a property-file name as second parameter!");
            return;
        }
        final Properties p = new Properties();
        p.load(new FileReader(new File(args[1])));
        for (int i = 0; i < args.length; i++) {
            p.setProperty("args" + i, args[i]);
        }
        final Class<ICRunnable<Properties>> rclass = (Class<ICRunnable<Properties>>) Class.forName(args[0]);
        final Method runMethod = rclass.getMethod("run", new Class[] { Object.class, Object[].class });

        log("starting " + runMethod + " with arguments: " + p);
        final Object result = runMethod.invoke(null, new Object[] { p });
        log("finished " + runMethod + " with result: " + result);
    }

    protected static void log(String text) {
        System.out.println(text);
    }

    public static Class[] methodArgs(Object[] args) {
        final Class[] margs = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            margs[i] = args[i].getClass();
        }
        return margs;
    }
}
