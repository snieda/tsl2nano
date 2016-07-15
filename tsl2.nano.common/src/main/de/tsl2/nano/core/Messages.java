package de.tsl2.nano.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.util.PrivateAccessor;

/**
 * message bundle delegator (common in eclipse).
 * 
 * @author ts 26.03.2009
 * @version $Revision$
 */
public class Messages {
    private static final String BUNDLE_NAME = "de.tsl2.nano.core.messages"; //$NON-NLS-1$

    private static List<ResourceBundle> BUNDLE_LIST = new ArrayList<ResourceBundle>();
    static {
        BUNDLE_LIST.add(ResourceBundle.getBundle(BUNDLE_NAME));
    }

    private static final Log LOG = LogFactory.getLog(Messages.class);

    public static String TOKEN_MSG_NOTFOUND = "!";
    public static String POSTFIX_TOOLTIP = ".tooltip";

    /**
     * default constructor
     */
    private Messages() {
    }

    /**
     * Register a new bundle. For performance reasons the "main" bundle should be added at the "head" of the bundle
     * list.
     * 
     * @param bundle the bundle
     * @param head true if the bundle should be set at the beginning of the list
     */
    public static void registerBundle(ResourceBundle bundle, boolean head) {
        assert bundle != null;
        if (head) {
            BUNDLE_LIST.add(0, bundle);
        } else {
            BUNDLE_LIST.add(bundle);
        }
    }

    /**
     * evaluates the full file name (without path) of the given resource name for the current default locale.
     * 
     * @param name resource name
     * @return full resource file name.
     */
    public static final String getResourceFileName(String name) {
        return name + "_" + Locale.getDefault().toString() + ".properties";
    }

    /**
     * checks, whether the resource {@link #getResourceFileName(String)} exists.
     * 
     * @param name resource name
     * @return true, if {@link #getResourceFileName(String)} could be found in the current threads classpath
     */
    public static boolean exists(String name) {
        return FileUtil.hasResource(getResourceFileName(name));
    }

    /**
     * Get the translation for the given key from the ResourceBundle pool.
     * 
     * @param key the bundle key
     * @return bundle value the translated value or the key itself if no translation is available
     */
    public static String getString(String key) {
        if (key == null) {
            return "!null-key!";
        }
        //key seems already to be translated. 
        if (!key.matches("[\\w\\d_.-]*")) {
            return key;
        }
        String entry = find(key);
        return entry != null ? entry : markProblem(key);
    }

    /**
     * find
     * 
     * @param key key to search for
     * @return key-value found in the first registered bundle or null if not found.
     */
    public static String find(String key) {
        for (final ResourceBundle bundle : BUNDLE_LIST) {
            try {
                return bundle.getString(key);
            } catch (final MissingResourceException ignore) {
//                LOG.trace("resource key not found: " + key);
            }
        }
        return null;
    }

    /**
     * markProblem
     * 
     * @param key text to be wrapped
     * @return {@link #TOKEN_MSG_NOTFOUND} + key + {@link #TOKEN_MSG_NOTFOUND}
     */
    public static String markProblem(String key) {
        return TOKEN_MSG_NOTFOUND + key + TOKEN_MSG_NOTFOUND;
    }

    /**
     * delegates to {@link #getStringOpt(String, boolean)} with format = false.
     */
    public static String getStringOpt(String key) {
        return getStringOpt(key, false);
    }

    /**
     * optional translation. tries to get the translation from resoucebundle pool. if not found, the naming part of the
     * key will be returned.
     * 
     * @param key key
     * @param format pre-format a key that wasn't found.
     * @return translated key , if found
     */
    public static String getStringOpt(String key, boolean format) {
        if (key.length() == 0) {
            return key;
        }
        String s = getString(key);
        if (unknown(s)) {
//            if (LOG.isTraceEnabled()) {
//                LOG.warn(key + " not found in messages.properties");
//            }
            if (!key.endsWith(".")) {
                int i = key.lastIndexOf(".");
                s = key.substring(i + 1);
            }
            if (!s.contains("&") && format) {
                s = StringUtil.toFirstUpper(s);
            }
        }
        return StringUtil.spaceCamelCase(s);
    }

