/*
 * Alle Rechte vorbehalten.
 * Copyright © 2002-2009 Thomas Schneider
 * Weiterverbreitung, Benutzung, Vervielfältigung oder Offenlegung,
 * auch auszugsweise, nur mit Genehmigung.
 * 
 * $Id$ 
 */
package de.tsl2.nano.serviceaccess.aas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import de.tsl2.nano.serviceaccess.aas.module.AbstractLoginModule;

/**
 * The application implements the CallbackHandler.
 * 
 * <p>
 * This application is text-based. Therefore it displays information to the user using the OutputStreams System.out and
 * System.err, and gathers input from the user using the InputStream System.in.
 * 
 * @author TS 20.12.2009
 * @version $Revision$
 */
public class ConsoleCallbackHandler implements CallbackHandler {

    /**
     * Invoke an array of Callbacks.
     * 
     * <p>
     * 
     * @param callbacks an array of <code>Callback</code> objects which contain the information requested by an
     *            underlying security service to be retrieved or displayed.
     * 
     * @exception java.io.IOException if an input or output error occurs.
     *                <p>
     * 
     * @exception UnsupportedCallbackException if the implementation of this method does not support one or more of the
     *                Callbacks specified in the <code>callbacks</code> parameter.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof TextOutputCallback) {

                // display the message according to the specified type
                final TextOutputCallback toc = (TextOutputCallback) callbacks[i];
                switch (toc.getMessageType()) {
                case TextOutputCallback.INFORMATION:
                    System.out.println(toc.getMessage());
                    break;
                case TextOutputCallback.ERROR:
                    System.out.println("ERROR: " + toc.getMessage());
                    break;
                case TextOutputCallback.WARNING:
                    System.out.println("WARNING: " + toc.getMessage());
                    break;
                default:
                    throw new IOException("Unsupported message type: " + toc.getMessageType());
                }

            } else if (callbacks[i] instanceof NameCallback) {

                // prompt the user for a username
                final NameCallback nc = (NameCallback) callbacks[i];

                System.err.print(nc.getPrompt());
                System.err.flush();

                final String username = System.getProperty(AbstractLoginModule.PROP_USER);
                if (username == null) {
                    nc.setName((new BufferedReader(new InputStreamReader(System.in))).readLine());
                } else {
                    nc.setName(username);
                }

            } else if (callbacks[i] instanceof PasswordCallback) {

                // prompt the user for sensitive information
                final PasswordCallback pc = (PasswordCallback) callbacks[i];
                System.err.print(pc.getPrompt());
                System.err.flush();

                final String password = System.getProperty(AbstractLoginModule.PROP_PASSWORD);
                if (password == null) {
                    pc.setPassword(readPassword(System.in));
                } else {
                    pc.setPassword(password.toCharArray());
                }

            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    // Reads user password from given input stream.
    private char[] readPassword(InputStream in) throws IOException {

        char[] lineBuffer;
        char[] buf;

        buf = lineBuffer = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;

        loop: while (true) {
            switch (c = in.read()) {
            case -1:
            case '\n':
                break loop;

            case '\r':
                final int c2 = in.read();
                if ((c2 != '\n') && (c2 != -1)) {
                    if (!(in instanceof PushbackInputStream)) {
                        in = new PushbackInputStream(in);
                    }
                    ((PushbackInputStream) in).unread(c2);
                } else {
                    break loop;
                }

            default:
                if (--room < 0) {
                    buf = new char[offset + 128];
                    room = buf.length - offset - 1;
                    System.arraycopy(lineBuffer, 0, buf, 0, offset);
                    Arrays.fill(lineBuffer, ' ');
                    lineBuffer = buf;
                }
                buf[offset++] = (char) c;
                break;
            }
        }

        if (offset == 0) {
            return null;
        }

        final char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');

        return ret;
    }
}
