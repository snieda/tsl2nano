/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: ts
 * created on: 11.11.2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.neuron;

import java.io.Serializable;

import de.tsl2.nano.incubation.vnet.ILocatable;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.core.messaging.IListener;

/**
 * Technical extension of {@link Neuron} to fulfill the preconditions of a node core in vnet.
 * 
 * @author ts
 * @version $Revision$
 */
public class VNeuron extends Neuron<String> implements
        IListener<Notification>,
        ILocatable,
        Serializable,
        Comparable<VNeuron> {

    /** serialVersionUID */
    private static final long serialVersionUID = 7844715887456324364L;

    /**
     * constructor
     * 
     * @param kernel
     */
    public VNeuron(String kernel) {
        super(kernel);
    }

    @Override
    public void handleEvent(Notification event) {
        if (feedSignal((Float) event.getNotification()) == Neuron.FIRE) {
            event.addResponse(getPath(), getPath());
        }
    }

    @Override
    public String getPath() {
        return getKernel();
    }

    @Override
    public int compareTo(VNeuron o) {
        return getPath().compareTo(o.getPath());
    }
}
