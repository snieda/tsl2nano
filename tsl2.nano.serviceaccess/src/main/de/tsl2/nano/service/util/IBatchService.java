/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Oct 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.util;

import de.tsl2.nano.service.util.batch.Part;

/**
 * service to load different data through several queries using a single communication. usable for performance aspects.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public interface IBatchService {
    /**
     * is able to load different data through different queries (finders) in one step. usable for performance aspects.
     * 
     * @param batchParts batch parts to be executed
     * @return batch parts filled with result lists
     */
    <T> Part<T>[] findBatch(Part<T>... batchParts);
}
