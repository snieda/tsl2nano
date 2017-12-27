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

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.exception.Message;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.core.util.XmlUtil;
import de.tsl2.nano.persistence.replication.Replication;

/**
 * bean class to define the content of a persistence.xml and jdbc-connection.properties
 * 
 * @author Thomas Schneider, Thomas Schneider
 * @version $Revision$
 */
public class Persistence implements Serializable, Cloneable {
    /** serialVersionUID */
    private static final long serialVersionUID = 2360829578078838714L;
    
    private static final String DEFAULT_DATABASE = "anyway";
    public static final String DEFAULT_SCHEMA = "PUBLIC";
    public static final String DEFAULT_CATALOG = DEFAULT_SCHEMA;
    
    protected String persistenceUnit = "genericPersistenceUnit";
    protected String transactionType = "RESOURCE_LOCAL";
    protected String provider = "org.hibernate.ejb.HibernatePersistence";
    protected String jtaDataSource = "<UNDEFINED>";
    protected String jarFile = DEFAULT_DATABASE + ".jar";
    protected String connectionDriverClass = STD_LOCAL_DATABASE_DRIVER;
    protected String connectionUrl = STD_LOCAL_DATABASE_URL;
    protected String connectionUserName = "SA";//used for persistence
    protected String connectionPassword = "";
    protected String hibernateDialect = "org.hibernate.dialect.H2Dialect";
    protected String defaultSchema = DEFAULT_SCHEMA;
//    protected String datasourceClass = "org.hsqldb.jdbc.JDBCDataSource";
    protected String datasourceClass = H2_DATABASE_DRIVER;
    protected String port = "9092";//"9003";
    protected String database = DEFAULT_DATABASE;
    private Persistence replication;
    transient private String auth = "SA"; //used for authentication/authorization
    /** One of 'hbm2java' or 'openjpa-reverse-eng' */
    private String generator = GEN_HIBERNATE;
    /**
     * whether to enable auto-ddl creation of the current provider. possible values: false, validate, update, create,
     * create-drop
     */
    private String autoddl = "false";
    /** jdbc connection properties - used by ejb creator */
    public static final String FILE_JDBC_PROP_FILE = "jdbc-connection.properties";
    /** xml serialization of Persistence object */
    public static final String FILE_PERSISTENCE_BEAN = "persistence-bean.xml";
    /** persistence template file name */
    public static final String FILE_PERSISTENCE_TML = "META-INF/persistence.tml";
    public static final String FILE_PERSISTENCE_NOREP_TML = "META-INF/persistence-noreplication.tml";
    /** standard ejb path to load persistence unit */
    public static final String FILE_PERSISTENCE_XML = "META-INF/persistence.xml";

    public static final String GEN_HIBERNATE = "hibernate-tools";
    public static final String GEN_OPENJPA = "openjpa-reverse-eng";

    public static final String HSQLDB_DATABASE_DRIVER = "org.hsqldb.jdbc.JDBCDriver";
    public static final String HSQLDB_DATABASE_URL = "jdbc:hsqldb:hsql://localhost:9003";
    public static final String H2_DATABASE_DRIVER = "org.h2.Driver";
    public static final String H2_DATABASE_URL = "jdbc:h2:tcp://localhost:9092/" + DEFAULT_CATALOG;

    public static final String STD_LOCAL_DATABASE_DRIVER = H2_DATABASE_DRIVER;
    public static final String STD_LOCAL_DATABASE_URL = H2_DATABASE_URL;

    public static final String[] STD_LOCAL_DATABASE_DRIVERS = { HSQLDB_DATABASE_DRIVER, "org.hsqldb.jdbcDriver",
        H2_DATABASE_DRIVER };

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
        if (jarFile != null) {
            this.jarFile = jarFile;
        }
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
        //Workaround for eclipselink, using it's own classloader - loading from parent of META-INF/
        return jarFile.startsWith("!") ? jarFile.substring(1) : StringUtil.substring(jarFile, "file:///", null);
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
        setPort(getPort(connectionUrl));

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
        if (Util.isEmpty(defaultSchema) && !Util.isEmpty(connectionUserName)) {
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
        ENV.saveBackup(getBeanFileName());
        provideDatabaseInputAsDDL();
        ENV.get(XmlUtil.class).saveXml(getPath(getBeanFileName()), this);
        saveJdbcProperties();
        return savePersistenceXml();
    }

    /**
     * if the user pastes a full database model ddl into the database input field, it will be saved to a file defined by
     * beans-jar file name.
     */
    private void provideDatabaseInputAsDDL() {
        if (database == null)
            throw new IllegalArgumentException("the field database must not be empty!");
        else if (database.toLowerCase().matches("(?s).*create\\s+table.*")) {
            String name = StringUtil.substring(getJarFile(), null, ".");
            if (name.equals(DEFAULT_DATABASE)) {
                name = FileUtil.getUniqueFileName("ddlcopy");
                setJarFile(name + ".jar");
            }
            String file = ENV.getConfigPath() + name + ".sql";
            Message.send("ddl script detected in field database. saving content to " + file);
            //remove ´` characters and save.
            FileUtil.writeBytes(database.replaceAll("[´`]", "").getBytes(), file , false);
            setDatabase(name);
        }
    }

