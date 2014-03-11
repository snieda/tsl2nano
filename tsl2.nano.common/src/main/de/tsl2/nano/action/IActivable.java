/*
 * Copyright © 2002-2009 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.action;

/**
 * Implementors of this interface provide the possibility to check if a component should be active or not. will be used
 * as callback by the tsl2nano framework.
 * 
 * @author ts 06.03.2009
 * @version $Revision$
 */
public interface IActivable {

    /**
     * used as callback by framework to check for activation/enabling. may be called more than one time for refreshings.
     * implementation should be fast - please don't call remote services.
     * 
     * @return true if the component should be activated (enabled and visible).
     */
    boolean isActive();

    /** {@link #isActive()} will return always true */
    public static final IActivable ACTIVE = new IActivable() {
        @Override
        public boolean isActive() {
            return true;
        }
        @Override
        public String toString() {
            return "Always Active";
        }
    };

    /** {@link #isActive()} will return always false */
    public static final IActivable INACTIVE = new IActivable() {
        @Override
        public boolean isActive() {
            return false;
        }
        @Override
        public String toString() {
            return "Always Inactive";
        }
    };
}
