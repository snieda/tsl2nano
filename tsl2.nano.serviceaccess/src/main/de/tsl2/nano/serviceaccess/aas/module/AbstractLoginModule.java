/*
 * @(#)AbstractLoginModule.java	1.0 20.12.2009
 * Copyright © 2002-2008 Thomas Schneider
 * Alle Rechte vorbehalten.
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 *
 */

package de.tsl2.nano.serviceaccess.aas.module;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;

import de.tsl2.nano.core.Environment;
import de.tsl2.nano.core.Messages;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.serviceaccess.Authorization;
import de.tsl2.nano.serviceaccess.IAuthorization;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import de.tsl2.nano.serviceaccess.aas.principal.UserPrincipal;

/**
 * <h2>1. Authentication</h2>
 * 
 * <pre>
 * override the methods:
 * - authenticate() (if you need more than user/password override authenticat(callbacks[]), too.
 * - authorize()
 *  
 * precondition on start:
 * -Djava.security.debug=all
 * -Djava.security.manager 
 * -Djava.security.policy=file:./bin/logincontext.policy
 * -Djava.security.auth.login.config=jaas-login.config
 * 
 * To set the user and password through program arguments, set system properties:
 * jaas.login.user for user name
 * jaas.login.password for password
 * 
 * On problems, check the following:
 * - the login-module name in the *login.config should be the same as granted in the policy file. 
 * - AccessControlException: 
 * -- is the policy file located in the classpath? (on rcp projects, the user.dir starts in the eclipse directory)
 * -- look at 'JAVA_HOME/jre/lib/security/java.policy' for available system grants. optional, it is
 *    possible to define system grants in ${user.home}/java.policy.
 * 
 * </pre>
 * 
 * For further informations, see {@linkplain Configuration} {@linkplain SecurityManager} and
 * {@linkplain LoginContext#LoginContext(String, CallbackHandler)}. {@linkplain http
 * ://openbook.galileocomputing.de/javainsel8/javainsel_26_003.htm} {@linkplain http
 * ://java.sun.com/developer/onlineTraining/Programming/JDCBook/appB .html#runtime} {@linkplain http
 * ://www.mooreds.com/jaas.html}
 * <p>
 * The base LoginModule authenticates users with a password. If the user successfully authenticates itself, a
 * <code>UserPrincipal</code> with user name is added to the Subject.
 * 
 * <p>
 * This LoginModule recognizes the debug option. If set to true in the login Configuration, debug messages will be
 * logged.
 * 
 * <h2>2. Authorization</h2>
 * 
 * <pre>
 * The default authorization() will set UserPrincipals with users name as principal. Means: the role name
 * is the users name. You have to configure the 'logincontext.policy' file to define the principals (roles)
 * and there permissions (rights). 
 * e.g.: grant ...UserPermission { ...ActionPermission action.exit;}
 * Only implemented 'AbstractPrincipalAction' instances will be able to check the permissions!
 * </pre>
 * 
 * @author TS
 * @version 1.0, 20.12.2009
 */
public class AbstractLoginModule implements LoginModule {
    protected static final Log LOG = LogFactory.getLog(AbstractLoginModule.class);

    public static final String PROP_USER = "jaas.login.user";
    public static final String PROP_PASSWORD = "jaas.login.password";
    private static final String ENCSUFFIX = "lkj sdf9872450nLJHG OUTWZ)(//&%!";
    // initial state
    protected Subject subject;
    private CallbackHandler callbackHandler;
    // configurable option
    private boolean debug = false;

    // the authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    // username and password
    protected String username;
    protected char[] password;
    protected char[] password1;
    protected char[] password2;

    // user principal
    private UserPrincipal userPrincipal;

