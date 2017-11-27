/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 12.02.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import java.util.Date;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.Period;
import de.tsl2.nano.core.util.StringUtil;

/**
 * used for a mapping from an public synthetic user to an internal real user. passwords and internal user-names are
 * hashed.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
class User implements Comparable<User> {
    protected String name;
    protected String passwd;
    protected Period valid;
    

    //only for interally use (loading instances through simple-xml)
    protected User() {
    }
    
    /**
     * constructor
     * @param name
     * @param passwd
     */
    public User(String name, String passwd) {
        super();
        this.name = name;
        setPasswd(passwd);
        DateUtil.getStartOfYear(null);
        Date end = DateUtil.getEndOfYear(null);
        this.valid = new Period(DateUtil.getStartOfYear(null), DateUtil.addYears(end, 10));
    }

    protected void setPasswd(String passwd) {
        if (passwd == null) {
            passwd = "";
        }
        this.passwd = hash(passwd);
    }

    private static final String hash(String txt) {
        return StringUtil.toHexString(StringUtil.cryptoHash(txt));
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the passwd.
     */
    public String getPasswd() {
        return passwd;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    public void check(String passwd) {
        passwd = passwd == null ? "" : passwd;
        if (!hash(passwd).equals(this.passwd))
            throw new IllegalArgumentException("user and/or password incorrect!");
        if (!isValid(new Date()))
            throw new IllegalArgumentException("user " + name + " is not active!");
    }
    public boolean isValid(Date when) {
        return valid.contains(new Period(when, when));
    }
    
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(User o) {
        return name.compareTo(o.name);
    }
    
    @Commit
    protected void initDeserialization() {
        if (passwd == null)
            passwd = "";
    }
    
    public static final void main(String...args) {
        if (args.length != 1) {
            System.out.println("Please give a password to be hashed!");
            System.exit(1);
        }
        System.out.println(hash(args[0]));
    }
}
