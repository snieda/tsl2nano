package de.tsl2.nano.serviceaccess;

import java.security.Principal;

import javax.security.auth.Subject;

public interface IAuthorization {

    /**
     * checks the current user subject to have an authorization for given role. delegates to {@link #hasAccess(String, String)} with type EXECUTE
     * 
     * @param roleName
     * @return true, if user has role
     */
    boolean hasRole(String roleName);

    /**
     * hasRole
     * 
     * @param roleName
     * @return true, if user has role
     */
    boolean hasAccess(String name, String type);

    /**
     * getUserObject
     * 
     * @return user object, if defined!
     */
    Object getUser();

    /**
     * getSubject
     * @return
     */
    Subject getSubject();

    public abstract boolean hasPrincipal(Principal principal);
}