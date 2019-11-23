package de.tsl2.nano.core.util;

import static de.tsl2.nano.core.util.StringUtil.substring;
import static de.tsl2.nano.core.util.StringUtil.toFormattedString;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;

import de.tsl2.nano.core.cls.BeanClass;

/**
 * some simple helpers on java main methods...for a full-featured solution, see
 * Argumentator + Arg.
 */
public class MainUtil {
    private static final String OPTION = "-";
    private static final String ASSIGN = "=";

    private static final String TAG_COLOR = "\033[%d;%d;%dm";
    public static final String NC = "\033[0m";

    public static final int COLOR_BASE = 30;
    public enum Color {BLACK, RED, GREEN, ORANGE, BLUE, PURPLE, CYAN, LIGHT_GRAY, // => now all with ligh=1
        DARK_GRAY, LIGHT_RED, LIGHT_GREEN, YELLOW, LIGHT_BLUE, LIGHT_PURPLE, LIGHT_CYAN, WHITE}

    public enum Style {NORMAL, BOLD, DARK, ITALIC, UNDERLINED}

    public static final String INFO = tag("INFO:  ", Color.BLUE);
    public static final String WARN = tag("WARN:  ", Color.YELLOW);
    public static final String ERROR = tag("ERROR: ", Color.RED);

    /**
     * maps command line arguments to a map of properties. each arg containting '='
     * will be mapped by 'name=value'. starting option-specifiers with '-' will be
     * ignored. args without '=' assignment will use the given mainArgNames as
     * names.
     * 
     * <pre>
     * Examples:
     * 
     * -myFirstOption myMainArg1 -mySecondOption=SecondOptionValue myMainArg2
     * -myFirstOption -mySecondOption=SecondOptionValue "myMainArg1       : This is the first argument"  "myMainArgument2: This is the second argument"
     * </pre>
     * 
     * @param prefix       (optional) name prefix
     * @param args         command line arguments
     * @param mainClass    if given, tries to load properties from file mainClass.simpleName.prefix
     * @param useUSLocale  if true, the US Locale will be set - means, that numbers and dates will not be formatted with current locale!
     *                     usefull for system apps with locale independent main arguments
     * @param mainArgNames argument names without assignment operator '='. each arg
     *                     name may have a description, starting with ':'
     * @return
     */
    public static Properties toProperties(String prefix, String[] args, Class<?> mainClass, boolean useUSLocale, String... mainArgNames) {
        if (args == null || (args.length == 1 && args[0].matches("\\?|[-]*h(elp)*")))
            if (mainClass != null && mainArgNames != null)
                throw new IllegalArgumentException("syntax: " + mainClass.getSimpleName() + "\n" + toFormattedString(mainArgNames, -1));
            else
                throw new IllegalArgumentException("no arguments given -- no help available!");
        mainArgNames = mainArgNames != null ? mainArgNames : new String[0];
        if (useUSLocale) {
            Locale.setDefault(Locale.US);
        }
        System.setProperty("enableassertions", "true");
        Properties p = new Properties();
        if (mainClass != null) {
            try {
                String propFile = mainClass.getSimpleName() + (prefix != null ? "." + prefix : "");
                p.load(new FileReader(new File(propFile)));
                System.out.println(propFile + " loaded");
            } catch (IOException e) {
                //ok, this is optional
            }
        }
        String name;
        Object value;
        int n = 0;
        List<String> argNames = getArgNames(mainArgNames);

        //eval options, named arguments and unnamed arguments without default value
        for (int i=0; i<args.length; i++) {
            if (args[i].contains(ASSIGN)) {
                name = substring(args[i], OPTION, ASSIGN);
                value = substring(args[i], ASSIGN, null);
            } else if (args[i].startsWith(OPTION)) {
                name = substring(args[i], OPTION, null);
                value = Boolean.TRUE;
            } else {
                if (n >= mainArgNames.length) //unfiltered mainArgNames!
                    throw new IllegalArgumentException("too many arguments. known arguments are:\n" + toFormattedString(mainArgNames, -1));
                name = substring(mainArgNames[n], null, ":").trim();
                value = getValueFromTypeDef(mainArgNames[n], args[i]);
                n++;
            }
            if (!argNames.contains(name)) {
                logn(WARN + "undescribed argument: " + tag(name, Color.GREEN));
            }
            p.put(prefix + name, value);
        }

        List<String> mandatoryArgs = new ArrayList<>(Arrays.asList(mainArgNames));
        defineDefaults(p, mandatoryArgs);

        if (n < mandatoryArgs.size())
            throw new IllegalArgumentException("please fill the following arguments\n" + toFormattedString(mandatoryArgs, -1));
        printInfo(mainClass, p);
        return p;
    }

    private static List<String> getArgNames(String[] mainArgNames) {
        List<String> argNames = new ArrayList<>(mainArgNames.length);
        for (String na : mainArgNames) {
            argNames.add(substring(na, OPTION, ":").trim());
        };
        return argNames;
    }

