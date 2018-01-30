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

import java.util.concurrent.Callable;

import de.tsl2.nano.core.messaging.EventController;
import de.tsl2.nano.core.messaging.IListener;
import de.tsl2.nano.incubation.network.JobServer;

/**
 * Overrides the fire-method of {@link EventController} to call the listeners on a network. This class may be used instead
 * of {@link EventController} to switch to parallelized working. Additionally, the original sequential firing method is
 * available through {@link #sendEvent(Object)}.<p/>
 * TODO: add addListeners(Collection), removeListeners(Collection)<br/>
 * TODO: test and extend implementation on a network
 * 
 * @author ts
 * @version $Revision$
 */
public class NetworkEventController extends EventController {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    private static transient JobServer jobServer = new JobServer();

    protected NetworkEventController() {
    }

    /**
     * disables the singelton to work on several objects
     * 
     * @return
     */
    public static final NetworkEventController createNewController() {
        return new NetworkEventController();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void handle(final IListener l, final Object e) {
        jobServer.execute(e.toString(), new Callable() {
            @Override
            public Object call() throws Exception {
                Net.log_("new task for '" + l + "' handling '" + e + "'\n");
                l.handleEvent(e);
                return null;
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
