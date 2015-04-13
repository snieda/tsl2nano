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

import java.util.Arrays;
import java.util.Properties;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanPresentationHelper;
import de.tsl2.nano.bean.def.IPresentable;
import de.tsl2.nano.bean.def.SecureAction;
import de.tsl2.nano.bean.def.ValueExpression;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.h5.websocket.WebSocketDependencyListener;
import de.tsl2.nano.persistence.Persistence;

/**
 * factory to create a user interface for {@link Persistence}.
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class PersistenceUI {

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
        if (login.isDefault()) {
            login.setAttributeFilter("connectionUserName", "connectionPassword", "connectionUrl", "jarFile",
                "connectionDriverClass", "provider", "datasourceClass", "jtaDataSource", "transactionType",
                "persistenceUnit", "hibernateDialect", "database", "defaultSchema", "port", "generator", "autoddl", "replication",
                "jdbcProperties");
            login.setValueExpression(new ValueExpression<Persistence>("{connectionUrl}"));

            /*
             * create dependency listeners
             */
            Properties props;
            String pfile = ENV.getConfigPath() + "persistence.properties";
            if ((props = FileUtil.loadPropertiesFromFile(pfile)) == null) {
                props = new Properties();
                props.setProperty("DRIVER_jdbc.odbc", "sun.jdbc.odbc.JdbcOdbcDriver");
                props.setProperty("DRIVER_jdbc.oracle", "oracle.jdbc.OracleDriver");
                props.setProperty("DRIVER_jdbc.db2", "com.ibm.db2.jcc.DB2Driver");
                props.setProperty("DRIVER_jdbc.hsqldb", persistence.STD_LOCAL_DATABASE_DRIVER);
                props.setProperty("DRIVER_jdbc.h2", "org.h2.Driver");
                props.setProperty("DRIVER_jdbc.sybase", "com.sybase.jdbc2.jdbc.SybDriver");
                props.setProperty("DRIVER_jdbc.derby", "org.apache.derby.jdbc.EmbeddedDriver");
//                props.setProperty("DRIVER_jdbc.derby", "org.apache.derby.jdbc.ClientDriver");
                props.setProperty("DRIVER_jdbc.xerial", "sqlite-jdbc");
                props.setProperty("DRIVER_jdbc.postgresql", "org.postgresql.Driver");
                props.setProperty("DRIVER_jdbc.mysql", "com.mysql.jdbc.Driver");
                props.setProperty("DRIVER_jdbc.jtds.sqlserver", "net.sourceforge.jtds.jdbc.Driver");
                props.setProperty("DRIVER_jdbc.firebirdsql", "jaybird-jdk17");

                props.setProperty("DATASOURCE_jdbc.oracle", "oracle.jdbc.pool.OracleDataSource");
                props.setProperty("DATASOURCE_jdbc.hsqldb", "org.hsqldb.jdbc.JDBCDataSource");
                props.setProperty("DATASOURCE_jdbc.sybase", "com.sybase.jdbc2.jdbc.SybDataSource");
                props.setProperty("DATASOURCE_org.apache.derby", "org.apache.derby.jdbc.ClientDriver");
                props.setProperty("DATASOURCE_org.xerial", "sqlite-jdbc");
                props.setProperty("DATASOURCE_org.postgresql", "postgresql/postgresql");
                props.setProperty("DATASOURCE_com.h2database", "org.h2.Driver");
                props.setProperty("DATASOURCE_com.mysql.jdbc", "mysql/mysql-connector-java");
                props.setProperty("DATASOURCE_net.sourceforge.jtds", "jtds");
                props.setProperty("DATASOURCE_org.firebirdsql.jdbc", "jaybird-jdk17");

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
                props.setProperty("DIALECT_jdbc.sapdb", "org.hibernate.dialect.SAPDBDialect");//jaybird
                FileUtil.saveProperties(pfile, props);
            }
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
                        public String evaluate(Object value) {
                            String eval = StringUtil.toString(value);
                            defaultSchema.delete(0, defaultSchema.length());
                            if (value != null)
                                defaultSchema.append(eval);
                            return eval;
                        }
                    });
            login
            .getAttribute("connectionUserName")
            .changeHandler()
            .addListener(
                new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                    .getAttribute("defaultSchema")) {
                    @Override
                    public String evaluate(Object value) {
                        Object userName = Util.asString(value);
                        String eval;
                        if (userName != null && Util.isEmpty(defaultSchema)) {
                            if (value != null && persistence.getConnectionUrl().contains("hsqldb"))
                                eval = "PUBLIC";
                            else
                                eval = userName.toString().toUpperCase();
                            defaultSchema.replace(0, defaultSchema.length(), eval);
                        } else {
                            eval = defaultSchema.toString();
                        }
                        return eval;
                    }
                });
            login
                .getAttribute("connectionUrl")
                .changeHandler()
                .addListener(
                    new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                        .getAttribute("connectionDriverClass")) {
                        @Override
                        public String evaluate(Object value) {
                            String url = Util.asString(value);
                            if (url != null) {
                                String prefix = StringUtil.extract(url, "^\\w+[:]\\w+").replace(':', '.');
                                if (!Util.isEmpty(prefix)) {
                                    return p.getProperty("DRIVER_" + prefix);
                                }
                            }
                            return null;
                        }
                    });
            login
                .getAttribute("connectionUrl")
                .changeHandler()
                .addListener(
                    new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                        .getAttribute("datasourceClass")) {
                        @Override
                        public String evaluate(Object value) {
                            String url = Util.asString(value);
                            if (url != null) {
                                String prefix = StringUtil.extract(url, "^\\w+[:]\\w+").replace(':', '.');
                                if (!Util.isEmpty(prefix)) {
                                    return p.getProperty("DATASOURCE_" + prefix);
                                }
                            }
                            return null;
                        }
                    });
            login
                .getAttribute("connectionUrl")
                .changeHandler()
                .addListener(
                    new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                        .getAttribute("hibernateDialect")) {
                        @Override
                        public String evaluate(Object value) {
                            String url = Util.asString(value);
                            if (url != null) {
                                String prefix = StringUtil.extract(url, "^\\w+[:]\\w+").replace(':', '.');
                                if (!Util.isEmpty(prefix)) {
                                    return p.getProperty("DIALECT_" + prefix);
                                }
                            }
                            return null;
                        }
                    });
            login
                .getAttribute("connectionUrl")
                .changeHandler()
                .addListener(
                    new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                        .getAttribute("database")) {
                        @Override
                        public String evaluate(Object value) {
                            String url = Util.asString(value);
                            if (url != null) {
                                String prefix = StringUtil.extract(url, "^\\w+[:]\\w+").replace(':', '.');
                                if (!Util.isEmpty(prefix)) {
                                    String database = StringUtil.extract(url, "[:]\\d+[:](\\w+)");
                                    return StringUtil.substring(database, ":", null, true);
                                }
                            }
                            //fallback
                            return persistence.getDatabase();
                        }
                    });
            login
                .getAttribute("connectionUrl")
                .changeHandler()
                .addListener(
                    new WebSocketDependencyListener<String>((AttributeDefinition<String>) login.getAttribute("port")) {
                        @Override
                        public String evaluate(Object value) {
                            String url = Util.asString(value);
                            if (url != null) {
                                String prefix = StringUtil.extract(url, "^\\w+[:]\\w+").replace(':', '.');
                                if (!Util.isEmpty(prefix)) {
                                    String port = StringUtil.extract(url, "[:]\\d+[:]\\w+");
                                    if (!Util.isEmpty(port))
                                        return StringUtil.substring(port, ":", ":");
                                    else {
                                        port = StringUtil.extract(url, "[:]\\d+$");
                                        if (!Util.isEmpty(port))
                                            return StringUtil.substring(port, ":", null);
                                        else {
                                            port = StringUtil.extract(url, "[:]\\d+;");
                                            if (!Util.isEmpty(port))
                                                return StringUtil.substring(port, ":", ";");
                                        }
                                    }

                                }
                            }
                            return null;
                        }
                    });
            login
                .getAttribute("connectionUrl")
                .changeHandler()
                .addListener(
                    new WebSocketDependencyListener<String>((AttributeDefinition<String>) login
                        .getAttribute("defaultSchema")) {
                        @Override
                        public String evaluate(Object value) {
                            Object userName = login.getAttribute("connectionUserName").getValue();
                            String eval = null;
                            if (userName != null) {
                                if (value != null && value.toString().contains("hsqldb"))
                                    eval = "PUBLIC";
                                else
                                    eval = userName.toString().toUpperCase();
                            }
                            return eval;
                        }
                    });
            login.getAttribute("provider").changeHandler().addListener(new WebSocketDependencyListener<String>() {
                @Override
                protected String evaluate(Object value) {
                    String url = Util.asString(value);
                    if (url != null) {
                        String prefix = StringUtil.extract(url, "\\w+[:]\\w+");
                        String v = p.getProperty("DIALECT_" + prefix);
                        return v;
                    }
                    return null;
                }
            });
            /*
             * create expandable panels
             */
            login.addValueGroup("connection", true, "connectionUserName", "connectionPassword", "connectionUrl",
                "jarFile");
            login.addValueGroup("details   ", false, "connectionDriverClass", "provider", "datasourceClass",
                "jtaDataSource",
                "transactionType",
                "persistenceUnit", "hibernateDialect", "database", "defaultSchema", "port", "generator", "autoddl", "replication");
        }
        if (login.toString().matches(ENV.get("default.present.attribute.multivalue", ".*")))
            login.removeAttributes("jdbcProperties");
        if (ENV.get("login.jarfile.fileselector", true)) {
            login.getAttribute("jarFile").getPresentation().setType(IPresentable.TYPE_ATTACHMENT);
            ((Html5Presentable) login.getAttribute("jarFile").getPresentation()).getLayoutConstraints().put("accept",
                ".jar");
        }
        login.getAttribute("generator").setRange(Arrays.asList(Persistence.GEN_HIBERNATE, Persistence.GEN_OPENJPA));
        login.getAttribute("autoddl").setRange(Arrays.asList("false", "validate", "update", "create", "create-drop"));
        
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_DESCRIPTION,
            ENV.translate("jarFile.tooltip", true),
            "jarFile");
        
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, false);
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, true, "connectionPassword");
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, true, "jarFile");
        login.getPresentationHelper().change(BeanPresentationHelper.PROP_NULLABLE, true, "replication");
        login.getPresentationHelper().chg("replication", BeanPresentationHelper.PROP_ENABLER, new IActivable() {
            @Override
            public boolean isActive() {
                return ENV.get("use.database.replication", false);
            }
        });
//        ((Map)login.getPresentable().getLayoutConstraints()).put("style", "opacity: 0.9;");
        IAction<Object> loginAction = new SecureAction<Object>("tsl2nano.login.ok") {
            //TODO: ref. to persistence class
            @Override
            public Object action() throws Exception {
                persistence.save();
                return connector.connect(persistence);
            }

            @Override
            public String getImagePath() {
                return "icons/open.png";
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
        return login;
    }
}
