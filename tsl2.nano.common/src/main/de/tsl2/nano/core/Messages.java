package de.tsl2.nano.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;

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
        if (key.contains(" ")) {
            return key;
        }
        for (final ResourceBundle bundle : BUNDLE_LIST) {
            try {
                return bundle.getString(key);
            } catch (final MissingResourceException ignore) {
//                LOG.trace("resource key not found: " + key);
            }
        }
        return TOKEN_MSG_NOTFOUND + key + TOKEN_MSG_NOTFOUND;
    }

    /**
     * markProblem
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
            if (!s.contains("&") && format)
                s = StringUtil.toFirstUpper(s);
        }
        return s;
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
     * if a resource bundle key was not found (through {@link #getString(String)}, the key, surrounded with
     * {@link #TOKEN_MSG_NOTFOUND} will be returned as result. this result will be evaluated.
     * 
     * @param searchedKeyResult result of {@link #getString(String)}
     * @return true, if result contains {@link #TOKEN_MSG_NOTFOUND}.
     */
    public static final boolean unknown(String searchedKeyResult) {
        return searchedKeyResult.startsWith(TOKEN_MSG_NOTFOUND) && searchedKeyResult.endsWith(TOKEN_MSG_NOTFOUND);
    }
}
