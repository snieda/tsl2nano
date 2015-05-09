/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom
 * created on: 04.12.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.util.parser;

import de.tsl2.nano.core.util.Util;

/**
 * 
 * @author Tom
 * @version $Revision$ 
 */
public abstract class Parser<INPUT> implements IParser<INPUT> {
    /**
     * isEmpty
     * 
     * @param term term to check
     * @return true, if term is empty
     */
    @Override
    public boolean isEmpty(INPUT term) {
        return Util.isEmpty(term, true);
    }
    /**
     * should be overridden if you need a transformation. F.e., if your INPUT is CharSequence and you need a conversion
     * from string to stringbuilder.
     * 
     * @param src source to be transformed/wrapped
     * @return transformed/wrapped value
     */
    @Override
    public INPUT wrap(INPUT src) {
        return src;
    }

    /**
     * see {@link #wrap(Object)}.
     * 
     * @param src source to be re-transformed
     * @return re-transformed/unwrapped value
     */
    @Override
    public INPUT unwrap(INPUT src) {
        return src;
    }


    @Override
    public INPUT extract(INPUT source, INPUT regexp) {
        return extract(source, regexp, null);
    }
}
