/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Feb 29, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.service.schedule;

import java.io.Serializable;

import javax.ejb.Singleton;

import de.tsl2.nano.execution.IRunnable;
import de.tsl2.nano.serviceaccess.ServiceFactory;

/**
 * 
 * implementation of {@link IJobScheduleLocalService} to call {@link IRunnable}s. the {@link IRunnable}s context will not
 * have a generic type definition to be simple.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
@Singleton
//@Startup
public class JobScheduleServiceBean extends AbstractJobScheduleServiceBean<Class<IRunnable>> implements
        IJobScheduleLocalService<Class<IRunnable>>, IJobScheduleService<Class<IRunnable>> {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void run(Class<IRunnable> runnable, Serializable context) {
        IRunnable service = ServiceFactory.instance().getService(runnable);
        service.run(context);
    }
}
