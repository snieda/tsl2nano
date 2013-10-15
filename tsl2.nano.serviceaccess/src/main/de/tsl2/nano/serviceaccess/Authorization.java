/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.10.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.serviceaccess;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Messages;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.serviceaccess.aas.principal.Role;
import de.tsl2.nano.serviceaccess.aas.principal.UserPrincipal;

/**
 * provides access to {@link Subject} of javax.security.auth with it's principals.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Authorization implements IAuthorization {
    private static final Log LOG = LogFactory.getLog(IAuthorization.class);

    Subject subject;

    /**
     * constructor
     * 
     * @param subject
     */
    public Authorization(Subject subject) {
        super();
        this.subject = subject;
        LOG.info("authorization with subject:\n" + subject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRole(String roleName) {
        return hasPrincipal(new Role(roleName));
    }

    /**
     * checkPrincipal, throws SecurityException if not
     * 
     * @param principal principal
     */
    public void checkPrincipal(Principal principal) {
        if (!hasPrincipal(principal)) {
            final String msg = Messages.getFormattedString(Messages.getString("tsl2nano.login.noprincipal"),
                new Object[] { getUser(), principal.getName() });
            throw new SecurityException(msg);
        }
    }

    /**
     * hasPrincipal
     * 
     * @param principal {@link Principal}
     * @return true, if subject contains this principal
     */
    @Override
    public boolean hasPrincipal(Principal principal) {
        if (getSubject() == null) {
            LOG.warn("ServiceFactory.hasPrincipal: no subject defined!");
            return false;
        }
        final Set<? extends Principal> subjectPrincipals = getSubject().getPrincipals(principal.getClass());

//        /*
//         * if no principal was defined, the principal-permissions will be
//         * switched off.
//         */
//        if (subjectPrincipals.isEmpty()) {
//            LOG.warn("ServiceFactory.hasPrincipal: no principal of type " + principal.getClass().getSimpleName()
//                + " was defined ==> permissions-system will be switched off for this type!");
//            return true;
//        }

        final boolean hasPrincipal = subjectPrincipals.contains(principal);
        if (!hasPrincipal) {
            LOG.warn(principal.getClass().getSimpleName() + " was not set for: " + principal.getName());
        }
        return hasPrincipal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getUser() {
        return subject != null ? subject.getPrincipals(UserPrincipal.class).iterator().next().getData() : null;
    }

    /**
     * @return the subject
     */
    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public boolean hasAccess(String name, String action) {
        return true;
    }

    @Override
    public String toString() {
        return subject.toString();
    }
}