    /**
     * Get a translation formatted using {@link MessageFormat} with the given parameters.
     * 
     * @param key the bundle key
     * @param parameter parameters for MessageFormat
     * @return localised messaged with formatted parameters
     */
    public static String getFormattedString(String key, Object... parameter) {
        String text = getString(key);
        if (parameter != null) {
            String a;
            for (int i = 0; i < parameter.length; i++) {
                a = parameter[i] != null ? getStringOpt(String.valueOf(parameter[i])) : null;
                if (a != null) {
                    parameter[i] = a;
                }
            }
            text = MessageFormat.format(text, parameter);
        }
        return text;
    }

    /**
     * Special getString for Enums. The Key is build from:
     * <P>
     * {@link Class#getSimpleName()} + "." + {@link Enum#name()}
     * 
     * @param enumValue an enum value
     * @return a translation for the enum value
     */
    public static String getString(Enum<?> enumValue) {
        /*
         * if enum implements e.g. the values toString() method, the class name (inner class) is empty, 
         * so we have to use the real enum class name.
         */
        final String enumClassName = enumValue.getDeclaringClass().getSimpleName();
        return getStringOpt(enumClassName + "." + enumValue.name());
    }

    /**
     * Special getString that uses the simpleName of the class as prefix for the key (Enum like). Typically this is used
     * for Constants defined in an Interface (pre Enum times ...).
     * 
     * @param clazz the "key" Class
     * @param key the key
     * @return the translation
     */
    public static String getString(Class<?> clazz, String key) {
        return getString(clazz.getSimpleName() + "." + key);
    }

    /**
     * Get the registered Bundled. Should only be used for some "exceptional" reasons. Regular usage MUST be registring
     * bundles and using one of the get...String methods.
     * 
     * @return all registered bundles
     */
    public static List<ResourceBundle> getBundles() {
        return BUNDLE_LIST;
    }

    /**
     * Remove the mnemonic character ("&") from the given string.
     * 
     * @param str s String
     * @return the string without mnemonics
     */
    public static String stripMnemonics(String str) {
        if (str == null) {
            return str;
        }
        return str.replaceAll("&", "");
    }

    public static String stripParameterBrackets(String str) {
        if (str == null) {
            return str;
        }
        return str.replace('{', '[').replace('}', ']');
    }

    /**
     * simply delegates to {@link #unknown(String)}.
     */
    public static final boolean isMarkedAsProblem(String text) {
        return unknown(text);
    }

    /**
     * hasKey
     * 
     * @param key key to search
     * @return true, if key was found in one of the registered bundles
     */
    public static boolean hasKey(String key) {
        return find(key) != null;
    }

    /**
     * if a resource bundle key was not found (through {@link #getString(String)}, the key, surrounded with
     * {@link #TOKEN_MSG_NOTFOUND} will be returned as result. this result will be evaluated.
     * 
     * @param searchedKeyResult result of {@link #getString(String)}
     * @return true, if result contains {@link #TOKEN_MSG_NOTFOUND}.
     */
    public static final boolean unknown(String searchedKeyResult) {
        return searchedKeyResult.startsWith(TOKEN_MSG_NOTFOUND) && searchedKeyResult.endsWith(TOKEN_MSG_NOTFOUND);
    }

    /**
     * reloads all cached resouce bundles
     */
    public static final void reload() {
        ResourceBundle.clearCache();
        for (int i = 0; i < BUNDLE_LIST.size(); i++) {
            LOG.info("reloading resourcebundle " + BUNDLE_LIST.get(i));
            BUNDLE_LIST.set(i,
                ResourceBundle.getBundle((String) new PrivateAccessor<ResourceBundle>(BUNDLE_LIST.get(i))
                    .member("name")));
        }
    }

    /**
     * getBundleRoot
     * 
     * @param name resource bundle
     * @return bundle without locale specification.
     */
    public static ResourceBundle getBundleRoot(String name) {
        return ResourceBundle.getBundle(name, Locale.ROOT);
    }
    /**
     * @return keySet of all registered bundles. order: last registered keys come first
     */
    public static Set<String> keySet() {
        HashSet<String> keySet = new LinkedHashSet<String>();
        ArrayList<ResourceBundle> reverseBundleList = new ArrayList<ResourceBundle>(BUNDLE_LIST);
        Collections.reverse(reverseBundleList);
        for (ResourceBundle bundle : reverseBundleList) {
            keySet.addAll(bundle.keySet());
        }
        return keySet;
    }
}
