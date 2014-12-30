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

import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.util.StringUtil;

/**
 * Usable to structure e.g. command line arguments. Use an instanceof Argumentator or simply call
 * {@link #defineArgs(Map, String[])} to get a property map through the given arguments.
 * <p/>
 * Standard-Format:<br/>
 * - key-value pairs separated by '='<br/>
 * - options starting with '-'<br/>
 * - a manual map can be given to validate the arguments and print a help message.<br/>
 * <p/>
 * If a manual map key starts with {@link #KEY_PREFIX_EXAMPLE} it will be handled as example - not as property! If a
 * manual map value starts with {@link #KEY_DUTY}, the {@link #check(PrintStream)} will use that property as duty. if
 * not set, a {@link #printManual()} will be done.
 * <p/>
 * Example 1:
 * 
 * <pre>
 *         Argumentator argumentator = new Argumentator("print", getManual(), args);
 *         if (argumentator.check(System.out)) {
 *             Properties am = argumentator.getArgMap();
 *             String source = am.getProperty("source");
 *             ...
 * </pre>
 * 
 * Example 2:
 * 
 * <pre>
 *      Properties p = Argumentator.defineArguments(getManual(), args);
 *      if (p.size() == 0) {
 *             Argumentator.printManual(name, getManual(), System.out, 80);
 *             return;
 *         }
 *         ...
 * </pre>
 * 
 * @author ts
 * @version $Revision$
 */
public class Argumentator {
    /** application name */
    String name;
    /** args of a main call */
    Properties argMap;
    int consumed = 0;
    
    //TODO: use dynamic syntax
    String syntax = "-+/=?";

    private Map<String, String> man;

    private static final String KEY_DUTY = "(!)";
    private static final String KEY_PREFIX_EXAMPLE = "example";

    /**
     * constructor
     * 
     * @param name program name
     * @param man manual
     * @param args program arguments
     */
    public Argumentator(String name, Map<String, String> man, String... args) {
        super();
        this.name = name;
        argMap = defineArgs(man, args);
        this.man = man;
    }

    /**
     * structures and stores the given args to a map
     * 
     * @param man
     * @param args
     */
    public static Properties defineArgs(Map<String, String> man, String[] args) {
        Properties argMap = new Properties();
        String n;
        Object v;
        String p[];
        boolean isOption;
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {//nulls will be ignored!
                isOption = args[i].startsWith("-");
                String arg = isOption ? args[i].substring(1) : args[i];
                p = arg.split("=");
                if (isOption) {
                    n = p[0];
                    v = p.length > 1 ? p[1] : Boolean.TRUE;
                } else {
                    if (p.length == 1) {
                        n = String.valueOf(i + 1);
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
        return argMap;
    }

    public Properties getArgMap() {
        return argMap;
    }

    public String get(String key) {
        return get(key, ++consumed);
    }
    
    /**
     * provides the desired keys value. if not stored by 'key=value', the alternative arg index will be used.
     * 
     * @param key property key
     * @param alternativeArgIndex one-based argument index
     * @return keys or indexed value
     */
    public String get(String key, int alternativeArgIndex) {
        String v = argMap.getProperty(key);
        return v != null ? v : argMap.getProperty(String.valueOf(alternativeArgIndex));
    }

    public boolean check(PrintStream out) {
        Set<String> keys = man.keySet();
        for (String k : keys) {
            if (isDuty(man, k) && !isSet(k)) {
                out.println("argument '" + k + "' was not set!\n");
                printManual(out);
                return false;
            }
        }
        consumed = 0;
        return true;
    }

    /**
     * checks a given value against the manual
     * 
     * @param name
     * @param value
     * @param man
     */
    private static void check(String name, Object value, Map<String, String> man) {
        if (man == null)
            return;
        //TODO: check args against manual 'man'
        String rule = null;
        if (false)
            throw new IllegalArgumentException(rule);
    }

    /**
     * hasOption
     * 
     * @param name
     * @return true, if given key was activate as option
     */
    public boolean hasOption(String name) {
        Object option = consume(name);
        return option instanceof Boolean && ((Boolean) option).booleanValue();
    }

    /**
     * isSet
     * 
     * @param name
     * @return true, if given key was set.
     */
    public boolean isSet(String name) {
        return get(name) != null;
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
     * 
     * @return next argument value
     */
    public Object next() {
        return consume((String) argMap.keySet().iterator().next());
    }

    /**
     * convenience to evaluate all sta
     * 
     * @param cls
     * @param fieldType
     * @return
     */
    public static final String staticNames(Class<?> cls, Class<?> fieldType) {
        return StringUtil.toString(BeanClass.getBeanClass(cls).getFieldNames(fieldType, true), Integer.MAX_VALUE);
    }

    public void printManual() {
        printManual(System.out);
    }

    public void printManual(PrintStream out) {
        printManual(name, man, out, 80);
    }

    static boolean isDuty(Map<String, String> man, String name) {
        return man.get(name).startsWith(KEY_DUTY);
    }

    /**
     * printManual
     * 
     * @param man manual to be logged
     */
    public static void printManual(String name, Map<String, String> man, PrintStream out, int width) {
        Set<String> keys = man.keySet();
        StringBuilder buf = new StringBuilder(keys.size() * width);
        int kw = 10;// key width
        int fkw = kw + 2; // full key width
        int w = width - fkw;//minus key-length + ': '
        int w1 = width - 4;//no key, but tab-width
        String newlinetab = "\n" + StringUtil.fixString("", fkw, ' ', true);
        buf.append(StringUtil.fixString("", width, '-', true) + "\n");
        buf.append(getSyntax(name, man, width) + "\n");
        boolean examplesStarted = false;
        for (String k : keys) {
            if (!examplesStarted && k.startsWith(KEY_PREFIX_EXAMPLE)) {
                examplesStarted = true;
                buf.append("\nEXAMPLES:\n");
            }
            buf.append(StringUtil.fixString(k, kw, ' ', true) + ": ");
            buf.append(StringUtil.split(man.get(k), newlinetab, w, w1 * 2, w1 * 3, w1 * 4, w1 * 5, w1 * 6, w1 * 7,
                w1 * 8, w1 * 9, w1 * 10) + "\n");
        }
        buf.append(StringUtil.fixString("", width, '-', true) + "\n");
        out.println(buf.toString());
    }

    static String getSyntax(String name, Map<String, String> man, int width) {
        Set<String> keys = man.keySet();
        StringBuilder buf = new StringBuilder(keys.size() * 15);
        buf.append("syntax: " + name);
        for (String k : keys) {
            if (isDuty(man, k)) {
                buf.append(" <" + k + ">");
            } else {
                buf.append(" [" + k + "]");
            }
        }
        buf.append("\n");
        return StringUtil.split(buf, "\n\t", width, width * 2, width * 3);
    }
}
