/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 09.07.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.websocket;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.def.IAttributeDefinition;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.specification.rules.RuleDependencyListener;

/**
 * Attribute dependency listener using websocket to refresh it's value on client-side. the real attribute value wont
 * change! Overwrite method {@link #evaluate(Object)}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class WebSocketRuleDependencyListener<T> extends RuleDependencyListener<T, WSEvent> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7261323751717858340L;

    private static final Log LOG = LogFactory.getLog(WebSocketRuleDependencyListener.class);
    
    /**
     * constructor
     */
    public WebSocketRuleDependencyListener() {
    }

    /**
     * constructor
     * 
     * @param attribute
     */
    public WebSocketRuleDependencyListener(IAttributeDefinition<T> attribute, String propertyName, String ruleName) {
        super(attribute, propertyName, ruleName);
    }

    @Override
    public void handleEvent(WSEvent evt) {
        if (evt.breakEvent == true) { // event was consumed
            LOG.trace(this + ": ignoring event '" + evt + "' in cause of already be consumed!");
            return;
        }
        initAttribute(evt);
        //the websocket-listener sends the result to the edit-field - so it must be formatted
        WebSocketDependencyListener.sendValue(attributeID, propertyName, getAttribute().getFormat().format(evaluate(evt)));
    }
}
