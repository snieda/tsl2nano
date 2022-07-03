package de.tsl2.nano.persistence;

import org.jfree.util.Log;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.FileUtil;

public class H2DatabaseTool {
    public static final String DEFAULT_REPLACE_POSTFIX = "0"; //replacement postfix (e.g. ORDER -> ORDER0)
    static final String KEYWORDS = ENV.get("database.h2.keywords", "BEGIN|END|DAY|ALL|CHECK|CONSTRAINT|CROSS|CURRENT_DATE|CURRENT_TIME|CURRENT_TIMESTAMP|DISTINCT|EXCEPT|EXISTS|FALSE|FETCH|FOR|FOREIGN|FROM|FULL|GROUP|HAVING|INNER|INTERSECT|IS|JOIN|LIKE|LIMIT|MINUS|NATURAL|NOT|NULL|OFFSET|ON|ORDER|PRIMARY(?!\\s+KEY)|ROWNUM|SELECT|SYSDATE|SYSTIME|SYSTIMESTAMP|TODAY|TRUE|UNION|UNIQUE|WHERE|WITH");
    /**
     * as WORKAROUND on H2 database not creating a table if contains any KEYWORD. As the solution in ant-file mda.xml does not work, we do the workaround twice here!
     */
    public static void replaceKeyWords(Persistence persistence) {
        String sqlFileName = persistence.evalSqlFileName(persistence.getDatabase());
        String rsql = replaceKeyWords(loadSqlFile(sqlFileName));
        FileUtil.writeBytes(rsql.getBytes(), sqlFileName, false);
    }
    public static String replaceKeyWords(String ddl) {
        String RPL = ENV.get("database.h2.keyword.postfix", DEFAULT_REPLACE_POSTFIX);
        if (RPL.isEmpty()) {
            Log.debug("'database.h2.keyword.postfix' is empty - h2 keywords won't be respected!");
            return ddl;
        }
            
        String table = "(?im:(((CREATE|ALTER)(\\s+TABLE\\s+\"?))|([,(]\\s*\"?)))(?im:(" + KEYWORDS + "))(?=\\W)";
        ddl = ddl.replaceAll(table, "$3$4$5$6" + RPL);
        // without quotations
        String column = "((^|\\(|[,])(^\"\\s*))(?i:(" + KEYWORDS + "))(^\"[\\s]+)";
        ddl = ddl.replaceAll(column, "$2$3$4" + RPL + "$5");
        // with quotations only
        String quot_column = "([\"'´])(?i:(" + KEYWORDS + "))(['`\"])";
        return ddl.replaceAll(quot_column, "$1$2" + RPL + "$3");
    }
    static String loadSqlFile(String sqlFileName) {
        return String.valueOf(FileUtil.getFileData(sqlFileName, null));
    }
}
