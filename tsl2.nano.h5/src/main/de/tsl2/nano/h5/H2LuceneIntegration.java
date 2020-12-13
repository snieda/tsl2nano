package de.tsl2.nano.h5;

import java.util.Collection;

import de.tsl2.nano.bean.BeanContainer;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;

/**
 * initializes Lucene on a H2 Database. see http://www.h2database.com/html/tutorial.html
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class H2LuceneIntegration {
    private Persistence persistence;

    static final String INIT_LUCENE = 
    "CREATE ALIAS IF NOT EXISTS FTL_INIT FOR \"org.h2.fulltext.FullTextLucene.init\";" +
    "CALL FTL_INIT();";
    
    //TODO: check, if unsafe for sql-injection...
    private static final String ACITVATE_ON_TABLE = "CALL FTL_CREATE_INDEX('${scheme}', '${table}', NULL)";

    public static final String FTL_SEARCH = "FTL_SEARCH";

    private static final String DROP_INDEX = "CALL FTL_DROP_INDEX('${scheme}', '${table}');";

    private String string;
    
    public H2LuceneIntegration(Persistence persistence) {
        this.persistence = persistence;
    }
    
    protected void initialize() {
        String init = ENV.get("app.lucene.init.stmt", INIT_LUCENE);
        executeStmt(init);
    }
    
    public int activateOnTables(String...tables) {
        return activateOnTables(persistence.getDefaultSchema(), tables);
    }
    /**
     * @param tables (optional) tables to activate the fulltextsearch on. if null or empty, all schema tables will be activated!
     */
    protected int activateOnTables(String scheme, String...tables) {
        return doOnTables(ENV.get("app.lucene.activate.on.table.stmt", ACITVATE_ON_TABLE), 
            persistence.getDefaultSchema(),
            tables
            );
    }
    protected int doOnTables(String stmt, String scheme, String...tables) {
        initialize();
        stmt = StringUtil.insertProperties(stmt, MapUtil.asMap("scheme", scheme));
        if (Util.isEmpty(tables)) {
            DatabaseTool dbTool = new DatabaseTool(persistence);
            tables = dbTool.getTableNames();
        }
        
        String actTableStmt;
        int i = 0;
        for (String t : tables) {
        	if (t == null)
        		continue;
            actTableStmt = StringUtil.insertProperties(stmt, MapUtil.asMap("table", t));
            i += executeStmt(actTableStmt);
        }
        return i;
    }

    Integer executeStmt(String stmt) {
        return BeanContainer.instance().executeStmt(stmt, true, null);
    }
    
    public int dropIndex(String scheme, String... tables) {
        return doOnTables(ENV.get("app.lucene.dropindex.on.table.stmt", DROP_INDEX), 
            persistence.getDefaultSchema(),
            tables
            );
    }
    public static Collection<Object> doSearch(String search) {
        return BeanContainer.instance().getBeansByQuery(createSearchQuery(search), true, (Object[])null);
    }

    public static String createSearchQuery(String search) {
        int start = 0;
        int end = 0;
        return "select * from " + H2LuceneIntegration.FTL_SEARCH + "(" + search + "," + start + "," + end + ")";
    }
}
