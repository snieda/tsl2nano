package de.tsl2.nano.format;

import java.io.Serializable;
import java.text.Format;
import java.text.ParseException;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.FormatUtil;
import de.tsl2.nano.core.util.Util;

/**
 * Define a parser for strings matching a given regex pattern
 * <p/>
 * Note: no Generictype was defined, because a caller won't know the materialized type in most cases.
 * 
 * @author Tom
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class TypeDef implements Serializable {
    private static final long serialVersionUID = -1626636544173135597L;

    /** simple name */
    String name;
    /** regular expression to be matched by a value */
    String pattern;
    /** optional mask to be presented for empty values */
    String mask;
    /** object type to be materialized by the given parser */
    Class type;
    /**
     * parses a string value to create an object of {@link #type}. if not defined, the default meachanisms through
     * FormatUtil etc. will be used.
     */
    Format parser;
    /** optional defaultValue on materialized instance */
    Object defaultValue;

    protected TypeDef() {
    }

    /**
     * constructor
     * 
     * @param name
     * @param pattern
     * @param type
     */
    public TypeDef(String name, String pattern, Class type, Format formatter) {
        this(name, pattern, null, type, formatter, type);
    }

    /**
     * constructor
     * 
     * @param name
     * @param pattern
     * @param mask
     * @param type
     * @param parser
     * @param defaultValue
     */
    public TypeDef(String name, String pattern, String mask, Class type, Format parser, Object defaultValue) {
        super();
        this.name = name;
        this.pattern = pattern;
        this.mask = mask;
        this.type = type;
        this.parser = parser;
        this.defaultValue = defaultValue;
    }

    /**
     * uses the parser to instantiate an object of {@link #type}.
     * 
     * @param description
     * @return
     */
    public Object materialize(String description) {
        if (description != null) {
            try {
                return parser != null ? parser.parseObject(description)
                    : FormatUtil.getDefaultFormat(type, true).parseObject(description);
            } catch (ParseException e) {
                ManagedException.forward(e);
            }
        }
        return description;
    }

    @Override
    public String toString() {
        return Util.toString(this.getClass(), name);
    }
}
