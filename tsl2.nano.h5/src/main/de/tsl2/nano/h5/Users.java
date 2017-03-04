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

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;

/**
 * create/load user mapping for secure logins
 * <p/>
 * see {@link #auth(String, String)} for further informations
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
@Default(value = DefaultType.FIELD, required = false)
public class Users {
    private static final Log LOG = LogFactory.getLog(PersistenceUI.class);
    private static String NAME_USERMAPPING = "users";

    @ElementMap(inline = true, entry = "mapping", key = "auth", required = false, keyType = User.class, value = "persist", valueType = CUser.class)
    private Map<User, User> userMapping = new TreeMap<User, User>();

    /**
     * constructor
     */
    private Users() {
        super();
    }

    public static Users load() {
        Users userCheck = null;
        try {
            userCheck = ENV.load(NAME_USERMAPPING, Users.class);
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            if (userCheck == null)
                userCheck = new Users();
        }
        return userCheck;
    }

    /**
     * checks the given (perhaps synthetic) user and may do some mapping to a real user. depends on ENV property
     * "app.login.secure".
     * <p/>
     * a mapping uses the authentication user (auth) to be checked against the password and a validation period. if
     * the check is ok, the persist/connection user will be returned.
     * 
     * @param name public user
     * @param passwd user passwd
     * @return internal user
     * @throws IllegalArgumentException if secure mode and no user entry was found.
     */
    public User auth(String name, String passwd) {
        if (!ENV.get("app.login.secure", false)) {
            //not secure: add all new users
            User user = new User(name, passwd);
            userMapping.put(user, new CUser(name, passwd));
            ENV.save(NAME_USERMAPPING, this);
        }
        // only defined (userMapping) users should be connected
        User user = null, cUser = null;
        for (Map.Entry<User, User> e : userMapping.entrySet()) {
            if (e.getKey().getName().equals(name)) {
                user = e.getKey();
                user.check(passwd);
                cUser = e.getValue();
                break;
            }
        }
        if (cUser == null)
            throw new IllegalArgumentException("user and/or password incorrect!");
        return cUser;
    }
}
