/*
 * @(#)SamplePrincipal.java	1.4 00/01/11
 *
 * Copyright 2000-2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * -Redistributions of source code must retain the above copyright  
 * notice, this  list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduct the above copyright 
 * notice, this list of conditions and the following disclaimer in 
 * the documentation and/or other materials provided with the 
 * distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY 
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY 
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR 
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR 
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, 
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility. 
 */

package de.tsl2.nano.serviceaccess.aas.principal;

import java.security.Principal;

/**
 * <p>
 * This class implements the <code>Principal</code> interface and represents a user role.
 * 
 * <p>
 * Principals such as this <code>Principal</code> may be associated with a particular <code>Subject</code> to augment
 * that <code>Subject</code> with an additional identity. Refer to the <code>Subject</code> class for more information
 * on how to achieve this. Authorization decisions can then be based upon the Principals associated with a
 * <code>Subject</code>.
 * 
 * @version 1.4, 01/11/00
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 */
public class UserPrincipal implements Principal, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * @serial
     */
    private final String name;

    /**
     * Create a Principal with a username.
     * 
     * @param name the username for this user.
     * 
     * @exception NullPointerException if the <code>name</code> is <code>null</code>.
     */
    public UserPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("username must not be null!");
        }

        this.name = name;
    }

    /**
     * Return the username for this <code>Principal</code>.
     * 
     * @return the username for this <code>Principal</code>
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Return a string representation of this <code>Principal</code>.
     * 
     * @return a string representation of this <code>Principal</code>.
     */
    @Override
    public String toString() {
        return ("UserPrincipal:  " + name);
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

        if (!(o instanceof UserPrincipal)) {
            return false;
        }
        final UserPrincipal that = (UserPrincipal) o;

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
