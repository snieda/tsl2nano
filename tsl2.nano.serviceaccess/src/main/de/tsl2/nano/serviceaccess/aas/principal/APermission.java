/*
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 */
package de.tsl2.nano.serviceaccess.aas.principal;

import java.security.BasicPermission;
import java.security.Permission;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;


/**
 * permission for actions e.g. of type {@linkplain de.tsl2.nano.action.IAction}.
 * 
 * @author TS
 * 
 */
public class APermission extends BasicPermission {

    private static final long serialVersionUID = 1L;

    String actions;
    
    public APermission(String name) {
        super(name);
    }

    public APermission(String name, String actions) {
        super(name, actions);
        //actions will be ignored by super class
        this.actions = actions;
    }

    @Override
    public void checkGuard(Object object) throws SecurityException {
        super.checkGuard(object);
        
        if (object instanceof Subject) {
            Subject subject = (Subject) object;
            Set<Role> roles = subject.getPrincipals(Role.class);
            for (Role role : roles) {
                Set<BasicPermission> permissions = role.getPermissions();
                for (BasicPermission p : permissions) {
                    if (p.implies(this))
                        return;
                }
            }
        } else {
            //TODO: are there other use cases?
        }
        throw new SecurityException("Object '" + object + "' has no permission to access " + this);
    }
    
    /**
     * hasAccess
     * @param subject
     * @return
     */
    public boolean hasAccess(Subject subject) {
        try {
            checkGuard(subject);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
    
    @Override
    public boolean implies(Permission p) {
        boolean result = super.implies(p);
        if (!result)
            return result;
        
        BasicPermission bp = (BasicPermission) p;
        if (actions == null || actions.equals("*"))
            return true;
        if (bp.getActions() == null || bp.getActions().equals("*"))
            return false;
        List<String> s = Arrays.asList(actions.split(","));
        List<String> sbp = Arrays.asList(bp.getActions().split(","));
        return s.containsAll(sbp);
        
    }
    
    @Override
    public String getActions() {
        return actions;
    }
}
