package de.tsl2.nano.serviceaccess;

import java.security.Principal;

import javax.security.auth.Subject;

public interface IAuthorization {
    static final String PERM_READ = "read";
    static final String PERM_WRITE = "write";
    static final String PERM_EXE = "execute";
    static final String PERM_WILDCARD = "*";

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
     * @param action
     * @return true, if user has permission to access role 'name' with action.
     */
    boolean hasAccess(String name, String action);

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

    abstract boolean hasPrincipal(Principal principal);
}