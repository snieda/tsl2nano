package de.tsl2.nano.format;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.core.util.FormatUtil;

/**
 * Helps on having only a string value without type informations. Holds a map of registered patterns to evaluate the
 * object type of a given string to be converted to. The order of the map defines the priority of type definitions - the
 * first added typeDef wins!
 * <p/>
 * FormatUtil, RegExpFormat and GenericParser are able to define the parsing.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class GenericTypeMatcher implements Serializable {
    private static final long serialVersionUID = -5974505014462461751L;

    /** registeredPatterns */
    @ElementMap(entry = "typeDef", key = "pattern", attribute = true, inline = true, value = "Definition", valueType = TypeDef.class, required = false)
    Map<String, TypeDef> registeredPatterns;

    public GenericTypeMatcher() {
        registeredPatterns = new LinkedHashMap<String, TypeDef>();

        //some defaults
        registerType(RegExpFormat.createDateRegExp().getPattern(), Date.class);
        registerType(RegExpFormat.createTimeRegExp().getPattern(), Time.class);
        registerType("true|false", Boolean.class);
        registerType("\\d{1,9}", Integer.class);
        registerType("\\d{10,18}", Long.class);
        //big-integer: e.g.: -10.000.000,0000
        DecimalFormatSymbols symbols =
            ((DecimalFormat) DecimalFormat.getNumberInstance()).getDecimalFormatSymbols();
        registerType(symbols.getMinusSign() + "?(\\d+" + symbols.getGroupingSeparator() + "?)+("
            + symbols.getDecimalSeparator() + "\\d+)?", BigInteger.class);
//            registereType("yes|no", Boolean.class);
//            registereType("y|n", Boolean.class);
//            registereType("yes|no", Boolean.class);
    }

    /**
     * tries to convert a string value (representing a standard java object) through expression-matching to a standard
     * java object
     * 
     * @param description value to parse
     * @return string, object or null
     */
    public Object materialize(String description) {
        if (description == null)
            return null;
        TypeDef typeDef = typeOf(description);
        return typeDef != null ? typeDef.materialize(description) : description;
    }

    /**
     * looks inside the {@link #registeredPatterns} for pattern to be matched by the given value.
     * 
     * @param value to find a pattern/type for.
     * @return type or null
     */
    TypeDef typeOf(String value) {
        Set<String> keys = registeredPatterns.keySet();
        for (String k : keys) {
            if (value.matches(k))
                return registeredPatterns.get(k);
        }
        return null;
    }

    public void registerType(String pattern, Class typeForPattern) {
        registerType(typeForPattern.getSimpleName() + "(" + pattern + ")", pattern, typeForPattern,
            null);
    }

    public void registerType(String name, String pattern, Class typeForPattern) {
        registerType(name, pattern, typeForPattern, null);
    }

    /**
     * registers a new type for a given pattern
     * 
     * @param pattern pattern to be matched for the given type
     * @param typeForPattern type to use for a string value matching the given pattern
     */
    public void registerType(String name, String pattern, Class typeForPattern, Format formatter) {
        registeredPatterns.put(pattern, new TypeDef(name, pattern, typeForPattern, formatter));
    }
    
    @SuppressWarnings("serial")
    public Format getParser() {
        return new Format() {
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(obj != null ? FormatUtil.getDefaultFormat(obj, false).format(obj) : "");
            }
            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return source != null ? typeOf(source).materialize(source) : null;
            }
        };
    }
}
