/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.messaging.IListener;

/**
 * Overrides the fire-method of {@link EventController} to call the listeners parallel. This class may be used instead
 * of {@link EventController} to switch to parallelized working. Additionally, the original sequential firing method is
 * available through {@link #sendEvent(Object)}.<p/>
 * TODO: add addListeners(Collection), removeListeners(Collection)
 * 
 * @author ts
 * @version $Revision$
 */
public class ThreadingEventController extends EventController {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    //TODO: use newFixedThreadPool(nThreads)
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    protected ThreadingEventController() {
    }

    /**
     * disables the singelton to work on several objects
     * 
     * @return
     */
    public static final ThreadingEventController createNewController() {
        return new ThreadingEventController();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void handle(final IListener l, final Object e) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Net.log_("new task " + Thread.currentThread() + " for '" + l + "' handling '" + e + "'\n");
                try {
					l.handleEvent(e);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
            }
        });
    }
    
    /**
     * provides the un-threaded super fire-method. see {@link EventController#fireEvent(Object)}
     * 
     * @param e new event
     */
    public void sendEvent(final Object e) {
        super.fireEvent(e);
    }
}