    /**
     * Initialize this <code>LoginModule</code>.
     * 
     * @param subject the <code>Subject</code> to be authenticated.
     * 
     * @param callbackHandler a <code>CallbackHandler</code> for communicating with the end user (prompting for user
     *            names and passwords, for example).
     * 
     * @param sharedState shared <code>LoginModule</code> state.
     * 
     * @param options options specified in the login <code>Configuration</code> for this particular
     *            <code>LoginModule</code>.
     */
    @Override
    public void initialize(Subject subject,
            CallbackHandler callbackHandler,
            Map<java.lang.String, ?> sharedState,
            Map<java.lang.String, ?> options) {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        // initialize any configured options
        debug = "true".equalsIgnoreCase((String) options.get("debug"));
    }

    /**
     * Authenticate the user by prompting for a user name and password.
     * 
     * <p>
     * 
     * @return true in all cases since this <code>LoginModule</code> should not be ignored.
     * 
     * @exception FailedLoginException if the authentication fails.
     *                <p>
     * 
     * @exception LoginException if this <code>LoginModule</code> is unable to perform the authentication.
     */
    @Override
    public boolean login() throws LoginException {

        // prompt for a user name and password
        if (callbackHandler == null) {
            throw new LoginException("Error: no CallbackHandler available " + "to garner authentication information from the user");
        }

        final Callback[] callbacks = new Callback[4];
        callbacks[0] = new NameCallback("user name: ");
        callbacks[1] = new PasswordCallback("password: ", false);
        callbacks[2] = new PasswordCallback("new password 1: ", false);
        callbacks[3] = new PasswordCallback("new password 2: ", false);

        try {
            callbackHandler.handle(callbacks);
        } catch (final java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (final UnsupportedCallbackException uce) {
            throw new LoginException("Error: " + uce.getCallback().toString()
                + " not available to garner authentication information "
                + "from the user");
        }

        return authenticate(callbacks);
    }

    /**
     * override this method, if you need more than a user and password authentication.
     * 
     * @param callbacks
     * @return true, if login is ok
     * @throws FailedLoginException on authentication error
     */
    protected boolean authenticate(Callback[] callbacks) throws FailedLoginException {
        username = ((NameCallback) callbacks[0]).getName();
        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        password = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, password, 0, tmpPassword.length);
        password = encode(password);
        ((PasswordCallback) callbacks[1]).clearPassword();

        //on password changing we need this
        tmpPassword = ((PasswordCallback) callbacks[2]).getPassword();
        if (tmpPassword != null) {
            password1 = new char[tmpPassword.length];
            System.arraycopy(tmpPassword, 0, password1, 0, tmpPassword.length);
            password1 = encode(password1);
            tmpPassword = ((PasswordCallback) callbacks[3]).getPassword();
            password2 = new char[tmpPassword.length];
            System.arraycopy(tmpPassword, 0, password2, 0, tmpPassword.length);
//            LOG.debug("user entered new password with length: " + password1.length);
            password2 = encode(password2);
        } else {
            password1 = null;
            password2 = null;
        }

        // print debugging information
        LOG.debug("user entered user name: " + username);
//        LOG.debug("user entered password-length: " + password.length);

        succeeded = authenticate();
        return succeeded;
    }

    /**
     * @return true, if authentication is ok
     * @throws FailedLoginException
     */
    protected boolean authenticate() throws FailedLoginException {
        // verify the username/password
        boolean usernameCorrect = false;
        if (username.length() > 0) {
            usernameCorrect = true;
        }
        if (usernameCorrect && password.length > 0) {
            if (debug) {
                LOG.debug("authentication succeeded");
            }
            return true;
        } else {

            // authentication failed -- clean out state
            if (debug) {
                LOG.debug("authentication failed");
            }
            username = null;
            for (int i = 0; i < password.length; i++) {
                password[i] = ' ';
            }
            password = null;
            if (!usernameCorrect) {
                throw new FailedLoginException(Messages.getString("tsl2nano.login.error.user"));
            } else {
                throw new FailedLoginException(Messages.getString("tsl2nano.login.error.password"));
            }
        }
    }

