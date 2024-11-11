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
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.Util;
import de.tsl2.nano.specification.rules.ActionScript;
/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
@net.jcip.annotations.NotThreadSafe
public class NanoH5IT extends NanoH5Unit {

	@Override
	public String getTestEnv() {
		return "pre-integration-test/.nanoh5.environment";
	}

    @Override
    protected int dbPort() {
    	return 9092; //must be the same as in nanoh5 pom.xml on pre-integration-test start 
    }
    

    @Before
    public void setUp() {
    	Locale.setDefault(Locale.GERMANY);
//    	ENVTestPreparation.removeCaches();
        System.setProperty("app.server.running", "true");
        super.setUp();
    }
    
    @After
    public void tearDown() {
    	shutdownNanoHttpServer(new File("tsl2.nano.h5/target/temp/instance-id.txt"));
    	super.tearDown();
    }

    @Test
    public void testNano() throws Exception {
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
        }
    }

    private String listBtn(String btnName) {
        return BEANCOLLECTORLIST + btnName;
    }

}
