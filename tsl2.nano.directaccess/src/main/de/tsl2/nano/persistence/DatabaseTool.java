package de.tsl2.nano.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.messaging.EMessage;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.util.SchedulerUtil;

/**
 * Use the jdbc connection to run/evaluate some database specific actions/properties
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class DatabaseTool {
    private static final Log LOG = LogFactory.getLog(DatabaseTool.class);
    
    Persistence persistence;
    
    public DatabaseTool(Persistence persistence) {
        this.persistence = persistence;
    }

    
    public Persistence getPersistence() {
        return persistence;
    }

    public boolean isLocalDatabase(Persistence persistence) {
        String url = persistence.getConnectionUrl();
        if (!Util.isEmpty(persistence.getPort()) || isH2(url)) {
            return Arrays.asList(persistence.STD_LOCAL_DATABASE_DRIVERS).contains(
                persistence.getConnectionDriverClass())
                && (url.contains("localhost") || url.contains("127.0.0.1") || isH2(url));
        }
        return false;
    }

    public boolean canConnectToLocalDatabase(Persistence persistence) {
        if (!Util.isEmpty(persistence.getPort())) {
            int p = Integer.valueOf(persistence.getPort());
            return NetUtil.isOpen(p);
        }
        return false;
    }

    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Executors.defaultThreadFactory().newThread(new Runnable() {
            @Override
            public void run() {
                if (BeanContainer.isInitialized()) {
                    EMessage.broadcast(this, "APPLICATION SHUTDOWN INITIALIZED...", "*");
                    LOG.info("preparing shutdown of local database " + persistence.getConnectionUrl());
                    try {
                        BeanContainer.instance().executeStmt(ENV.get("app.shutdown.statement", "SHUTDOWN"), true,
                            null);
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        LOG.error(e.toString());
                    }
                    String hsqldbScript = isH2(persistence.getConnectionUrl())
                        ? persistence.getDefaultSchema() + ".mv.db" : persistence.getDatabase() + ".script";
                    String backupFile =
                        ENV.getTempPath() + FileUtil.getUniqueFileName(ENV.get("app.database.backup.file",
                            persistence.getDatabase()) + ".zip");
                    LOG.info("creating database backup to file " + backupFile);
                    FileUtil.writeToZip(backupFile, hsqldbScript, FileUtil.getFileBytes(ENV.getConfigPath() + hsqldbScript, null));
                }
            }
        }));
    }

    public void doPeriodicalBackup() {
        //do a periodical backup
        SchedulerUtil.runAt(0, -1, TimeUnit.DAYS, new Runnable() {
            @Override
            public void run() {
                LOG.info("preparing backup of local database " + persistence.getConnectionUrl());
                try {
                    BeanContainer.instance().executeStmt(
                        ENV.get("app.backup.statement", "backup to temp/database-daily-backup.zip"), true, null);
                } catch (Exception e) {
                    LOG.error(e.toString());
                }
            }
        });
    }

    public Connection getConnection() {
        Connection con = null;
        try {
            Class.forName(persistence.getConnectionDriverClass());
            con = DriverManager.getConnection(persistence.getConnectionUrl(), persistence.getConnectionUserName(), persistence.getConnectionPassword());
        } catch (Exception e) {
            ManagedException.forward(e);
        }
        return con;
    }
    
    public void checkJDBCConnection() {
        Connection con = null;
        try {
            con = getConnection();
            ResultSet tables = getTableNames(con);
            
            if (!tables.next()) {
                LOG.info("Available tables are:\n" + getTablesAsString(con.getMetaData().getTables(null, null, null, null)));
                throw new ManagedException("The desired jdbc connection provides no tables to work on!");
            }
        } catch (Exception e) {
            ManagedException.forward(e);
        } finally {
            close(con);
        }
        
    }


    private static void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                ManagedException.forward(e);
            }
        }
    }

    private ResultSet getTableNames(Connection con) throws SQLException {
        String schema = !Util.isEmpty(persistence.getDefaultSchema()) ? persistence.getDefaultSchema() : null;
        ResultSet tables = con.getMetaData().getTables(null, schema, null, null);
        return tables;
    }

    public String[] getTableNames() {
        Connection con = null;
        try {
            con = getConnection();
            ResultSet tables = getTableNames(con);
            int cc = tables.getMetaData().getColumnCount();
            String[] result = new String[cc];
            int i = 0;
            while(tables.next()) {
                result[i++] = tables.getObject("TABLE_NAME").toString();
            }
            return result;
        } catch (Exception e) {
            ManagedException.forward(e);
            return null;
        } finally {
            close(con);
        }
        
    }

    private String getTablesAsString(ResultSet tables) throws SQLException {
        StringBuilder str = new StringBuilder();
        int cc = tables.getMetaData().getColumnCount();
        ArrayList<Object> row = new ArrayList<>(cc);
        while(tables.next()) {
            for (int i = 1; i < cc; i++) {
                row.add(tables.getObject(i));
            }
            str.append(StringUtil.toString(row, -1) + "\n");
            row.clear();
        }
        return str.toString();
    }

    public void copyJavaDBDriverFiles(Persistence persistence) {
        String dest = ENV.getConfigPath();
        if (persistence.getConnectionUrl().contains("derby") && ! new File(dest + "derby.jar").exists()) {
            String path = System.getProperty("java.home") + "/../db/lib/";
            if (new File(path + "derby.jar").canRead()) {
                LOG.info("copying derby/javadb database driver files to environment");
                FileUtil.copy(path + "derby.jar", dest + "derby.jar");
                FileUtil.copy(path + "derbynet.jar", dest + "derbynet.jar");
                FileUtil.copy(path + "derbytools.jar", dest + "derbytools.jar");
                FileUtil.copy(path + "derbyclient.jar", dest + "derbyclient.jar");
                
                try {
                    FileUtil.writeBytes("java -cp * org.apache.derby.drda.NetworkServerControl start %*".getBytes(), dest + "runServer.cmd", false);
                    FileUtil.writeBytes("java -cp derby*.jar org.apache.derby.drda.NetworkServerControl start %*".getBytes(), dest + "runServer.sh", false);
                } catch (Exception e) {
                    LOG.warn(e.toString());
                }
            } else {
                LOG.warn("cannot copy derby driver files from jdk path: " + path);
            }
        }
    }

    /**
     * extracts the port of the given database url
     * @param url database url
     * @return port or null
     */
    public static String getPort(String url) {
        return StringUtil.extract(url, "[:](\\d+)([:/;]\\w+)?", 1);
    }

    /**
     * see {@link #isEmbeddedDatabase(String)}
     */
    public boolean isEmbeddedDatabase() {
        return isEmbeddedDatabase(persistence.getConnectionUrl());
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

    public static boolean isH2(String url) {
        return url.matches("jdbc[:]h2[:].*");
    }
    /**
     * url to an sql tool, if it is an embedded database.
     * @return optional SQL Tool like the one of H2 on port 8082
     */
    public String getSQLToolURL() {
        return isEmbeddedDatabase() && isH2(persistence.getConnectionUrl()) ? "http://localhost:8082" : null;
    }
    
}
