/*
 * Copyright � 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 M�nchen. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielf�ltigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 */
package de.tsl2.nano.serviceaccess.aas.principal;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tsl2.nano.action.CommonAction;
import de.tsl2.nano.action.IActivator;
import de.tsl2.nano.serviceaccess.ServiceFactory;

/**
 * base class to define security managed actions.
 * 
 * @author TS
 * 
 */
public abstract class AbstractPrincipalAction<RETURNTYPE> extends CommonAction<RETURNTYPE> {
    private static final Log LOG = LogFactory.getLog(AbstractPrincipalAction.class);

    public AbstractPrincipalAction() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public AbstractPrincipalAction(String id,
            String shortDescription,
            String longDescription,
            IActivator enabler,
            Collection<String> receiverIDs) {
        super(id, shortDescription, longDescription, enabler, receiverIDs);
    }

    /**
     * {@inheritDoc}
     */
    public AbstractPrincipalAction(String id, String shortDescription, String longDescription, IActivator actionEnabler) {
        super(id, shortDescription, longDescription, actionEnabler);
    }

    /**
     * {@inheritDoc}
     */
    public AbstractPrincipalAction(String id, String shortDescription, String longDescription) {
        super(id, shortDescription, longDescription);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RETURNTYPE activate() {
        // the static permission check is done by the subject.doAs()
        if (!isPermitted()) {
            throw new SecurityException("the current user is not allowed to start the action " + getId());
        }
        final CommonAction<RETURNTYPE> _thisAction = this;
        final Subject subject = ServiceFactory.instance().getSubject();
        // needs security policy entry "doAs"
        return Subject.doAs(subject, new PrivilegedAction<RETURNTYPE>() {
            @Override
            public RETURNTYPE run() {
                _thisAction.run();
                return _thisAction.getLastResult();
            }
        });
    }

    /**
     * @return true, if security managers permission check was ok.
     */
    public boolean isPermitted() {
        try {
            // needs security policy entry "createAccessControlContext"
            final AccessControlContext accessControlContext = new AccessControlContext(AccessController.getContext(),
                new SubjectDomainCombiner(ServiceFactory.instance().getSubject()));
            accessControlContext.checkPermission(new ActionPermission(getId()));
            // System.getSecurityManager().checkPermission(new
            // ActionPermission(getId()));
            return true;
        } catch (final SecurityException e) {
            LOG.error("", e);
            return false;
        }
    }
}