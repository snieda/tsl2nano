package de.tsl2.nano.h5;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;

@net.jcip.annotations.NotThreadSafe
public class H2LuceneIntegrationTest implements ENVTestPreparation {
	Persistence persistence;
	DatabaseTool databaseTool;
    Connection con;
    
    @Before
    public void setUp() throws SQLException {
        final String BASE_DIR = ENVTestPreparation.super.setUp("h5");
        
        persistence = new Persistence();
//        persistence.setConnectionUrl("jdbc:h2:tcp://localhost:9099/PUBLIC");
        persistence.setConnectionUrl("jdbc:h2:./h2lucene-test");
        databaseTool = new DatabaseTool(persistence);
//        databaseTool.runDBServer();
        createTestDatabase();
//        BeanContainerUtil.initGenericServices(Thread.currentThread().getContextClassLoader());
    }
    
    @After
    public void tearDown() throws SQLException {
//        databaseTool.shutdownDatabase(); //don't shutdown the server -> that would  shutdown all instances of other tests
//      ENVTestPreparation.tearDown();
    }
    
    private void createTestDatabase() throws SQLException {
        String stmt = "CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR);" +
        "INSERT INTO TEST VALUES(1, 'Hello World');";
        con = databaseTool.getConnection();
        con.prepareStatement(stmt).execute();
    }

    @Test
//    @Ignore("there are collisions with other tests like NanoH5Test")
    public void testLuceneSearch() {
        H2LuceneIntegration h2Luc = new H2LuceneIntegration(persistence) {
            Integer executeStmt(String stmt) {
                try {
                    PreparedStatement prepareStatement = con.prepareStatement(stmt);
                    return prepareStatement.execute() ? 1 : 0;
                } catch (SQLException e) {
                    ManagedException.forward(e);
                }
                return 0;
            };
        };
        // will be called in initialize, but on an empty BeanContainer!
        h2Luc.executeStmt(ENV.get("app.lucene.init.stmt", H2LuceneIntegration.INIT_LUCENE)).intValue();
        assertEquals(1, h2Luc.activateOnTables());
    }

}