    protected String getBeanFileName() {
        return FILE_PERSISTENCE_BEAN;
    }

    /**
     * saveJdbcProperties
     * 
     * @throws IOException
     */
    protected void saveJdbcProperties() throws IOException {
        Properties prop = getJdbcProperties();

        ENV.saveBackup(FILE_JDBC_PROP_FILE);

        prop.store(new FileWriter(new File(getPath(FILE_JDBC_PROP_FILE))),
            "Property file generated by nano.directaccess");
    }

    public Properties getJdbcProperties() {
        return getJdbcProperties(false);
    }
    
    /**
     * getJdbcProperties
     * @param viewOnly if true, not all properties (like passwd) are filled
     * @return jdbc-properties
     */
    public Properties getJdbcProperties(boolean viewOnly) {
        Properties prop = new Properties();
        put(prop, "hibernate.dialect", getHibernateDialect());
        put(prop, "hibernate.connection.driver_class", getConnectionDriverClass());
        put(prop, "hibernate.connection.url", getConnectionUrl());
        put(prop, "hibernate.connection.username", getConnectionUserName());
        put(prop, "hibernate.connection.password", getConnectionPassword());
        put(prop, "hibernate.default_schema", getDefaultSchema());

        //WORKAROUND FOR OPENJPA REVERSE ENGENINEERING
        put(prop, "openjpa.provider", "org.apache.openjpa.persistence.PersistenceProviderImpl");

        put(prop, "generator", getGenerator());
        put(prop, "autoddl", getAutoddl());

        put(prop, "javax.persistence.provider", getProvider());
        put(prop, "javax.persistence.jdbc.driver", getConnectionDriverClass());
        put(prop, "javax.persistence.jdbc.driver", getConnectionDriverClass());
        put(prop, "javax.persistence.jdbc.user", getConnectionUserName());
        put(prop, "javax.persistence.jdbc.url", getConnectionUrl());
        put(prop, "javax.persistence.jdbc.password", getConnectionPassword());

        put(prop, "jpa.beansjar", jarFileInEnvironment());

        put(prop, "jdbc.dialect", getHibernateDialect());
        put(prop, "jdbc.driver", getConnectionDriverClass());
        put(prop, "jdbc.datasource", getDatasourceClass());
        put(prop, "jdbc.url", getConnectionUrl());
        put(prop, "jdbc.username", getConnectionUserName());
        if (!viewOnly)
            put(prop, "jdbc.password", getConnectionPassword());
        put(prop, "jdbc.database", getDatabase());
        put(prop, "jdbc.port", port);
        put(prop, "jdbc.scheme", getDefaultSchema());
        return prop;
    }

    protected <S> void put(Map<S, Object> prop, S key, String value) {
        prop.put(key, StringUtil.toString(value));
    }

    protected String savePersistenceXml() throws IOException {
        /*
         * create the mypersistenc.xml
         */
        InputStream stream = (InputStream) Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(Util.isEmpty(getReplication()) ? FILE_PERSISTENCE_NOREP_TML : FILE_PERSISTENCE_TML);
        
        String persistence_xml = String.copyValueOf(FileUtil.getFileData(stream, null));
        Map<String, Object> prop = new HashMap<String, Object>();
        addPersistenceProperties(null, prop);

        if (replication != null) {
            replication.addPersistenceProperties(this, prop);
        } else {
            new Replication().addPersistenceProperties(this, prop);
        }
        persistence_xml = StringUtil.insertProperties(persistence_xml, prop);
        ENV.saveBackup(FILE_PERSISTENCE_XML);
        FileUtil.removeToBackup(getBackupPath(FILE_PERSISTENCE_XML));
        FileUtil.writeBytes(persistence_xml.getBytes(), getPath(FILE_PERSISTENCE_XML), false);
        return persistence_xml;
    }

    /**
     * addPersistenceProperties
     * 
     * @param prop
     */
    protected void addPersistenceProperties(Persistence parent, Map<String, Object> prop) {
        put(prop, "persistence-unit", getPersistenceUnit());
        put(prop, "transaction-type", "RESOURCE_LOCAL");
        put(prop, "provider", getProvider());

        put(prop, "jar-file", jarURL());
        put(prop, "jta-data-source", getJtaDataSource());
        put(prop, "hibernate.dialect", getHibernateDialect());
        put(prop, "connection.driver_class", getConnectionDriverClass());
        put(prop, "connection.url", getConnectionUrl());
        put(prop, "connection.username", getConnectionUserName());
        put(prop, "connection.password", getConnectionPassword());
        put(prop, "autoddl", getAutoddl());
    }

