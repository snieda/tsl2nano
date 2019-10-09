package de.tsl2.nano.codegen;

import java.util.Properties;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
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
     * if key starts with 'obj', it will be interpreted as class name and an instance of that class will be returned
     * if key starts with 'cls', it will be interpreted as class name and that class will be returned
     * if key starts with 'bls', it will be interpreted as class name and that class will be returned - packed into BeanClass
     * @param key key
     * @return value or empty string
     */
    public Object get(String key) {
        final Object value = props.get(key);
        if (value == null) {
            String cls = props.getProperty(key.substring(3));
            if (key.startsWith("cls"))
                return clazz(cls != null ? cls : key.substring(3));
            else if (key.startsWith("obj"))
                return object(cls != null ? cls : key.substring(3));
            else if (key.startsWith("bls"))
                return beanclass(cls != null ? cls : key.substring(3));
        }
        return value != null ? value : "";
    }

    public String getFilePathFromPackage(String pck) {
        return pck.replace('.', '/');
    }

    public String toValidName(String txt) {
        return StringUtil.toValidName(txt);
    }

    /**
     * @param cls class name having default constructor
     * @return new instance of given class. this result object will be available as property 'obj'+simple-class-name
     */
    public Object object(String cls) {
        Object instance = BeanClass.createInstance(cls);
        put("obj" + instance.getClass().getSimpleName(), instance);
        return instance;
    }

    /**
     * @param cls class name
     * @return beanclass of given class
     */
    public BeanClass<?>  beanclass(String cls) {
        BeanClass<?> bc = BeanClass.createBeanClass(cls);
        put("bls" + bc.getClass().getSimpleName(), bc);
        return bc;
    }

    /**
     * @param cls class name
     * @return class. this result object will be available as property 'cls'+simple-class-name
     */
    public Class<?> clazz(String cls) {
        Class<?> clazz = BeanClass.load(cls);
        put("cls" + clazz.getClass().getSimpleName(), clazz);
        return clazz;
    }
}
