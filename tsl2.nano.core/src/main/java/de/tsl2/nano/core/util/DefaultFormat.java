package de.tsl2.nano.core.util;

import java.lang.reflect.Proxy;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Attribute;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.execution.CompatibilityLayer;
import de.tsl2.nano.core.log.LogFactory;

/**
 * Default formatter for all objects used by all dynamicform components. Override this class to use special formatting
 * in your application.
 * 
 * Features:
 * <ul>
 * <li>uses {@link MessageFormat} for primitive data types (and wrapper)</li>
 * <li>Translates Enum' s from {@link Messages}</li>
 * <li>"toString" formatting with {@link MessageFormat} for all other objects</li>
 * </ul>
 * 
 * @author ts
 * @version $Revision$
 */
public class DefaultFormat extends Format {
    private static final long serialVersionUID = -2809606292844082233L;

    private static final Log LOG = LogFactory.getLog(DefaultFormat.class);
    
    @Attribute(required=false) //workaround for simple-xml to have at least one field!
    Boolean empty;
    
    /**
     * constructor
     */
    public DefaultFormat() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        final StringBuffer result = toAppendTo;
        if (obj == null) {
            return result;
        }
        if (Date.class.isAssignableFrom(obj.getClass())) {
            //on dates, we can't use the MessageFormat - it uses a fix date formatter!
            result.append(FormatUtil.getDefaultFormat(obj, false).format(obj));
        } else if (obj.getClass().isEnum()) {
            final String translated = Messages.getString((Enum<?>) obj);
            result.append(translated);
        } else if (obj.getClass().isPrimitive() || obj.getClass().getName().startsWith("java.lang")) {
            //simple numbers without format chars
            if (obj instanceof Integer || obj instanceof Long || obj instanceof Short) {
                result.append(obj);
            } else {
                result.append(MessageFormat.format("{0}", new Object[] { obj }));
            }
        } else if (Proxy.isProxyClass(obj.getClass())) {
            result.append(obj.getClass().getSimpleName());
        } else if (obj instanceof Collection) {
            //recursive call
            final Collection c = (Collection) obj;
            for (final Object object : c) {
                result.append(format(object, new StringBuffer(), pos) + ", ");
            }
            if (result.length() > 1) {
                result.replace(result.length() - 2, result.length() - 1, " ");
            }
            return result;
//        } else if ((obj instanceof List)) {
//            List<?> listObj = (List<?>) obj;
//            if (listObj.size() > 0) {
//                    result.append(MessageFormat.format("{0}", new Object[] { obj }));
//            }
        } else if (obj.getClass().isArray() || obj instanceof Map) {
            result.append(StringUtil.toString(obj, 80));
        } else {
            //pure objects, representing there instance id --> use reflection
            CompatibilityLayer cl = ENV.isAvailable() ? ENV.get(CompatibilityLayer.class) : new CompatibilityLayer();
            if (!ObjectUtil.hasToString(obj) && cl.isAvailable("de.tsl2.nano.format.ToStringBuilder")) {
                try {
                    Object runRegistered = cl.runRegistered("reflectionToString", obj);
					result.append(StringUtil.toString(runRegistered != null ? runRegistered : obj, 80));
                } catch (final Exception e) {
                    LOG.error("Error on calling ToStringBuilder.reflectionToString().\nPlease define a toString() in " + obj.getClass()
                        + ". Or extend the implementation of your DefaultFormat extension!",
                        e);
                    result.append(MessageFormat.format("{0}", new Object[] { obj }));
                }
            } else {
                result.append(MessageFormat.format("{0}", new Object[] { obj }));
            }
        }
        return result;
    }
    
    

    /**
     * {@inheritDoc}
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }
}
