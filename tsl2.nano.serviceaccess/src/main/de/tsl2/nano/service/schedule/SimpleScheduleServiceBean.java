/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Jan 11, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.schedule;

import java.io.Serializable;

import javax.ejb.Singleton;

/**
 * implementation of {@link IJobScheduleLocalService} to call {@link Runnable}s
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@Singleton
public class SimpleScheduleServiceBean extends AbstractJobScheduleServiceBean<Runnable> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void run(Runnable runnable, Serializable context) {
        runnable.run();
    }
}
