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

import de.tsl2.nano.core.execution.ICRunnable;
import de.tsl2.nano.serviceaccess.ServiceFactory;

/**
 * 
 * implementation of {@link IJobScheduleLocalService} to call {@link ICRunnable}s. the {@link ICRunnable}s context will not
 * have a generic type definition to be simple.
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
@Singleton
//@Startup
public class JobScheduleServiceBean extends AbstractJobScheduleServiceBean<Class<ICRunnable>> implements
        IJobScheduleLocalService<Class<ICRunnable>>, IJobScheduleService<Class<ICRunnable>> {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void run(Class<ICRunnable> runnable, Serializable context) {
        ICRunnable service = ServiceFactory.instance().getService(runnable);
        service.run(context);
    }
}
