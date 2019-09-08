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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;

import de.tsl2.nano.core.classloader.RuntimeClassloader;
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
 *         argumentator.start(System.out, r -> r.run()); 
 * </pre>
 * 
 * Example 2:
 * 
 * <pre>
 *         Argumentator argumentator = new Argumentator("print", getManual(), args);
 *         if (argumentator.check(System.out)) {
 *             Properties am = argumentator.getArgMap();
 *             String source = am.getProperty("source");
 *             ...
 * </pre>
 * 
 * Example 3:
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

    private static final String KEY_DUTY = Arg.KEY_DUTY;
    /** example: arg0: my description [(!) @java.lang.String(*******) ? {test...testZZZ} : test] */
    private static final String EXP_CONSTRAINT = "\\[[\\w.\\d]: ";
    private static final String KEY_PREFIX_EXAMPLE = "example";
    private Map<String, Arg<?>> man;
    private int errorCodeOnExit;

    /**
     * constructor
     * 
     * @param name program name
     * @param man  manual
     * @param args program arguments
     */
    public Argumentator(String name, Map<String, String> man, String... args) {
        this(name, decorateMan(man), 0, args);
    }

    public Argumentator(String name, Map<String, Arg<?>> man, int errorCodeOnExit, String... args) {
        this.name = name;
        this.man = man;
        this.errorCodeOnExit = errorCodeOnExit;
        argMap = defineArgs(man, args);
    }

    private static Map<String, Arg<?>> decorateMan(Map<String, String> man) {
        Map<String, Arg<?>> argDef = new LinkedHashMap<>(man.size());
        for (String k : man.keySet()) {
            argDef.put(k, new Arg(k, man.get(k)));
        }
        return argDef;
    }

    /**
     * structures and stores the given args to a map
     * 
     * @param man
     * @param args
     */
    public static Properties defineArgs(Map<String, Arg<?>> man, String[] args) {
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
                        storeWithName(i + 1, v, man, argMap);
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

    private static void storeWithName(int i, Object v, Map<String, Arg<?>> man, Properties argMap) {
        if (man.size() > i) {
            Iterator<String> it = man.keySet().iterator();
            String k = null;
            for(int n = 0; n < i; n++) {
                k = it.next();
            }
            check(k, v, man);
            argMap.put(k, v);
        }
    }

    public Properties getArgMap() {
        return argMap;
    }

    public String get(String key) {
        return get(key, ++consumed);
    }

    /**
     * convenience to evaluate argument types. if an argument is null, the type will be the given defaultType 
     * @param args arguments for method call
     * @return array of argument types
     */
    public static Class[] getArgumentClasses(Class defaultType, Object...args) {
        Class[] clss = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            clss[i] = args[i] != null ? args[i].getClass() : defaultType;
        }
        return clss;
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
                out.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                out.println("ERROR: argument '" + k + "' was not set!");
                out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                printManual(out);
                if (errorCodeOnExit > 0)
                    System.exit(errorCodeOnExit);
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
    private static void check(String name, Object value, Map<String, Arg<?>> man) {
        if (man == null) {
            return;
        }
        //TODO: check args against manual 'man'
//        String rule = null;
//        if (false) {
//            throw new IllegalArgumentException(rule);
//        }
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
     * convenience to evaluate all static field names (constants) of a class
     * 
     * @param cls class to evaluate
     * @param fieldType constant type
     * @return string containing all static field values of given type.
     */
    public static final String staticNames(Class<?> cls, Class<?> fieldType) {
        return StringUtil.toString(BeanClass.getBeanClass(cls).getFieldNames(fieldType, true), Integer.MAX_VALUE);
    }

    /**
     * evaluates all static field names through {@link #staticNames(Class, Class)} and fills their values to a string.
     * 
     * @param cls class to evaluate
     * @param fieldType constant type
     * @return string containing all static field names of given type.
     */
    public static final String staticValues(Class<?> cls, Class<?> fieldType) {
        BeanClass<?> bc = BeanClass.getBeanClass(cls);
        String[] names = bc.getFieldNames(fieldType, true);
        StringBuilder buf = new StringBuilder(names.length * 10);
        for (int i = 0; i < names.length; i++) {
            buf.append(BeanClass.getStatic(cls, names[i]) + " ");
        }
        return buf.toString();
    }

    /**
     * evaluates all static field names through {@link #staticNames(Class, Class)} and fills their values to a string.
     * 
     * @param cls class to evaluate
     * @param fieldType constant type
     * @return string containing all static field names of given type.
     */
    public static final String staticKeyValues(Class<?> cls, Class<?> fieldType) {
        BeanClass<?> bc = BeanClass.getBeanClass(cls);
        String[] names = bc.getFieldNames(fieldType, true);
        StringBuilder buf = new StringBuilder(names.length * 10);
        for (int i = 0; i < names.length; i++) {
            buf.append(names[i] + "\t\t\t= " + BeanClass.getStatic(cls, names[i]) + "\n");
        }
        return buf.toString();
    }

    public void printManual() {
        printManual(System.out);
    }

    public void printManual(PrintStream out) {
        printManual(name, man, out, 80);
    }

    static boolean isDuty(Map<String, ?> man, String name) {
        return man.get(name) instanceof Arg ? ((Arg)man.get(name)).mandatory : man.get(name).toString().contains(KEY_DUTY);
    }

    /**
     * printManual
     * 
     * @param man manual to be logged
     */
    public static void printManual(String name, Map<String, ?> man, PrintStream out, int width) {
        Set<String> keys = man.keySet();
        StringBuilder buf = new StringBuilder(keys.size() * width);
        int kw = 10;// key width
        int fkw = kw + 2; // full key width
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
            buf.append(StringUtil.split(man.get(k), newlinetab, w1, w1 * 2, w1 * 3, w1 * 4, w1 * 5, w1 * 6, w1 * 7,
                w1 * 8, w1 * 9, w1 * 10) + "\n");
        }
        buf.append(StringUtil.fixString("", width, '-', true) + "\n");
        out.println(buf.toString());
    }

    static String getSyntax(String name, Map<String, ?> man, int width) {
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
    
    public static Attributes readManifest() {
        return RuntimeClassloader.readManifest();
    }
    
    /**
     * 
     * @param prinStream
     * @param runner
     * @return result of runner function
     */
    public void start(PrintStream printStream, Function<Properties, Object> runner) {
        if (check( printStream)) {
            printStream.println("\n>>>>> " + runner.apply(getArgMap()) + " <<<<<\n");           
        }
    }

    @Override
    public String toString() {
    	String ln = "===============================================================================\n";
    	return ln + getClass().getSimpleName() + ":\n" + StringUtil.toFormattedString(man, -1) + "\ncalled with:\n" + StringUtil.toFormattedString(argMap, -1) + ln;
    }

}