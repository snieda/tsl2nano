/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 13.03.2015
 * 
 * Copyright: (c) Thomas Schneider 2015, all rights reserved
 */
package de.tsl2.nano.terminal.item.selector;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.simpleframework.xml.Element;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.execution.AntRunner;

/**
 * creates a list of options for this container, reading an sql query from a given database connection.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class SQLSelector extends Selector<String> {
    /** serialVersionUID */
    private static final long serialVersionUID = -8246582918469244440L;
    /** database connection url */
    @Element
    String driver;
    String url;
    String user;
    String passwd;
    /** sql query */
    @Element
    String query;

    /**
     * constructor
     */
    public SQLSelector() {
        super();
    }

    /**
     * constructor
     * 
     * @param roots
     * @param filter
     */
    public SQLSelector(String name, String description, String driver, String url, String user, String passwd, String query) {
        super(name, description);
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.passwd = passwd;
        this.query = query;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected List<String> createItems(Map context) {
        String tmpFile = ENV.getTempPath() + "sql" + System.currentTimeMillis();
        FileUtil.writeBytes(query.getBytes(), tmpFile, false);
        String url = StringUtil.insertProperties(this.url, context);
        List<String> items = new LinkedList<String>();
        Properties tprops = new Properties();
        tprops.put("driver", driver);
        tprops.put("url", url);
        tprops.put("userid", user);
        tprops.put("password", passwd);
        tprops.put("print", true);
        AntRunner.runTask("sql", tprops, tmpFile);
        //TODO: how to return the result?
        return items;
    }

}
