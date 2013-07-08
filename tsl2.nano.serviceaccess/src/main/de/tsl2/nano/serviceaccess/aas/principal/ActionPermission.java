/*
 * Copyright © 2002-2008 Thomas Schneider
 * Schwanthaler Strasse 69, 80336 München. Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 */
package de.tsl2.nano.serviceaccess.aas.principal;

import java.security.BasicPermission;

import de.tsl2.nano.action.IAction;

/**
 * permission for actions of type {@linkplain IAction}.
 * 
 * @author TS
 * 
 */
public class ActionPermission extends BasicPermission {

    private static final long serialVersionUID = 1L;

    public ActionPermission(String name) {
        super(name);
    }

    public ActionPermission(String name, String actions) {
        super(name, actions);
    }

}
