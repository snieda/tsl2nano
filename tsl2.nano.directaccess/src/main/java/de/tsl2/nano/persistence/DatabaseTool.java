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
import de.tsl2.nano.core.cls.BeanClass;
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

    public boolean canConnectToLocalDatabase() {
    	return canConnectToLocalDatabase(persistence);
    }
    
    public static boolean canConnectToLocalDatabase(Persistence persistence) {
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
                	Persistence persistence = Persistence.current();
                    EMessage.broadcast(this, "APPLICATION SHUTDOWN INITIALIZED...", "*");
                    
                    String scriptFile = ENV.get("app.backup.statement.file", "db-backup.sql");
                    String backupScript = ENV.get("app.backup.statement", "SCRIPT TO '<FILE>'");
                    if (backupScript != null)
                    	BeanContainer.instance().executeStmt(backupScript.replace("<FILE>", FileUtil.getUniqueFileName(ENV.getTempPath() + scriptFile)), true, null);
    			    
                    shutdownDBServer();
//                    shutdownDatabase(); //doppelt gemoppelt hält besser ;-)
                    String hsqldbScript = isH2() ? persistence.getDefaultSchema() + ".mv.db" : persistence.getDatabase() + ".script";
                    String backupFile =
                        ENV.getTempPath() + FileUtil.getUniqueFileName(ENV.get("app.database.backup.file",
                            persistence.getDatabase()) + ".zip");
                    LOG.info("creating database backup to file " + backupFile);
                    FileUtil.writeToZip(backupFile, hsqldbScript, FileUtil.getFileBytes(ENV.getConfigPath() + hsqldbScript, null));
                }
            }
        }));
    }

	public static void shutdownDatabaseDefault() {
		shutdownDatabase(Persistence.H2_DATABASE_URL);
	}
	public void shutdownDatabase() {
		shutdownDatabase(persistence.getConnectionUrl());
	}
	public static void shutdownDatabase(String url) {
		if (BeanContainer.isInitialized()) {
			LOG.info("preparing shutdown of local database " + url);
			try {
			    BeanContainer.instance().executeStmt(ENV.get("app.shutdown.statement", "SHUTDOWN"), true, null);
			    Thread.sleep(2000);
			} catch (Exception e) {
			    LOG.error(e.toString());
			}
		}
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

    public boolean isOpen() {
    	return getConnection(persistence, false) != null;
    }
    
    public Connection getConnection() {
    	return getConnection(persistence, true);
    }
    public static Connection getConnection(Persistence p, boolean throwException) {
        Connection con = null;
        try {
            Class.forName(p.getConnectionDriverClass());
            con = DriverManager.getConnection(p.getConnectionUrl(), p.getConnectionUserName(), p.getConnectionPassword());
        } catch (Exception e) {
        	if (throwException)
        		ManagedException.forward(e);
        	else
        		LOG.warn(e.toString());
        }
        return con;
    }
    
    public boolean checkJDBCConnection(boolean throwExceptionOnEmpty) {
        Connection con = null;
        try {
            con = getConnection();
            ResultSet tables = getTableNames(con);
            
            if (!tables.next()) {
                LOG.info("Available tables are:\n" + getTablesAsString(con.getMetaData().getTables(null, null, null, null)));
                if (throwExceptionOnEmpty)
                	throw new ManagedException("The desired jdbc connection provides no tables to work on!");
                return false;
            }
            return true;
        } catch (Exception e) {
            ManagedException.forward(e);
            return false;
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
     * see {@link #isInternalDatabase(String)}
     */
    public boolean isInternalDatabase() {
        return isInternalDatabase(persistence.getConnectionUrl());
    }

    /**
     * started for this application only
     * @param urlOrDriver
     * @return true, if it contains hsqldb or h2
     */
    public static boolean isInternalDatabase(String urlOrDriver) {
        return (urlOrDriver.contains("hsqldb")
                || urlOrDriver.contains("h2"));
    }

    public boolean isEmbeddedDatabase() {
    	return isInternalDatabase() && isEmbeddedDatabase(persistence.getConnectionUrl());
    }
    
    public static boolean isEmbeddedDatabase(String urlOrDriver) {
    	return isInternalDatabase(urlOrDriver) && !urlOrDriver.contains(":tcp:") && !urlOrDriver.matches(".*[:](hsql|http)[s]?[:].*");
    }
    
    public boolean isH2() {
    	return isH2(persistence.getConnectionUrl());
    }
    
    public static boolean isH2(String url) {
        return url.matches("jdbc[:]h2[:].*");
    }
    /**
     * url to an sql tool, if it is an embedded database.
     * @return optional SQL Tool like the one of H2 on port 8082
     */
    public String getSQLToolURL() {
        return isInternalDatabase() && isH2() 
        		? ENV.get("app.database.sqltool.url", "http://localhost:8082") : null;
    }

    public void replaceKeyWords() {
        if (isH2())
            H2DatabaseTool.replaceKeyWords(persistence);
    }
	public static Boolean isDBRunInternally() {
		return ENV.get("app.database.internal.server.run", false);
	}

	/* mostly H2 functions - generalized and without linking to dependencies */
	
	public void runDBServer() {
		runDBServer(ENV.getConfigPath(), persistence.getPort());
	}
	public static void runDBServerDefault() {
		if (getConnection(Persistence.current(), false) == null)
			runDBServer(ENV.getConfigPath(), Persistence.current().getPort());
	}
	/** calls h2 server directly though java...*/
	public static void runDBServer(String... args) {
		String cmd = ENV.get("app.database.internal.server.run.cmd", "org.h2.tools.Server.main(-baseDir, {0}, -tcp, -tcpPort, {1}, -trace, -ifNotExists)");
		LOG.info("running database internally: " + cmd + " <- [" + Arrays.toString(args) + "]");
		BeanClass.callEx(cmd, args);
	}
	
	public static void shutdownDBServerDefault() {
		new DatabaseTool(Persistence.current()).shutdownDBServer();
	}
	
	public void shutdownDBServer() {
		if (isEmbeddedDatabase() && !isH2())
			shutdownDatabase();
		else
			stopDBServer(persistence.getConnectionUrl(), persistence.getConnectionPassword());
	}
	
	/** calls h2 server directly though java...*/
	static void stopDBServer(String... args) {
		String cmd = ENV.get("app.database.internal.server.shutdown.cmd", "org.h2.tools.Server.shutdownTcpServer({0}, {1}, true, true)");
		LOG.info("shutdown database server: " + cmd + "[" + args[0] + ", ***]");
        try {
        	BeanClass.callEx(cmd, args);
        } catch (Exception e) {
            LOG.error(e.toString());
        }
	}
	
	public void dbDump() {
		dbDump(persistence);
	}
	
	public static void dbDump(Persistence p) {
		dbDump(p.getConnectionDriverClass(), p.getConnectionUrl(), p.getConnectionUserName(), p.getConnectionPassword());
	}
	
	/**
	 * calls h2 server directly though java...<p/>
	 * args: driver, url, user, password
	 */
	public static void dbDump(String... args) {
		FileUtil.writeBytes("SCRIPT TO 'db-dump.sql'".getBytes(), "backup.sql", false);
		FileUtil.writeBytes("SCRIPT TO 'db-dump.sql'".getBytes(), "../backup.sql", false); //for tests...
		String cmd = ENV.get("app.database.internal.server.dump.cmd", "org.h2.tools.RunScript.main(-driver, {0}, -url, {1}, -user, {2}, -password, {3}"); //, -script, dump.sql"); //, -showResults");
		LOG.info("dump database : " + cmd + "[" + args[0] + ", ***]");
        try {
        	BeanClass.callEx(cmd, args);
        } catch (Exception e) {
            LOG.error(e.toString());
        }
	}


	public boolean hasLocalDatabaseFile() {
		return hasLocalDatabaseFile(persistence);
	}


	public static boolean hasLocalDatabaseFile(Persistence persistence) {
		return new File(ENV.getConfigPath() + persistence.getDatabase() 
				+ (isH2(persistence.getConnectionUrl()) ? ".mv.db" : ".script")).exists();
	}
}
