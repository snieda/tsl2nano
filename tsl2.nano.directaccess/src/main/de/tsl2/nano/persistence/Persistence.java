package de.tsl2.nano.persistence;

/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider, Thomas Schneider
 * created on: Apr 26, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.tsl2.nano.Environment;
import de.tsl2.nano.exception.ManagedException;
import de.tsl2.nano.persistence.replication.Replication;
import de.tsl2.nano.util.FileUtil;
import de.tsl2.nano.util.StringUtil;

/**
 * bean class to define the content of a persistence.xml and jdbc-connection.properties
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Persistence implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 2360829578078838714L;
    protected String persistenceUnit = "genericPersistenceUnit";
    protected String transactionType = "RESOURCE_LOCAL";
    protected String provider = "org.hibernate.ejb.HibernatePersistence";
    protected String jtaDataSource = "<UNDEFINED>";
    protected String jarFile = "";
    protected String connectionDriverClass = "oracle.jdbc.OracleDriver";
    protected String connectionUrl = "jdbc:oracle:thin:@localhost:1521:xe";
    protected String connectionUserName = "";
    protected String connectionPassword = "";
    protected String hibernateDialect = "org.hibernate.dialect.Oracle10gDialect";
    protected String defaultSchema = "";
    protected String datasourceClass = "oracle.jdbc.pool.OracleDataSource";
    protected String port = "1521";
    protected String database = "xe";

    private Persistence replication;

    /** jdbc connection properties - used by ejb creator */
    public static final String FILE_JDBC_PROP_FILE = "jdbc-connection.properties";
    /** xml serialization of Persistence object */
    public static final String FILE_MYPERSISTENCE_BEAN = "mypersistence-bean.xml";
    /** mypersistence file name */
    public static final String FILE_MYPERSISTENCE = "mypersistence.xml";
    /** standard ejb path to load persistence unit */
    public static final String FILE_PERSISTENCE_XML = "META-INF/persistence.xml";

    /**
     * constructor
     */
    public Persistence() {
        this(null);
    }

    /**
     * constructor
     */
    public Persistence(String jarFile) {
        if (jarFile != null)
            this.jarFile = jarFile;
    }

    /**
     * @return Returns the persistenceUnit.
     */
    public String getPersistenceUnit() {
        return persistenceUnit;
    }

    /**
     * @param persistenceUnit The persistenceUnit to set.
     */
    public void setPersistenceUnit(String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    /**
     * @return Returns the jtaDataSource.
     */
    public String getJtaDataSource() {
        return jtaDataSource;
    }

    /**
     * @param jtaDataSource The jtaDataSource to set.
     */
    public void setJtaDataSource(String jtaDataSource) {
        this.jtaDataSource = jtaDataSource;
    }

    /**
     * @return Returns the jarFile.
     */
    public String getJarFile() {
        return jarFile;
    }

    /**
     * @param jarFile The jarFile to set.
     */
    public void setJarFile(String jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * @return Returns the connectionDriverClass.
     */
    public String getConnectionDriverClass() {
        return connectionDriverClass;
    }

    /**
     * @param connectionDriverClass The connectionDriverClass to set.
     */
    public void setConnectionDriverClass(String connectionDriverClass) {
        this.connectionDriverClass = connectionDriverClass;
    }

    /**
     * @return Returns the connectionUrl.
     */
    public String getConnectionUrl() {
        return connectionUrl;
    }

    /**
     * @param connectionUrl The connectionUrl to set.
     */
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
        String portExtract = StringUtil.extract(connectionUrl, "[:][0-9]{4,7}([:]|)");
        setPort(StringUtil.substring(portExtract, ":", ":"));

        setDatabase(StringUtil.substring(connectionUrl, getPort() + ":", null));
    }

    /**
     * @return Returns the connectionUserName.
     */
    public String getConnectionUserName() {
        return connectionUserName;
    }

    /**
     * @param connectionUserName The connectionUserName to set.
     */
    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    /**
     * @return Returns the connectionPassword.
     */
    public String getConnectionPassword() {
        return connectionPassword;
    }

    /**
     * @param connectionPassword The connectionPassword to set.
     */
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    /**
     * @return Returns the hibernateDialect.
     */
    public String getHibernateDialect() {
        return hibernateDialect;
    }

    /**
     * @param hibernateDialect The hibernateDialect to set.
     */
    public void setHibernateDialect(String hibernateDialect) {
        this.hibernateDialect = hibernateDialect;
    }

    /**
     * @return Returns the transactionType.
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * @param transactionType The transactionType to set.
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * @return Returns the provider.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param provider The provider to set.
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @return Returns the defaultSchema.
     */
    public String getDefaultSchema() {
        if (defaultSchema == null && connectionUserName != null) {
            defaultSchema = connectionUserName.toUpperCase();
        }
        return defaultSchema;
    }

    /**
     * @param defaultSchema The defaultSchema to set.
     */
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    /**
     * @return Returns the datasourceClass.
     */
    public String getDatasourceClass() {
        return datasourceClass;
    }

    /**
     * @param datasourceClass The datasourceClass to set.
     */
    public void setDatasourceClass(String datasourceClass) {
        this.datasourceClass = datasourceClass;
    }

    /**
     * @return Returns the port.
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port The port to set.
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * @return Returns the database.
     */
    public String getDatabase() {
        return database;
    }

    /**
     * @param database The database to set.
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * save serialization, persistence.xml and jdbc property file
     * 
     * @return persistenc.xml content
     * @throws IOException
     */
    public String save() throws IOException {
        FileUtil.removeToBackup(getPath(getBeanFileName()));
        FileUtil.saveXml(this, getPath(getBeanFileName()));
        saveJdbcProperties();
        return savePersistenceXml();
    }

    protected String getBeanFileName() {
        return FILE_MYPERSISTENCE_BEAN;
    }

    /**
     * saveJdbcProperties
     * 
     * @throws IOException
     */
    private void saveJdbcProperties() throws IOException {
        Properties prop = getJdbcProperties();

        FileUtil.removeToBackup(getPath(FILE_JDBC_PROP_FILE));
        prop.store(new FileWriter(new File(getPath(FILE_JDBC_PROP_FILE))), "Property file generated by BeanBrowser");
    }

    /**
     * getJdbcProperties
     * 
     * @return
     */
    public Properties getJdbcProperties() {
        Properties prop = new Properties();
        prop.put("hibernate.dialect", getHibernateDialect());
        prop.put("hibernate.connection.driver_class", getConnectionDriverClass());
        prop.put("hibernate.connection.url", getConnectionUrl());
        prop.put("hibernate.connection.username", getConnectionUserName());
        prop.put("hibernate.connection.password", StringUtil.toString(getConnectionPassword()));
        prop.put("hibernate.default_schema", getDefaultSchema());

        prop.put("jdbc.dialect", getHibernateDialect());
        prop.put("jdbc.driver", getConnectionDriverClass());
        prop.put("jdbc.datasource", getDatasourceClass());
        prop.put("jdbc.url", getConnectionUrl());
        prop.put("jdbc.username", getConnectionUserName());
        prop.put("jdbc.password", StringUtil.toString(getConnectionPassword()));
        prop.put("jdbc.database", "xe");
        prop.put("jdbc.port", "1521");
        prop.put("jdbc.scheme", getDefaultSchema());
        return prop;
    }

    protected String savePersistenceXml() throws IOException {
        /*
         * create the mypersistenc.xml
         */
        InputStream stream = (InputStream) Thread.currentThread()
            .getContextClassLoader()
            .getResource(Persistence.FILE_PERSISTENCE_XML)
            .getContent();
        String persistence_xml = String.copyValueOf(FileUtil.getFileData(stream, null));
        Map<String, Object> prop = new HashMap<String, Object>();
        addPersistenceProperties(null, prop);

        if (replication != null)
            replication.addPersistenceProperties(this, prop);
        persistence_xml = StringUtil.insertProperties(persistence_xml, prop);
        FileUtil.removeToBackup(getPath(FILE_MYPERSISTENCE));
        FileUtil.writeBytes(persistence_xml.getBytes(), getPath(FILE_MYPERSISTENCE), false);
        return persistence_xml;
    }

    /**
     * addPersistenceProperties
     * 
     * @param prop
     */
    protected void addPersistenceProperties(Persistence parent, Map<String, Object> prop) {
        prop.put("persistence-unit", getPersistenceUnit());
        prop.put("transaction-type", "RESOURCE_LOCAL");
        prop.put("provider", getProvider());
        prop.put("jar-file", getJarFile());
        prop.put("jta-data-source", getJtaDataSource());
        prop.put("hibernate.dialect", getHibernateDialect());
        prop.put("connection.driver_class", getConnectionDriverClass());
        prop.put("connection.url", getConnectionUrl());
        prop.put("connection.username", getConnectionUserName());
        prop.put("connection.password", getConnectionPassword());
    }

    /**
     * exists
     * 
     * @return true, if already defined and saved
     */
    public static final boolean exists() {
        return new File(getPath(FILE_MYPERSISTENCE)).canRead();
    }

    /**
     * delete
     */
    public static final boolean delete() {
        if (exists()) {
            return new File(getPath(FILE_MYPERSISTENCE)).delete();
        }
        return false;
    }

    /**
     * change
     * 
     * @param beanjar
     * @return
     */
    public final boolean change(String beanjar) {
        if (Persistence.exists()) {
            Persistence persistence = (Persistence) FileUtil.loadXml(getPath(getBeanFileName()));
            persistence.setJarFile(beanjar);
            try {
                persistence.save();
                return true;
            } catch (IOException e) {
                ManagedException.forward(e);
            }
        }
        return false;
    }

    public Persistence getReplication() {
        return replication;
    }

    public void setReplication(Persistence replication) {
        this.replication = replication;
    }

    @Override
    public String toString() {
        return getPersistenceUnit() + "-->" + getConnectionUrl() + "-->" + getConnectionUserName();
    }
    
    /**
     * returns the full workspace path for the given file
     * 
     * @param file file name
     * @return path
     */
    public static String getPath(String file) {
        return Environment.getConfigPath() + file;
    }

    /**
     * loads an existing one or creates a new one.
     * 
     * @return loaded or created instance
     */
    public static Persistence current() {
        Persistence p;
        if (Persistence.exists()) {
            p = (Persistence) FileUtil.loadXml(getPath(FILE_MYPERSISTENCE_BEAN));
        } else {
            p = new Persistence();
        }
        if (p.getReplication() == null && Environment.get("use.database.replication", true))
            p.setReplication(new Replication());
        return p;
    }
}
