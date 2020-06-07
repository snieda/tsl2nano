package de.tsl2.nano.codegen;

import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;

import de.tsl2.nano.core.cls.BeanAttribute;
import de.tsl2.nano.core.cls.BeanClass;
import de.tsl2.nano.core.cls.CallingPath;
import de.tsl2.nano.core.util.CUtil;
import de.tsl2.nano.core.util.CollectionUtil;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.ObjectUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;

/**
 * helper class for {@linkplain ClassGenerator}
 * 
 * @author ts 07.12.2008
 * @version $Revision: 1.0 $
 * 
 */
public class GeneratorUtility {
    Properties props = new Properties(System.getProperties());
    private VelocityContext context;

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

    public Class<StringUtil> strings() {
        return StringUtil.class;
    }
    
    public Class<ObjectUtil> objects() {
        return ObjectUtil.class;
    }
    
    public Class<CUtil> compareables() {
        return CUtil.class;
    }
    
    public Class<MapUtil> maps() {
        return MapUtil.class;
    }
    
    public Class<CollectionUtil> collections() {
        return CollectionUtil.class;
    }
    
    public Class<FormatUtil> formats() {
        return FormatUtil.class;
    }
    
    public Class<DateUtil> dates() {
        return DateUtil.class;
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
        if (context != null)
            context.put(key, value);
    }

    /**
     * if the value was found, it will return the value. if not, an empty string
     * will be returned to delete the variable-entry inside the template.
     * 
     * if key starts with 'obj:', it will be interpreted as class name and an
     * instance of that class will be returned if key starts with 'cls:', it will be
     * interpreted as class name and that class will be returned if key starts with
     * 'bls:', it will be interpreted as class name and that class will be returned -
     * packed into BeanClass instance.
     * 
     * @param key key
     * @return value or empty string
     */
    public Object get(String key) {
        final Object value = props.get(key);
        if (value == null) {
            String cls = props.getProperty(key.substring(4));
            if (key.startsWith("cls:"))
                return clazz(cls != null ? cls : key.substring(4));
            else if (key.startsWith("obj:"))
                return object(cls != null ? cls : key.substring(4));
            else if (key.startsWith("bls:"))
                return beanclass(cls != null ? cls : key.substring(4));
        }
        return value != null ? value : key;
    }

    public String getFilePathFromPackage(String pck) {
        return pck.replace('.', '/');
    }

    public String toValidName(String txt) {
        return StringUtil.toValidName(txt);
    }

    /**
     * @param cls class name having default constructor
     * @return new instance of given class. this result object will be available as
     *         property 'obj'+simple-class-name
     */
    public Object object(String cls) {
        Object instance = BeanClass.createInstance(cls);
        put("obj:" + instance.getClass().getSimpleName(), instance);
        return instance;
    }

    /**
     * @param cls class name
     * @return beanclass of given class
     */
    public BeanClass<?> beanclass(String cls) {
        BeanClass<?> bc = BeanClass.createBeanClass(cls);
        put("bls:" + bc.getClass().getSimpleName(), bc);
        return bc;
    }

    /**
     * @param cls class name
     * @return class. this result object will be available as property
     *         'cls'+simple-class-name
     */
    public Class<?> clazz(String cls) {
        Class<?> clazz = BeanClass.load(cls);
        put("cls:" + clazz.getClass().getSimpleName(), clazz);
        return clazz;
    }

    /**
     * creates an enumeration string (list seperated with ',', e.g.: SATURDAY,
     * SUNDAY).
     */
    public String toString(Object obj) {
        String t = StringUtil.toString(obj, -1);
        return Util.isContainer(obj) ? t.substring(1, t.length() - 1) : t;
    }

    /** tries to evaluate the given expression. if expression is not invokable, returns false */
    public Object evalOrNull(String expression) {
        try {
            return eval(expression);
        } catch (Exception ex) {
            System.err.println("expression is not invokable: " + expression + " => " + ex.toString());
            return null;
        }
    }
    
    public Object eval(String expression) {
        String objRef = StringUtil.substring(expression, null, ".");
        return eval(get(objRef), expression);
    }

    public Object eval(Object obj, String expression) {
        return CallingPath.eval(obj, expression, (Map)props);
    }

    public void setContext(VelocityContext context) {
        this.context = context;
    }
    
    // public void addAspectAround(String pointcut, Function replacementCallback) {
        
    // }
}
