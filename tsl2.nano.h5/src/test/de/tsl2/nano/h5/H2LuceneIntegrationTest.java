package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;

public class H2LuceneIntegrationTest implements ENVTestPreparation {
    Connection con;
    
    @Before
    public void setUp() throws SQLException {
        final String BASE_DIR = ENVTestPreparation.setUp("h2lucene", false);
        Server.createTcpServer("-baseDir", new File(BASE_DIR).getAbsolutePath(), "-tcpPort", "9092").start();
        createTestDatabase();
//        BeanContainerUtil.initGenericServices(Thread.currentThread().getContextClassLoader());
    }
    
    private void createTestDatabase() throws SQLException {
        String stmt = "CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR);" +
        "INSERT INTO TEST VALUES(1, 'Hello World');";
        con = new DatabaseTool(Persistence.current()).getConnection();
        con.prepareStatement(stmt).execute();
    }

    @After
    public void tearDown() throws SQLException {
        Server.shutdownTcpServer(Persistence.current().getConnectionUrl(), "", true, true);
    }
    
    @AfterClass
    public static void tearDownClass() {
        ENVTestPreparation.tearDown();
    }
    
    @Test
    @Ignore("org.h2.jdbc.JdbcSQLException: Eingabe/Ausgabe: \"java.io.IOException: Die Syntax für den Dateinamen, Verzeichnisnamen oder die Datenträgerbezeichnung ist falsch\"; \"jdbc:h2:tcp://localhost:9092/PUBLIC/mem:management_db_9092.mv.db\"")
    public void testLuceneSearch() {
        H2LuceneIntegration h2Luc = new H2LuceneIntegration(Persistence.current()) {
            Integer executeStmt(String stmt) {
                try {
                    con.prepareStatement(stmt);
                } catch (SQLException e) {
                    ManagedException.forward(e);
                }
                return 1;
            };
        };
        assertEquals(1, h2Luc.activateOnTables());
    }

}
