/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 08.08.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5;

import static de.tsl2.nano.bean.def.BeanPresentationHelper.PROP_ENABLER;
import static de.tsl2.nano.bean.def.BeanPresentationHelper.PROP_LENGTH;
import static de.tsl2.nano.bean.def.BeanPresentationHelper.PROP_NULLABLE;
import static de.tsl2.nano.bean.def.BeanPresentationHelper.PROP_STYLE;
import static de.tsl2.nano.bean.def.BeanPresentationHelper.PROP_VISIBLE;
import static de.tsl2.nano.persistence.Persistence.NOSQL_HIBERNATE_OGM_PERSISTENCE;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.BeanValue;
import de.tsl2.nano.bean.def.IIPresentable;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.SortedProperties;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.format.RegExpFormat;
import de.tsl2.nano.h5.plugin.INanoPlugin;
import de.tsl2.nano.h5.websocket.WSEvent;
import de.tsl2.nano.h5.websocket.WebSocketDependencyListener;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.persistence.Persistence.NoSqlDriverClass;
import de.tsl2.nano.plugin.Plugins;

/**
 * factory to create a user interface for {@link Persistence}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class PersistenceUI {
    
    public static final String ACTION_LOGIN_OK = "tsl2nano.login.ok";

	/**
     * constructor
     */
    private PersistenceUI() {
    }

    /**
     * createPersistenceUI
     * 
     * @param persistence
     * @return
     */
    @SuppressWarnings({ "serial", "unchecked" })
    public static Bean<Persistence> createPersistenceUI(final Persistence persistence,
            final ISystemConnector<Persistence> connector) {
        final Bean<Persistence> login = Bean.getBean(persistence);
        BeanPresentationHelper<Persistence> helper = login.getPresentationHelper();

        if (login.isDefault()) {
            login.setAttributeFilter("connectionUserName", "connectionPassword", "connectionUrl", "jarFile",
                "connectionDriverClass", "provider", "datasourceClass", "jtaDataSource", "transactionType",
                "persistenceUnit", "hibernateDialect", "database", "defaultSchema", "port", "generator", "autoddl",
                "replication",
                "jdbcProperties");
            login.setValueExpression(new ValueExpression<Persistence>("{connectionUrl}"));

            if (ENV.get("app.login.administration", true)) {
                /*
                 * create dependency listeners
                 */
                Properties props;
                String pfile = ENV.getConfigPath() + "persistence.properties";
                if ((props = FileUtil.loadPropertiesFromFile(pfile)) == null) {
                    props = new SortedProperties();
                    addFileNames(props, "BEANJAR_");
                    props.setProperty("PROVIDER_Hibernate", "org.hibernate.ejb.HibernatePersistence");
                    props.setProperty("PROVIDER_Hibernate5", "org.hibernate.jpa.HibernatePersistenceProvider");
                    props.setProperty("PROVIDER_OpenJPA", "org.apache.openjpa.persistence.PersistenceProviderImpl");
                    props.setProperty("PROVIDER_EclipseLink", "org.eclipse.persistence.jpa.PersistenceProvider");
                    props.setProperty("PROVIDER_TopLink", "oracle.toplink.essentials.PersistenceProvider");
                    props.setProperty("PROVIDER_Acme", "com.acme.persistence");
                    props.setProperty("PROVIDER_DataNucleus", "org.datanucleus.api.jpa.PersistenceProviderImpl");
                    props.setProperty("PROVIDER_BatooJPA", "org.batoo.jpa.core.BatooPersistenceProvider");
                    props.setProperty("PROVIDER_EBean", "de.tsl2.nano.ebeanprovider.EntityManager");
                    props.setProperty("PROVIDER_ORMLite", "de.tsl2.nano.ormliteprovider.EntityManager");
                    props.setProperty("PROVIDER_PROVIDER_Hibernate_OMG_NoSQL", NOSQL_HIBERNATE_OGM_PERSISTENCE);

                    props.setProperty("DRIVER_jdbc.odbc", "sun.jdbc.odbc.JdbcOdbcDriver");
                    props.setProperty("DRIVER_jdbc.oracle", "oracle.jdbc.OracleDriver");
                    props.setProperty("DRIVER_jdbc.db2", "com.ibm.db2.jcc.DB2Driver");
                    props.setProperty("DRIVER_jdbc.hsqldb", Persistence.HSQLDB_DATABASE_DRIVER);
                    props.setProperty("DRIVER_jdbc.h2", Persistence.H2_DATABASE_DRIVER);
                    props.setProperty("DRIVER_jdbc.sybase", "com.sybase.jdbc2.jdbc.SybDriver");
                    props.setProperty("DRIVER_jdbc.derby.embedded", "org.apache.derby.jdbc.EmbeddedDriver");
                    props.setProperty("DRIVER_jdbc.derby.client", Persistence.DERBY_DATABASE_DRIVER);
                    props.setProperty("DRIVER_jdbc.xerial", "org.sqlite.JDBC");
                    props.setProperty("DRIVER_jdbc.sqldroid", "org.sqldroid.SQLDroidDriver");
                    props.setProperty("DRIVER_jdbc.postgresql", "org.postgresql.Driver");
                    props.setProperty("DRIVER_jdbc.mysql", "com.mysql.jdbc.Driver");
                    props.setProperty("DRIVER_jdbc.jtds.sqlserver", "net.sourceforge.jtds.jdbc.Driver");
                    props.setProperty("DRIVER_jdbc.firebirdsql", "jaybird-jdk17");
                    props.setProperty("DRIVER_jdbc.ucanaccess", "net.ucanaccess.jdbc.UcanaccessDriver");
                    props.setProperty("DRIVER_jdbc.sapdb", "com.sap.dbtech.jdbc.DriverSapDB");

                    // nosql with hibernate-ogm (couchdb, cassandra, redis, ignite are experimental)
                    props.setProperty("DRIVER_map", "map");
                    props.setProperty("DRIVER_infinispan_embedded", "infinispan_embedded");
                    props.setProperty("DRIVER_infinispan_remote", "infinispan_remote");
                    props.setProperty("DRIVER_ehcache", "ehcache");
                    props.setProperty("DRIVER_mongodb", "mongodb");
                    props.setProperty("DRIVER_neo4j_embedded", "neo4j_embedded");
                    props.setProperty("DRIVER_neo4j_http", "neo4j_http");
                    props.setProperty("DRIVER_neo4j_bolt", "neo4j_bolt");

                    //if unknown, we use the driver name...
                    props.setProperty("DATASOURCE_jdbc.odbc", "sun.jdbc.odbc.JdbcOdbcDriver");
                    props.setProperty("DATASOURCE_jdbc.oracle", "oracle.jdbc.pool.OracleDataSource");
                    props.setProperty("DATASOURCE_jdbc.db2", "com.ibm.db2.jcc.DB2Driver");
                    props.setProperty("DATASOURCE_jdbc.hsqldb", "org.hsqldb.jdbc.JDBCDataSource");
                    props.setProperty("DATASOURCE_jdbc.h2", Persistence.H2_DATABASE_DRIVER);
                    props.setProperty("DATASOURCE_jdbc.sybase", "com.sybase.jdbc2.jdbc.SybDataSource");
                    props.setProperty("DATASOURCE_jdbc.derby", Persistence.DERBY_DATABASE_DRIVER);
                    props.setProperty("DATASOURCE_jdbc.xerial", "org.sqlite.JDBC");
                    props.setProperty("DATASOURCE_jdbc.sqldroid", "org.sqldroid.SQLDroidDriver");
                    props.setProperty("DATASOURCE_jdbc.postgresql", "org.postgresql.Driver");
                    props.setProperty("DATASOURCE_jdbc.mysql", "com.mysql.jdbc.Driver");
                    props.setProperty("DATASOURCE_jdbc.jtds", "net.sourceforge.jtds.jdbc.Driver");
                    props.setProperty("DATASOURCE_jdbc.firebirdsql", "jaybird-jdk17");
                    props.setProperty("DATASOURCE_jdbc.ucanaccess", "net.ucanaccess.jdbc.UcanaccessDriver");
                    props.setProperty("DATASOURCE_jdbc.sapdb", "com.sap.dbtech.jdbc.DriverSapDB");

                    props.setProperty("DIALECT_jdbc.oracle", "org.hibernate.dialect.Oracle10gDialect");
                    props.setProperty("DIALECT_jdbc.db2", "org.hibernate.dialect.DB2Dialect");
                    props.setProperty("DIALECT_jdbc.derby", "org.hibernate.dialect.DerbyDialect");
                    props.setProperty("DIALECT_jdbc.informix", "org.hibernate.dialect.InformixDialect");
                    props.setProperty("DIALECT_jdbc.hsqldb", "org.hibernate.dialect.HSQLDialect");
                    props.setProperty("DIALECT_jdbc.h2", "org.hibernate.dialect.H2Dialect");
                    props.setProperty("DIALECT_jdbc.sybase", "org.hibernate.dialect.SybaseDialect");
                    props.setProperty("DIALECT_jdbc.sqlite", "org.hibernate.dialect.SQLiteDialect");//xerial SQLite
                    props.setProperty("DIALECT_jdbc.postgresql", "org.hibernate.dialect.PostgresDialect");
                    props.setProperty("DIALECT_jdbc.mysql", "org.hibernate.dialect.MySQLDialect");
                    props.setProperty("DIALECT_jdbc.sqlserver", "org.hibernate.dialect.SQLServerDialect");//Microsoft SQL-Server
                    props.setProperty("DIALECT_jdbc.firebirdsql", "org.hibernate.dialect.FirebirdDialect");//jaybird
                    props.setProperty("DIALECT_jdbc.sapdb", "org.hibernate.dialect.SAPDBDialect");

                    // nosql with hibernate-ogm
                    props.setProperty("DIALECT_GridDialect", "GridDialect");
                    props.setProperty("DIALECT_QueryableGridDialect", "QueryableGridDialect");
                    props.setProperty("DIALECT_BatchableGridDialect", "BatchableGridDialect");
                    props.setProperty("DIALECT_IdentityColumnAwareGridDialect", "IdentityColumnAwareGridDialect");
                    props.setProperty("DIALECT_OptimisticLockingAwareGridDialect", "OptimisticLockingAwareGridDialect");
                    props.setProperty("DIALECT_MultigetGridDialect", "MultigetGridDialect");

                    props.setProperty("URLSYNTAX_jdbc.odbc", "jdbc:odbc:<alias>");
                    props.setProperty("URLSYNTAX_jdbc.oracle",
                        "jdbc:oracle:thin:@<server>[:<port(default 1521)>]:<database_name (default xe)>");
                    props.setProperty("URLSYNTAX_jdbc.db2", "jdbc:db2://<HOST>:<PORT>/<DB>");
                    props.setProperty("URLSYNTAX_jdbc.hsqldb", "jdbc:hsqldb:<databaseName>");
                    props.setProperty("URLSYNTAX_jdbc.h2", "jdbc:h2:[tcp://]<HOST>[:port(default 9092)]");
                    props.setProperty("URLSYNTAX_jdbc.sybase", "jdbc:sybase:Tds:<HOST>:<PORT>");
                    props.setProperty("URLSYNTAX_jdbc.derby",
                        "jdbc:derby://[server:][:port][databaseName][;attribute=value]*");
                    props.setProperty("URLSYNTAX_jdbc.xerial", "jdbc:sqlite:<db-file-path>");
                    props.setProperty("URLSYNTAX_jdbc.sqldroid", "jdbc:sqlite:<db-file-path>");
                    props.setProperty("URLSYNTAX_jdbc.postgresql", "jdbc:postgresql://<HOST>:<PORT>/<DB>");
                    props.setProperty("URLSYNTAX_jdbc.mysql",
                        "jdbc:mysql://<hostname>[,<failoverhost>][<:3306>]/<dbname>[?<param1>=<value1>][&<param2>=<value2>]");
                    props.setProperty("URLSYNTAX_jdbc.jtds",
                        "jdbc:jtds:<server_type>://<server>[:<port>][/<database>][;<property>=<value>[;...]]");
                    props.setProperty("URLSYNTAX_jdbc.firebirdsql", "jdbc:firebirdsql://host[:port]/<database>");
                    props.setProperty("URLSYNTAX_jdbc.ucanaccess", "jdbc:ucanaccess://<mdb-file-path>");//ms-access
                    props.setProperty("URLSYNTAX_jdbc.sapdb", "jdbc:sapdb://<server>[:<port>]/<databaseName>");//default-port: 7210
                    props.setProperty("URLSYNTAX_mongodb", "mongodb+srv://<username>:<password>@beyondthebasics.abcde.mongodb.net/<databasename>");

                    FileUtil.saveProperties(pfile, props);
                }

                //input assists - completions
                ((IIPresentable) login.getAttribute("jarFile").getPresentation())
                    .setItemList(MapUtil.getValues(props, "BEANJAR_.*"));
                ((IIPresentable) login.getAttribute("connectionUrl").getPresentation())
                    .setItemList(MapUtil.getValues(props, "URLSYNTAX_.*"));
                ((IIPresentable) login.getAttribute("connectionDriverClass").getPresentation())
                    .setItemList(MapUtil.getValues(props, "DRIVER_.*"));
                ((IIPresentable) login.getAttribute("datasourceClass").getPresentation())
                    .setItemList(MapUtil.getValues(props, "DATASOURCE_.*"));
                ((IIPresentable) login.getAttribute("provider").getPresentation())
                    .setItemList(MapUtil.getValues(props, "PROVIDER_.*"));
                ((IIPresentable) login.getAttribute("hibernateDialect").getPresentation())
                    .setItemList(MapUtil.getValues(props, "DIALECT_.*"));
                ((IIPresentable) login.getAttribute("transactionType").getPresentation())
                    .setItemList(Arrays.asList("RESOURCE_LOCAL", "JTA"));

                final Properties p = props;
                //refresh the default schema to be asked by the username listener
                String ds = (String) login.getValue("defaultSchema");
                final StringBuilder defaultSchema = ds != null ? new StringBuilder(ds) : new StringBuilder();
                login
                    .getAttribute("defaultSchema")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>() {
                            @Override
                            public String evaluate(WSEvent evt) {
                                String value = (String) evt.newValue;
                                String eval = StringUtil.toString(value);
                                defaultSchema.delete(0, defaultSchema.length());
                                if (value != null) {
                                    defaultSchema.append(eval);
                                }
                                return eval;
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUserName")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("defaultSchema")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                Object userName = Util.asString(value);
                                String eval;
                                if (userName != null && Util.isEmpty(defaultSchema)) {
                                    if (value != null && DatabaseTool.isInternalDatabase(persistence.getConnectionUrl())) {
                                        eval = Persistence.DEFAULT_SCHEMA;
                                    } else {
                                        eval = userName.toString().toUpperCase();
                                    }
                                    defaultSchema.replace(0, defaultSchema.length(), eval);
                                } else {
                                    eval = defaultSchema.toString();
                                }
                                return eval;
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUrl")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("connectionDriverClass")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String url = Util.asString(value);
                                if (url != null) {
                                    String prefix = StringUtil.extract(url, "^\\w+[:]\\w+").replace(':', '.');
                                    if (!Util.isEmpty(prefix) && p.containsKey("DRIVER_" + prefix)) {
                                        return p.getProperty("DRIVER_" + prefix);
                                    } else if (url.contains("27017") && !persistence.getProvider().equals(NOSQL_HIBERNATE_OGM_PERSISTENCE)) {
                                        return NoSqlDriverClass.mongodb.name();
                                    }
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUrl")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("datasourceClass")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String url = Util.asString(value);
                                if (url != null) {
                                    String prefix = getDriverPrefix(url);
                                    if (!Util.isEmpty(prefix) && p.containsKey("DATASOURCE_" + prefix)) {
                                        return p.getProperty("DATASOURCE_" + prefix);
                                    } else if (url.contains("27017") && !persistence.getProvider().equals(NOSQL_HIBERNATE_OGM_PERSISTENCE)) {
                                        return NoSqlDriverClass.mongodb.name();
                                    }
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUrl")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("hibernateDialect")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String url = Util.asString(value);
                                if (url != null) {
                                    String prefix = getDriverPrefix(url);
                                    if (!Util.isEmpty(prefix) && p.containsKey("DIALECT_" + prefix)) {
                                        return p.getProperty("DIALECT_" + prefix);
                                    } else if (url.contains("27017") && !persistence.getProvider().equals(NOSQL_HIBERNATE_OGM_PERSISTENCE)) {
                                        return "GridDialect";
                                    }
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUrl")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("database")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                            	if (!Util.isEmpty(evt.newValue) && evt.getOldValue() != null) {
	                                Object value = evt.newValue;
	                                String url = Util.asString(value);
	                                if (url != null && !DatabaseTool.isEmbeddedDatabase(url)) {
	                                    String prefix = getDriverPrefix(url);
	                                    if (!Util.isEmpty(prefix)) {
	                                        String newValue = StringUtil.extract(url, "[:]\\d+[:/;](\\w+)");
                                            if (!Util.isEmpty(newValue))
                                                return newValue;
	                                    } else {
	                                        String newValue = StringUtil.extract(url, "(\\w+)");
                                            if (!Util.isEmpty(newValue))
                                                return newValue;
	                                    }
	                                }
                            	}
                                //fallback
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUrl")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>(
                            (AttributeDefinition<String>) login.getAttribute("port")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String url = Util.asString(value);
                                if (url != null) {
                                    String prefix = getDriverPrefix(url);
                                    if (!Util.isEmpty(prefix)) {
                                        return DatabaseTool.getPort(url);
                                    }
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUrl")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>(
                            (AttributeDefinition<String>) login.getAttribute("provider")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String url = Util.asString(value);
                                if (url != null) {
                                    String port = DatabaseTool.getPort(url);
                                    if (port.equals("27017")) {
                                        return Persistence.NOSQL_HIBERNATE_OGM_PERSISTENCE;
                                    }
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("connectionUrl")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("defaultSchema")) {
                            @Override
                            public String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                Object userName = login.getAttribute("connectionUserName").getValue();
                                String eval = null;
                                if (userName != null) {
                                    if (value != null && DatabaseTool.isInternalDatabase((String) value)) {
                                        eval = Persistence.DEFAULT_SCHEMA;
                                    } else {
                                        eval = userName.toString().toUpperCase();
                                    }
                                }
                                return eval;
                            }
                        }, WSEvent.class);
//            login.getAttribute("provider").changeHandler().addListener(new WebSocketDependencyListener<String>() {
//                @Override
//                protected String evaluate(WSEvent evt) {
//                    Object value = evt.newValue;
//                    String url = Util.asString(value);
//                    if (url != null) {
//                        String prefix = StringUtil.extract(url, "\\w+[:]\\w+");
//                        String v = p.getProperty("DIALECT_" + prefix);
//                        return v;
//                    }
//                    return null;
//                }
//            }, WSEvent.class);
                login
                    .getAttribute("provider")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("provider")) {
                            @Override
                            protected String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String provider = Util.asString(value);
                                if (p != null) {
                                    String k, prov = provider.toLowerCase();
                                    for (Object key : p.keySet()) {
                                        k = (String) key;
                                        if (k.startsWith("PROVIDER_") && k.toLowerCase().contains(prov)) {
                                            provider = p.getProperty(k);
                                            break;
                                        }
                                    }
                                }
                                if (!NOSQL_HIBERNATE_OGM_PERSISTENCE.equals(provider) && persistence.getAutoddl().equals("true"))
                                    persistence.setAutoddl("false");
                                return provider;
                            }
                        }, WSEvent.class);
                // NOSQL with hibernate-ogm
                login
                    .getAttribute("provider")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("connectionDriverClass")) {
                            @Override
                            protected String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String provider = Util.asString(value);
                                if (provider.equals(NOSQL_HIBERNATE_OGM_PERSISTENCE)) {
                                    return NoSqlDriverClass.mongodb.name();
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("provider")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("datasourceClass")) {
                            @Override
                            protected String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String provider = Util.asString(value);
                                if (provider.equals(NOSQL_HIBERNATE_OGM_PERSISTENCE)) {
                                    return NoSqlDriverClass.mongodb.name();
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                login
                    .getAttribute("provider")
                    .changeHandler()
                    .addListener(
                        new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                            .getAttribute("hibernateDialect")) {
                            @Override
                            protected String evaluate(WSEvent evt) {
                                Object value = evt.newValue;
                                String provider = Util.asString(value);
                                if (provider.equals(NOSQL_HIBERNATE_OGM_PERSISTENCE)) {
                                    return "GridDialect";
                                }
                                return (String)((BeanValue<?>)attribute).getValue();
                            }
                        }, WSEvent.class);
                /*
                 * create expandable panels
                 */
                login.addValueGroup("connection", true, "connectionUserName", "connectionPassword", "connectionUrl",
                    "jarFile");
                login.addValueGroup("details   ", false, "connectionDriverClass", "provider", "datasourceClass",
                    "jtaDataSource",
                    "transactionType",
                    "persistenceUnit", "hibernateDialect", "database", "defaultSchema", "port", "generator", "autoddl",
                    "replication");
            } else { //simple user login
                helper.change(PROP_VISIBLE, false, "connectionUrl", "jarFile",
                    "connectionDriverClass", "provider", "datasourceClass", "jtaDataSource", "transactionType",
                    "persistenceUnit", "hibernateDialect", "database", "defaultSchema", "port", "generator", "autoddl",
                    "replication",
                    "jdbcProperties");
                persistence.setConnectionUserName(null);
                persistence.setConnectionPassword(null);
                login.getAttribute("connectionUserName").getPresentation()
                .setLayoutConstraints((Serializable) MapUtil.asMap("style", "width:120;"));
                login.getAttribute("connectionPassword").getPresentation()
                .setLayoutConstraints((Serializable) MapUtil.asMap("style", "width:120;"));
                login.setValueExpression(new ValueExpression<Persistence>(ENV.translate("tsl2nano.login.title", true)));
            }
        }
        if (login.toString().matches(ENV.get("app.login.present.attribute.multivalue", ".*"))) {
            login.removeAttributes("jdbcProperties");
        }
        if (ENV.get("app.login.jarfile.fileselector", false)) {
            login.getAttribute("jarFile").getPresentation().setType(IPresentable.TYPE_ATTACHMENT);
            ((Html5Presentable) login.getAttribute("jarFile").getPresentation()).getLayoutConstraints().put("accept",
                ".jar");
        }
        login.getAttribute("generator").setRange(Arrays.asList(Persistence.GEN_HIBERNATE, Persistence.GEN_OPENJPA));
        login.getAttribute("autoddl").setRange(Arrays.asList("false", "validate", "update", "create", "create-drop"));

        helper.change(BeanPresentationHelper.PROP_DESCRIPTION,
            ENV.translate("jarFile.tooltip", true),
            "jarFile");
        login.getAttribute("connectionPassword").getPresentation().setType(IPresentable.TYPE_INPUT_PASSWORD);

        helper.change(PROP_NULLABLE, false);
        helper.change(PROP_NULLABLE, true, "connectionPassword");
        helper.change(PROP_NULLABLE, true, "jarFile");
        helper.change(PROP_NULLABLE, true, "replication");

        helper.change(PROP_STYLE, IPresentable.STYLE_MULTI, "database");
        helper.change(PROP_LENGTH, 100000, "database");
        ((RegExpFormat) login.getAttribute("database").getFormat()).setPattern(RegExpFormat.alphanum(100000, false),
            null, 100000, Pattern.MULTILINE);
        login.getAttribute("database").getPresentation()
            .setLayoutConstraints((Serializable) MapUtil.asMap("rows", 1, "cols", "50", "style", "width:382; height: 60px; background-color: lightyellow;"));

        /*
         * caution: this is a flag to disable changing any field of the persistence.xml. The persistence.xml 
         * will be loaded by the provider as is. This is an issue for testing purposes on differen providers.
         */
        helper.change(PROP_ENABLER, new IActivable() {
            @Override
            public boolean isActive() {
                return ENV.get("app.login.save.persistence", true);
            }
        });

        helper.chg("replication", PROP_ENABLER, new IActivable() {
            @Override
            public boolean isActive() {
                return ENV.get("service.use.database.replication", false);
            }
        });

//        ((Map)login.getPresentable().getLayoutConstraints()).put("style", "opacity: 0.9;");
        IAction<Object> loginAction = new SecureAction<Object>(ACTION_LOGIN_OK) {
            //TODO: ref. to persistence class
            @Override
            public Object action() throws Exception {
                User user = Users.load().auth(persistence.getConnectionUserName(), persistence.getConnectionPassword());
                persistence.setAuth(persistence.getConnectionUserName());
                persistence.setConnectionUserName(user.getName());
                persistence.setConnectionPassword(user.getPasswd());
                    
                if (/*ENV.get("app.login.administration", true) && */ENV.get("app.login.save.persistence", true))
                    persistence.save();
                return connector.connect(persistence);
            }

            @Override
            public String getImagePath() {
                return "icons/open.png";
            }

            @Override
            public String getLongDescription() {
                return ENV.translate(ACTION_LOGIN_OK + ".tooltip", true);
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean isSynchron() {
                //TODO: for long term use, it's asynchron
                return true;//TODO: check if beans.jar readable and if authorization profile existent;
            }

            @Override
            public boolean isDefault() {
                return true;
            }

            @Override
            public int getActionMode() {
                return MODE_DLG_OK;
            }
        };
        login.addAction(loginAction);
        Plugins.process(INanoPlugin.class).definePersistence(persistence);
        return login;
    }

    private static void addFileNames(Properties props, String prefix) {
        addFiles(props, prefix, ".*\\.sql");
        addFiles(props, prefix, ".*\\.jar");
    }

    private static void addFiles(Properties props, String prefix, String ext) {
        File[] files = FileUtil.getFiles(ENV.getConfigPath(), ext);
        for (int i = 0; i < files.length; i++) {
            props.put(prefix + files[i].getName(), files[i].getName());
        }
    }

    protected static String getDriverPrefix(String url) {
        return StringUtil.extract(url, "^\\w+[:]\\w+").replace(':', '.');
    }
}
