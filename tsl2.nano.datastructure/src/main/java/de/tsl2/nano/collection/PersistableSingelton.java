/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Oct 13, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.collection;

import java.io.File;
import java.util.Hashtable;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.serialize.XmlUtil;

/**
 * simple base class for a stored object as singelton.
 * <p>
 * NOT USED YET!
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@SuppressWarnings("rawtypes")
public class PersistableSingelton extends Hashtable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static PersistableSingelton self = null;

    /**
     * constructor
     */
    private PersistableSingelton() {
        super();
    }

    /**
     * instance
     * 
     * @return singelton
     */
    public static final PersistableSingelton instance() {
        if (self == null) {
            final File f = new File(getStorePath());
            if (f.exists()) {
                self = ENV.get(XmlUtil.class).loadXml(getStorePath(), PersistableSingelton.class);
            } else {
                self = new PersistableSingelton();
            }
        }
        return self;
    }

    /**
     * exists
     * 
     * @return true, if serializing file exists
     */
    public static final boolean exists() {
        return new File(getStorePath()).exists();
    }

    /**
     * to be overwritten - defines the xml serializing file name
     * 
     * @return file name
     */
    protected static String getStorePath() {
        return System.getProperty("user.home") + PersistableSingelton.class.getSimpleName() + ".xml";
    }

    /**
     * save
     */
    public void save() {
        try {
            ENV.get(XmlUtil.class).saveXml(getStorePath(), this);
        } catch (final Exception e) {
            ManagedException.forward(e);
        }
    }

    public static void clearCache() {
        self = null;
    }
}