    private static void defineDefaults(Properties p, List<String> mainArgList) {
        String name;
        String description, defaultString;
        Object argValue;
        for (Iterator<String> it = mainArgList.iterator(); it.hasNext();) {
            description = it.next();
            if (description.startsWith(OPTION)) {
                it.remove();
                continue;
            }
            name = substring(description, OPTION, ":").trim();
            argValue = p.get(name);
            if (argValue != null && !(argValue instanceof String))
                continue;
            defaultString = (String)argValue;
            if (defaultString == null)
                defaultString = StringUtil.substring(description, "[default:", "]", false, true);
            if (defaultString != null) {
                p.put(name, getValueFromTypeDef(description, defaultString.trim()));
                it.remove();
            }
        }
    }

    /**
     * example-1:
     * <code>myMainArgument: {@java.lang.Long:1..100}[default: 10]</code><br/>
     * will constrain the arg value to be a long between 1 and 100. if no value was
     * given, 10 is the default value.
     * <p/>
     * example-2: <code>myMainArgument: {@java.lang.Long:1, 100}</code><br/>
     * will constrain the arg value to be a long exactly beeing 1 or 100. there is
     * no default, so it is mandatory.
     * <p/>
     * 
     * @param mainArgName defined main argument name with description
     * @param arg         arg from main args array
     * @return evaluated value through main argument description
     */
    static Object getValueFromTypeDef(String mainArgName, String arg) {
        Object value;
        String typeDef = substring(mainArgName, "{", "}", false, true);
        if (typeDef != null) {
            String type = substring(typeDef, "@", typeDef.contains(":") ? ":" : null, false, true);
            String valueset = substring(typeDef, ":", null, false, true);
            Class<?> cls = type != null ? BeanClass.load(type) : String.class;
            value = cls.isInterface() ? BeanClass.createInstance(arg) : FormatUtil.parse(cls, arg);
            // check
            if (cls.isInterface())
                assertw (cls.isAssignableFrom(value.getClass()), amsg(mainArgName, value, " of type ", cls));
            if (valueset != null) {
                if (valueset.contains(","))
                    assertw (Arrays.asList(valueset.split(",")).contains(arg), amsg(mainArgName, arg, "one of ", valueset));
                else if (valueset.contains("..")) {
                    String[] minmax = valueset.split("\\.\\.");
                    assertw( ((Comparable) FormatUtil.parse(cls, minmax[0])).compareTo(value) <= 0, amsg(mainArgName, value, ">= ", minmax[0]));
                    assertw( ((Comparable) FormatUtil.parse(cls, minmax[1])).compareTo(value) >= 0, amsg(mainArgName, value, "<= ", minmax[1]));
                } else {
                    logn(WARN + mainArgName + ": valueset description should use '..' or ','");
                }
            }
        } else {
            value = arg;
        }
        return value;
    }

    static void printInfo(Class<?> mainClass, Properties p) {
        String mainClsName = mainClass != null ? mainClass.getSimpleName() : "UNKNOWN";
        log(new String(FileUtil.getFileBytes("tsl-logo.txt", null)));
        logn("\n=============================================================================");
        logn(mainClsName + " inititialized with:");
        logn(StringUtil.toFormattedString(new TreeMap(p), -1));
        logn("=============================================================================");
    }

    public static void logn(Object txt) {
        log(txt + "\n");
    }
    public static void logn(Object txt, String split, Color...colors) {
        log(txt + "\n", split, colors);
    }
    public static void log(Object txt, String split, Color...colors) {
        String parts[] = txt.toString().split(split);
        for (int i=0; i<parts.length; i++) {
            log((i < colors.length ? tag(colors[i]) : "") + parts[i]);
        }
        if (colors.length > 0)
            log(NC);
    }

    public static void log(Object txt) {
        System.out.print(txt);
    }

    public static String tag(Object txt, Color color) {
        return tag(txt, color, null);
    }
    public static String tag(Object txt, Color color, Color background) {
        return tag(txt, color, background, null);
    }
    public static String tag(Object txt, Color color, Color background, Style style) {
        return tag(color, background, style) + txt + NC;
    }

    public static String tag(Color color) {
        return tag(color, null);
    }
    public static String tag(Color color, Color background) {
        return tag(color, background, (Style)null);
    }
    public static String tag(Color color, Color background, Style style) {
        color = color != null ? color : Color.LIGHT_GRAY;
        int lightOrStyle = style != null ? style.ordinal() : color.ordinal() > 7 ? 1 : 0;
        int c = (color.ordinal() % 8) + COLOR_BASE;
        int b = background != null ? background.ordinal() + COLOR_BASE + 10 : 1; //1: default value of ansi escape codes
        return String.format(TAG_COLOR, lightOrStyle, c, b);
    }

    public static void assertw(boolean expression, String msg) {
        if (!expression)
            logn(WARN + msg);
    }
    private static String amsg(String name, Object value, String condition, Object valueset) {
        return tag(name, Color.GREEN) + " is " + tag(value, Color.LIGHT_BLUE) + " but must be " + condition + valueset;
    }

}