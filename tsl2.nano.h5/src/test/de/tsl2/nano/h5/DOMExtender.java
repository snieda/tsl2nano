/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 22.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tsl2.nano.core.ISession;
import de.tsl2.nano.h5.plugin.IDOMDecorator;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class DOMExtender implements IDOMDecorator {

    /**
     * {@inheritDoc}
     */
    @Override
    public void decorate(Document doc, ISession<?> session) {
        Element node = doc.createElement("h1");
        node.setTextContent(this.getClass().getName());
       doc.getDocumentElement().appendChild(node);
    }

}
