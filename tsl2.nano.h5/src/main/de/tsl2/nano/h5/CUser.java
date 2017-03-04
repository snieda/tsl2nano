/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 26.02.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.UnsupportedEncodingException;

import org.simpleframework.xml.core.Commit;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.Crypt;

/**
 * encrypted user passwd
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class CUser extends User {
    private static final String ALG = Crypt.ALGO_AES;

    //16 byte key for AES
    static final byte[] key = {'m', 'E', 'I', 'n', 's', 'C', 'H', 'L', 'u', 'S', 's', 'e', 'L', '1', '2', '3'};

    protected CUser() {
    }

    public CUser(String name, String passwd) {
        super(name, passwd);
    }

    @Override
    protected void setPasswd(String passwd) {
        if (passwd == null) {
            passwd = emptyWrap();
        }
        this.passwd = Crypt.encrypt(passwd, key(), ALG);
    }

    protected String key() {
//      String k = hash(key.toString());
//      return k.substring(0, 16);
        try {
            return new String(key, "utf-8");
        } catch (UnsupportedEncodingException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    private String emptyWrap() {
        try {
            return new String(key, "utf-8");
        } catch (UnsupportedEncodingException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    /**
     * @return Returns the passwd.
     */
    public String getPasswd() {
        String p;
        p = Crypt.decrypt(passwd, key(), ALG);
        if (p.equals(emptyWrap()))
            p = "";
        return p;
    }

    @Commit
    protected void initDeserialization() {
        if (passwd == null)
            passwd = emptyWrap();
    }

}
