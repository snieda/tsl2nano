/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.incubation.terminal.item.selector;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.simpleframework.xml.Element;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.XmlUtil;

/**
 * creates a list of options for this container, reading xpath expression patterns from an xml file.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class XPathSelector extends Selector<String> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8246582918469244440L;

    private static final Log LOG = LogFactory.getLog(XPathSelector.class);

    /** csv file name */
    @Element
    String xml;
    /** xpath expression pattern to extract the child nodes. */
    @Element
    String xpath;

    /**
     * constructor
     */
    public XPathSelector() {
        super();
    }

    /**
     * constructor
     * 
     * @param roots
     * @param filter
     */
    public XPathSelector(String name, String description, String xmlName, String xpath) {
        super(name, description);
        this.xml = xmlName;
        this.xpath = xpath;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected List<String> createItems(Map context) {
        String xmlFileName = StringUtil.insertProperties(xml, context);
        List<String> items = new LinkedList<String>();
        Map result;
        if (!new File(xmlFileName).canRead()) {
            LOG.error("file " + xmlFileName + " can't be read!");
            result = null;
        } else {
            result = ENV.get(XmlUtil.class).xpath(xpath, xmlFileName, Map.class);
        }
        return result != null ? new ArrayList<String>(result.values()) : new LinkedList<String>();
    }

}
