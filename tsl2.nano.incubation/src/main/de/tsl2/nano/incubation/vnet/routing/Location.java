/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Dec 2, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.incubation.vnet.routing;

import java.io.Serializable;

import de.tsl2.nano.incubation.vnet.ILocatable;
import de.tsl2.nano.incubation.vnet.Notification;
import de.tsl2.nano.math.vector.Vector3D;
import de.tsl2.nano.messaging.IListener;

/**
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$ 
 */
public class Location implements IListener<Notification>, ILocatable, Serializable, Comparable<Location> {
    String name;
    Vector3D pos;
    
    /** serialVersionUID */
    private static final long serialVersionUID = 4978868720558695580L;

    public Location(String name, float x, float y, float z) {
        this.name = name;
        this.pos = new Vector3D(x, y, z);
    }

    @Override
    public int compareTo(Location o) {
        return (int) Vector3D.subtract(pos, o.pos).len();
    }

    @Override
    public String getPath() {
        return name;
    }

    @Override
    public void handleEvent(Notification event) {
        
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(pos.len());
    }
    
    @Override
    public boolean equals(Object obj) {
        return name.equals(((Location)obj).name);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
