/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Jun 2, 2010
 * 
 * Copyright: (c) Thomas Schneider 2010, all rights reserved
 */
package de.tsl2.nano.serviceaccess.aas.principal;

import java.io.Serializable;
import java.security.BasicPermission;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

/**
 * user role principal
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Role implements Principal, Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = -276482232395760912L;
    @Attribute
    String name;

    @ElementList(inline=true, name="permission", type=BasicPermission.class)
    Set<BasicPermission> permissions;
    
    public Role() {
        this("<deserialized>");
    }
    
    /**
     * constructor
     * 
     * @param name role name
     */
    public Role(String name) {
        this(name, new APermission[0]);
    }

    public Role(String name, APermission... permissions) {
        assert name != null : "name must not be null";
        this.name = name;
        this.permissions = new HashSet<BasicPermission>(Arrays.asList(permissions));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    public Set<BasicPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<BasicPermission> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(BasicPermission permission) {
        this.permissions.add(permission);
    }
    
    /**
     * Return a string representation of this <code>Principal</code>.
     * 
     * @return a string representation of this <code>Principal</code>.
     */
    @Override
    public String toString() {
        return ("Role:  " + name);
    }

    /**
     * Compares the specified Object with this <code>Principal</code> for equality. Returns true if the given object is
     * also a <code>Principal</code> and the two Principals have the same username.
     * 
     * @param o Object to be compared for equality with this <code>Principal</code>.
     * 
     * @return true if the specified Object is equal equal to this <code>Principal</code>.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof Role)) {
            return false;
        }
        final Role that = (Role) o;

        if (this.getName().equals(that.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Return a hash code for this <code>Principal</code>.
     * 
     * @return a hash code for this <code>Principal</code>.
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
