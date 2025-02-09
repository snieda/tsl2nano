package de.tsl2.nano.persistence;

import static de.tsl2.nano.persistence.H2DatabaseTool.DEFAULT_REPLACE_POSTFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

public class H2DatabaseToolTest {
    @Test
//    @Ignore("test failing on column 'begin'")
    public void testReplaceKeyWords() {
        //TODO: without space before 'begin' it does not work!
        String sql = " CREATE TABLE orDer( begin Date unique not null, end Date); CREATE TABLE MYTABLE(myorder Number(5), orderme Number(5));";
        String exp = " CREATE TABLE orDer0( begin0 Date unique not null, end0 Date); CREATE TABLE MYTABLE(myorder Number(5), orderme Number(5));";
        String result = H2DatabaseTool.replaceKeyWords(sql);
        System.out.println(result);
        assertEquals(exp, result);
    }

    @Test
    public void testReplaceKeyWordsInFile() {
        String ddl = "ddlcopy-test";
        String targetDDL = "target/" + ddl;
        FileUtil.copy("src/test/resources/" + ddl + ".sql", targetDDL + ".sql");
        ddl = targetDDL;
        
        Persistence p = new Persistence();
        p.setDatabase(ddl);
        H2DatabaseTool.replaceKeyWords(p);
        String result = FileUtil.getFileString(targetDDL + ".sql");
        assertEquals(9, StringUtil.countFindings(result, "ORDER" + DEFAULT_REPLACE_POSTFIX));
        
        String[] keywords = H2DatabaseTool.KEYWORDS.split("\\|");
        for (int i = 0; i < keywords.length; i++) {
            if (keywords[i].equals("ORDER") /*|| keywords[i].equals("CONSTRAINT")*/)
                continue;
            assertFalse("wrong replacement: " + keywords[i] + DEFAULT_REPLACE_POSTFIX, result.contains(keywords[i] + DEFAULT_REPLACE_POSTFIX));
        }
    }
}
