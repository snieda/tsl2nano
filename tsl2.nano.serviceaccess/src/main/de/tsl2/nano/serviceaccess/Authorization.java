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

import java.io.File;
import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.serialize.XmlUtil;
import de.tsl2.nano.serviceaccess.aas.principal.APermission;
import de.tsl2.nano.serviceaccess.aas.principal.Role;
import de.tsl2.nano.serviceaccess.aas.principal.UserPrincipal;

/**
 * provides access to {@link Subject} of javax.security.auth with it's principals.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Authorization implements IAuthorization, Serializable {
    private static final Log LOG = LogFactory.getLog(IAuthorization.class);

    Subject subject;
    long timestamp;
    /**
     * constructor
     * 
     * @param subject
     */
    public Authorization(Subject subject) {
        super();
        this.subject = subject;
        this.timestamp = System.currentTimeMillis();
        LOG.info("authorization with subject:\n" + subject);
    }

    /**
     * creates a default authorization holding a subject with given user-principal. if secure is false, an admin-role
     * holding a wildcard for all permissions will be added.
     * 
     * @param userName user for new subject
     * @param secure if false, a wildcard for all permissions will be added.
     * @return new authorization instance
     */
    public static Authorization create(String userName, boolean secure) {
        Subject subject = new Subject();
        String permissions = ENV.getConfigPath() + userName + "-permissons.xml";
        if (new File(permissions).canRead()) {
            Subject subjectDef = XmlUtil.loadXml(permissions, Subject.class);
            subject.getPrincipals().addAll(subjectDef.getPrincipals());
        } else if (ENV.get("service.autorization.new.createdefault", true)){
            //if no permission was defined, a wildcard for all permissions will be set.
            subject.getPrincipals().add(new UserPrincipal(userName));
            if (!secure) {
                subject.getPrincipals().add(new Role("admin", new APermission("*", "*")));
            }
            try {
                XmlUtil.saveXml(permissions, subject);
            } catch (Exception e) {
                LOG.error("Couldn't save authorization info in file '" + permissions + "'", e);
            }
        } else {
            throw new IllegalArgumentException("User '" + userName + "' not known!");
        }
        
        return new Authorization(subject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAccess(String name, String action) {
        boolean p = new APermission(name, action).hasAccess(getSubject());
        if (!p) {
            LOG.warn("permission for '" + name + "(" + (action == null ? PERM_EXE : action) + ")' not availabe!");
        }
        return p;
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
            LOG.debug(principal.getClass().getSimpleName() + " was not set for: " + principal.getName());
        }
        return hasPrincipal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getUser() {
        UserPrincipal principal = subject != null ? subject.getPrincipals(UserPrincipal.class).iterator().next() : null;
        return principal != null && principal.getData() != null ? ENV.format(principal.getData()) : principal.getName();
    }

    /**
     * @return the subject
     */
    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return subject.toString();
    }
}
