/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts, Thomas Schneider
 * created on: 19.05.2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.service.util;

import java.util.Properties;

import javax.ejb.Remote;


/**
 * provides base services. used by {@link IGenericService}.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@Remote
public interface IStatelessService {
    /**
     * only for tests - creates an empty server side factory.
     */
    void initServerSideFactories();
    
    /**
     * only for debugging
     * @return server system properties
     */
    Properties getServerInfo();

}
