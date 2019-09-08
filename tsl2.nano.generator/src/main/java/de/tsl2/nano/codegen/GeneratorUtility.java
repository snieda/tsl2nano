package de.tsl2.nano.codegen;

import java.util.Properties;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.util.StringUtil;

/**
 * helper class for {@linkplain ClassGenerator}
 * 
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 * 
 */
public class GeneratorUtility {
    Properties props = new Properties(System.getProperties());

    /**
     * Constructor
     */
    protected GeneratorUtility() {
        super();
    }

    /**
     * firstToUpperCase
     * 
     * @param string to convert
     * @return converted string
     */
    public String toFirstUpperCase(String string) {
        return BeanAttribute.toFirstUpper(string);
    }

    /**
     * firstToLowerCase
     * 
     * @param string to convert
     * @return converted string
     */
    public String toFirstLowerCase(String string) {
        return BeanAttribute.toFirstLower(string);
    }

    public String toLowerCase(String text) {
        return text.toLowerCase();
    }

    /**
     * toUpperCase
     * 
     * @param string to convert
     * @return converted string
     */
    public String toUpperCase(String text) {
        return text.toUpperCase();
    }

    public void put(String key, Object value) {
        props.put(key, value);
    }

    /**
     * if the value was found, it will return the value. if not, an empty string will be returned to delete the
     * variable-entry inside the template.
     * 
     * @param key key
     * @return value or empty string
     */
    public Object get(String key) {
        final Object value = props.get(key);
        return value != null ? value : "";
    }

    public String getFilePathFromPackage(String pck) {
        return pck.replace('.', '/');
    }

    public String toValidName(String txt) {
        return StringUtil.toValidName(txt);
    }
}
