/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 06.06.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.websocket;

import de.tsl2.nano.bean.def.SecureAction;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class WebSocketAction<RETURNTYPE> extends SecureAction<RETURNTYPE> {

    /**
     * constructor
     */
    public WebSocketAction() {
        super();
    }

    /**
     * constructor
     * @param prefix
     * @param name
     * @param actionMode
     * @param isdefault
     * @param imagePath
     */
    public WebSocketAction(Class<?> prefix, String name, int actionMode, boolean isdefault, String imagePath) {
        super(prefix, name, actionMode, isdefault, imagePath);
    }

    /**
     * constructor
     * @param id
     * @param actionMode
     */
    public WebSocketAction(String id, int actionMode) {
        super(id, actionMode);
    }

    /**
     * constructor
     * @param id
     * @param shortDescription
     * @param longDescription
     * @param actionMode
     */
    public WebSocketAction(String id, String shortDescription, String longDescription, int actionMode) {
        super(id, shortDescription, longDescription, actionMode);
    }

    /**
     * constructor
     * @param id
     * @param label
     */
    public WebSocketAction(String id, String label) {
        super(id, label);
    }

    /**
     * constructor
     * @param id
     */
    public WebSocketAction(String id) {
        super(id);
    }

    @Override
    public RETURNTYPE action() throws Exception {
//        WebSocketFactory
        return null;
    }

}
