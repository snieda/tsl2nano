/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.09.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.core;

import java.util.LinkedHashMap;
import java.util.Map;

import de.tsl2.nano.core.util.StringUtil;

/**
 * Usable to structure e.g. command line arguments.
 * <p/>
 * Standard-Format:<br/>
 * - key-value pairs separated by '='<br/>
 * - options starting with '-'<br/>
 * - a manual map can be given to validate the arguments and print a help message.<br/>
 * 
 * @author ts
 * @version $Revision$
 */
public class Argumentator {
    /** args of a main call */
    Map<String, Object> argMap;

    //TODO: use dynamic syntax
    String syntax = "-+/=?";

    /**
     * constructor
     * 
     * @param name program name
     * @param man manual
     * @param args program arguments
     */
    public Argumentator(String name, Map<String, String> man, String... args) {
        super();
        defineArgs(man, args);
    }

    /**
     * structures and stores the given args to a map
     * @param man
     * @param args
     */
    private void defineArgs(Map<String, String> man, String[] args) {
        argMap = new LinkedHashMap<String, Object>();
        String n;
        Object v;
        String p[];
        boolean isOption;
        for (int i = 0; i < args.length; i++) {
            isOption = args[i].startsWith("-");
            String arg = isOption ? args[i].substring(1) : args[i];
            p = arg.split("=");
            if (isOption) {
                n = p[0];
                v = p.length > 1 ? p[1] : Boolean.TRUE;
            } else {
                if (p.length == 1) {
                    n = String.valueOf(i);
                    v = p[0];
                } else {
                    n = p[0];
                    v = p[1];
                }
            }
            check(n, v, man);
            argMap.put(n, v);
        }
    }

    /**
     * checks a given value against the manual
     * @param name
     * @param value
     * @param man
     */
    private void check(String name, Object value, Map<String, String> man) {
        if (man == null)
            return;
        //TODO: check args against manual 'man'
        String rule = null;
        if (false)
            throw new IllegalArgumentException(rule);
    }

    /**
     * hasOption
     * @param name
     * @return true, if given key was activate as option
     */
    public boolean hasOption(String name) {
        Object option = consume(name);
        return option instanceof Boolean && ((Boolean) option).booleanValue();
    }

    /**
     * isSet
     * @param name
     * @return true, if given key was set.
     */
    public boolean isSet(String name) {
        return consume(name) != null;
    }

    public Object consume(String name) {
        return argMap.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T consume(String name, T defaultValue) {
        T v = (T) consume(name);
        return v != null ? v : defaultValue;
    }

    /**
     * iterate through args
     * @return next argument value
     */
    public Object next() {
        return consume(argMap.keySet().iterator().next());
    }

    /**
     * printManual
     * @param man manual to be logged
     */
    public static void printManual(Map<String, String> man) {
        log(StringUtil.toFormattedString(man, -1, false));
    }

    private static void log(Object msg) {
        System.out.println(msg);
    }
}