    public String jarURL() {
        String jarFile = jarFileInEnvironment();
        //a relative file url should start with file:///, too - but the persistence providers can't work on that
        return "file:" + (new File(jarFile).isAbsolute() ? "///" + (jarFile.startsWith("/") ? jarFile.substring(1) : jarFile) : "./" + jarFile);
    }

    /**
     * the bean-jar must be in the class-path: this has to be inside the environment directory. jarFileInEnvironment
     * 
     * @return
     */
    public String jarFileInEnvironment() {
        //Workaround for eclipselink, using it's own classloader - loading from parent of META-INF/
        return jarFile.startsWith("!") ? getJarFile() : FileUtil.getRelativePath(ENV
            .getConfigPath() + new File(getJarFile()).getName());
    }

    /**
     * exists
     * 
     * @return true, if already defined and saved
     */
    public static final boolean exists() {
        return new File(getPath(FILE_PERSISTENCE_BEAN)).canRead();
    }

    /**
     * delete
     */
    public static final boolean delete() {
        if (exists()) {
            return new File(getPath(FILE_PERSISTENCE_BEAN)).delete();
        }
        return false;
    }

    /**
     * change
     * 
     * @param beanjar
     * @return
     */
    @SuppressWarnings("static-access")
    public final boolean change(String beanjar) {
        if (Persistence.exists()) {
            Persistence persistence =
                ENV.get(XmlUtil.class).loadXml(getPath(getBeanFileName()), Persistence.class);
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

    /**
     * @return Returns the generator.
     */
    public String getGenerator() {
        return generator;
    }

    /**
     * @param generator The generator to set. One of 'hbm2java' or 'openjpa-reverse-eng'
     */
    public void setGenerator(String generator) {
        this.generator = generator;
    }

    /**
     * @return Returns the autoddl.
     */
    public String getAutoddl() {
        if (provider.contains("toplink") || provider.contains("eclipselink")) {
            if (autoddl.equals("false")) {
                return "none";
            } else if (autoddl.equals("validate")) {
                return "none";
            } else if (autoddl.equals("update")) {
                return "create-tables";
            } else if (autoddl.equals("create")) {
                return "create-tables";
            } else if (autoddl.equals("create-drop")) {
                return "drop-and-create-tables";
            }
        } else if (provider.contains("openjpa")) {
            if (autoddl.equals("false")) {
                return "false";
            } else if (autoddl.equals("validate")) {
                return "false";
            } else if (autoddl.equals("update")) {
                return "buildSchema(ForeignKeys=true";
            } else if (autoddl.equals("create")) {
                return "buildSchema(ForeignKeys=true";
            } else if (autoddl.equals("create-drop")) {
                return "buildSchema(ForeignKeys=true";
            }
        } else if (provider.contains("hibernate")) {
            if (autoddl.equals("false")) {
                return "";
            }
        }
        return autoddl;
    }

    /**
     * autoDllIsCreateDrop
     * @return true, if {@link #autoddl} equals 'create-drop'
     */
    public boolean autoDllIsCreateDrop() {
        return autoddl.equals("create-drop");
    }
    /**
     * @param autoddl The autoddl to set.
     */
    public void setAutoddl(String autoddl) {
        this.autoddl = autoddl;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Persistence && Util.equals(jarFile, connectionUrl, connectionUserName, connectionPassword);
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
        return ENV.getConfigPath() + file;
    }

    public static String getBackupPath(String file) {
        return ENV.getTempPath() + file;
    }

    /**
     * loads an existing one or creates a new one.
     * 
     * @return loaded or created instance
     */
    @SuppressWarnings("static-access")
    public static Persistence current() {
        Persistence p;
        if (Persistence.exists()) {
            p = ENV.get(XmlUtil.class).loadXml(getPath(FILE_PERSISTENCE_BEAN),
                Persistence.class);
        } else {
            p = new Persistence();
        }
        if (p.getReplication() == null && ENV.get("service.use.database.replication", false)) {
            p.setReplication(new Replication());
        }
        return p;
    }

    /**
     * extracts the port of the given database url
     * @param url database url
     * @return port or null
     */
    public String getPort(String url) {
        return StringUtil.extract(url, "[:](\\d+)([:/;]\\w+)?", 1);
    }

    /**
     * see {@link #isEmbeddedDatabase(String)}
     */
    public boolean isEmbeddedDatabase() {
        return isEmbeddedDatabase(getConnectionUrl());
    }

    /**
     * isEmbeddedDatabase
     * @param urlOrDriver
     * @return true, if it contains hsqldb or h2
     */
    public static boolean isEmbeddedDatabase(String urlOrDriver) {
        return (urlOrDriver.contains("hsqldb")
                || urlOrDriver.contains("h2"));
    }
    @Override
    public Persistence clone() throws CloneNotSupportedException {
        return (Persistence) super.clone();
    }
    /**
     * getAuth
     * @return user authentication/authorization name
     */
    public String getAuth() {
        return auth;
    }
    public void setAuth(String auth) {
        this.auth = auth;
    }
}