    /**
     * encodes the given word
     * @param word
     * @return encoded word
     */
    protected char[] encode(char[] word) {
        StringBuilder sb = new StringBuilder(String.valueOf(word));
        int i;
        for (i = 0; i < word.length; i++) {
            sb.insert(i, ENCSUFFIX.charAt(i));
        }
        if (i < ENCSUFFIX.length())
            sb.append(ENCSUFFIX.substring(i));
        return StringUtil.toHexString(StringUtil.cryptoHash(sb.toString())).toCharArray();
    }
    
    /**
     * <p>
     * This method is called if the LoginContext's overall authentication succeeded (the relevant REQUIRED, REQUISITE,
     * SUFFICIENT and OPTIONAL LoginModules succeeded).
     * 
     * <p>
     * If this LoginModule's own authentication attempt succeeded (checked by retrieving the private state saved by the
     * <code>login</code> method), then this method associates a <code>UserPrincipal</code> with the
     * <code>Subject</code> located in the <code>LoginModule</code>. If this LoginModule's own authentication attempted
     * failed, then this method removes any state that was originally saved.
     * 
     * <p>
     * 
     * @exception LoginException if the commit fails.
     * 
     * @return true if this LoginModule's own login and commit attempts succeeded, or false otherwise.
     */
    @Override
    public boolean commit() throws LoginException {
        if (succeeded == false) {
            return false;
        } else {
            // add a Principal (authenticated identity)
            // to the Subject

            // assume the user we authenticated is the UserPrincipal
            authorize();

            // in any case, clean out state
            username = null;
            for (int i = 0; i < password.length; i++) {
                password[i] = ' ';
            }
            password = null;

            commitSucceeded = true;
            return commitSucceeded;
        }
    }

    /**
     * override this method to set the user roles!
     */
    protected void authorize() {
        userPrincipal = new UserPrincipal(username);
        if (!subject.getPrincipals().contains(userPrincipal)) {
            subject.getPrincipals().add(userPrincipal);
        }

        //deprecated: access to the user subject should be done through environment
        ServiceFactory.instance().setSubject(subject);
        
        Environment.addService(IAuthorization.class, new Authorization(subject));
        LOG.debug("added UserPrincipal to Subject");
    }

    /**
     * <p>
     * This method is called if the LoginContext's overall authentication failed. (the relevant REQUIRED, REQUISITE,
     * SUFFICIENT and OPTIONAL LoginModules did not succeed).
     * 
     * <p>
     * If this LoginModule's own authentication attempt succeeded (checked by retrieving the private state saved by the
     * <code>login</code> and <code>commit</code> methods), then this method cleans up any state that was originally
     * saved.
     * 
     * <p>
     * 
     * @exception LoginException if the abort fails.
     * 
     * @return false if this LoginModule's own login and/or commit attempts failed, and true otherwise.
     */
    @Override
    public boolean abort() throws LoginException {
        if (succeeded == false) {
            return false;
        } else if (succeeded == true && commitSucceeded == false) {
            // login succeeded but overall authentication failed
            succeeded = false;
            username = null;
            if (password != null) {
                for (int i = 0; i < password.length; i++) {
                    password[i] = ' ';
                }
                password = null;
            }
            userPrincipal = null;
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    /**
     * Logout the user.
     * 
     * <p>
     * This method removes the <code>UserPrincipal</code> that was added by the <code>commit</code> method.
     * 
     * <p>
     * 
     * @exception LoginException if the logout fails.
     * 
     * @return true in all cases since this <code>LoginModule</code> should not be ignored.
     */
    @Override
    public boolean logout() throws LoginException {

        subject.getPrincipals().remove(userPrincipal);
        succeeded = commitSucceeded;
        username = null;
        if (password != null) {
            for (int i = 0; i < password.length; i++) {
                password[i] = ' ';
            }
            password = null;
        }
        userPrincipal = null;
        return true;
    }
}
