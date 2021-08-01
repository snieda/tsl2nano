/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 03.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.incubation.specification.rules.ActionScript;
import de.tsl2.nano.persistence.DatabaseTool;
import de.tsl2.nano.persistence.Persistence;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@Ignore
@net.jcip.annotations.NotThreadSafe
public class NanoH5HtmlUnitTest extends NanoH5Unit {

    @Before
    public void setUp() {
        System.setProperty("app.server.running", "false");
        System.setProperty("app.database.internal.server.run", "true");
        System.setProperty("app.session.anticsrf", "false");
        System.setProperty("app.session.htmlheader", "");
        port = 8068;
        super.setUp();
    }
    
    @After
    public void tearDown() {
    	DatabaseTool.dbDump(Persistence.current());
    	super.tearDown();
    }

    @Override
    protected int dbPort() {
    	return 9093;
    }
    
    @Test
    public void testNano() throws Exception {
        Process process = null;
        HtmlPage page = null;
        PipedOutputStream myOut = SystemUtil.setPipedInput();
        try {
            //is javascript through classloader available
            assertTrue(ActionScript.createEngine(null) != null);
            
            System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "info");
            page = runWebClient();
            page = submit(page, BTN_LOGIN_OK);
            page = submit(page, BEANCOLLECTORLIST + BTN_SELECTALL);
            page = submit(page, BEANCOLLECTORLIST + BTN_OPEN);

            //-> all collectors now open - closing them in the testObjectCreation()
            int beanTypeCount = 1;//TODO: test it for all bean-types!
            for (int i = 0; i < beanTypeCount; i++) {
                //create and delete objects of all sample types
//                HtmlCheckBoxInput checkbox = page.getElementByName(String.valueOf(i));
//                page = submit(page, "beancollectorliste.open");
                page = crudBean(page);
            }

            assertTrue( new File(ENV.getConfigPath() + "doc/graph/anyway.sql.svg").exists());
        } finally {
            if (page != null) {
                int i = 0;
                String id;
                //TODO: does not work yet!!!
                while (i ++ < 30) {
                    id = page.getBody().getId();
                    if (!Util.isEmpty(id) && id.toLowerCase().equals(BEANCOLLECTORLIST)) {
                        page = submit(page, listBtn(BTN_ADMINISTRATION));
                        page = submit(page, BEANCOLLECTORLIST + BTN_SHUTDOWN);
                        break;
                    }
                    page = back(page);
                }
            }
            if (process != null) {
                System.out.println("trying to shutdown nanoh5 server through ENTER...");
                if (myOut != null)
                    myOut.write("\n\n".getBytes());
//                process.destroy();
            }
        }
    }

    private String listBtn(String btnName) {
        return BEANCOLLECTORLIST + btnName;
    }

}
