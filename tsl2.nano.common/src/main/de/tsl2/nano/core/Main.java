/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 27.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core;

import java.util.Map;


/**
 * base class for application main. provides a generic main-method to instantiate an extension of {@link Main}. please
 * overwrite the main method.<br/>
 * Example:
 * 
 * <pre>
 * public static void main(String[] args) {
 *     startApplication(NanoH5.class, MapUtil.asMap(0, &quot;http.port&quot;), args);
 * }
 * </pre>
 * 
 * TODO: use {@link Argumentator} instead of {@link #setEnvironmentArguments(String[], Map)}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class Main {

    /**
     * main entry
     * 
     * @param args launching args
     */
    public static void main(String[] args) {
        startApplication(Main.class, null, args);
    }

    /**
     * startApplication
     * 
     * @param nanoH5
     * @param args
     */
    public static void startApplication(Class<?> application, Map<Integer, String> argMapping, String[] args) {
        try {
            setEnvironmentArguments(args, argMapping);
            //use reflection on construction to handle exception inside this method
            Main main = (Main) application.getConstructor(new Class[0]).newInstance(new Object[0]);
            main.start();
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    /**
     * provides all args mapped by argMapping to the system environment properties.
     * 
     * @param args main arguments, given by {@link #main(String[])}.
     * @param argMapping application specific mapping of args. each name is an argument name for an argument at
     *            key-position.
     */
    protected static void setEnvironmentArguments(String[] args, Map<Integer, String> argMapping) {
        if (argMapping != null && argMapping.size() > 0) {
            for (int i = 0; i < args.length; i++) {
                String argName = argMapping.get(i);
                if (argName != null)
                    Environment.setProperty(argName, args[i]);
            }
        }
    }

    public void start() {
        throw new UnsupportedOperationException();
    }
}