/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: TS, Thomas Schneider
 * created on: Jan 11, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.service.util;

import javax.ejb.Remote;

/**
 * provides some basic service access methods to work with entity beans.
 * 
 * @author TS
 * 
 */
@Remote
public interface IGenericService extends IGenericBaseService, IQueryService , IBatchService {
}
