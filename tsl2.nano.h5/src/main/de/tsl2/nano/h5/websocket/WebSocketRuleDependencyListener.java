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

import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.incubation.specification.rules.RuleDependencyListener;

/**
 * Attribute dependency listener using websocket to refresh it's value on client-side. the real attribute value wont
 * change! Overwrite method {@link #evaluate(Object)}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class WebSocketRuleDependencyListener<T> extends RuleDependencyListener<T> {
    /** serialVersionUID */
    private static final long serialVersionUID = 7261323751717858340L;

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
    public WebSocketRuleDependencyListener(AttributeDefinition<T> attribute, String propertyName, String ruleName) {
        super(attribute, propertyName, ruleName);
    }

    @Override
    public void handleEvent(Object source) {
        WebSocketDependencyListener.sendValue(attributeID, propertyName, evaluate(source));
    }
}
