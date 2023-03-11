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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.ElementMap;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.ConcurrentUtil;
import de.tsl2.nano.serviceaccess.Authorization;

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

    private static final Map<String, Integer>userTries = new ConcurrentHashMap<String, Integer>();
    
    /**
     * constructor
     */
    private Users() {
        super();
    }

    public static Users load() {
        return load(false);
    }

    public static Users load(boolean force) {
        Users userCheck = null;
        try {
        	if (force || ENV.get("app.login.secure", false))
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
        return auth(name, passwd, name, passwd, false);
    }
    
    public User auth(String name, String passwd, String dbName, String dbPasswd, boolean admin) {
        try{
            if (!ENV.get("app.login.secure", false)) {
                //not secure: add all new users
                User user = new User(name, passwd);
                // special case on secure=true -> after closing last session, the Users instance will be lost
                if (userMapping.isEmpty())
                    userMapping = load(true).userMapping;
                userMapping.put(user, new CUser(dbName, dbPasswd));
                ENV.save(NAME_USERMAPPING, this);
                if (admin)
                    Authorization.create(name, false);
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
            if (cUser == null) {
                throw new IllegalArgumentException("user and/or password incorrect!");
            }
            resetRetries(name);
            return cUser;
        } catch (Exception ex) {
            sleepOnRetry(name);
            ManagedException.forward(ex);
            return null;
        }
    }
    
    private static void sleepOnRetry(String userName) {
        if (userName == null)
            userName = "XXXXXXXX";
        Integer tries = userTries.get(userName);
        tries = tries == null ? 2 : tries + 1;
        userTries.put(userName, tries);
        long sleepTime = tries * tries * tries * ENV.get("app.session.loginfailure.sleep.mul.ms", 1000);
        Message.send("sleep on retry: user login failed " + tries + " times, sleeping: " + sleepTime);
        ConcurrentUtil.sleep(sleepTime);
    }

    private static void resetRetries(String userName) {
        userTries.remove(userName);
    }
    
}
