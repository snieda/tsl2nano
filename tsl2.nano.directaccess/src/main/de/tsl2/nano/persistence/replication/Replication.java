/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 16.11.2013
 * 
 * Copyright: (c) Thomas Schneider 2013, all rights reserved
 */
package de.tsl2.nano.persistence.replication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;

import de.tsl2.nano.Environment;
import de.tsl2.nano.exception.ForwardedException;
import de.tsl2.nano.execution.CompatibilityLayer;
import de.tsl2.nano.log.LogFactory;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * Persistence bean only to be used as additional connection. Default values are set for hsqldb/hibernate
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class Replication extends Persistence implements Runnable {
    /** serialVersionUID */
    private static final long serialVersionUID = 8157364611484654367L;

    private static final Log LOG = LogFactory.getLog(Replication.class);
    /** xml serialization of Persistence object */
    public static final String FILE_REPLICATION_BEAN = "replication-bean.xml";

    Map<String, Object> p;

    /**
     * constructor
     */
    public Replication() {
        super();
        persistenceUnit = "replication";
        transactionType = "RESOURCE_LOCAL";
        provider = "org.hibernate.ejb.HibernatePersistence";
        connectionDriverClass = "org.hsqldb.jdbcDriver";
        connectionUrl = "jdbc:hsqldb:hsql://localhost:9898";
        connectionUserName = "SA";
        connectionPassword = "";
        hibernateDialect = "org.hibernate.dialect.HSQLDialect";
        defaultSchema = "PUBLIC";
        datasourceClass = "org.hsqldb.jdbc.jdbcDataSource";
        port = "9898";
        database = "replication";
    }

    public static Replication current() {
        if (Persistence.exists()) {
            return (Replication) FileUtil.loadXml(getPath(FILE_REPLICATION_BEAN));
        } else {
            return new Replication();
        }
    }

    /**
     * addPersistenceProperties
     * 
     * @param prop
     */
    @Override
    protected void addPersistenceProperties(Persistence parent, Map<String, Object> prop) {
        database = "replication-" + parent.getConnectionUserName();
        prop.put("replication-unit", "replication");
        prop.put("replication.transaction-type", "RESOURCE_LOCAL");
        prop.put("replication.provider", parent.getProvider());
        prop.put("replication.jta-data-source", parent.getJtaDataSource());
        prop.put("replication.jar-file", parent.getJarFile());
        prop.put("replication.dialect", getHibernateDialect());
        prop.put("replication.driver_class", getConnectionDriverClass());
        prop.put("replication.url", getConnectionUrl());
        prop.put("replication.database", getDatabase());
        prop.put("replication.username", parent.getConnectionUserName());
        prop.put("replication.password", parent.getConnectionPassword());

        p = prop;

        if (Environment.get("use.database.replication", false)) {
            startReplicationThread();
        }
    }

    @Override
    protected String getBeanFileName() {
        return FILE_REPLICATION_BEAN;
    }

    public Object actionOk() {
        return this;
    }

    public String save() throws IOException {
        FileUtil.removeToBackup(getPath(getBeanFileName()));
        FileUtil.saveXml(this, getPath(getBeanFileName()));
        return null;
    }

    /**
     * startReplicationThread
     */
    private void startReplicationThread() {
        try {
            //first: check, if connection available
            Socket socket = new Socket((String) null, Integer.valueOf(getPort()));
            socket.close();
            LogFactory.getLog(Replication.class).warn("connection localhost:" + getPort()
                + " already in use. can't start the replication-database!");
        } catch (Exception e) {
            LogFactory.getLog(Replication.class).info("starting replication database '" + database
                + "' on port "
                + port);
            Thread replicationRunner = Executors.defaultThreadFactory().newThread(this);
            replicationRunner.setName("replication-database");
            replicationRunner.setDaemon(true);
            replicationRunner.start();
        }
    }

    @Override
    public void run() {
        String databaseName = getPath((String) p.get("replication.database"));
        String databaseFile = databaseName + ".script";
        if (!new File(databaseFile).exists()) {
            /*
             * create hsqldb database script
             */
            try {
                URL resource = Thread.currentThread()
                    .getContextClassLoader()
                    .getResource("replication.script");
                //on nested jars, the resource may be unloadable...
                if (resource == null) {
                    LOG.error("unable to load replication.script ==> canceling replication!");
                    return;
                }
                    
                InputStream stream = (InputStream) resource.getContent();
                String database_script = String.copyValueOf(FileUtil.getFileData(stream, null));

                //hsqldb needs user in uppercase
                p.put("replication.username", p.get("replication.username").toString().toUpperCase());
                p.put("replication.password", p.get("replication.password").toString().toUpperCase());

                //some properties are twice...
                database_script = StringUtil.insertProperties(database_script, p);
                database_script = StringUtil.insertProperties(database_script, p);
                FileUtil.writeBytes(database_script.getBytes(), databaseFile, false);
            } catch (IOException e) {
                ForwardedException.forward(e);
            }
        }
        /*
         * start the hsqldb database
         */
        Environment.get(CompatibilityLayer.class).runOptional("org.hsqldb.Server",
            "main",
            new Class[] { String[].class },
            new Object[] { new String[] { "-database",
                databaseName,
                "-port",
                getPort(),
                "-silent",
                "false",
                "-trace",
                "true" } });
    }
}
