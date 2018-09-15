package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;

public class H2LuceneIntegrationTest implements ENVTestPreparation {

    @BeforeClass
    public static void setUp() throws SQLException {
        final String BASE_DIR = ENVTestPreparation.setUp("h2lucene", false);
        Server.createTcpServer("-baseDir", BASE_DIR, "-tcpPort", "9092").start();
        createTestDatabase();
        BeanContainerUtil.initGenericServices(Thread.currentThread().getContextClassLoader());
    }
    
    private static void createTestDatabase() throws SQLException {
        String stmt = "CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR);" +
        "INSERT INTO TEST VALUES(1, 'Hello World');";
        /*assertTrue(*/new DatabaseTool(Persistence.current()).getConnection().prepareStatement(stmt).execute();//);
    }

    @AfterClass
    public static void tearDown() throws SQLException {
        Server.shutdownTcpServer(Persistence.current().getConnectionUrl(), "", true, true);
        ENVTestPreparation.tearDown();
    }
    
    @Test
    public void testLuceneSearch() {
        H2LuceneIntegration h2Luc = new H2LuceneIntegration(Persistence.current());
        assertEquals(1, h2Luc.activateOnTables());
    }

}
