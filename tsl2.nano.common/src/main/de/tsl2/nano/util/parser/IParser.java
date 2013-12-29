/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 06.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.parser;

/**
 * Defines all methods to create a simple parser. the parser is independent of a type like string - so extensions are
 * portable to other systems or languages.
 * 
 * @param <INPUT>
 * @author Tom
 * @version $Revision$
 */
public interface IParser<INPUT> {
    /**
     * isEmpty
     * 
     * @param term term to check
     * @return true, if term is empty
     */
    boolean isEmpty(INPUT term);

    /**
     * replace
     * 
     * @param src
     * @param expression
     * @param replace
     */
    void replace(INPUT src, INPUT expression, INPUT replace);

    /**
     * should be overridden if you need a transformation. F.e., if your INPUT is CharSequence and you need a conversion
     * from string to stringbuilder.
     * 
     * @param src source to be transformed/wrapped
     * @return transformed/wrapped value
     */
    INPUT wrap(INPUT src);

    /**
     * see {@link #wrap(Object)}.
     * 
     * @param src source to be re-transformed
     * @return re-transformed/unwrapped value
     */
    INPUT unwrap(INPUT src);

    /**
     * extracts the given part from source 
     * @param source
     * @param part
     * @return extracted part
     */
    INPUT extract(INPUT source, INPUT part);

    /**
     * extracts the given part from source and replaces it with replacement
     * @param source
     * @param match
     * @param replacement
     * @return extracted part
     */
    INPUT extract(INPUT source, INPUT match, INPUT replacement);

    /**
     * searches for the given block starting with begin and ending with end. if last is true, the last block will be searched.
     * @param src source
     * @param begin block start
     * @param end block end
     * @param last whether to search from the end.
     * @return block between begin and end. if not found, the whole source will be returned
     */
    INPUT subElement(INPUT src, INPUT begin, INPUT end, boolean last);

    /**
     * searches in src for a block starting (excluding) from first begin and ending with last end. 
     * @param src source
     * @param begin block start
     * @param end block end
     * @return found block or null if not found
     */
    INPUT subEnclosing(INPUT src, INPUT begin, INPUT end);

    /**
     * concatenates given elements of type INPUT
     * 
     * @param input input array to concatenate. the array is of type Object as a technical workaround on auto-creating
     *            an INPUT[].
     * @return concatenation of input
     */
    INPUT concat(Object... input);
    /**
     * right and left trim
     * @param totrim
     * @return trimmed object
     */
    INPUT trim(INPUT totrim);
}
